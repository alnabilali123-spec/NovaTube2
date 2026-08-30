package com.novatube.app.nav

sealed class Routes(val route: String) {
    data object Home : Routes("home")
    data object Search : Routes("search")
    data object Downloads : Routes("downloads")
    data object Library : Routes("library")
    data object Player : Routes("player/{url}/{title}") {
        fun build(url: String, title: String): String =
            "player/${java.net.URLEncoder.encode(url, "UTF-8")}/${java.net.URLEncoder.encode(title, "UTF-8")}"
    }
    data object Browser : Routes("browser")
    data object BrowserOpen : Routes("browser?url={url}") {
        fun build(url: String?): String = if (url.isNullOrBlank()) "browser" else "browser?url=${java.net.URLEncoder.encode(url, "UTF-8")}"
    }
    data object Music : Routes("music")
    data object Playlists : Routes("playlists")
    data object History : Routes("history")
    data object Settings : Routes("settings")
    data object FormatSelection : Routes("format?url={url}") {
        fun build(url: String): String = "format?url=${java.net.URLEncoder.encode(url, "UTF-8")}"
    }
}
