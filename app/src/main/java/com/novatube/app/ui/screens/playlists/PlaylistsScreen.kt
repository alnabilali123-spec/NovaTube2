package com.novatube.app.ui.screens.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.novatube.app.NovaTubeApp
import com.novatube.app.R
import com.novatube.app.data.entity.PlaylistEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistsViewModel(app: NovaTubeApp) : ViewModel() {
    private val dao = app.database.playlistDao()
    val playlists: StateFlow<List<PlaylistEntity>> = dao.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String) = viewModelScope.launch { dao.insertPlaylist(PlaylistEntity(name = name)) }
    fun delete(id: Long) = viewModelScope.launch { dao.deletePlaylist(id) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    onBack: () -> Unit,
    app: NovaTubeApp
) {
    val vm: PlaylistsViewModel = viewModel(factory = viewModelFactory { initializer { PlaylistsViewModel(app) } })
    val playlists by vm.playlists.collectAsState()
    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.music_playlists)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = null) } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showCreate = true }, icon = { Icon(Icons.Outlined.Add, contentDescription = null) }, text = { Text(stringResource(R.string.music_create_playlist)) })
        }
    ) { padding ->
        if (playlists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.library_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(playlists, key = { it.id }) { p ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { /* open detail */ }) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.MusicNote, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("${p.trackCount} tracks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { vm.delete(p.id) }) { Text(stringResource(R.string.common_delete)) }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            confirmButton = { TextButton(onClick = { if (name.isNotBlank()) { vm.create(name.trim()); showCreate = false } }) { Text(stringResource(R.string.common_save)) } },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text(stringResource(R.string.common_cancel)) } },
            title = { Text(stringResource(R.string.music_create_playlist)) },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        )
    }
}
