package com.novatube.app.ui.screens.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.novatube.app.R
import com.novatube.app.data.entity.DownloadEntity
import com.novatube.app.viewmodel.LibraryViewModel
import com.novatube.app.util.FileUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    onOpenPlayer: (String, String) -> Unit,
    onBack: () -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val context = LocalContext.current
    val audio by viewModel.audio.collectAsState()
    var queue by remember { mutableStateOf<List<DownloadEntity>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(-1) }
    var playing by remember { mutableStateOf(false) }
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }

    val current = if (currentIndex in queue.indices) queue[currentIndex] else null

    LaunchedEffect(audio) {
        if (queue.isEmpty() && audio.isNotEmpty()) queue = audio
    }

    LaunchedEffect(playing, current?.id) {
        while (playing) {
            positionMs += 1000
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_music)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (current != null) {
                NowPlayingBar(
                    current = current,
                    playing = playing,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onPlayPause = { playing = !playing },
                    onSkipNext = { if (currentIndex < queue.lastIndex) currentIndex++; positionMs = 0 },
                    onSkipPrev = { if (currentIndex > 0) currentIndex--; positionMs = 0 },
                    onSeek = { positionMs = it }
                )
            }
            Text(stringResource(R.string.music_queue), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            if (queue.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.library_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items(queue, key = { it.id }) { item ->
                        val idx = queue.indexOf(item)
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                currentIndex = idx
                                positionMs = 0
                                playing = true
                                item.filePath?.let { onOpenPlayer(it, item.title) }
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.MusicNote, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                                Text(item.uploader ?: "—", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(FileUtils.humanReadableSize(item.fileSize), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingBar(
    current: DownloadEntity,
    playing: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrev: () -> Unit,
    onSeek: (Long) -> Unit
) {
    Surface(tonalElevation = 4.dp) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                            )
                        )
                ) {
                    if (!current.thumbnail.isNullOrBlank()) {
                        AsyncImage(model = current.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(current.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(current.uploader ?: "—", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                IconButton(onClick = onSkipPrev) { Icon(Icons.Outlined.SkipPrevious, contentDescription = null) }
                FilledIconButton(onClick = onPlayPause) {
                    Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
                }
                IconButton(onClick = onSkipNext) { Icon(Icons.Outlined.SkipNext, contentDescription = null) }
            }
            Slider(
                value = positionMs.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..(durationMs.coerceAtLeast(1)).toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
