package com.novatube.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novatube.app.NovaTubeApp
import com.novatube.app.data.entity.BookmarkEntity
import com.novatube.app.data.entity.HistoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "New tab",
    var url: String = ""
)

class BrowserViewModel(app: Application) : AndroidViewModel(app) {

    private val nova: NovaTubeApp = app as NovaTubeApp

    private val _tabs = MutableStateFlow(listOf(BrowserTab()))
    val tabs: StateFlow<List<BrowserTab>> = _tabs

    private val _activeTab = MutableStateFlow(_tabs.value.first().id)
    val activeTab: StateFlow<String> = _activeTab

    val bookmarks: StateFlow<List<BookmarkEntity>> = nova.database.bookmarkDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = nova.database.historyDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun newTab(url: String? = null): BrowserTab {
        val tab = BrowserTab(url = url ?: "")
        _tabs.value = _tabs.value + tab
        _activeTab.value = tab.id
        return tab
    }

    fun closeTab(id: String) {
        val list = _tabs.value.toMutableList()
        list.removeAll { it.id == id }
        if (list.isEmpty()) list.add(BrowserTab())
        _tabs.value = list
        if (_activeTab.value == id) _activeTab.value = list.first().id
    }

    fun selectTab(id: String) { _activeTab.value = id }

    fun updateActive(url: String, title: String) {
        val id = _activeTab.value
        _tabs.value = _tabs.value.map { if (it.id == id) it.copy(url = url, title = title) else it }
        if (url.isNotBlank()) {
            viewModelScope.launch {
                nova.database.historyDao().insert(
                    HistoryEntity(title = title.ifBlank { url }, url = url, thumbnail = null, uploader = null, duration = null)
                )
            }
        }
    }

    fun updateActiveUrl(url: String) {
        val id = _activeTab.value
        _tabs.value = _tabs.value.map { if (it.id == id) it.copy(url = url) else it }
    }

    fun updateActiveTitle(title: String) {
        val id = _activeTab.value
        _tabs.value = _tabs.value.map { if (it.id == id) it.copy(title = title) else it }
    }

    fun addBookmark(title: String, url: String) = viewModelScope.launch {
        nova.database.bookmarkDao().insert(BookmarkEntity(title = title.ifBlank { url }, url = url))
    }

    fun removeBookmark(id: Long) = viewModelScope.launch { nova.database.bookmarkDao().delete(id) }

    fun clearHistory() = viewModelScope.launch { nova.database.historyDao().clear() }
}
