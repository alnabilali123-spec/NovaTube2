package com.novatube.app.data.repository

import android.content.Context
import com.novatube.app.data.dao.DownloadDao
import com.novatube.app.data.entity.DownloadEntity
import com.novatube.app.data.entity.DownloadStatus
import com.novatube.app.data.model.RequestedDownload
import com.novatube.app.util.FileUtils
import kotlinx.coroutines.flow.Flow

class DownloadRepository(
    private val downloadDao: DownloadDao,
    private val context: Context
) {

    fun observeAll(): Flow<List<DownloadEntity>> = downloadDao.observeAll()
    fun observeActive(): Flow<List<DownloadEntity>> = downloadDao.observeActive()
    fun observeAudio(): Flow<List<DownloadEntity>> = downloadDao.observeAudioLibrary()
    fun observeVideo(): Flow<List<DownloadEntity>> = downloadDao.observeVideoLibrary()
    fun observeByStatus(status: DownloadStatus) = downloadDao.observeByStatus(status)
    fun observeById(id: Long) = downloadDao.observeById(id)

    suspend fun enqueue(request: RequestedDownload): Long {
        val sanitized = FileUtils.sanitizeFileName(request.fileName)
        val ext = if (request.isAudioOnly) request.audioFormat else guessExt(request.formatId)
        val fullName = "$sanitized.$ext"
        val targetDir = FileUtils.downloadDir(context)
        val finalPath = "$targetDir/$fullName"

        val entity = DownloadEntity(
            url = request.url,
            webpageUrl = request.webpageUrl ?: request.url,
            title = request.title ?: request.fileName,
            uploader = request.uploader,
            thumbnail = request.thumbnail,
            duration = request.duration,
            formatId = request.formatId,
            ext = ext,
            isAudioOnly = request.isAudioOnly,
            audioFormat = if (request.isAudioOnly) request.audioFormat else null,
            fileName = fullName,
            filePath = finalPath,
            fileSize = 0,
            downloadedBytes = 0,
            progress = 0,
            status = DownloadStatus.QUEUED
        )
        return downloadDao.insert(entity)
    }

    suspend fun get(id: Long): DownloadEntity? = downloadDao.getById(id)

    suspend fun markRunning(id: Long) = downloadDao.updateStatus(id, DownloadStatus.RUNNING)
    suspend fun markQueued(id: Long) = downloadDao.updateStatus(id, DownloadStatus.QUEUED)
    suspend fun markPaused(id: Long) = downloadDao.updateStatus(id, DownloadStatus.PAUSED)
    suspend fun markCancelled(id: Long) = downloadDao.updateStatus(id, DownloadStatus.CANCELLED)

    suspend fun updateProgress(id: Long, progress: Int, bytes: Long) =
        downloadDao.updateProgress(id, progress, bytes)

    suspend fun markCompleted(id: Long, filePath: String, fileSize: Long) =
        downloadDao.markCompleted(id, DownloadStatus.COMPLETED, filePath, fileSize)

    suspend fun markFailed(id: Long, error: String?) =
        downloadDao.markFailed(id, DownloadStatus.FAILED, error)

    suspend fun delete(id: Long) = downloadDao.delete(id)
    suspend fun clearCompleted() = downloadDao.clearCompleted()

    private fun guessExt(formatId: String): String {
        // yt-dlp chooses extension by format; we let the output template handle it in practice,
        // but keep a default for the database record.
        return "mp4"
    }
}
