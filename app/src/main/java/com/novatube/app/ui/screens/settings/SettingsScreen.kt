package com.novatube.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.novatube.app.NovaTubeApp
import com.novatube.app.R
import com.novatube.app.data.prefs.AppPreferences
import com.novatube.app.data.prefs.AudioFormat
import com.novatube.app.data.prefs.ThemeMode
import com.novatube.app.data.prefs.VideoQuality
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as NovaTubeApp
    val prefs = app.preferencesRepository
    val scope = rememberCoroutineScope()
    val state by prefs.preferences.collectAsState(initial = AppPreferences())

    var ytdlpVersion by remember { mutableStateOf<String?>(null) }
    var ytdlpError by remember { mutableStateOf<String?>(null) }
    var updateMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        ytdlpVersion = app.ytDlpEngine.binaryVersion ?: "—"
        ytdlpError = app.ytDlpEngine.lastError
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SectionTitle(stringResource(R.string.settings_theme)) }
            item {
                Column {
                    ThemeMode.values().forEach { mode ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = state.themeMode == mode,
                                onClick = { scope.launch { prefs.setThemeMode(mode) } }
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                                    ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                                    ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                                }
                            )
                        }
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.settings_quality)) }
            item {
                Column {
                    VideoQuality.values().forEach { q ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = state.videoQuality == q,
                                onClick = { scope.launch { prefs.setVideoQuality(q) } }
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(q.name)
                        }
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.settings_audio_format)) }
            item {
                Column {
                    AudioFormat.values().forEach { a ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = state.audioFormat == a,
                                onClick = { scope.launch { prefs.setAudioFormat(a) } }
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(a.name)
                        }
                    }
                }
            }

            item { SectionTitle("Network & data") }
            item {
                SwitchRow(
                    title = stringResource(R.string.settings_wifi_only),
                    checked = state.wifiOnly,
                    onChange = { scope.launch { prefs.setWifiOnly(it) } }
                )
            }
            item {
                SwitchRow(
                    title = stringResource(R.string.settings_clipboard),
                    checked = state.clipboardDetection,
                    onChange = { scope.launch { prefs.setClipboardDetection(it) } }
                )
            }

            item { SectionTitle("History") }
            item {
                Column {
                    OutlinedButton(onClick = { scope.launch { app.database.searchHistoryDao().clear() } }) { Text(stringResource(R.string.settings_clear_search)) }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { scope.launch { app.database.historyDao().clear() } }) { Text(stringResource(R.string.settings_clear_history)) }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { scope.launch { app.downloadRepository.clearCompleted() } }) { Text(stringResource(R.string.settings_clear_downloads)) }
                }
            }

            item { SectionTitle(stringResource(R.string.settings_engine)) }
            item { InfoRow(stringResource(R.string.settings_ytdlp_version), ytdlpVersion ?: "—") }
            ytdlpError?.let { err ->
                item {
                    Text(
                        "Error: $err",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            item {
                Column {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                updateMessage = "Checking for updates…"
                                app.ytDlpEngine.init(context)
                                ytdlpVersion = app.ytDlpEngine.binaryVersion
                                ytdlpError = app.ytDlpEngine.lastError
                                updateMessage = if (app.ytDlpEngine.initialized) "Ready" else "Failed"
                            }
                        }
                    ) { Text(stringResource(R.string.settings_check_update)) }
                    updateMessage?.let { Spacer(Modifier.height(8.dp)); Text(it, style = MaterialTheme.typography.labelSmall) }
                }
            }

            item { SectionTitle(stringResource(R.string.settings_about)) }
            item {
                Column {
                    Text("NovaTube v1.0.0", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "yt-dlp is downloaded on first use from github.com/yt-dlp/yt-dlp. FFmpeg is bundled.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
