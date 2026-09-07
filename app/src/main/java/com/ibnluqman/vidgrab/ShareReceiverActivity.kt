package com.ibnluqman.vidgrab

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

/**
 * Appears in the Android Share Sheet (and its three activity-aliases: Share480Activity,
 * Share720Activity, Share1080Activity). No UI — extracts the URL, inserts a queued row into
 * the database, kicks off the download service, and finishes instantly.
 */
class ShareReceiverActivity : Activity() {

    private val urlPattern = Pattern.compile("(https?://\\S+)")

    override fun onCreate(savedInstanceState: Bundle?) {
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        }
        super.onCreate(savedInstanceState)

        val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val url = sharedText?.let { extractUrl(it) }

        if (url == null) {
            Toast.makeText(this, getString(R.string.toast_no_url), Toast.LENGTH_SHORT).show()
            finishAndClose()
            return
        }

        val forcedQuality = when {
            componentName.className.endsWith("Share480Activity") -> Settings.Quality.P480
            componentName.className.endsWith("Share720Activity") -> Settings.Quality.P720
            componentName.className.endsWith("Share1080Activity") -> Settings.Quality.P1080
            else -> null
        }

        val forcedMediaType = when {
            componentName.className.endsWith("ShareAudioActivity") -> "AUDIO"
            componentName.className.endsWith("SharePhotoActivity") -> "PHOTO"
            else -> null
        }

        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.get(applicationContext).downloadDao()
            dao.insert(DownloadRecord(url = url, forcedQuality = forcedQuality?.name, mediaType = forcedMediaType))
            val serviceIntent = Intent(applicationContext, DownloadService::class.java).apply {
                action = DownloadService.ACTION_ENQUEUE
            }
            ContextCompat.startForegroundService(applicationContext, serviceIntent)
        }

        finishAndClose()
    }

    private fun extractUrl(text: String): String? {
        val matcher = urlPattern.matcher(text)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun finishAndClose() {
        finish()
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
