package com.novatube.app.util

import java.net.URLDecoder
import java.util.regex.Pattern

object UrlUtils {

    private val URL_REGEX = Pattern.compile(
        "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
        Pattern.CASE_INSENSITIVE
    )

    fun extractFirstUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val m = URL_REGEX.matcher(text)
        return if (m.find()) m.group(1) else null
    }

    fun decode(url: String): String = runCatching { URLDecoder.decode(url, "UTF-8") }.getOrDefault(url)

    fun looksLikeMediaUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase()
        val hosts = listOf(
            "youtube.com", "youtu.be", "m.youtube.com", "music.youtube.com",
            "soundcloud.com", "vimeo.com", "twitch.tv",
            "twitter.com", "x.com", "t.co",
            "tiktok.com", "instagram.com", "facebook.com", "fb.watch",
            "dailymotion.com", "dai.ly",
            "reddit.com", "redgifs.com"
        )
        return hosts.any { lower.contains(it) }
    }

    fun platformName(url: String?): String {
        if (url.isNullOrBlank()) return "Web"
        val lower = url.lowercase()
        return when {
            "youtube.com" in lower || "youtu.be" in lower -> "YouTube"
            "soundcloud.com" in lower -> "SoundCloud"
            "vimeo.com" in lower -> "Vimeo"
            "twitch.tv" in lower -> "Twitch"
            "twitter.com" in lower || "x.com" in lower -> "Twitter"
            "tiktok.com" in lower -> "TikTok"
            "instagram.com" in lower -> "Instagram"
            "facebook.com" in lower || "fb.watch" in lower -> "Facebook"
            "dailymotion.com" in lower || "dai.ly" in lower -> "Dailymotion"
            "reddit.com" in lower -> "Reddit"
            "redgifs.com" in lower -> "RedGifs"
            else -> "Web"
        }
    }
}
