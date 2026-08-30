package com.novatube.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "novatube_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class VideoQuality { BEST, HIGH, MEDIUM, LOW }
enum class AudioFormat { MP3, M4A, OPUS, WAV }

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val videoQuality: VideoQuality = VideoQuality.BEST,
    val audioFormat: AudioFormat = AudioFormat.MP3,
    val wifiOnly: Boolean = false,
    val clipboardDetection: Boolean = true,
    val desktopMode: Boolean = false,
    val javascriptEnabled: Boolean = true,
    val lastSharedUrl: String? = null,
    val lastDetectedClipboard: String? = null
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        val AUDIO_FORMAT = stringPreferencesKey("audio_format")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val CLIPBOARD = booleanPreferencesKey("clipboard_detection")
        val DESKTOP_MODE = booleanPreferencesKey("desktop_mode")
        val JAVASCRIPT = booleanPreferencesKey("javascript_enabled")
        val LAST_SHARED = stringPreferencesKey("last_shared_url")
        val LAST_CLIPBOARD = stringPreferencesKey("last_detected_clipboard")
    }

    val preferences: Flow<AppPreferences> = context.appDataStore.data.map { p ->
        AppPreferences(
            themeMode = runCatching { ThemeMode.valueOf(p[Keys.THEME] ?: ThemeMode.SYSTEM.name) }.getOrDefault(ThemeMode.SYSTEM),
            videoQuality = runCatching { VideoQuality.valueOf(p[Keys.VIDEO_QUALITY] ?: VideoQuality.BEST.name) }.getOrDefault(VideoQuality.BEST),
            audioFormat = runCatching { AudioFormat.valueOf(p[Keys.AUDIO_FORMAT] ?: AudioFormat.MP3.name) }.getOrDefault(AudioFormat.MP3),
            wifiOnly = p[Keys.WIFI_ONLY] ?: false,
            clipboardDetection = p[Keys.CLIPBOARD] ?: true,
            desktopMode = p[Keys.DESKTOP_MODE] ?: false,
            javascriptEnabled = p[Keys.JAVASCRIPT] ?: true,
            lastSharedUrl = p[Keys.LAST_SHARED],
            lastDetectedClipboard = p[Keys.LAST_CLIPBOARD]
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = context.appDataStore.edit { it[Keys.THEME] = mode.name }
    suspend fun setVideoQuality(q: VideoQuality) = context.appDataStore.edit { it[Keys.VIDEO_QUALITY] = q.name }
    suspend fun setAudioFormat(a: AudioFormat) = context.appDataStore.edit { it[Keys.AUDIO_FORMAT] = a.name }
    suspend fun setWifiOnly(v: Boolean) = context.appDataStore.edit { it[Keys.WIFI_ONLY] = v }
    suspend fun setClipboardDetection(v: Boolean) = context.appDataStore.edit { it[Keys.CLIPBOARD] = v }
    suspend fun setDesktopMode(v: Boolean) = context.appDataStore.edit { it[Keys.DESKTOP_MODE] = v }
    suspend fun setJavascript(v: Boolean) = context.appDataStore.edit { it[Keys.JAVASCRIPT] = v }
    suspend fun setLastSharedUrl(v: String?) = context.appDataStore.edit { if (v == null) it.remove(Keys.LAST_SHARED) else it[Keys.LAST_SHARED] = v }
    suspend fun setLastClipboardUrl(v: String?) = context.appDataStore.edit { if (v == null) it.remove(Keys.LAST_CLIPBOARD) else it[Keys.LAST_CLIPBOARD] = v }
}
