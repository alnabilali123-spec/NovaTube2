package com.novatube.app.data.model

import com.google.gson.annotations.SerializedName

/** Subset of a yt-dlp format entry, normalized for UI display. */
data class MediaFormat(
    @SerializedName("format_id") val formatId: String? = null,
    @SerializedName("format_note") val formatNote: String? = null,
    @SerializedName("ext") val ext: String? = null,
    @SerializedName("acodec") val acodec: String? = null,
    @SerializedName("vcodec") val vcodec: String? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null,
    @SerializedName("tbr") val tbr: Double? = null,
    @SerializedName("abr") val abr: Double? = null,
    @SerializedName("vbr") val vbr: Double? = null,
    @SerializedName("fps") val fps: Double? = null,
    @SerializedName("filesize") val filesize: Long? = null,
    @SerializedName("filesize_approx") val filesizeApprox: Long? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("manifest_url") val manifestUrl: String? = null,
    @SerializedName("protocol") val protocol: String? = null,
    @SerializedName("format") val format: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("dynamic_range") val dynamicRange: String? = null,
    @SerializedName("container") val container: String? = null
) {
    val isVideo: Boolean get() = !vcodec.isNullOrEmpty() && vcodec != "none" && height != null
    val isAudio: Boolean get() = (!acodec.isNullOrEmpty() && acodec != "none") && (vcodec.isNullOrEmpty() || vcodec == "none")
    val displayResolution: String
        get() = when {
            height != null -> "${height}p"
            vbr != null -> "${vbr.toInt()}kbps"
            tbr != null -> "${tbr.toInt()}kbps"
            else -> formatNote ?: formatId ?: "—"
        }
    val displayExt: String get() = ext?.uppercase() ?: "?"
    val displaySize: String? get() = (filesize ?: filesizeApprox)?.let { humanReadableSize(it) }
}

fun humanReadableSize(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var i = 0
    while (value >= 1024 && i < units.lastIndex) {
        value /= 1024
        i++
    }
    return String.format("%.1f %s", value, units[i])
}
