package com.novatube.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novatube.app.NovaTubeApp
import com.novatube.app.data.entity.DownloadEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val nova: NovaTubeApp = app as NovaTubeApp

    val audio: StateFlow<List<DownloadEntity>> = nova.downloadRepository.observeAudio()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val video: StateFlow<List<DownloadEntity>> = nova.downloadRepository.observeVideo()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(entity: DownloadEntity) = viewModelScope.launch {
        entity.filePath?.let { File(it).delete() }
        nova.downloadRepository.delete(entity.id)
    }

    fun rename(entity: DownloadEntity, newName: String) = viewModelScope.launch {
        val old = entity.filePath?.let { File(it) } ?: return@launch
        if (!old.exists()) return@launch
        val newFile = File(old.parentFile, "$newName.${entity.ext}")
        if (old.renameTo(newFile)) {
            nova.database.downloadDao().update(entity.copy(fileName = newFile.name, filePath = newFile.absolutePath))
        }
    }
}
