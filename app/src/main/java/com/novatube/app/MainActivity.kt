package com.novatube.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.collectAsState
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.novatube.app.nav.Routes
import com.novatube.app.ui.screens.browser.BrowserScreen
import com.novatube.app.ui.screens.downloads.DownloadsScreen
import com.novatube.app.ui.screens.format.FormatSelectionScreen
import com.novatube.app.ui.screens.history.HistoryScreen
import com.novatube.app.ui.screens.home.HomeScreen
import com.novatube.app.ui.screens.library.LibraryScreen
import com.novatube.app.ui.screens.music.MusicScreen
import com.novatube.app.ui.screens.player.PlayerScreen
import com.novatube.app.ui.screens.playlists.PlaylistsScreen
import com.novatube.app.ui.screens.search.SearchScreen
import com.novatube.app.ui.screens.settings.SettingsScreen
import com.novatube.app.ui.theme.NovaTubeTheme
import com.novatube.app.util.UrlUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLDecoder

class MainActivity : ComponentActivity() {

    private val pendingSharedUrl = androidx.compose.runtime.mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val sharedUrl = extractUrlFromIntent(intent)
        if (sharedUrl != null) pendingSharedUrl.value = sharedUrl
        setContent {
            val app = applicationContext as NovaTubeApp
            val prefs by app.preferencesRepository.preferences.collectAsState(initial = com.novatube.app.data.prefs.AppPreferences())
            val currentShared by pendingSharedUrl
            NovaTubeTheme(themeMode = prefs.themeMode) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot(sharedUrl = currentShared, intent = intent, onConsumed = { pendingSharedUrl.value = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val sharedUrl = extractUrlFromIntent(intent)
        if (sharedUrl != null) {
            pendingSharedUrl.value = sharedUrl
        }
    }

    private fun extractUrlFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
                UrlUtils.extractFirstUrl(text)
            }
            Intent.ACTION_VIEW -> intent.dataString?.let { UrlUtils.extractFirstUrl(it) ?: it }
            else -> null
        }
    }
}

