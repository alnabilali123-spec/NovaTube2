package com.novatube.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novatube.app.R
import com.novatube.app.ui.components.MediaCard
import com.novatube.app.ui.components.PlatformBadge
import com.novatube.app.util.UrlUtils
import com.novatube.app.viewmodel.DownloadsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onOpenPlayer: (url: String, title: String) -> Unit,
    onOpenFormat: (url: String) -> Unit,
    onOpenBrowser: (url: String?) -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    downloadsViewModel: DownloadsViewModel = viewModel()
) {
    val ctx = LocalContext.current
    val downloads by downloadsViewModel.downloads.collectAsState()
    val active by downloadsViewModel.active.collectAsState()

    var showQuickDialog by remember { mutableStateOf(false) }
    var quickUrl by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 2.sp),
                        fontWeight = FontWeight.Black
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSearch) { Icon(Icons.Outlined.Search, contentDescription = "Search") }
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "More") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.nav_browser)) }, onClick = { onOpenBrowser(null); showMenu = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.nav_music)) }, onClick = { onOpenBrowser("music://playlists"); showMenu = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.nav_playlists)) }, onClick = { onOpenLibrary(); showMenu = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.nav_history)) }, onClick = { onOpenBrowser(null); showMenu = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.nav_settings)) }, onClick = onOpenSettings)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeroSection(onOpenSearch = onOpenSearch, onQuickDownload = { showQuickDialog = true })
            }
            item {
                QuickActions(
                    onDownloads = onOpenDownloads,
                    onLibrary = onOpenLibrary,
                    onBrowser = { onOpenBrowser(null) }
                )
            }
            if (active.isNotEmpty()) {
                item { DownloadStatusMiniPanel(active = active.map { it.title to it.progress }) }
            }
            item { SectionHeader(text = stringResource(R.string.home_trending)) }
            item { TrendingRow(onOpen = onOpenPlayer, onDownload = onOpenFormat) }
            item { SectionHeader(text = stringResource(R.string.home_recently_downloaded)) }
            if (downloads.isEmpty()) {
                item { EmptyState(text = stringResource(R.string.downloads_empty)) }
            } else {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(downloads.take(10)) { d ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.width(220.dp).clickable {
                                    d.filePath?.let { onOpenPlayer(it, d.title) }
                                }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(d.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                                    Spacer(Modifier.height(4.dp))
                                    Text(d.uploader ?: "—", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(6.dp))
                                    AssistChip(onClick = {}, label = { Text(d.ext.uppercase()) })
                                }
                            }
                        }
                    }
                }
            }
            item { SectionHeader(text = stringResource(R.string.home_popular_platforms)) }
            item { PlatformsGrid(onClick = { onOpenBrowser(it) }) }
            item { SectionHeader(text = stringResource(R.string.home_recommended)) }
            items(recommended()) { r ->
                MediaCard(
                    title = r.title,
                    uploader = r.uploader,
                    durationSec = r.duration,
                    thumbnail = r.thumbnail,
                    platform = r.platform,
                    onClick = { r.url.let { onOpenFormat(it) } },
                    onDownload = { r.url.let { onOpenFormat(it) } },
                    onMore = { r.url.let { onOpenPlayer(it, r.title) } },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showQuickDialog) {
        AlertDialog(
            onDismissRequest = { showQuickDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    if (quickUrl.isNotBlank()) onOpenFormat(quickUrl)
                    showQuickDialog = false
                    quickUrl = ""
                }) { Text(stringResource(R.string.format_download)) }
            },
            dismissButton = { TextButton(onClick = { showQuickDialog = false }) { Text(stringResource(R.string.common_cancel)) } },
            title = { Text(stringResource(R.string.home_quick_download)) },
            text = {
                OutlinedTextField(
                    value = quickUrl,
                    onValueChange = { quickUrl = it },
                    placeholder = { Text("Paste a media URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}

@Composable
private fun HeroSection(onOpenSearch: () -> Unit, onQuickDownload: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(160.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                Column {
                    Text(stringResource(R.string.home_greeting), style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Black)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onOpenSearch) {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Search")
                    }
                    FilledTonalButton(onClick = onQuickDownload) {
                        Icon(Icons.Outlined.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.home_quick_download))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActions(
    onDownloads: () -> Unit,
    onLibrary: () -> Unit,
    onBrowser: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionTile(icon = Icons.Outlined.Download, label = "Downloads", onClick = onDownloads, modifier = Modifier.weight(1f))
        ActionTile(icon = Icons.Outlined.LibraryMusic, label = "Library", onClick = onLibrary, modifier = Modifier.weight(1f))
        ActionTile(icon = Icons.Outlined.Public, label = "Browser", onClick = onBrowser, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ActionTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun DownloadStatusMiniPanel(active: List<Pair<String, Int>>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.home_mini_panel), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            active.take(3).forEach { (title, progress) ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(title, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    Spacer(Modifier.height(2.dp))
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun TrendingRow(onOpen: (String, String) -> Unit, onDownload: (String) -> Unit) {
    val items = listOf(
        Triple("Lo-fi beats to study", "https://www.youtube.com/watch?v=jfKfPfyJRdk", "Lo-Fi Girl"),
        Triple("NASA Artemis Highlights", "https://www.youtube.com/watch?v=21X5lGlDOfg", "NASA"),
        Triple("Top tracks 2024", "https://www.youtube.com/watch?v=9bZkp7q19f0", "Various Artists"),
        Triple("Cooking 101", "https://www.youtube.com/watch?v=cE0wfjsybIQ", "Chef Anna"),
        Triple("Travel Vlog", "https://www.youtube.com/watch?v=2Vv-BfVoq4g", "Wanderlust")
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { (title, url, uploader) ->
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.width(240.dp).clickable { onDownload(url) }
            ) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(MaterialTheme.colorScheme.primaryContainer))
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                        Text(uploader, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformsGrid(onClick: (String) -> Unit) {
    val platforms = listOf(
        "YouTube" to "https://m.youtube.com",
        "SoundCloud" to "https://soundcloud.com",
        "Vimeo" to "https://vimeo.com",
        "Twitter" to "https://x.com",
        "TikTok" to "https://www.tiktok.com",
        "Instagram" to "https://www.instagram.com",
        "Reddit" to "https://www.reddit.com",
        "Dailymotion" to "https://www.dailymotion.com"
    )
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        platforms.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (name, url) ->
                    Card(
                        modifier = Modifier.weight(1f).clickable { onClick(url) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private data class Recommended(val title: String, val uploader: String, val duration: Long, val thumbnail: String?, val platform: String, val url: String)

private fun recommended(): List<Recommended> = listOf(
    Recommended("Inside the ISS", "NASA", 596L, null, "YouTube", "https://www.youtube.com/watch?v=21X5lGlDOfg"),
    Recommended("Productive Morning Playlist", "Various", 3600L, null, "YouTube", "https://www.youtube.com/watch?v=jfKfPfyJRdk"),
    Recommended("Wildlife documentary", "BBC Earth", 1800L, null, "Vimeo", "https://vimeo.com/76979871"),
    Recommended("Indie Mix 2024", "IndieFM", 4200L, null, "SoundCloud", "https://soundcloud.com/discover")
)
