package com.novatube.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novatube.app.NovaTubeApp
import com.novatube.app.data.model.SearchKind
import com.novatube.app.data.model.SearchResult
import com.novatube.app.extractor.SearchEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(app: Application) : AndroidViewModel(app) {

    private val nova: NovaTubeApp = app as NovaTubeApp
    private val engine = SearchEngine()

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state

    private var suggestionsJob: Job? = null

    init {
        viewModelScope.launch {
            nova.database.searchHistoryDao().observeAll().collect { history ->
                _state.value = _state.value.copy(recent = history.map { it.query })
            }
        }
        viewModelScope.launch { engine.ensureReady(nova) }
    }

    fun onQueryChange(query: String) {
        _state.value = _state.value.copy(query = query)
        suggestionsJob?.cancel()
        if (query.isBlank()) {
            _state.value = _state.value.copy(suggestions = emptyList(), results = emptyList())
            return
        }
        suggestionsJob = viewModelScope.launch {
            delay(250)
            val sug = engine.suggestions(nova, query)
            _state.value = _state.value.copy(suggestions = sug)
        }
    }

    fun submit(query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        _state.value = _state.value.copy(query = q, loading = true, results = emptyList(), error = null)
        viewModelScope.launch {
            try {
                nova.database.searchHistoryDao().insert(
                    com.novatube.app.data.entity.SearchHistoryEntity(query = q)
                )
                val results = engine.search(nova, q).let { applyFilters(it, _state.value.kindFilter, _state.value.sort) }
                _state.value = _state.value.copy(loading = false, results = results, suggestions = emptyList())
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Search failed")
            }
        }
    }

    fun setFilter(kind: SearchKind) {
        _state.value = _state.value.copy(kindFilter = kind)
        if (_state.value.results.isNotEmpty()) {
            _state.value = _state.value.copy(results = applyFilters(_state.value.results, kind, _state.value.sort))
        }
    }

    fun setSort(sort: SearchSort) {
        _state.value = _state.value.copy(sort = sort)
        _state.value = _state.value.copy(results = applyFilters(_state.value.results, _state.value.kindFilter, sort))
    }

    private fun applyFilters(list: List<SearchResult>, kind: SearchKind, sort: SearchSort): List<SearchResult> {
        val filtered = if (kind == SearchKind.GENERIC) list else list.filter { it.kind == kind }
        return when (sort) {
            SearchSort.RELEVANCE -> filtered
            SearchSort.DATE -> filtered.sortedByDescending { it.uploadDate ?: "" }
            SearchSort.VIEWS -> filtered.sortedByDescending { it.viewCount ?: 0L }
        }
    }

    fun clearHistory() = viewModelScope.launch { nova.database.searchHistoryDao().clear() }
    fun removeHistory(query: String) = viewModelScope.launch { nova.database.searchHistoryDao().deleteByQuery(query) }

    fun trending(): List<String> = listOf(
        "Top music videos",
        "Trending now",
        "Best of 2024",
        "Viral clips",
        "Lo-fi study music",
        "Sports highlights"
    )
}

enum class SearchSort { RELEVANCE, DATE, VIEWS }

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<SearchResult> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val recent: List<String> = emptyList(),
    val kindFilter: SearchKind = SearchKind.VIDEO,
    val sort: SearchSort = SearchSort.RELEVANCE,
    val error: String? = null
)
