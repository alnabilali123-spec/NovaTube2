package com.novatube.app.ui.screens.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novatube.app.R
import com.novatube.app.util.UrlUtils
import com.novatube.app.viewmodel.BrowserTab
import com.novatube.app.viewmodel.BrowserViewModel
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    initialUrl: String? = null,
    onClose: () -> Unit,
    onSendToFormat: (String) -> Unit,
    viewModel: BrowserViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefs = (context.applicationContext as com.novatube.app.NovaTubeApp).preferencesRepository
    val preferences by prefs.preferences.collectAsState(initial = com.novatube.app.data.prefs.AppPreferences())

    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTab.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val history by viewModel.history.collectAsState()

    val activeTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()

    var urlInput by remember(activeTab.id) { mutableStateOf(activeTab.url) }
    var progress by remember { mutableStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var mediaDetectedUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialUrl, activeTab.id) {
        if (initialUrl != null && webView != null) {
            webView?.loadUrl(initialUrl)
        } else if (activeTab.url.isNotBlank() && webView != null) {
            webView?.loadUrl(activeTab.url)
        }
    }

    LaunchedEffect(preferences.desktopMode, preferences.javascriptEnabled) {
        webView?.settings?.apply {
            userAgentString = if (preferences.desktopMode) DESKTOP_UA else null
            javaScriptEnabled = preferences.javascriptEnabled
        }
    }

    BackHandler(enabled = true) {
        val wv = webView
        when {
            showSheet -> showSheet = false
            wv != null && wv.canGoBack() -> wv.goBack()
            else -> onClose()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(activeTab.title.ifBlank { stringResource(R.string.browser_home_page) }) },
                    navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = null) } },
                    actions = {
                        IconButton(onClick = { viewModel.newTab() }) { Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.browser_new_tab)) }
                        IconButton(onClick = { showSheet = true }) { Icon(Icons.Outlined.Tab, contentDescription = "Tabs (${tabs.size})") }
                    }
                )
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = { Text(stringResource(R.string.browser_url_hint)) },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { webView?.reload() }) { Icon(Icons.Outlined.Refresh, contentDescription = null) }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(20.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { webView?.goBack() }, enabled = canGoBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = null)
                    }
                    IconButton(onClick = { webView?.goForward() }, enabled = canGoForward) {
                        Icon(Icons.Outlined.ArrowForward, contentDescription = null)
                    }
                    IconButton(onClick = { webView?.loadUrl("https://www.google.com") }) { Icon(Icons.Outlined.Home, contentDescription = "Home") }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { viewModel.addBookmark(activeTab.title, activeTab.url) }) { Icon(Icons.Outlined.BookmarkAdd, contentDescription = null) }
                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, activeTab.url) }
                        context.startActivity(Intent.createChooser(send, "Share"))
                    }) { Icon(Icons.Outlined.Share, contentDescription = null) }
                    IconButton(onClick = { showSheet = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = null) }
                }
                if (progress in 1..99) {
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        bottomBar = {
            if (UrlUtils.looksLikeMediaUrl(activeTab.url)) {
                Surface(tonalElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.browser_download_media), style = MaterialTheme.typography.titleSmall)
                            Text(activeTab.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        FilledTonalButton(onClick = { onSendToFormat(activeTab.url) }) { Text("Download") }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        settings.apply {
                            javaScriptEnabled = preferences.javascriptEnabled
                            domStorageEnabled = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportMultipleWindows(false)
                            allowFileAccess = true
                            allowContentAccess = true
                            mediaPlaybackRequiresUserGesture = false
                            if (preferences.desktopMode) userAgentString = DESKTOP_UA
                        }
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) { progress = newProgress }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                urlInput = url ?: ""
                                viewModel.updateActiveUrl(url ?: "")
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                urlInput = url ?: ""
                                viewModel.updateActive(url ?: "", view?.title ?: url ?: "")
                                canGoBack = view?.canGoBack() ?: false
                                canGoForward = view?.canGoForward() ?: false
                                if (url != null && UrlUtils.looksLikeMediaUrl(url)) mediaDetectedUrl = url
                            }
                        }
                        if (initialUrl != null) loadUrl(initialUrl)
                        else if (activeTab.url.isNotBlank()) loadUrl(activeTab.url)
                        webView = this
                    }
                },
                update = { wv ->
                    wv.settings.javaScriptEnabled = preferences.javascriptEnabled
                    wv.settings.userAgentString = if (preferences.desktopMode) DESKTOP_UA else null
                }
            )

            if (activeTab.url.isBlank()) {
                BrowserHome(
                    bookmarks = bookmarks.map { it.title to it.url },
                    onPick = { url -> webView?.loadUrl(url) },
                    onRemove = { id -> viewModel.removeBookmark(id) }
                )
            }
        }
    }

    if (showSheet) {
        val coroutineScope = rememberCoroutineScope()
        BrowserSheet(
            tabs = tabs,
            activeId = activeTabId,
            onSelect = { viewModel.selectTab(it) },
            onClose = { viewModel.closeTab(it) },
            onNew = { viewModel.newTab() },
            onCopyUrl = {
                val cm = context.getSystemService(android.content.ClipboardManager::class.java)
                cm?.setPrimaryClip(android.content.ClipData.newPlainText("url", activeTab.url))
            },
            onOpenExternal = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeTab.url))
                context.startActivity(intent)
            },
            onSendToDownloader = { onSendToFormat(activeTab.url); showSheet = false },
            onDesktopModeToggle = { coroutineScope.launch { prefs.setDesktopMode(it) } },
            onJavascriptToggle = { coroutineScope.launch { prefs.setJavascript(it) } },
            desktopMode = preferences.desktopMode,
            javascriptEnabled = preferences.javascriptEnabled,
            onDismiss = { showSheet = false }
        )
    }
}

