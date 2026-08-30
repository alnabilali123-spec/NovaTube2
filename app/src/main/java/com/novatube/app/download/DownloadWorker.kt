package com.novatube.app.download

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.novatube.app.NovaTubeApp
import com.novatube.app.R
import com.novatube.app.data.model.RequestedDownload
import com.novatube.app.service.DownloadService
import com.novatube.app.util.NotificationHelper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * Owns a single download run. Receives a [com.novatube.app.data.entity.DownloadEntity] id
 * via input data, loads it from Room, delegates to [DownloadManager] for the actual
 * yt-dlp call, and keeps Room + notification state in sync.
 *
 * The manager invokes progress callbacks from a non-suspending context (a regular
 * `useLines { ... }` lambda). We bridge that to our suspend-only DAO by funnelling
 * events through an unlimited [Channel] which is then drained by a coroutine we
 * launch here.
 */
class DownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as NovaTubeApp
        val id = inputData.getLong(KEY_DOWNLOAD_ID, -1L)
        if (id < 0) return Result.failure(workDataOf("error" to "missing id"))

        val entity = app.downloadRepository.get(id)
            ?: return Result.failure(workDataOf("error" to "no row"))
        val request = RequestedDownload(
            url = entity.webpageUrl ?: entity.url,
            formatId = entity.formatId,
            fileName = entity.fileName.substringBeforeLast('.', entity.fileName),
            isAudioOnly = entity.isAudioOnly,
            audioFormat = entity.audioFormat ?: "mp3",
            title = entity.title,
            uploader = entity.uploader,
            thumbnail = entity.thumbnail,
            duration = entity.duration,
            webpageUrl = entity.webpageUrl
        )

        setForeground(createForegroundInfo(entity.title, 0))
        app.downloadRepository.markRunning(id)

        val completedSignal = CompletableDeferred<File?>()
        val manager = DownloadManager(applicationContext)

        // Channel to bridge non-suspend progress callbacks to our coroutine context.
        val progressChannel = Channel<DownloadManager.ProgressEvent>(Channel.UNLIMITED)

        // Dedicated scope that lives for the duration of the work.
        val progressScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        val progressJob = progressScope.launch {
            for (event in progressChannel) {
                val percent = event.percent.toInt().coerceIn(0, 100)
                try {
                    app.downloadRepository.updateProgress(id, percent, event.downloadedBytes)
                } catch (t: Throwable) {
                    Log.w(TAG, "updateProgress failed", t)
                }
                try {
                    setProgress(
                        workDataOf(
                            KEY_PROGRESS to percent,
                            KEY_BYTES to event.downloadedBytes,
                            KEY_TOTAL to event.totalBytes,
                            KEY_SPEED to event.speed
                        )
                    )
                } catch (t: Throwable) {
                    Log.w(TAG, "setProgress failed", t)
                }
                try {
                    setForeground(createForegroundInfo(entity.title, percent))
                } catch (t: Throwable) {
                    Log.w(TAG, "setForeground failed", t)
                }
                DownloadService.broadcastProgress(applicationContext, id, percent, entity.title)
            }
        }

        val result = withTimeoutOrNull(6 * 60 * 60 * 1000L) {
            manager.run(
                request = request,
                listener = object : DownloadManager.ProgressListener {
                    override fun onProgress(line: DownloadManager.ProgressEvent) {
                        // Non-blocking; the channel has UNLIMITED capacity so this never
                        // suspends and never drops events.
                        progressChannel.trySend(line)
                    }
                    override fun onCompleted(file: File) {
                        completedSignal.complete(file)
                    }
                    override fun onError(message: String, cause: Throwable?) {
                        completedSignal.completeExceptionally(RuntimeException(message, cause))
                    }
                },
                shouldCancel = { isStopped }
            )
            try { completedSignal.await() } catch (e: Exception) { null }
        }

        // Tear down the progress pipeline.
        progressChannel.close()
        progressJob.join()
        progressScope.cancel()

        return if (result != null) {
            val file = result
            val size = file.length()
            app.downloadRepository.markCompleted(id, file.absolutePath, size)
            DownloadService.broadcastComplete(applicationContext, id, entity.title, file.absolutePath)
            try {
                setProgress(workDataOf(KEY_PROGRESS to 100, KEY_TOTAL to size, KEY_BYTES to size))
            } catch (_: Throwable) {}
            Result.success(workDataOf("path" to file.absolutePath, "size" to size))
        } else {
            val msg = if (isStopped) "Cancelled" else if (!app.ytDlpEngine.initialized)
                "yt-dlp engine not initialised: ${app.ytDlpEngine.lastError ?: "unknown error"}"
            else "Timed out"
            app.downloadRepository.markFailed(id, msg)
            DownloadService.broadcastFailed(applicationContext, id, entity.title, msg)
            Result.failure(workDataOf("error" to msg))
        }
    }

    private fun createForegroundInfo(title: String, percent: Int): ForegroundInfo {
        val notification = buildNotification(title, percent)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIF_ID, notification)
        }
    }

    private fun buildNotification(title: String, percent: Int): Notification {
        val cancelIntent = DownloadService.cancelIntent(applicationContext)
        val pi = android.app.PendingIntent.getService(
            applicationContext, 0, cancelIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_DOWNLOADS)
            .setContentTitle(applicationContext.getString(R.string.notif_downloading, title))
            .setContentText(applicationContext.getString(R.string.notif_progress, percent))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percent, percent == 0)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, applicationContext.getString(R.string.common_cancel), pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun workDataOf(vararg pairs: Pair<String, Any?>): Data {
        val b = Data.Builder()
        pairs.forEach { (k, v) ->
            if (v == null) b.putString(k, null) else when (v) {
                is String -> b.putString(k, v)
                is Int -> b.putInt(k, v)
                is Long -> b.putLong(k, v)
                is Float -> b.putFloat(k, v)
                is Double -> b.putDouble(k, v)
                is Boolean -> b.putBoolean(k, v)
                else -> b.putString(k, v.toString())
            }
        }
        return b.build()
    }

    companion object {
        private const val TAG = "DownloadWorker"
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_PROGRESS = "progress"
        const val KEY_BYTES = "bytes"
        const val KEY_TOTAL = "total"
        const val KEY_SPEED = "speed"
        private const val NOTIF_ID = 1001
    }
}
