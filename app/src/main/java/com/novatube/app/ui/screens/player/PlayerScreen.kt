package com.novatube.app.ui.screens.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.novatube.app.R
import com.novatube.app.player.video.VideoPlayerHolder
import com.novatube.app.ui.components.formatDuration
import com.novatube.app.util.OkHttpProvider
import com.novatube.app.NovaTubeApp
import kotlinx.coroutines.delay

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun PlayerScreen(
    url: String,
    title: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as NovaTubeApp
    val activity = context as? Activity

    val holder = remember { VideoPlayerHolder(context, OkHttpProvider.client, app.preferencesRepository) }
    DisposableEffect(Unit) {
        onDispose { holder.release() }
    }
    LaunchedEffect(url) { holder.setSource(url, title) }

    val state by holder.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var brightness by remember { mutableStateOf(0.5f) }
    var volume by remember { mutableStateOf(0.8f) }
    var showOverlay by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var seeking by remember { mutableStateOf<Float?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val window = activity?.window
        window?.attributes?.screenBrightness?.let { brightness = it.coerceIn(0f, 1f) }
    }

    LaunchedEffect(state.isPlaying) {
        while (state.isPlaying) {
            holder.setPosition(holder.player.currentPosition, holder.player.duration.coerceAtLeast(0L))
            delay(500)
        }
    }

    BackHandler(enabled = isFullscreen) {
        setFullscreen(activity, false)
        isFullscreen = false
    }

    // Picture-in-Picture
    DisposableEffect(activity) {
        if (activity != null) {
            activity.packageManager
        }
        onDispose {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val width = size.width.toFloat()
                        if (offset.x < width / 3) holder.seekBy(-10_000) else holder.seekBy(10_000)
                    },
                    onTap = { showOverlay = !showOverlay }
                )
            }
            .pointerInput(Unit) {
                var totalDx = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDx = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDx += dragAmount
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        seeking = (totalDx / w * 60_000f).coerceIn(-60_000f, 60_000f)
                    },
                    onDragEnd = {
                        seeking?.let { holder.seekBy(it.toLong()) }
                        seeking = null
                    }
                )
            }
            .pointerInput(Unit) {
                var totalDy = 0f
                var side: Side? = null
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        totalDy = 0f
                        side = if (offset.x < size.width / 2) Side.LEFT else Side.RIGHT
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        totalDy += dragAmount
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val delta = -totalDy / h
                        if (side == Side.LEFT) {
                            brightness = (brightness + delta).coerceIn(0f, 1f)
                            activity?.window?.attributes?.let { attrs ->
                                attrs.screenBrightness = brightness
                                activity.window.attributes = attrs
                            }
                        } else {
                            volume = (volume + delta).coerceIn(0f, 1f)
                            holder.setVolume(volume)
                        }
                    },
                    onDragEnd = { side = null }
                )
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    setPlayer(holder.player)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            }
        )

        if (showOverlay) {
            PlayerOverlay(
                state = state,
                isFullscreen = isFullscreen,
                onPlayPause = { holder.togglePlay() },
                onSeekBack = { holder.seekBy(-10_000) },
                onSeekForward = { holder.seekBy(10_000) },
                onSeek = { holder.seekTo(it) },
                onSpeed = { holder.setSpeed(it) },
                onFullscreen = {
                    isFullscreen = !isFullscreen
                    setFullscreen(activity, isFullscreen)
                },
                onPip = { enterPip(activity) },
                onBack = if (isFullscreen) {
                    { isFullscreen = false; setFullscreen(activity, false) }
                } else onBack,
                onSettings = { showSettings = true },
                seekingMs = seeking?.toLong(),
                brightness = brightness,
                volume = volume
            )
        }
    }

    if (showSettings) {
        SettingsSheet(
            currentSpeed = state.playbackSpeed,
            onSpeed = { holder.setSpeed(it); showSettings = false },
            onDismiss = { showSettings = false }
        )
    }
}

private enum class Side { LEFT, RIGHT }

@OptIn(UnstableApi::class)
@Composable
private fun PlayerOverlay(
    state: com.novatube.app.player.video.PlayerUiState,
    isFullscreen: Boolean,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeed: (Float) -> Unit,
    onFullscreen: () -> Unit,
    onPip: () -> Unit,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    seekingMs: Long?,
    brightness: Float,
    volume: Float
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.0f))) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = null, tint = Color.White) }
            Spacer(Modifier.width(8.dp))
            Text(
                text = state.title ?: "",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = null, tint = Color.White) }
            IconButton(onClick = onPip) { Icon(Icons.Outlined.PictureInPicture, contentDescription = null, tint = Color.White) }
            IconButton(onClick = onFullscreen) {
                Icon(if (isFullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen, contentDescription = null, tint = Color.White)
            }
        }

        // Center controls
        Row(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSeekBack) { Icon(Icons.Outlined.Replay10, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp)) }
            FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(72.dp)) {
                Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = onSeekForward) { Icon(Icons.Outlined.Forward10, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp)) }
        }

        // Bottom seek bar
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp)
        ) {
            if (seekingMs != null) {
                Text("Seeking ${seekingMs / 1000}s", color = Color.White, style = MaterialTheme.typography.labelSmall)
            } else if (state.errorMessage != null) {
                Text(state.errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            } else if (state.playbackState == androidx.media3.common.Player.STATE_BUFFERING) {
                Text(stringResource(R.string.player_loading), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
            Slider(
                value = (state.position.coerceAtLeast(0)).toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..(state.duration.coerceAtLeast(1)).toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
            Row {
                Text(formatDuration(state.position / 1000), color = Color.White, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(1f))
                Text(formatDuration(state.duration / 1000), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Brightness6, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                LinearProgressIndicator(progress = { brightness }, modifier = Modifier.weight(1f).height(4.dp))
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Outlined.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                LinearProgressIndicator(progress = { volume }, modifier = Modifier.weight(1f).height(4.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(currentSpeed: Float, onSpeed: (Float) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.player_speed), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { speed ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${speed}x", modifier = Modifier.weight(1f))
                    RadioButton(selected = currentSpeed == speed, onClick = { onSpeed(speed) })
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun setFullscreen(activity: Activity?, fullscreen: Boolean) {
    if (activity == null) return
    if (fullscreen) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    } else {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }
}

private fun enterPip(activity: Activity?) {
    if (activity == null) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        activity.enterPictureInPictureMode(params)
    }
}