private data class BottomItem(val route: String, val labelRes: Int, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(sharedUrl: String?, intent: Intent?, onConsumed: () -> Unit) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as NovaTubeApp
    val prefs by app.preferencesRepository.preferences.collectAsState(initial = com.novatube.app.data.prefs.AppPreferences())
    val scope = rememberCoroutineScope()

    var pendingShareUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingClipboardUrl by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(sharedUrl) {
        sharedUrl?.let {
            pendingShareUrl = it
            onConsumed()
        }
    }

    // Clipboard detection on launch
    LaunchedEffect(Unit) {
        if (prefs.clipboardDetection) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val text = cm?.primaryClip?.getItemAt(0)?.text?.toString()
            val url = UrlUtils.extractFirstUrl(text)
            if (url != null && url != prefs.lastDetectedClipboard && UrlUtils.looksLikeMediaUrl(url)) {
                pendingClipboardUrl = url
            }
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val playerBase = Routes.Player.route.substringBefore("/")
    val bottomItems = listOf(
        BottomItem(Routes.Home.route, R.string.nav_home, Icons.Outlined.Home),
        BottomItem(Routes.Search.route, R.string.nav_search, Icons.Outlined.Search),
        BottomItem(Routes.Downloads.route, R.string.nav_downloads, Icons.Outlined.Download),
        BottomItem(Routes.Library.route, R.string.nav_library, Icons.Outlined.LibraryMusic),
        BottomItem(playerBase, R.string.nav_player, Icons.Outlined.PlayCircle)
    )

    Scaffold(
        bottomBar = {
            val showBar = currentRoute in bottomItems.map { it.route }
            if (showBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute?.startsWith(item.route) == true,
                            onClick = {
                                if (item.route == playerBase) {
                                    navController.navigate(Routes.Player.build("about:blank", "Player"))
                                } else {
                                    navController.navigate(item.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                        popUpTo(Routes.Home.route) { saveState = true }
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Home.route,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            composable(Routes.Home.route) {
                HomeScreen(
                    onOpenSearch = { navController.navigate(Routes.Search.route) },
                    onOpenPlayer = { url, title -> navController.navigate(Routes.Player.build(url, title)) },
                    onOpenFormat = { url -> navController.navigate(Routes.FormatSelection.build(url)) },
                    onOpenBrowser = { url -> navController.navigate(Routes.BrowserOpen.build(url)) },
                    onOpenDownloads = { navController.navigate(Routes.Downloads.route) },
                    onOpenLibrary = { navController.navigate(Routes.Library.route) },
                    onOpenSettings = { navController.navigate(Routes.Settings.route) }
                )
            }
            composable(Routes.Search.route) {
                SearchScreen(
                    onOpenFormat = { url -> navController.navigate(Routes.FormatSelection.build(url)) },
                    onOpenPlayer = { url, title -> navController.navigate(Routes.Player.build(url, title)) },
                    onOpenBrowser = { url -> navController.navigate(Routes.BrowserOpen.build(url)) }
                )
            }
            composable(Routes.Downloads.route) {
                DownloadsScreen(
                    onOpenPlayer = { url, title -> navController.navigate(Routes.Player.build(url, title)) }
                )
            }
            composable(Routes.Library.route) {
                LibraryScreen(
                    onOpenPlayer = { url, title -> navController.navigate(Routes.Player.build(url, title)) }
                )
            }
            composable(
                route = Routes.Player.route,
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType }
                )
            ) { entry ->
                val url = URLDecoder.decode(entry.arguments?.getString("url") ?: "", "UTF-8")
                val title = URLDecoder.decode(entry.arguments?.getString("title") ?: "", "UTF-8")
                PlayerScreen(url = url, title = title, onBack = { navController.popBackStack() })
            }
            composable(Routes.Browser.route) {
                BrowserScreen(
                    initialUrl = null,
                    onClose = { navController.popBackStack() },
                    onSendToFormat = { url -> navController.navigate(Routes.FormatSelection.build(url)) }
                )
            }
            composable(
                route = Routes.BrowserOpen.route,
                arguments = listOf(navArgument("url") { type = NavType.StringType; defaultValue = ""; nullable = true })
            ) { entry ->
                val raw = entry.arguments?.getString("url") ?: ""
                val url = if (raw.isBlank()) null else URLDecoder.decode(raw, "UTF-8")
                BrowserScreen(
                    initialUrl = url,
                    onClose = { navController.popBackStack() },
                    onSendToFormat = { u -> navController.navigate(Routes.FormatSelection.build(u)) }
                )
            }
            composable(
                route = Routes.FormatSelection.route,
                arguments = listOf(navArgument("url") { type = NavType.StringType })
            ) { entry ->
                val raw = entry.arguments?.getString("url") ?: ""
                val url = URLDecoder.decode(raw, "UTF-8")
                FormatSelectionScreen(
                    url = url,
                    onBack = { navController.popBackStack() },
                    onEnqueued = { navController.navigate(Routes.Downloads.route) { popUpTo(Routes.Home.route) } }
                )
            }
            composable(Routes.Music.route) {
                MusicScreen(
                    onOpenPlayer = { url, title -> navController.navigate(Routes.Player.build(url, title)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.Playlists.route) {
                PlaylistsScreen(onBack = { navController.popBackStack() }, app = app)
            }
            composable(Routes.History.route) {
                HistoryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenUrl = { url -> navController.navigate(Routes.BrowserOpen.build(url)) }
                )
            }
            composable(Routes.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }

    pendingShareUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { pendingShareUrl = null },
            confirmButton = {
                TextButton(onClick = {
                    pendingShareUrl = null
                    navController.navigate(Routes.FormatSelection.build(url))
                }) { Text(stringResource(R.string.share_received_open)) }
            },
            dismissButton = { TextButton(onClick = { pendingShareUrl = null }) { Text(stringResource(R.string.share_received_dismiss)) } },
            title = { Text(stringResource(R.string.share_received_title)) },
            text = { Text("$url\n\n${stringResource(R.string.share_received_message)}") }
        )
    }
    pendingClipboardUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { pendingClipboardUrl = null },
            confirmButton = {
                TextButton(onClick = {
                    pendingClipboardUrl = null
                    scope.launch { app.preferencesRepository.setLastClipboardUrl(url) }
                    navController.navigate(Routes.FormatSelection.build(url))
                }) { Text(stringResource(R.string.clipboard_action)) }
            },
            dismissButton = { TextButton(onClick = { pendingClipboardUrl = null }) { Text(stringResource(R.string.common_cancel)) } },
            title = { Text(stringResource(R.string.clipboard_detected)) },
            text = { Text(url) }
        )
    }
}
