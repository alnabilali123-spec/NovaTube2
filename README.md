# NovaTube

A full-featured Android media downloader, browser, and player. NovaTube wraps
**yt-dlp**, **FFmpeg**, and **Media3 ExoPlayer** behind a Material 3 / Jetpack
Compose UI with full Arabic + English support.

> NovaTube is an original project. It is not affiliated with, endorsed by, or
> sponsored by any third-party media platform. The bundled yt-dlp engine
> supports a wide range of public sites; please respect each site's terms of
> service and copyright law in your jurisdiction.

## Features

### Downloader
- `yt-dlp` integration via [`youtubedl-android`](https://github.com/yausername/youtubedl-android)
  (real media extraction — no fakes).
- `-J` JSON metadata extraction → `MediaInfo` / `MediaFormat` data classes.
- Format selection UI with separate video and audio tracks.
- Audio-only extraction to MP3 / M4A / Opus / WAV via FFmpeg.
- Background downloads via `WorkManager` with a foreground service for the
  active notification.
- Live progress streamed from the worker to the database and the UI.
- File saved to app-specific external storage at
  `getExternalFilesDir(...)/NovaTube`.

### Player
- Media3 / ExoPlayer with hardware extension renderers and OkHttp data source.
- Gesture controls: double-tap skip ±10s, vertical drag for brightness/volume,
  horizontal drag for seek.
- 0.25× – 2× playback speed, slider-based seek, fullscreen toggle, Picture-in-Picture.
- Plays both local files and remote streams (HLS, DASH, MP4, WebM, MP3…).
- Background music playback via `MediaSessionService` (lock-screen + notification
  controls).

### Browser
- Full WebView with multiple tabs, bookmarks, history, and back/forward/reload.
- Home page with quick links, desktop-mode toggle, JavaScript toggle, dark mode.
- Detects media URLs in the current page and shows a "Download media" banner.
- Share current URL, copy URL, open externally, "Send URL to Downloader".
- Receives `ACTION_SEND text/plain` and `ACTION_VIEW` intents from other apps.

### Search
- `ytsearch:` / `scsearch:` backends via yt-dlp.
- Live suggestions while typing, recent searches (Room), trending list.
- Filter by Video / Audio / Playlist; sort by relevance / date / views.
- Fallback: opens the browser with the query for platforms that don't expose an
  internal search API.

### Library
- Browses the local download directory.
- Plays, renames, shares, deletes each item.
- Two-tab layout (Audio / Video).

### Home & Settings
- Hero, quick actions, mini download-status panel, trending carousel, recent
  downloads, recommended media, popular platforms grid.
- Settings: theme (system / light / dark), preferred video quality, preferred
  audio format, Wi-Fi-only, clipboard detection, history clearing,
  yt-dlp & FFmpeg version display, in-app update check.

## Tech stack

- **Language:** Kotlin 1.9.24
- **UI:** Jetpack Compose (BOM 2024.06), Material 3, Navigation Compose
- **Architecture:** MVVM (Compose + ViewModel + Flow)
- **Persistence:** Room 2.6.1 (with KSP), DataStore Preferences 1.1
- **Background work:** WorkManager 2.9 + foreground Service
- **Media:** AndroidX Media3 (ExoPlayer, UI, Session, OkHttp data source)
- **Networking:** OkHttp 4.12
- **Engine:** youtubedl-android 0.15.0 (yt-dlp + bundled FFmpeg)
- **Image loading:** Coil 2.6.0
- **JSON:** Gson 2.11.0

## Project layout

```
NovaTube/
├── build.gradle.kts                 # Root plugin pins
├── settings.gradle.kts              # Repos + module include
├── gradle.properties
├── gradle/wrapper/                  # Gradle wrapper (jar fetched by scripts/fetch-wrapper.sh)
├── gradlew, gradlew.bat
├── scripts/fetch-wrapper.sh
└── app/
    ├── build.gradle.kts             # App module (deps, sdk, build types)
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/novatube/app/
        │   ├── NovaTubeApp.kt                # Application — inits yt-dlp + FFmpeg
        │   ├── MainActivity.kt               # Single Activity, share intent handling
        │   ├── nav/Routes.kt                 # Compose nav routes
        │   ├── data/
        │   │   ├── db/AppDatabase.kt         # Room database
        │   │   ├── dao/                      # DAOs for downloads, history, etc.
        │   │   ├── entity/                   # Room entities
        │   │   ├── model/                    # MediaInfo, MediaFormat, SearchResult
        │   │   ├── prefs/PreferencesRepository.kt
        │   │   └── repository/DownloadRepository.kt
        │   ├── download/
        │   │   ├── DownloadManager.kt        # Wraps yt-dlp execute with progress parsing
        │   │   └── DownloadWorker.kt         # CoroutineWorker + ForegroundInfo
        │   ├── extractor/
        │   │   ├── MediaExtractor.kt         # `yt-dlp -J`
        │   │   └── SearchEngine.kt           # ytsearch / scsearch
        │   ├── player/
        │   │   ├── video/VideoPlayerHolder.kt
        │   │   └── music/MusicPlaybackService.kt
        │   ├── service/DownloadService.kt    # Foreground service aggregator
        │   ├── ui/
        │   │   ├── theme/                    # Material 3 theme + colors + type
        │   │   ├── components/Cards.kt       # Reusable card / row / chip
        │   │   └── screens/
        │   │       ├── home/HomeScreen.kt
        │   │       ├── search/SearchScreen.kt
        │   │       ├── format/FormatSelectionScreen.kt
        │   │       ├── downloads/DownloadsScreen.kt
        │   │       ├── library/LibraryScreen.kt
        │   │       ├── player/PlayerScreen.kt
        │   │       ├── browser/BrowserScreen.kt
        │   │       ├── music/MusicScreen.kt
        │   │       ├── playlists/PlaylistsScreen.kt
        │   │       ├── history/HistoryScreen.kt
        │   │       └── settings/SettingsScreen.kt
        │   ├── util/                         # FileUtils, UrlUtils, OkHttpProvider…
        │   └── viewmodel/                    # Compose ViewModels
        └── res/
            ├── values/strings.xml            # English strings
            ├── values-ar/strings.xml         # Arabic strings
            ├── values/colors.xml, themes.xml
            ├── values-night/themes.xml
            ├── drawable/                     # Vector launcher art
            ├── mipmap-anydpi-v26/            # Adaptive launcher icons
            └── xml/                          # FileProvider paths, backup rules
```

## Build

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17 (`org.gradle.java.installations.auto-download=true` in `gradle.properties` will let Gradle fetch one if needed)
- Internet access on first build (to download the Gradle distribution and Maven dependencies, including yt-dlp native binaries)

### Steps
```bash
# 1. Fetch the gradle wrapper jar (only needed once)
./scripts/fetch-wrapper.sh

# 2. Build a debug APK
./gradlew assembleDebug

# 3. Install on a device
./gradlew installDebug
```

The resulting APK is at `app/build/outputs/apk/debug/app-debug.apk`.

### Common Gradle tasks
```bash
./gradlew clean                       # Clean build outputs
./gradlew assembleDebug               # Build debug APK
./gradlew assembleRelease             # Build release APK (unsigned)
./gradlew lintDebug                   # Run lint on debug variant
./gradlew :app:dependencies           # Inspect resolved dependencies
```

## Runtime notes

- **First launch** downloads yt-dlp + FFmpeg binaries into the app's private
  storage (handled by `youtubedl-android`). The Application class awaits init
  before the UI starts processing any URL.
- **Downloads** land in
  `Android/data/com.novatube.app/files/Movies/NovaTube` (videos) and
  `…/Music/NovaTube` (audio). The exact location is platform-specific; the
  app uses `getExternalFilesDir(...)` so no extra permission is required on
  Android 10+ scoped storage.
- **Foreground service** runs while a download is in progress so the OS keeps
  the worker alive; on completion or failure the service is torn down.
- **Picture-in-Picture** requires the device to support it; the option is
  hidden gracefully on older devices.
- **Chromecast** support is not bundled in this build (no Google Cast SDK
  dependency) but the player uses standard Media3 APIs that work with any
  external cast integration. To add Chromecast, add the
  `androidx.media3:media3-cast` dependency and a `CastPlayer` instance.
- **RTL** is fully supported (Arabic strings + Material 3 mirroring).
  Force RTL from developer options to preview the Arabic layout.

## Permissions

| Permission | Why |
| --- | --- |
| `INTERNET` | WebView, OkHttp, yt-dlp HTTP fetches |
| `ACCESS_NETWORK_STATE` / `ACCESS_WIFI_STATE` | "Wi-Fi only" setting + network awareness |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` | Long-running downloads |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Music playback in background |
| `POST_NOTIFICATIONS` | Download / music / player notifications (Android 13+) |
| `WAKE_LOCK` | Keep the screen on during playback (via ExoPlayer) |
| `READ_CLIPBOARD` (Android 12-) | Detect a media URL on the clipboard |
| `PICTURE_IN_PICTURE` | PiP support |
| `VIBRATE` | Future use (haptics) |
| `RECEIVE_BOOT_COMPLETED` | Reserved for future scheduled work |

## Acknowledgements

- [youtubedl-android](https://github.com/yausername/youtubedl-android) for
  packaging yt-dlp + FFmpeg as an Android library.
- [yt-dlp](https://github.com/yt-dlp/yt-dlp) for the actual extraction logic.
- [FFmpeg](https://ffmpeg.org/) for transcoding and audio extraction.
- [AndroidX Media3](https://developer.android.com/media/media3) for the
  modern playback stack.

## License

This source code in this repository is released under the MIT License. Note
that the bundled engine (yt-dlp) is Unlicense and FFmpeg is LGPL — see the
respective upstream projects for full license texts.
