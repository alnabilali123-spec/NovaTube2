package com.novatube.app.download

import android.content.Context
import android.util.Log
import com.novatube.app.data.model.RequestedDownload
import com.novatube.app.extractor.YtDlpEngine
import com.novatube.app.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern

/**
 * Owns a single yt-dlp download at a time. The worker thread of [com.novatube.app.download.DownloadWorker]
 * delegates here for the actual blocking I/O, then we stream progress events to the supplied [ProgressListener].
 */
class DownloadManager(private val context: Context) {

    interface ProgressListener {
        fun onProgress(line: ProgressEvent)
        fun onCompleted(file: File)
        fun onError(message: String, cause: Throwable?)
    }

    data class ProgressEvent(
        val percent: Float,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speed: Long,
        val eta: Long,
        val line: String
    )

    private val engine = YtDlpEngine()

    suspend fun run(
        request: RequestedDownload,
        listener: ProgressListener,
        shouldCancel: () -> Boolean = { false }
    ) = withContext(Dispatchers.IO) {
        if (!engine.initialized) engine.init(context)
        if (!engine.initialized) {
            listener.onError(engine.lastError ?: "yt-dlp not ready", null)
            return@withContext
        }
        val targetDir = FileUtils.downloadDir(context)
        targetDir.mkdirs()
        val sanitized = FileUtils.sanitizeFileName(request.fileName)
        val outputTemplate = "${targetDir.absolutePath}/$sanitized.%(ext)s"

        Log.i(TAG, "Starting yt-dlp for ${request.url} (format=${request.formatId}, audio=${request.isAudioOnly})")

        try {
            val file = engine.download(
                url = request.url,
                formatId = request.formatId,
                outputTemplate = outputTemplate,
                audioOnly = request.isAudioOnly,
                audioFormat = request.audioFormat,
                progress = { line ->
                    val event = parseProgress(line)
                    if (event != null) listener.onProgress(event)
                }
            )
            if (shouldCancel()) {
                listener.onError("Cancelled", null)
                return@withContext
            }
            if (file != null && file.exists() && file.length() > 0) {
                listener.onCompleted(file)
            } else {
                listener.onError("yt-dlp did not produce an output file", null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download exception", e)
            listener.onError(e.message ?: "Unknown error", e)
        }
    }

    private fun parseProgress(line: String): ProgressEvent? {
        if (line.isBlank()) return null
        val matcher = PROGRESS_PATTERN.matcher(line)
        if (!matcher.find()) return null
        val percent = matcher.group(1)?.toFloatOrNull() ?: return null
        val total = matcher.group(2)?.parseSize() ?: 0L
        val speed = matcher.group(3)?.parseSpeed() ?: 0L
        val eta = matcher.group(4)?.toLongOrNull() ?: 0L
        return ProgressEvent(
            percent = percent,
            downloadedBytes = (percent / 100f * total).toLong(),
            totalBytes = total,
            speed = speed,
            eta = eta,
            line = line
        )
    }

    companion object {
        private const val TAG = "DownloadManager"

        // Matches lines like:
        //   [download]  45.2% of  120.50MiB at    2.50MiB/s ETA 00:30
        private val PROGRESS_PATTERN: Pattern =
            Pattern.compile("\\[download\\]\\s+([0-9.]+)%\\s+of\\s+([0-9.]+\\s*\\S+)(?:\\s+at\\s+([0-9.]+\\s*\\S+))?(?:\\s+ETA\\s+([0-9:]+))?")

        private fun String.parseSize(): Long? {
            val parts = trim().split(" ")
            if (parts.size != 2) return null
            val value = parts[0].toDoubleOrNull() ?: return null
            return when (parts[1].lowercase()) {
                "b" -> value.toLong()
                "kb", "kib" -> (value * 1024).toLong()
                "mb", "mib" -> (value * 1024 * 1024).toLong()
                "gb", "gib" -> (value * 1024 * 1024 * 1024).toLong()
                else -> null
            }
        }

        private fun String.parseSpeed(): Long? = this.parseSize()
    }
}
