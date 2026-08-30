package com.novatube.app.data.model

/** What the format selection screen returns to the downloader. */
data class RequestedDownload(
    val url: String,
    val formatId: String,
    val fileName: String,
    val isAudioOnly: Boolean,
    val audioFormat: String = "mp3",
    val title: String? = null,
    val uploader: String? = null,
    val thumbnail: String? = null,
    val duration: Long? = null,
    val webpageUrl: String? = null
)
