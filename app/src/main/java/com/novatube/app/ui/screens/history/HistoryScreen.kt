package com.novatube.app.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novatube.app.R
import com.novatube.app.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    viewModel: BrowserViewModel = viewModel()
) {
    val history by viewModel.history.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_history)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = null) } },
                actions = { TextButton(onClick = { viewModel.clearHistory() }) { Text(stringResource(R.string.search_clear)) } }
            )
        }
    ) { padding ->
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.library_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(history, key = { it.id }) { h ->
                    ListItem(
                        headlineContent = { Text(h.title) },
                        supportingContent = { Text(h.url) },
                        leadingContent = { Icon(Icons.Outlined.History, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().clickable { onOpenUrl(h.url) }
                    )
                    Divider()
                }
            }
        }
    }
}
