package com.novatube.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novatube.app.NovaTubeApp
import com.novatube.app.data.model.MediaInfo
import com.novatube.app.extractor.MediaExtractor
import com.novatube.app.extractor.MediaExtractionException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FormatSelectionViewModel(app: Application) : AndroidViewModel(app) {

    private val novaApp: NovaTubeApp = app as NovaTubeApp
    private val extractor = MediaExtractor()

    private val _state = MutableStateFlow(FormatUiState())
    val state: StateFlow<FormatUiState> = _state

    fun load(url: String) {
        if (url.isBlank()) return
        _state.value = FormatUiState(loading = true, url = url)
        viewModelScope.launch {
            try {
                val info = extractor.extract(novaApp, url)
                val videoFormats = info.formats?.filter { it.isVideo }
                    ?.sortedWith(
                        compareByDescending<com.novatube.app.data.model.MediaFormat> { it.height ?: 0 }
                            .thenByDescending { it.tbr ?: 0.0 }
                    )
                    ?: emptyList()
                val audioFormats = info.formats?.filter { it.isAudio }
                    ?.sortedByDescending { it.abr ?: 0.0 }
                    ?: emptyList()
                _state.value = FormatUiState(
                    loading = false,
                    url = url,
                    mediaInfo = info,
                    videoFormats = videoFormats,
                    audioFormats = audioFormats,
                    error = null
                )
            } catch (e: MediaExtractionException) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Failed to extract")
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Unexpected error")
            }
        }
    }
}

data class FormatUiState(
    val loading: Boolean = false,
    val url: String = "",
    val mediaInfo: MediaInfo? = null,
    val videoFormats: List<com.novatube.app.data.model.MediaFormat> = emptyList(),
    val audioFormats: List<com.novatube.app.data.model.MediaFormat> = emptyList(),
    val error: String? = null
)
