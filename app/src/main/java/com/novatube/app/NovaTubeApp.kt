package com.novatube.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.novatube.app.data.db.AppDatabase
import com.novatube.app.data.prefs.PreferencesRepository
import com.novatube.app.data.repository.DownloadRepository
import com.novatube.app.extractor.YtDlpEngine
import com.novatube.app.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NovaTubeApp : Application() {

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(this) }

    val downloadRepository: DownloadRepository by lazy {
        DownloadRepository(
            downloadDao = database.downloadDao(),
            context = this
        )
    }

    val ytDlpEngine: YtDlpEngine = YtDlpEngine()

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
        initEngines()
    }

    private fun initEngines() {
        applicationScope.launch {
            ytDlpEngine.init(this@NovaTubeApp)
            Log.i(
                TAG,
                "yt-dlp init: ok=${ytDlpEngine.initialized} " +
                    "version=${ytDlpEngine.binaryVersion} " +
                    "err=${ytDlpEngine.lastError}"
            )
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                NotificationHelper.CHANNEL_DOWNLOADS,
                getString(R.string.notif_channel_downloads),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                NotificationHelper.CHANNEL_PLAYER,
                getString(R.string.notif_channel_player),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                NotificationHelper.CHANNEL_MUSIC,
                getString(R.string.notif_channel_music),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
        )
    }

    companion object {
        private const val TAG = "NovaTubeApp"
        @Volatile
        private var instance: NovaTubeApp? = null

        fun get(): NovaTubeApp = requireNotNull(instance) { "NovaTubeApp not yet created" }
    }
}
