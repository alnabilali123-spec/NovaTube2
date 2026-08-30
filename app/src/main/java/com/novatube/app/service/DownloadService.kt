package com.novatube.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.novatube.app.MainActivity
import com.novatube.app.NovaTubeApp
import com.novatube.app.R
import com.novatube.app.download.DownloadWorker
import com.novatube.app.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Foreground service that aggregates download notifications. WorkManager can promote the
 * worker itself to foreground; this service exists for: (a) starting the app context to
 * downloads, (b) broadcasting global progress events, and (c) providing system notification
 * entries that the OS can group.
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val active = mutableMapOf<Long, Active>()

    private data class Active(val title: String, var percent: Int)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.downloads_title)
                val id = intent.getLongExtra(EXTRA_ID, -1L)
                if (id >= 0) {
                    active[id] = Active(title, 0)
                    refreshNotification()
                }
            }
            ACTION_PROGRESS -> {
                val id = intent.getLongExtra(EXTRA_ID, -1L)
                val percent = intent.getIntExtra(EXTRA_PERCENT, 0)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: active[id]?.title ?: getString(R.string.downloads_title)
                if (id >= 0) {
                    active[id] = active[id]?.copy(percent = percent) ?: Active(title, percent)
                    refreshNotification()
                }
            }
            ACTION_COMPLETE -> {
                val id = intent.getLongExtra(EXTRA_ID, -1L)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.downloads_title)
                active.remove(id)
                notifyComplete(title)
                if (active.isEmpty()) stopSelf()
            }
            ACTION_FAILED -> {
                val id = intent.getLongExtra(EXTRA_ID, -1L)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.downloads_title)
                val reason = intent.getStringExtra(EXTRA_REASON) ?: ""
                active.remove(id)
                notifyFailed(title, reason)
                if (active.isEmpty()) stopSelf()
            }
            ACTION_CANCEL_ALL -> {
                scope.launch {
                    val app = applicationContext as NovaTubeApp
                    val active = app.database.downloadDao().observeActive()
                    // Best-effort: cancel running work via WorkManager
                    androidx.work.WorkManager.getInstance(applicationContext).cancelAllWorkByTag(DownloadWorker::class.java.simpleName)
                }
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun refreshNotification() {
        val count = active.size
        if (count == 0) { stopSelf(); return }
        val first = active.values.first()
        val title = if (count == 1) first.title else getString(R.string.downloads_title)
        val text = if (count == 1) getString(R.string.notif_progress, first.percent)
        else "$count active • ${first.percent}%"

        val notification: Notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_DOWNLOADS)
            .setContentTitle(getString(R.string.notif_downloading, title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent())
            .setProgress(100, first.percent, first.percent == 0)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun notifyComplete(title: String) {
        val n = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_DOWNLOADS)
            .setContentTitle(getString(R.string.notif_download_complete))
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        androidx.core.app.NotificationManagerCompat.from(this)
            .notify(("complete_$title").hashCode(), n)
    }

    private fun notifyFailed(title: String, reason: String) {
        val n = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_DOWNLOADS)
            .setContentTitle(getString(R.string.notif_download_failed))
            .setContentText("$title — $reason")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        androidx.core.app.NotificationManagerCompat.from(this)
            .notify(("failed_$title").hashCode(), n)
    }

    private fun openAppIntent(): PendingIntent {
        val i = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        private const val TAG = "DownloadService"
        private const val NOTIF_ID = 1100

        const val ACTION_START = "com.novatube.app.action.DOWNLOAD_START"
        const val ACTION_PROGRESS = "com.novatube.app.action.DOWNLOAD_PROGRESS"
        const val ACTION_COMPLETE = "com.novatube.app.action.DOWNLOAD_COMPLETE"
        const val ACTION_FAILED = "com.novatube.app.action.DOWNLOAD_FAILED"
        const val ACTION_CANCEL_ALL = "com.novatube.app.action.DOWNLOAD_CANCEL_ALL"

        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_PERCENT = "percent"
        const val EXTRA_REASON = "reason"

        fun start(context: Context, id: Long, title: String) {
            val i = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ID, id)
                putExtra(EXTRA_TITLE, title)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun broadcastProgress(context: Context, id: Long, percent: Int, title: String) {
            val i = Intent(context, DownloadService::class.java).apply {
                action = ACTION_PROGRESS
                putExtra(EXTRA_ID, id); putExtra(EXTRA_PERCENT, percent); putExtra(EXTRA_TITLE, title)
            }
            try { context.startService(i) } catch (e: Exception) { Log.w(TAG, "broadcastProgress failed", e) }
        }

        fun broadcastComplete(context: Context, id: Long, title: String, path: String) {
            val i = Intent(context, DownloadService::class.java).apply {
                action = ACTION_COMPLETE
                putExtra(EXTRA_ID, id); putExtra(EXTRA_TITLE, title); putExtra("path", path)
            }
            try { context.startService(i) } catch (e: Exception) { Log.w(TAG, "broadcastComplete failed", e) }
        }

        fun broadcastFailed(context: Context, id: Long, title: String, reason: String) {
            val i = Intent(context, DownloadService::class.java).apply {
                action = ACTION_FAILED
                putExtra(EXTRA_ID, id); putExtra(EXTRA_TITLE, title); putExtra(EXTRA_REASON, reason)
            }
            try { context.startService(i) } catch (e: Exception) { Log.w(TAG, "broadcastFailed failed", e) }
        }

        fun cancelIntent(context: Context): Intent = Intent(context, DownloadService::class.java).apply {
            action = ACTION_CANCEL_ALL
        }
    }
}
