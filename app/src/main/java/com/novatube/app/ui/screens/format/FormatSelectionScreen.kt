@file:OptIn(ExperimentalMaterial3Api::class)

package com.novatube.app.ui.screens.format

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.novatube.app.R
import com.novatube.app.data.model.MediaFormat
import com.novatube.app.data.model.RequestedDownload
import com.novatube.app.data.prefs.AudioFormat
import com.novatube.app.viewmodel.DownloadsViewModel
import com.novatube.app.viewmodel.FormatSelectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatSelectionScreen(
    url: String,
    onBack: () -> Unit,
    onEnqueued: () -> Unit,
    viewModel: FormatSelectionViewModel = viewModel(),
    downloadsViewModel: DownloadsViewModel = viewModel()
) {
    LaunchedEffect(url) { viewModel.load(url) }
    val state by viewModel.state.collectAsState()
    var selectedFormat by remember { mutableStateOf<MediaFormat?>(null) }
    var isAudio by remember { mutableStateOf(false) }
    var audioFormat by remember { mutableStateOf(AudioFormat.MP3) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.format_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.format_extracting))
                }
            }
            state.error != null -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            state.mediaInfo == null -> Box(modifier = Modifier.fillMaxSize().padding(padding))
            else -> Content(
                state = state,
                paddingValues = padding,
                selectedFormat = selectedFormat,
                onSelect = { selectedFormat = it; isAudio = it.isAudio },
                isAudio = isAudio,
                audioFormat = audioFormat,
                onAudioFormat = { audioFormat = it },
                onDownload = {
                    val fmt = selectedFormat
                    val info = state.mediaInfo!!
                    if (fmt != null) {
                        val req = RequestedDownload(
                            url = info.webpageUrl ?: url,
                            formatId = fmt.formatId ?: "best",
                            fileName = (info.title ?: "media").take(80),
                            isAudioOnly = fmt.isAudio,
                            audioFormat = audioFormat.name.lowercase(),
                            title = info.title,
                            uploader = info.uploader,
                            thumbnail = info.thumbnail,
                            duration = info.duration,
                            webpageUrl = info.webpageUrl
                        )
                        downloadsViewModel.enqueueDownload(req)
                        onEnqueued()
                    } else {
                        val req = RequestedDownload(
                            url = info.webpageUrl ?: url,
                            formatId = "best",
                            fileName = (info.title ?: "media").take(80),
                            isAudioOnly = isAudio,
                            audioFormat = audioFormat.name.lowercase(),
                            title = info.title,
                            uploader = info.uploader,
                            thumbnail = info.thumbnail,
                            duration = info.duration,
                            webpageUrl = info.webpageUrl
                        )
                        downloadsViewModel.enqueueDownload(req)
                        onEnqueued()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    state: com.novatube.app.viewmodel.FormatUiState,
    paddingValues: PaddingValues,
    selectedFormat: MediaFormat?,
    onSelect: (MediaFormat) -> Unit,
    isAudio: Boolean,
    audioFormat: AudioFormat,
    onAudioFormat: (AudioFormat) -> Unit,
    onDownload: () -> Unit
) {
    val info = state.mediaInfo!!
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(20.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(120.dp, 80.dp).clip(RoundedCornerShape(12.dp))) {
                        if (!info.thumbnail.isNullOrBlank()) {
                            AsyncImage(model = info.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxSize()) {}
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(info.title ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        Text(info.uploader ?: "—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(info.duration?.let { "${it / 60}m ${it % 60}s" } ?: "—", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.PlayCircle, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.format_video), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        if (state.videoFormats.isEmpty()) {
            item { Text(stringResource(R.string.format_no_video), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(state.videoFormats) { f ->
            FormatRow(
                format = f,
                selected = selectedFormat?.formatId == f.formatId,
                onClick = { onSelect(f) }
            )
        }
        item {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.MusicNote, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.format_audio), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        if (state.audioFormats.isEmpty()) {
            item { Text(stringResource(R.string.format_no_audio), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(state.audioFormats) { f ->
            FormatRow(
                format = f,
                selected = selectedFormat?.formatId == f.formatId,
                onClick = { onSelect(f) }
            )
        }
        if (isAudio) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Audio format", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AudioFormat.values().forEach { af ->
                        FilterChip(
                            selected = audioFormat == af,
                            onClick = { onAudioFormat(af) },
                            label = { Text(af.name) }
                        )
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.format_download))
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatRow(format: MediaFormat, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(format.displayResolution, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(format.formatNote ?: format.formatId ?: "—", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(format.displayExt, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(8.dp))
            Text(format.displaySize ?: "—", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