private const val DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

@Composable
private fun BrowserHome(bookmarks: List<Pair<String, String>>, onPick: (String) -> Unit, onRemove: (Long) -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Quick links", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            val quick = listOf(
                "YouTube" to "https://m.youtube.com",
                "SoundCloud" to "https://soundcloud.com/discover",
                "Vimeo" to "https://vimeo.com",
                "Google" to "https://www.google.com",
                "DuckDuckGo" to "https://duckduckgo.com",
                "Reddit" to "https://www.reddit.com"
            )
            items(quick.chunked(2)) { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { (name, url) ->
                        Card(
                            modifier = Modifier.weight(1f).clickable { onPick(url) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(url.removePrefix("https://"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            item { Text("Bookmarks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            if (bookmarks.isEmpty()) {
                item { Text("No bookmarks yet", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(bookmarks) { (title, url) ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { onPick(url) }, shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(title.ifBlank { url }, style = MaterialTheme.typography.titleSmall)
                            Text(url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserSheet(
    tabs: List<BrowserTab>,
    activeId: String,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onNew: () -> Unit,
    onCopyUrl: () -> Unit,
    onOpenExternal: () -> Unit,
    onSendToDownloader: () -> Unit,
    onDesktopModeToggle: (Boolean) -> Unit,
    onJavascriptToggle: (Boolean) -> Unit,
    desktopMode: Boolean,
    javascriptEnabled: Boolean,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tabs (${tabs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                items(tabs, key = { it.id }) { t ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        RadioButton(selected = t.id == activeId, onClick = { onSelect(t.id) })
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.title.ifBlank { t.url.ifBlank { "New tab" } }, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                            Text(t.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        IconButton(onClick = { onClose(t.id) }) { Icon(Icons.Outlined.Close, contentDescription = null) }
                    }
                }
            }
            FilledTonalButton(onClick = onNew, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.browser_new_tab))
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            ListItem(headlineContent = { Text(stringResource(R.string.browser_copy_url)) }, leadingContent = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) }, modifier = Modifier.clickable { onCopyUrl() })
            ListItem(headlineContent = { Text(stringResource(R.string.browser_open_external)) }, leadingContent = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) }, modifier = Modifier.clickable { onOpenExternal() })
            ListItem(headlineContent = { Text(stringResource(R.string.browser_send_to_downloader)) }, leadingContent = { Icon(Icons.Outlined.Download, contentDescription = null) }, modifier = Modifier.clickable { onSendToDownloader() })
            ListItem(
                headlineContent = { Text(stringResource(R.string.browser_desktop_mode)) },
                leadingContent = { Icon(Icons.Outlined.DesktopMac, contentDescription = null) },
                trailingContent = { Switch(checked = desktopMode, onCheckedChange = { onDesktopModeToggle(it) }) }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.browser_javascript)) },
                leadingContent = { Icon(Icons.Outlined.Code, contentDescription = null) },
                trailingContent = { Switch(checked = javascriptEnabled, onCheckedChange = { onJavascriptToggle(it) }) }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
