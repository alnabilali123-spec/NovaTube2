package com.novatube.app.ui.screens.downloads

import android.content.Intent
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novatube.app.R
import com.novatube.app.data.entity.DownloadEntity
import com.novatube.app.data.entity.DownloadStatus
import com.novatube.app.util.FileUtils
import com.novatube.app.viewmodel.DownloadsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onOpenPlayer: (String, String) -> Unit,
    viewModel: DownloadsViewModel = viewModel()
) {
    val ctx = LocalContext.current
    val downloads by viewModel.downloads.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.downloads_title)) },
                actions = {
                    TextButton(onClick = { confirmClear = true }) { Text(stringResource(R.string.search_clear)) }
                }
            )
        }
    ) { padding ->
        if (downloads.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.downloads_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(downloads, key = { it.id }) { d ->
                    DownloadRow(
                        download = d,
                        onPlay = { d.filePath?.let { onOpenPlayer(it, d.title) } },
                        onShare = { shareFile(ctx, d) },
                        onRetry = { viewModel.retry(d) },
                        onDelete = { viewModel.delete(d) },
                        onCancel = { viewModel.cancel(d) }
                    )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            confirmButton = { TextButton(onClick = { viewModel.clearCompleted(); confirmClear = false }) { Text(stringResource(R.string.common_delete)) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.common_cancel)) } },
            title = { Text(stringResource(R.string.settings_clear_downloads)) }
        )
    }
}

@Composable
private fun DownloadRow(
    download: DownloadEntity,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(download.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    Text(
                        text = "${download.uploader ?: "—"} • ${download.ext.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(download.status)
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = null) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.downloads_action_play)) },
                        leadingIcon = { Icon(Icons.Outlined.PlayArrow, contentDescription = null) },
                        enabled = download.status == DownloadStatus.COMPLETED,
                        onClick = { onPlay(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.downloads_action_share)) },
                        leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                        enabled = download.filePath != null,
                        onClick = { onShare(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.downloads_action_retry)) },
                        leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                        enabled = download.status == DownloadStatus.FAILED || download.status == DownloadStatus.CANCELLED,
                        onClick = { onRetry(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.downloads_action_delete)) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = { onDelete(); showMenu = false }
                    )
                }
            }
            if (download.status == DownloadStatus.RUNNING || download.status == DownloadStatus.QUEUED) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (download.progress / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    Text("${download.progress}%", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.weight(1f))
                    Text(FileUtils.humanReadableSize(download.downloadedBytes) + " / " + FileUtils.humanReadableSize(download.fileSize), style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = onCancel) { Text(stringResource(R.string.downloads_action_cancel)) }
            } else if (download.status == DownloadStatus.FAILED) {
                Spacer(Modifier.height(6.dp))
                Text(download.errorMessage ?: "Failed", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            } else if (download.status == DownloadStatus.COMPLETED) {
                Spacer(Modifier.height(4.dp))
                Text(FileUtils.humanReadableSize(download.fileSize), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StatusPill(status: DownloadStatus) {
    val (text, color) = when (status) {
        DownloadStatus.QUEUED -> stringResource(R.string.downloads_status_queued) to MaterialTheme.colorScheme.tertiary
        DownloadStatus.RUNNING -> stringResource(R.string.downloads_status_downloading) to MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED -> stringResource(R.string.downloads_status_paused) to MaterialTheme.colorScheme.secondary
        DownloadStatus.COMPLETED -> stringResource(R.string.downloads_status_completed) to MaterialTheme.colorScheme.primary
        DownloadStatus.FAILED -> stringResource(R.string.downloads_status_failed) to MaterialTheme.colorScheme.error
        DownloadStatus.CANCELLED -> stringResource(R.string.downloads_status_failed) to MaterialTheme.colorScheme.outline
    }
    Surface(shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.15f)) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

private fun shareFile(ctx: android.content.Context, download: DownloadEntity) {
    val path = download.filePath ?: return
    val file = File(path)
    if (!file.exists()) return
    val authority = ctx.packageName + ".fileprovider"
    val uri = FileProvider.getUriForFile(ctx, authority, file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeFor(download.ext)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(Intent.createChooser(intent, "Share via"))
}

private fun mimeFor(ext: String): String = when (ext.lowercase()) {
    "mp4", "mkv", "webm" -> "video/*"
    "mp3" -> "audio/mpeg"
    "m4a" -> "audio/mp4"
    "opus" -> "audio/ogg"
    "wav" -> "audio/wav"
    else -> "*/*"
}
