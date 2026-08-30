package com.novatube.app.data.model

import androidx.compose.runtime.Immutable

enum class SearchKind { VIDEO, AUDIO, PLAYLIST, CHANNEL, GENERIC }

@Immutable
data class SearchResult(
    val id: String,
    val title: String,
    val uploader: String?,
    val duration: Long?,
    val thumbnail: String?,
    val url: String,
    val kind: SearchKind,
    val platform: String,
    val viewCount: Long? = null,
    val uploadDate: String? = null
)

@Immutable
data class SearchSuggestion(
    val query: String,
    val displayText: String,
    val kind: SearchKind
)
