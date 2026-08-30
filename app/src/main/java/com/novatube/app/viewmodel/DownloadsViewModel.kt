package com.novatube.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.novatube.app.NovaTubeApp
import com.novatube.app.data.entity.DownloadEntity
import com.novatube.app.data.entity.DownloadStatus
import com.novatube.app.data.model.RequestedDownload
import com.novatube.app.download.DownloadWorker
import com.novatube.app.service.DownloadService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(app: Application) : AndroidViewModel(app) {

    private val nova: NovaTubeApp = app as NovaTubeApp

    val downloads: StateFlow<List<DownloadEntity>> = nova.downloadRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val active: StateFlow<List<DownloadEntity>> = nova.downloadRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun enqueueDownload(request: RequestedDownload) {
        viewModelScope.launch {
            val id = nova.downloadRepository.enqueue(request)
            startWork(id)
            DownloadService.start(getApplication(), id, request.title ?: request.fileName)
        }
    }

    private fun startWork(id: Long) {
        val data = Data.Builder().putLong(DownloadWorker.KEY_DOWNLOAD_ID, id).build()
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(data)
            .addTag(DownloadWorker::class.java.simpleName)
            .build()
        WorkManager.getInstance(getApplication()).enqueue(request)
    }

    fun retry(entity: DownloadEntity) {
        viewModelScope.launch {
            nova.downloadRepository.markQueued(entity.id)
            startWork(entity.id)
            DownloadService.start(getApplication(), entity.id, entity.title)
        }
    }

    fun cancel(entity: DownloadEntity) {
        viewModelScope.launch {
            WorkManager.getInstance(getApplication()).cancelAllWorkByTag("download_${entity.id}")
            nova.downloadRepository.markCancelled(entity.id)
        }
    }

    fun delete(entity: DownloadEntity) {
        viewModelScope.launch {
            entity.filePath?.let { java.io.File(it).delete() }
            nova.downloadRepository.delete(entity.id)
        }
    }

    fun clearCompleted() = viewModelScope.launch { nova.downloadRepository.clearCompleted() }
}
