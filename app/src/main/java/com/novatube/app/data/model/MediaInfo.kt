package com.novatube.app.data.model

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/** Mirrors the JSON returned by `yt-dlp -J --dump-single-json`. */
data class MediaInfo(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("uploader") val uploader: String? = null,
    @SerializedName("uploader_id") val uploaderId: String? = null,
    @SerializedName("channel") val channel: String? = null,
    @SerializedName("channel_url") val channelUrl: String? = null,
    @SerializedName("duration") val duration: Long? = null,
    @SerializedName("view_count") val viewCount: Long? = null,
    @SerializedName("like_count") val likeCount: Long? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("thumbnails") val thumbnails: List<JsonObject>? = null,
    @SerializedName("webpage_url") val webpageUrl: String? = null,
    @SerializedName("extractor") val extractor: String? = null,
    @SerializedName("extractor_key") val extractorKey: String? = null,
    @SerializedName("uploader_url") val uploaderUrl: String? = null,
    @SerializedName("upload_date") val uploadDate: String? = null,
    @SerializedName("categories") val categories: List<String>? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("is_live") val isLive: Boolean? = null,
    @SerializedName("formats") val formats: List<MediaFormat>? = null,
    @SerializedName("entries") val entries: List<JsonObject>? = null
)
