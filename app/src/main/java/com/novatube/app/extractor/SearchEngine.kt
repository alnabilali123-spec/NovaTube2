package com.novatube.app.extractor

import android.content.Context
import com.google.gson.Gson
import com.novatube.app.data.model.SearchKind
import com.novatube.app.data.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Search backend. Tries platform-specific yt-dlp extractors first (ytsearch:, scsearch:, etc.).
 */
class SearchEngine(private val gson: Gson = Gson()) {

    private val engine = YtDlpEngine(gson)

    suspend fun ensureReady(context: Context) = withContext(Dispatchers.IO) {
        if (!engine.initialized) engine.init(context)
    }

    suspend fun search(context: Context, query: String, max: Int = 25): List<SearchResult> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            if (!engine.initialized) engine.init(context)
            if (!engine.initialized) return@withContext emptyList()
            val results = mutableListOf<SearchResult>()
            results += engine.flatSearch("ytsearch$max:$query", "YouTube", SearchKind.VIDEO)
            results += engine.flatSearch("scsearch$max:$query", "SoundCloud", SearchKind.AUDIO)
            results.distinctBy { it.url }
        }

    suspend fun suggestions(context: Context, query: String, max: Int = 8): List<String> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            if (!engine.initialized) engine.init(context)
            if (!engine.initialized) return@withContext emptyList()
            engine.titleSuggestions("ytsearch$max:$query", max)
        }
}
