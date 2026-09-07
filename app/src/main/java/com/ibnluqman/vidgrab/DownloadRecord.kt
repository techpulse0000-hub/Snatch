package com.ibnluqman.vidgrab

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val forcedQuality: String?, // null = use whatever's set in Settings
    var mediaType: String? = null,
    var title: String = "Fetching…",
    var status: String = Status.QUEUED.name,
    var progress: Int = 0,
    var speedText: String = "",
    var etaText: String = "",
    var filePath: String? = null,
    var thumbnailUrl: String? = null,
    var errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    var downloadedBytes: Long = 0,
    var totalBytes: Long = 0,
    var playlistIndex: Int? = null,
    var carouselGroupId: Long? = null,
    var carouselIndex: Int? = null,
    var carouselTotal: Int? = null,
    var isDirectMediaUrl: Boolean = false,
) {
    enum class Status { QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED }
}
