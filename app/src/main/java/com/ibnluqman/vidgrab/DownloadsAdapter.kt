package com.ibnluqman.vidgrab

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.button.MaterialButton

class DownloadsAdapter(
    private val onPauseResume: (DownloadRecord) -> Unit,
    private val onPlay: (DownloadRecord) -> Unit,
    private val onDelete: (DownloadRecord) -> Unit
) : RecyclerView.Adapter<DownloadsAdapter.ViewHolder>() {

    private var items: List<DownloadRecord> = emptyList()

    fun submitList(newItems: List<DownloadRecord>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) = items[oldPos].id == newItems[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) = items[oldPos] == newItems[newPos]
        })
        items = newItems
        diff.dispatchUpdatesTo(this)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.itemThumbnail)
        val title: TextView = view.findViewById(R.id.itemTitle)
        val status: TextView = view.findViewById(R.id.itemStatus)
        val progress: ProgressBar = view.findViewById(R.id.itemProgress)
        val pauseResumeButton: MaterialButton = view.findViewById(R.id.pauseResumeButton)
        val playButton: MaterialButton = view.findViewById(R.id.playButton)
        val deleteButton: MaterialButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_download, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = items[position]
        holder.title.text = record.title
        holder.thumbnail.load(record.thumbnailUrl) {
            crossfade(true)
            addHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
            )
            val referer = runCatching {
                val u = java.net.URL(record.url)
                "${u.protocol}://${u.host}/"
            }.getOrNull()
            referer?.let { addHeader("Referer", it) }
        }

        val statusEnum = runCatching { DownloadRecord.Status.valueOf(record.status) }.getOrNull()

        holder.status.text = when (statusEnum) {
            DownloadRecord.Status.DOWNLOADING -> {
                val sizeText = if (record.totalBytes > 0)
                    "${formatBytesUi(record.downloadedBytes)} / ${formatBytesUi(record.totalBytes)}"
                else formatBytesUi(record.downloadedBytes)
                "Downloading — ${record.progress}%  •  $sizeText  •  ${record.speedText}  •  ETA ${record.etaText}"
            }            DownloadRecord.Status.QUEUED -> "Queued"
            DownloadRecord.Status.PAUSED -> "Paused — ${record.progress}%"
            DownloadRecord.Status.COMPLETED -> "Completed"
            DownloadRecord.Status.FAILED -> "Failed: ${record.errorMessage ?: "unknown error"}"
            null -> record.status
        }

        holder.progress.progress = record.progress
        holder.progress.visibility = if (statusEnum == DownloadRecord.Status.COMPLETED) View.GONE else View.VISIBLE

        holder.pauseResumeButton.visibility =
            if (statusEnum == DownloadRecord.Status.COMPLETED) View.GONE else View.VISIBLE
        holder.pauseResumeButton.text = if (statusEnum == DownloadRecord.Status.PAUSED
            || statusEnum == DownloadRecord.Status.FAILED) "Resume" else "Pause"
        holder.pauseResumeButton.setOnClickListener { onPauseResume(record) }

        holder.playButton.visibility = if (statusEnum == DownloadRecord.Status.COMPLETED) View.VISIBLE else View.GONE
        holder.playButton.text = if (record.mediaType == "PHOTO") "View" else "Play"
        holder.playButton.setOnClickListener { onPlay(record) }

        holder.deleteButton.setOnClickListener { onDelete(record) }
    }

    private fun formatBytesUi(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb < 1024) "%.1f MB".format(mb) else "%.2f GB".format(mb / 1024.0)
    }
}
