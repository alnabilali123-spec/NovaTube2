package com.novatube.app.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novatube.app.R
import com.novatube.app.data.model.SearchKind
import com.novatube.app.ui.components.ResultListItem
import com.novatube.app.viewmodel.SearchSort
import com.novatube.app.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenFormat: (String) -> Unit,
    onOpenPlayer: (String, String) -> Unit,
    onOpenBrowser: (String) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val focus = LocalFocusManager.current
    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SearchBar(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
                onSubmit = {
                    viewModel.submit(it)
                    focus.clearFocus()
                },
                onFilterClick = { showFilters = true },
                onClearRecent = viewModel::clearHistory
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.error != null -> CenteredMessage(state.error!!)
                state.query.isBlank() -> IdleContent(
                    recent = state.recent,
                    trending = viewModel.trending(),
                    onPick = { viewModel.submit(it); focus.clearFocus() },
                    onRemove = viewModel::removeHistory,
                    onClear = { viewModel.clearHistory() }
                )
                state.results.isNotEmpty() -> ResultsContent(
                    results = state.results,
                    onResultClick = { onOpenFormat(it.url) },
                    onDownload = { onOpenFormat(it.url) },
                    onOpen = { onOpenPlayer(it.url, it.title) }
                )
                state.suggestions.isNotEmpty() -> SuggestionsContent(
                    suggestions = state.suggestions,
                    onPick = { viewModel.submit(it); focus.clearFocus() }
                )
                else -> CenteredMessage(stringResource(R.string.search_no_results))
            }
        }
    }

    if (showFilters) {
        FilterSheet(
            kind = state.kindFilter,
            sort = state.sort,
            onKind = { viewModel.setFilter(it) },
            onSort = { viewModel.setSort(it) },
            onDismiss = { showFilters = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onFilterClick: () -> Unit,
    onClearRecent: () -> Unit
) {
    Surface(tonalElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onFilterClick) { Icon(Icons.Outlined.Tune, contentDescription = "Filter") }
        }
    }
}

@Composable
private fun SuggestionsContent(suggestions: List<String>, onPick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item { Text(stringResource(R.string.search_suggestions), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
        items(suggestions) { s ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.NorthWest, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { onPick(s) }, modifier = Modifier.weight(1f)) { Text(s, modifier = Modifier.fillMaxWidth()) }
            }
            Divider()
        }
    }
}

@Composable
private fun ResultsContent(
    results: List<com.novatube.app.data.model.SearchResult>,
    onResultClick: (com.novatube.app.data.model.SearchResult) -> Unit,
    onDownload: (com.novatube.app.data.model.SearchResult) -> Unit,
    onOpen: (com.novatube.app.data.model.SearchResult) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        items(results, key = { it.id }) { r ->
            ResultListItem(
                result = r,
                onClick = { onOpen(r) },
                onDownload = { onDownload(r) }
            )
            Divider()
        }
    }
}

@Composable
private fun IdleContent(
    recent: List<String>,
    trending: List<String>,
    onPick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.search_recent), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = onClear) { Text(stringResource(R.string.search_clear)) }
            }
        }
        if (recent.isEmpty()) {
            item { Text("No recent searches", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp)) }
        }
        items(recent) { q ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.History, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { onPick(q) }, modifier = Modifier.weight(1f)) { Text(q) }
                IconButton(onClick = { onRemove(q) }) { Icon(Icons.Outlined.Close, contentDescription = "Remove") }
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.search_trending), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        items(trending) { t ->
            AssistChip(onClick = { onPick(t) }, label = { Text(t) }, modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    kind: SearchKind,
    sort: SearchSort,
    onKind: (SearchKind) -> Unit,
    onSort: (SearchSort) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Filter & sort", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Text("Type", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = kind == SearchKind.VIDEO, onClick = { onKind(SearchKind.VIDEO) }, label = { Text(stringResource(R.string.search_filter_video)) })
                FilterChip(selected = kind == SearchKind.AUDIO, onClick = { onKind(SearchKind.AUDIO) }, label = { Text(stringResource(R.string.search_filter_audio)) })
                FilterChip(selected = kind == SearchKind.PLAYLIST, onClick = { onKind(SearchKind.PLAYLIST) }, label = { Text(stringResource(R.string.search_filter_playlist)) })
                FilterChip(selected = kind == SearchKind.GENERIC, onClick = { onKind(SearchKind.GENERIC) }, label = { Text("All") })
            }
            Spacer(Modifier.height(12.dp))
            Text("Sort", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = sort == SearchSort.RELEVANCE, onClick = { onSort(SearchSort.RELEVANCE) }, label = { Text(stringResource(R.string.search_sort_relevance)) })
                FilterChip(selected = sort == SearchSort.DATE, onClick = { onSort(SearchSort.DATE) }, label = { Text(stringResource(R.string.search_sort_date)) })
                FilterChip(selected = sort == SearchSort.VIEWS, onClick = { onSort(SearchSort.VIEWS) }, label = { Text(stringResource(R.string.search_sort_views)) })
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CenteredMessage(msg: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
