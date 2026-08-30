package com.novatube.app.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novatube.app.R
import com.novatube.app.data.entity.DownloadEntity
import com.novatube.app.util.FileUtils
import com.novatube.app.viewmodel.LibraryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenPlayer: (String, String) -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val audio by viewModel.audio.collectAsState()
    val video by viewModel.video.collectAsState()
    var tab by remember { mutableStateOf(0) }
    val items = if (tab == 0) audio else video

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.library_title)) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.library_audio)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.library_video)) })
            }
            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.library_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        LibraryRow(
                            item = item,
                            onPlay = { item.filePath?.let { onOpenPlayer(it, item.title) } },
                            onRename = { newName -> viewModel.rename(item, newName) },
                            onDelete = { viewModel.delete(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryRow(
    item: DownloadEntity,
    onPlay: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().clickable { onPlay() }) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (item.isAudioOnly) Icons.Outlined.MusicNote else Icons.Outlined.Movie,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(
                    "${item.uploader ?: "—"} • ${dateFormat.format(Date(item.createdAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${item.ext.uppercase()} • ${FileUtils.humanReadableSize(item.fileSize)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onPlay) { Icon(Icons.Outlined.PlayArrow, contentDescription = "Play") }
            IconButton(onClick = { showMenu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = null) }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.common_rename)) }, leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }, onClick = { showRename = true; showMenu = false })
                DropdownMenuItem(text = { Text(stringResource(R.string.common_share)) }, leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) }, onClick = { /* share via chooser */ showMenu = false })
                DropdownMenuItem(text = { Text(stringResource(R.string.common_delete)) }, leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }, onClick = { onDelete(); showMenu = false })
            }
        }
    }

    if (showRename) {
        var newName by remember { mutableStateOf(item.fileName.substringBeforeLast('.')) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            confirmButton = { TextButton(onClick = { onRename(newName); showRename = false }) { Text(stringResource(R.string.common_save)) } },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text(stringResource(R.string.common_cancel)) } },
            title = { Text(stringResource(R.string.common_rename)) },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        )
    }
}
