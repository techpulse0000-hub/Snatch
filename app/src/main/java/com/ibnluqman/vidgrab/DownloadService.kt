package com.ibnluqman.vidgrab

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.ffmpeg.FFmpeg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class DownloadService : Service() {

    companion object {
        const val ACTION_ENQUEUE = "com.ibnluqman.vidgrab.action.ENQUEUE"
        const val ACTION_PAUSE = "com.ibnluqman.vidgrab.action.PAUSE"
        const val ACTION_RESUME = "com.ibnluqman.vidgrab.action.RESUME"
        const val ACTION_DELETE = "com.ibnluqman.vidgrab.action.DELETE"
        const val EXTRA_RECORD_ID = "extra_record_id"

        private const val CHANNEL_ID_PROGRESS = "vidgrab_progress"
        private const val CHANNEL_ID_RESULT = "vidgrab_result"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var initialized = false
    private lateinit var dao: DownloadDao

    private val queueMutex = Mutex()
    private val claimMutex = Mutex()
    private var workerRunning = false

    private val activeRecordIds = ConcurrentHashMap.newKeySet<Long>()
    private val pausedIds = ConcurrentHashMap.newKeySet<Long>()
    private val cancelledIds = ConcurrentHashMap.newKeySet<Long>()
    private val activeNotifIdByRecord = ConcurrentHashMap<Long, Int>()

    private val noVideoErrorPatterns = listOf(
        "There is no video in this post",
        "No video formats found",
        "requested format not available"
    )

    override fun onCreate() {
        super.onCreate()
        dao = AppDatabase.get(applicationContext).downloadDao()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> {
                val id = intent.getLongExtra(EXTRA_RECORD_ID, -1)
                if (id != -1L) scope.launch { pauseDownload(id) }
                return START_NOT_STICKY
            }
            ACTION_RESUME -> {
                val id = intent.getLongExtra(EXTRA_RECORD_ID, -1)
                if (id != -1L) scope.launch { resumeDownload(id) }
                return START_NOT_STICKY
            }
            ACTION_DELETE -> {
                val id = intent.getLongExtra(EXTRA_RECORD_ID, -1)
                if (id != -1L) scope.launch { deleteDownload(id) }
                return START_NOT_STICKY
            }
            else -> { }
        }

        scope.launch {
            var shouldStartWorker = false
            queueMutex.withLock {
                if (!workerRunning) {
                    workerRunning = true
                    shouldStartWorker = true
                }
            }
            if (shouldStartWorker) {
                startForeground(1001, buildProgressNotification(getString(R.string.notif_downloading), ongoing = true))
                val concurrency = maxConcurrentDownloads()
                val jobs = (0 until concurrency).map { slot -> scope.launch { workerLoop(1001 + slot) } }
                jobs.joinAll()
                queueMutex.withLock { workerRunning = false }
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun maxConcurrentDownloads(): Int {
        return try {
            val cm = getSystemService(ConnectivityManager::class.java)
            val network = cm.activeNetwork ?: return 1
            val caps = cm.getNetworkCapabilities(network) ?: return 1
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) 2 else 1
        } catch (e: Exception) {
            1
        }
    }

    private suspend fun claimNextQueued(): DownloadRecord? = claimMutex.withLock {
        val next = dao.nextQueued() ?: return@withLock null
        next.status = DownloadRecord.Status.DOWNLOADING.name
        dao.update(next)
        next
    }

    private suspend fun workerLoop(notifId: Int) {
        while (true) {
            val next = claimNextQueued() ?: break
            processOne(next, notifId)
        }
    }

    private suspend fun pauseDownload(id: Long) {
        val record = dao.getById(id) ?: return
        if (record.status == DownloadRecord.Status.DOWNLOADING.name && activeRecordIds.contains(id)) {
            pausedIds.add(id)
            if (!record.isDirectMediaUrl) {
                YoutubeDL.getInstance().destroyProcessById(id.toString())
            }
        } else if (record.status == DownloadRecord.Status.QUEUED.name) {
            record.status = DownloadRecord.Status.PAUSED.name
            dao.update(record)
        }
    }

    private suspend fun resumeDownload(id: Long) {
        val record = dao.getById(id) ?: return
        if (record.status == DownloadRecord.Status.PAUSED.name || record.status == DownloadRecord.Status.FAILED.name) {
            record.status = DownloadRecord.Status.QUEUED.name
            record.errorMessage = null
            dao.update(record)
        }
        val intent = Intent(this, DownloadService::class.java).apply { action = ACTION_ENQUEUE }
        androidx.core.content.ContextCompat.startForegroundService(this, intent)
    }

    private suspend fun deleteDownload(id: Long) {
        if (activeRecordIds.contains(id)) {
            cancelledIds.add(id)
            try {
                YoutubeDL.getInstance().destroyProcessById(id.toString())
            } catch (e: Exception) {
                android.util.Log.w("Snatch", "Could not kill process for deleted download $id", e)
            }
        }
        pausedIds.remove(id)
        val record = dao.getById(id)
        if (record?.carouselGroupId == null) {
            activeNotifIdByRecord[id]?.let { getSystemService(NotificationManager::class.java).cancel(it) }
        }
        record?.let { dao.delete(it) }
    }

    private fun fetchHtml(url: String): String {
        return try {
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
            )
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            text
        } catch (e: Exception) {
            android.util.Log.w("Snatch", "Instagram HTML fetch failed", e)
            ""
        }
    }

    /**
     * Fetches an Instagram post page directly and pulls out each media item's real URL and
     * type (photo/video) from the page's own embedded data — bypassing yt-dlp's Instagram
     * extractor entirely, since that's been the source of most of the misclassification issues.
     * Works for single posts and carousels alike. Returns empty if scraping fails (e.g. private
     * post, or Instagram changed their page structure) so callers can fall back to yt-dlp.
     */
    private fun fetchInstagramCarouselMedia(postUrl: String): List<Pair<String, String>> {
        val html = fetchHtml(postUrl)
        if (html.isEmpty()) return emptyList()

        fun unescape(s: String) = s.replace("\\u0026", "&").replace("\\/", "/")

        val displayUrlRegex = Regex("\"display_url\":\"(https:[^\"]+)\"")
        val videoUrlRegex = Regex("\"video_url\":\"(https:[^\"]+)\"")

        val displayMatches = displayUrlRegex.findAll(html).toList()
        val videoMatches = videoUrlRegex.findAll(html).toList()
        if (displayMatches.isEmpty()) return emptyList()

        val results = mutableListOf<Pair<String, String>>()
        for (dm in displayMatches) {
            // A video item's video_url is serialized close to its own display_url in the
            // embedded data; a photo item has no nearby video_url at all.
            val nearbyVideo = videoMatches.firstOrNull { it.range.first in dm.range.last..(dm.range.last + 3000) }
            if (nearbyVideo != null) {
                results.add(unescape(nearbyVideo.groupValues[1]) to "VIDEO")
            } else {
                results.add(unescape(dm.groupValues[1]) to "PHOTO")
            }
        }
        return results.distinctBy { it.first }
    }

    /** Simple direct HTTP download for a raw media URL — used for Instagram items fetched above, no yt-dlp involved. */
    private suspend fun downloadDirectMediaUrl(record: DownloadRecord, notifId: Int): String = withContext(Dispatchers.IO) {
        val outDir = File(filesDir, "downloads/${record.id}").apply { mkdirs() }
        val freeBytes = outDir.freeSpace
        if (freeBytes < 500L * 1024 * 1024) {
            throw IllegalStateException("Not enough free storage — only ${freeBytes / (1024 * 1024)}MB available.")
        }

        val extension = if (record.mediaType == "PHOTO") "jpg" else "mp4"
        val outFile = File(outDir, "snatch_${record.id}.$extension")

        val connection = java.net.URL(record.url).openConnection() as java.net.HttpURLConnection
        connection.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
        )
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.connect()

        val totalBytes = connection.contentLengthLong
        if (totalBytes > 0) {
            dao.getById(record.id)?.let { it.totalBytes = totalBytes; dao.update(it) }
        }

        var downloaded = 0L
        var lastUpdateTime = System.currentTimeMillis()
        var lastBytesAtUpdate = 0L

        connection.inputStream.use { input ->
            java.io.FileOutputStream(outFile).use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    if (dao.getById(record.id) == null || cancelledIds.contains(record.id)) {
                        throw java.util.concurrent.CancellationException("Download was removed")
                    }
                    if (pausedIds.contains(record.id)) {
                        throw java.io.IOException("Paused")
                    }
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    downloaded += read

                    val now = System.currentTimeMillis()
                    if (now - lastUpdateTime >= 1000) {
                        val deltaSeconds = (now - lastUpdateTime) / 1000f
                        val bps = ((downloaded - lastBytesAtUpdate) / deltaSeconds).toLong()
                        val speedText = formatSpeed(bps)
                        val percent = if (totalBytes > 0) ((downloaded * 100) / totalBytes).toInt() else 0
                        val sizeText = if (totalBytes > 0) "${formatBytes(downloaded)} / ${formatBytes(totalBytes)}" else "~${formatBytes(downloaded)}"
                        val liveRecord = dao.getById(record.id)
                        val label = liveRecord?.let { labelFor(it) } ?: record.title
                        postProgress(record.id, notifId, percent, sizeText, speedText, "…", label)
                        if (liveRecord != null) {
                            liveRecord.progress = percent
                            liveRecord.speedText = speedText
                            liveRecord.downloadedBytes = downloaded
                            dao.update(liveRecord)
                        }
                        lastUpdateTime = now
                        lastBytesAtUpdate = downloaded
                    }
                }
            }
        }
        connection.disconnect()
        outFile.absolutePath
    }

    /** Lightweight metadata-only probe (no download) — checks whether a link is a single item or a carousel/playlist. */
    private suspend fun probeForCarouselItems(url: String): List<String> = withContext(Dispatchers.IO) {
        val request = YoutubeDLRequest(url).apply {
            addOption("--flat-playlist")
            addOption("--dump-json")
            addOption("--skip-download")
            addOption("--socket-timeout", "20")
        }
        val response = YoutubeDL.getInstance().execute(request)
        val lines = response.out.lines().filter { it.trim().startsWith("{") }
        if (lines.size <= 1) return@withContext listOf(url)
        val urls = lines.mapNotNull { line ->
            try {
                val obj = org.json.JSONObject(line)
                val u = obj.optString("url", "")
                if (u.isNotEmpty()) u else obj.optString("webpage_url", "").ifEmpty { null }
            } catch (e: Exception) {
                null
            }
        }
        if (urls.isEmpty()) listOf(url) else urls
    }

    private fun notifIdForCarousel(groupId: Long): Int = (5000 + (groupId % 100000)).toInt()

    private fun effectiveNotifId(record: DownloadRecord, workerNotifId: Int): Int =
        record.carouselGroupId?.let { notifIdForCarousel(it) } ?: workerNotifId

    private fun labelFor(record: DownloadRecord): String =
        if (record.carouselIndex != null && record.carouselTotal != null)
            "(item ${record.carouselIndex} of ${record.carouselTotal}) ${record.title}"
        else record.title

    private suspend fun processOne(record: DownloadRecord, workerNotifId: Int) {
        activeRecordIds.add(record.id)

        if (record.playlistIndex == null && !record.isDirectMediaUrl) {
            activeNotifIdByRecord[record.id] = workerNotifId
            postStatus(record.id, workerNotifId, "Checking link…")

            if (record.url.contains("instagram.com", ignoreCase = true)) {
                val igItems = try {
                    fetchInstagramCarouselMedia(record.url)
                } catch (e: Exception) {
                    android.util.Log.w("Snatch", "Instagram direct fetch failed, falling back to yt-dlp", e)
                    emptyList()
                }
                if (igItems.isNotEmpty()) {
                    dao.delete(record)
                    val groupId = if (igItems.size > 1) System.currentTimeMillis() else null
                    igItems.forEachIndexed { index, pair ->
                        val (mediaUrl, type) = pair
                        dao.insert(
                            DownloadRecord(
                                url = mediaUrl,
                                forcedQuality = record.forcedQuality,
                                mediaType = if (type == "PHOTO") "PHOTO" else record.mediaType,
                                isDirectMediaUrl = true,
                                carouselGroupId = groupId,
                                carouselIndex = if (groupId != null) index + 1 else null,
                                carouselTotal = if (groupId != null) igItems.size else null,
                                title = if (igItems.size > 1) "Fetching…" else record.title,
                                status = DownloadRecord.Status.QUEUED.name
                            )
                        )
                    }
                    activeRecordIds.remove(record.id)
                    activeNotifIdByRecord.remove(record.id)
                    getSystemService(NotificationManager::class.java).cancel(workerNotifId)
                    return
                }
            }

            val itemUrls = try {
                probeForCarouselItems(record.url)
            } catch (e: Exception) {
                android.util.Log.w("Snatch", "Carousel probe failed, treating as single item", e)
                listOf(record.url)
            }

            if (itemUrls.size > 1) {
                dao.delete(record)
                val groupId = System.currentTimeMillis()
                itemUrls.forEachIndexed { index, itemUrl ->
                    dao.insert(
                        DownloadRecord(
                            url = itemUrl,
                            forcedQuality = record.forcedQuality,
                            mediaType = record.mediaType,
                            playlistIndex = index + 1,
                            carouselGroupId = groupId,
                            carouselIndex = index + 1,
                            carouselTotal = itemUrls.size,
                            title = "Fetching…",
                            status = DownloadRecord.Status.QUEUED.name
                        )
                    )
                }
                activeRecordIds.remove(record.id)
                activeNotifIdByRecord.remove(record.id)
                getSystemService(NotificationManager::class.java).cancel(workerNotifId)
                return
            }
        }

        val notifId = effectiveNotifId(record, workerNotifId)
        activeNotifIdByRecord[record.id] = notifId
        postStatus(record.id, notifId, "Downloading: ${labelFor(record)}")

        try {
            val info = YoutubeDL.getInstance().getInfo(record.url)
            val infoRecord = dao.getById(record.id) ?: record
            infoRecord.title = info.title ?: infoRecord.title
            infoRecord.totalBytes = estimateTotalBytes(info)

            // A carousel item can independently be a photo or a video regardless of which
            // share button was tapped — detect the real type two ways: by file extension,
            // and (more reliably) by whether it has any actual video-capable stream at all.
            val imageExtensions = setOf("jpg", "jpeg", "png", "webp")
            val hasVideoStream = info.formats?.any { !it.vcodec.isNullOrEmpty() && it.vcodec != "none" } ?: false
            if ((info.ext ?: "").lowercase() in imageExtensions || !hasVideoStream) {
                infoRecord.mediaType = "PHOTO"
            }

            dao.update(infoRecord)
        } catch (e: Exception) {
            android.util.Log.w("Snatch", "Could not fetch title/size up front", e)
        }

        try {
            ensureInit()
            val savedPath = runDownloadWithRetry(record, notifId)
            generateLocalThumbnail(record.id, savedPath)
            val result = dao.getById(record.id) ?: record
            result.filePath = savedPath
            val actualSize = File(savedPath).length()
            result.downloadedBytes = actualSize
            result.totalBytes = actualSize
            result.status = DownloadRecord.Status.COMPLETED.name
            result.progress = 100
            if (result.title == "Fetching…") {
                result.title = File(savedPath).nameWithoutExtension.substringBeforeLast(" [")
            }
            dao.update(result)
            exposeToMediaStore(savedPath, result)
            postResult(result.id, notifId, "${getString(R.string.notif_done)}: ${labelFor(result)}", success = true)
        } catch (e: Exception) {
            android.util.Log.e("Snatch", "Download failed", e)
            if (cancelledIds.remove(record.id)) {
            } else {
                val result = dao.getById(record.id)
                if (result != null) {
                    if (pausedIds.remove(record.id)) {
                        result.status = DownloadRecord.Status.PAUSED.name
                    } else {
                        result.status = DownloadRecord.Status.FAILED.name
                        result.errorMessage = e.message
                        postResult(result.id, notifId, "${getString(R.string.notif_failed)}: ${labelFor(result)} — ${e.message}", success = false)
                    }
                    dao.update(result)
                }
            }
        }

        activeRecordIds.remove(record.id)
        activeNotifIdByRecord.remove(record.id)
        delay(500)
    }

    private suspend fun ensureInit() = withContext(Dispatchers.IO) {
        if (!initialized) {
            YoutubeDL.getInstance().init(applicationContext)
            FFmpeg.getInstance().init(applicationContext)
            val prefs = Settings(applicationContext)
            val oneDayMs = 24 * 60 * 60 * 1000L
            val now = System.currentTimeMillis()
            if (now - prefs.lastUpdateCheckMillis > oneDayMs) {
                try {
                    YoutubeDL.getInstance().updateYoutubeDL(applicationContext, YoutubeDL.UpdateChannel.STABLE)
                    prefs.lastUpdateCheckMillis = now
                } catch (e: Exception) {
                    android.util.Log.w("Snatch", "yt-dlp update failed, continuing with bundled version", e)
                }
            }
            initialized = true
        }
    }

    private fun buildCascadingFormatSelector(minQuality: Settings.Quality): String {
        val heights = listOf(480, 720, 1080)
        val startIndex = when (minQuality) {
            Settings.Quality.P480 -> 0
            Settings.Quality.P720 -> 1
            Settings.Quality.P1080 -> 2
            else -> heights.size
        }
        val parts = mutableListOf<String>()
        for (i in startIndex until heights.size) {
            val h = heights[i]
            parts += "bestvideo[height<=$h][vcodec^=avc1]+bestaudio[acodec^=mp4a]/bestvideo[height<=$h]+bestaudio/best[height<=$h]"
        }
        parts += "bestvideo[vcodec^=avc1]+bestaudio[acodec^=mp4a]/bestvideo+bestaudio/best"
        return parts.joinToString("/")
    }

    private fun sizeOf(format: com.yausername.youtubedl_android.mapper.VideoFormat): Long {
        return if (format.fileSize > 0) format.fileSize else format.fileSizeApproximate
    }

    private fun estimateTotalBytes(info: com.yausername.youtubedl_android.mapper.VideoInfo): Long {
        val formats = info.formats ?: return 0L
        val videoCandidates = formats.filter { !it.vcodec.isNullOrEmpty() && it.vcodec != "none" }
        val audioCandidates = formats.filter { !it.acodec.isNullOrEmpty() && it.acodec != "none" && (it.vcodec.isNullOrEmpty() || it.vcodec == "none") }
        val bestVideo = videoCandidates.maxByOrNull { it.height }
        val bestAudio = audioCandidates.maxByOrNull { sizeOf(it) }
        val videoSize = bestVideo?.let { sizeOf(it) } ?: 0L
        val audioSize = bestAudio?.let { sizeOf(it) } ?: 0L
        return videoSize + audioSize
    }

    private val networkErrorPatterns = listOf(
        "No address associated with hostname", "Temporary failure in name resolution",
        "Network is unreachable", "Connection reset", "Connection refused",
        "Failed to establish a new connection", "timed out", "unreachable",
        "ENETUNREACH", "ETIMEDOUT", "Errno 7", "Errno 110", "Errno 113",
        "HTTP Error 429", "Too Many Requests"
    )

    private fun isNetworkRelatedError(e: Throwable): Boolean {
        val message = (e.message ?: "") + (e.cause?.message ?: "")
        return networkErrorPatterns.any { message.contains(it, ignoreCase = true) }
    }

    private fun isNoVideoError(e: Throwable): Boolean {
        val message = (e.message ?: "") + (e.cause?.message ?: "")
        return noVideoErrorPatterns.any { message.contains(it, ignoreCase = true) }
    }

    private suspend fun runDownloadWithRetry(record: DownloadRecord, notifId: Int): String {
        val maxAttempts = 8
        var attempt = 0
        var delayMs = 5000L
        var reclassifiedAsPhoto = false
        while (true) {
            attempt++
            if (dao.getById(record.id) == null || cancelledIds.contains(record.id)) {
                throw java.util.concurrent.CancellationException("Download was removed")
            }
            try {
                return if (record.isDirectMediaUrl) downloadDirectMediaUrl(record, notifId) else runDownload(record, notifId)
            } catch (e: Exception) {
                if (cancelledIds.contains(record.id) || pausedIds.contains(record.id)) throw e

                // Safety net: if the format-based attempt fails because there's genuinely no
                // video here, reclassify as a photo and retry immediately — no delay, doesn't
                // count against the normal retry budget.
                if (!reclassifiedAsPhoto && record.mediaType != "PHOTO" && isNoVideoError(e)) {
                    reclassifiedAsPhoto = true
                    record.mediaType = "PHOTO"
                    dao.getById(record.id)?.let {
                        it.mediaType = "PHOTO"
                        dao.update(it)
                    }
                    postStatus(record.id, notifId, "Re-detected as a photo, retrying…")
                    continue
                }

                if (!isNetworkRelatedError(e) || attempt >= maxAttempts) throw e
                android.util.Log.w("Snatch", "Network hiccup (attempt $attempt/$maxAttempts), retrying in ${delayMs / 1000}s", e)
                postStatus(record.id, notifId, "Connection lost — retrying (${attempt}/${maxAttempts})…")
                delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(60_000L)
            }
        }
    }

    private suspend fun runDownload(record: DownloadRecord, notifId: Int): String = withContext(Dispatchers.IO) {
        val prefs = Settings(applicationContext)
        val outDir = File(filesDir, "downloads/${record.id}").apply { mkdirs() }

        val freeBytes = outDir.freeSpace
        val minRequiredBytes = 3L * 1024 * 1024 * 1024
        if (freeBytes < minRequiredBytes) {
            throw IllegalStateException("Not enough free storage — only ${freeBytes / (1024 * 1024)}MB available. Free up space and try again.")
        }

        val outputTemplate = File(outDir, "%(title).80s [%(id)s].%(ext)s").absolutePath

        val request = YoutubeDLRequest(record.url).apply {
            addOption("-o", outputTemplate)
            when (record.mediaType) {
                "AUDIO" -> {
                    addOption("-x")
                    addOption("--audio-format", "mp3")
                }
                "PHOTO" -> {
                    addOption("-f", "best[ext=jpg]/best[ext=png]/best[ext=webp]/best")
                }
                else -> {
                    if (record.forcedQuality != null) {
                        addOption("-f", buildCascadingFormatSelector(Settings.Quality.valueOf(record.forcedQuality)))
                    } else {
                        when (prefs.quality) {
                            Settings.Quality.AUDIO_ONLY -> {
                                addOption("-x")
                                addOption("--audio-format", "mp3")
                            }
                            Settings.Quality.P720 -> addOption("-f", buildCascadingFormatSelector(Settings.Quality.P720))
                            Settings.Quality.P480 -> addOption("-f", buildCascadingFormatSelector(Settings.Quality.P480))
                            Settings.Quality.P1080 -> addOption("-f", buildCascadingFormatSelector(Settings.Quality.P1080))
                            Settings.Quality.BEST -> addOption("-f", "bestvideo[vcodec^=avc1]+bestaudio[acodec^=mp4a]/bestvideo+bestaudio/best")
                        }
                    }
                    addOption("--merge-output-format", "mp4")
                    addOption("--postprocessor-args", "ffmpeg:-movflags +faststart")
                }
            }
            addOption("--retries", "50")
            addOption("--fragment-retries", "50")
            addOption("--retry-sleep", "linear=2:30:2")
            addOption("--socket-timeout", "30")
            addOption("-N", "4")
            addOption("-4")
            if (record.playlistIndex != null) {
                addOption("--playlist-items", record.playlistIndex.toString())
            } else {
                addOption("--no-playlist")
            }
            addOption("--sleep-requests", "1")
        }

        val filePath: String = coroutineScope {
            var localSpeedText = "…"
            var localEtaText = "…"
            var localProgressPercent = 0

            val speedJob = launch {
                var lastBytes = 0L
                var lastTime = System.currentTimeMillis()
                while (isActive) {
                    delay(1000)

                    if (dao.getById(record.id) == null || cancelledIds.contains(record.id)) {
                        try { YoutubeDL.getInstance().destroyProcessById(record.id.toString()) } catch (e: Exception) {}
                        return@launch
                    }

                    val currentBytes = outDir.listFiles()?.sumOf { it.length() } ?: 0L
                    val now = System.currentTimeMillis()
                    val deltaSeconds = (now - lastTime) / 1000f
                    val bps = if (deltaSeconds > 0) ((currentBytes - lastBytes).coerceAtLeast(0) / deltaSeconds).toLong() else 0L
                    localSpeedText = formatSpeed(bps)
                    lastBytes = currentBytes
                    lastTime = now

                    val knownTotal = dao.getById(record.id)?.totalBytes ?: 0L
                    val effectiveTotal = if (knownTotal > 0) knownTotal
                    else if (localProgressPercent >= 3) (currentBytes / (localProgressPercent / 100.0)).toLong()
                    else 0L
                    val sizeText = when {
                        knownTotal > 0 -> "${formatBytes(currentBytes)} / ${formatBytes(knownTotal)}"
                        effectiveTotal > 0 -> "~${formatBytes(currentBytes)} / ~${formatBytes(effectiveTotal)}"
                        else -> "~${formatBytes(currentBytes)}"
                    }

                    val liveRecord = dao.getById(record.id)
                    val label = liveRecord?.let { labelFor(it) } ?: record.title
                    postProgress(record.id, notifId, localProgressPercent, sizeText, localSpeedText, localEtaText, label)

                    if (liveRecord != null) {
                        liveRecord.progress = localProgressPercent
                        liveRecord.speedText = localSpeedText
                        liveRecord.etaText = localEtaText
                        liveRecord.downloadedBytes = currentBytes
                        if (knownTotal <= 0 && effectiveTotal > 0) liveRecord.totalBytes = effectiveTotal
                        dao.update(liveRecord)
                    }
                }
            }

            try {
                val response = YoutubeDL.getInstance().execute(request, record.id.toString()) { progress, etaSeconds, _ ->
                    localProgressPercent = progress.toInt().coerceIn(0, 100)
                    localEtaText = formatEta(etaSeconds)
                }

                val ignoredExtensions = setOf("part", "ytdl", "temp", "json", "description")
                val produced = outDir.listFiles()
                    ?.filter { it.extension.lowercase() !in ignoredExtensions }
                    ?.maxByOrNull { it.lastModified() }
                    ?: throw IllegalStateException("No file produced. yt-dlp output: ${response.out}")

                produced.absolutePath
            } finally {
                speedJob.cancel()
            }
        }
        filePath
    }

    private suspend fun exposeToMediaStore(path: String, record: DownloadRecord) {
        val file = File(path)
        val imageExtensions = setOf("jpg", "jpeg", "png", "webp")
        val audioExtensions = setOf("mp3", "m4a", "aac", "opus", "ogg", "wav")
        val isImage = file.extension.lowercase() in imageExtensions
        val isAudio = !isImage && file.extension.lowercase() in audioExtensions
        val mimeType = android.webkit.MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(file.extension.lowercase())
            ?: when {
                isImage -> "image/jpeg"
                isAudio -> "audio/mpeg"
                else -> "video/mp4"
            }

        val treeUriString = Settings(applicationContext).storageTreeUri

        val finalUri: android.net.Uri = if (treeUriString != null) {
            val treeUri = android.net.Uri.parse(treeUriString)
            val rootDir = androidx.documentfile.provider.DocumentFile.fromTreeUri(applicationContext, treeUri)
                ?: throw IllegalStateException("Chosen folder is no longer accessible")
            val dir = rootDir.findFile("Snatch") ?: rootDir.createDirectory("Snatch")
            ?: throw IllegalStateException("Could not create Snatch folder")
            val newFile = dir.createFile(mimeType, file.name)
                ?: throw IllegalStateException("Could not create file in chosen folder")
            contentResolver.openOutputStream(newFile.uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            }
            tryTriggerMediaScan(applicationContext, newFile.uri)
            newFile.uri
        } else {
            val collection = when {
                isImage -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                isAudio -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, when {
                    isImage -> "Pictures/Snatch"
                    isAudio -> "Music/Snatch"
                    else -> "Movies/Snatch"
                })
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(collection, values) ?: throw IllegalStateException("MediaStore insert failed")
            contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
            if (!isImage && Build.VERSION.SDK_INT >= 29) {
                try {
                    contentResolver.loadThumbnail(uri, android.util.Size(320, 240), null)
                } catch (e: Exception) {
                    android.util.Log.w("Snatch", "Thumbnail pre-generation failed", e)
                }
            }
            uri
        }

        val r = dao.getById(record.id)
        if (r != null) {
            r.filePath = finalUri.toString()
            dao.update(r)
        }

        file.delete()
        file.parentFile?.delete()
    }

    private fun tryTriggerMediaScan(context: android.content.Context, documentUri: android.net.Uri) {
        try {
            val docId = android.provider.DocumentsContract.getDocumentId(documentUri)
            if (docId.startsWith("primary:")) {
                val relativePath = docId.removePrefix("primary:")
                val realPath = "${android.os.Environment.getExternalStorageDirectory()}/$relativePath"
                android.media.MediaScannerConnection.scanFile(context, arrayOf(realPath), null, null)
            }
        } catch (e: Exception) {
            android.util.Log.w("Snatch", "Could not trigger immediate media scan (it'll still appear on the next automatic scan)", e)
        }
    }

    private suspend fun generateLocalThumbnail(recordId: Long, videoPath: String) = withContext(Dispatchers.IO) {
        try {
            val imageExtensions = setOf("jpg", "jpeg", "png", "webp")
            if (File(videoPath).extension.lowercase() in imageExtensions) {
                val thumbDir = File(cacheDir, "thumbnails").apply { mkdirs() }
                val thumbFile = File(thumbDir, "$recordId.jpg")
                File(videoPath).copyTo(thumbFile, overwrite = true)
                val r = dao.getById(recordId) ?: return@withContext
                r.thumbnailUrl = thumbFile.absolutePath
                dao.update(r)
                return@withContext
            }

            val bitmap: android.graphics.Bitmap? = if (Build.VERSION.SDK_INT >= 29) {
                android.media.ThumbnailUtils.createVideoThumbnail(File(videoPath), android.util.Size(320, 240), null)
            } else {
                @Suppress("DEPRECATION")
                android.media.ThumbnailUtils.createVideoThumbnail(videoPath, android.provider.MediaStore.Video.Thumbnails.MINI_KIND)
            }
            if (bitmap != null) {
                val thumbDir = File(cacheDir, "thumbnails").apply { mkdirs() }
                val thumbFile = File(thumbDir, "$recordId.jpg")
                java.io.FileOutputStream(thumbFile).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                }
                val r = dao.getById(recordId) ?: return@withContext
                r.thumbnailUrl = thumbFile.absolutePath
                dao.update(r)
            }
        } catch (e: Exception) {
            android.util.Log.w("Snatch", "Thumbnail generation failed (likely an audio-only file, that's fine)", e)
        }
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return "…"
        val kb = bytesPerSec / 1024.0
        return if (kb < 1024) "%.0f KB/s".format(kb) else "%.1f MB/s".format(kb / 1024.0)
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb < 1024) "%.1f MB".format(mb) else "%.2f GB".format(mb / 1024.0)
    }

    private fun formatEta(seconds: Long): String {
        if (seconds <= 0) return "…"
        val m = seconds / 60
        val s = seconds % 60
        return "%d:%02d".format(m, s)
    }

    private fun contentIntentForApp(): PendingIntent {
        val intent = Intent(this, DownloadsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun actionIntent(recordId: Long, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, DownloadService::class.java).apply {
            this.action = action
            putExtra(EXTRA_RECORD_ID, recordId)
        }
        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun pauseAction(recordId: Long): NotificationCompat.Action {
        val pending = actionIntent(recordId, ACTION_PAUSE, (recordId * 10 + 1).toInt())
        return NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", pending)
    }

    private fun deleteAction(recordId: Long): NotificationCompat.Action {
        val pending = actionIntent(recordId, ACTION_DELETE, (recordId * 10 + 2).toInt())
        return NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel, "Delete", pending)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID_PROGRESS, "Download progress", NotificationManager.IMPORTANCE_LOW)
            )
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID_RESULT, "Download results", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    private fun buildProgressNotification(text: String, ongoing: Boolean): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID_PROGRESS)
            .setContentTitle("Snatch")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntentForApp())
            .build()

    private fun postStatus(recordId: Long, id: Int, text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_PROGRESS)
            .setContentTitle("Snatch")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntentForApp())
            .addAction(pauseAction(recordId))
            .addAction(deleteAction(recordId))
            .build()
        getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    private fun postProgress(recordId: Long, id: Int, percent: Int, sizeText: String, speed: String, eta: String, label: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_PROGRESS)
            .setContentTitle("Snatch — Downloading $label")
            .setContentText("$percent%  •  $sizeText  •  $speed  •  ETA $eta")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, percent.coerceIn(0, 100), false)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(contentIntentForApp())
            .addAction(pauseAction(recordId))
            .addAction(deleteAction(recordId))
            .build()
        getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    private fun postResult(recordId: Long, id: Int, text: String, success: Boolean) {
        val icon = if (success) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_RESULT)
            .setContentTitle("Snatch")
            .setContentText(text)
            .setSmallIcon(icon)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(contentIntentForApp())
            .build()
        getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}