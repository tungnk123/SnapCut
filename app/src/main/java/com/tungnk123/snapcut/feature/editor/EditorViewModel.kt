package com.tungnk123.snapcut.feature.editor

import android.graphics.Bitmap
import android.graphics.Path
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tungnk123.snapcut.core.bitmap.BitmapProcessor
import com.tungnk123.snapcut.core.ml.SubjectSegmentationManager
import com.tungnk123.snapcut.data.repository.StickerRepository
import com.tungnk123.snapcut.di.DefaultDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EditorUiState {
    data object Idle : EditorUiState
    data class ImageLoaded(val sourceBitmap: Bitmap) : EditorUiState
    data object Segmenting : EditorUiState
    data class SubjectLifted(
        val sourceBitmap: Bitmap,
        val subjectBitmap: Bitmap,
        val outlinePath: android.graphics.Path,
        val isLifted: Boolean = true
    ) : EditorUiState
    data class Error(val message: String) : EditorUiState
}

sealed interface EditorEvent {
    data class ShowMessage(val message: String) : EditorEvent
    data object SubjectSaved : EditorEvent
    data class SubjectShared(val uri: Uri) : EditorEvent
}

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val bitmapProcessor: BitmapProcessor,
    private val segmentationManager: SubjectSegmentationManager,
    private val stickerRepository: StickerRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditorUiState>(EditorUiState.Idle)
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<EditorEvent>(Channel.BUFFERED)
    val events: Flow<EditorEvent> = _events.receiveAsFlow()

    fun loadImage(uri: Uri) {
        viewModelScope.launch {
            bitmapProcessor.decodeBitmap(uri)
                .onSuccess { bitmap ->
                    _uiState.update { EditorUiState.ImageLoaded(bitmap) }
                }
                .onFailure { e ->
                    _uiState.update { EditorUiState.Error("Failed to load image: ${e.message}") }
                }
        }
    }

    fun onLongPressDetected() {
        val current = _uiState.value
        if (current !is EditorUiState.ImageLoaded) return

        viewModelScope.launch {
            _uiState.update { EditorUiState.Segmenting }
            segmentationManager.segmentSubject(current.sourceBitmap)
                .onSuccess { result ->
                    val outlinePath = bitmapProcessor.extractOutlinePath(result)
                    _uiState.update {
                        EditorUiState.SubjectLifted(
                            sourceBitmap = current.sourceBitmap,
                            subjectBitmap = result.foregroundBitmap,
                            outlinePath = outlinePath,
                            isLifted = true
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { EditorUiState.ImageLoaded(current.sourceBitmap) }
                    _events.send(EditorEvent.ShowMessage("No subject found. Try a different area."))
                }
        }
    }

    fun saveSubject() {
        val current = _uiState.value as? EditorUiState.SubjectLifted ?: return
        viewModelScope.launch {
            val fileName = "cut_${System.currentTimeMillis()}"
            bitmapProcessor.saveCutBitmapToFile(current.subjectBitmap, fileName)
                .onSuccess { path ->
                    // Save URI as placeholder — actual source URI needs to be tracked
                    _events.send(EditorEvent.SubjectSaved)
                    _events.send(EditorEvent.ShowMessage("Saved to gallery!"))
                }
                .onFailure {
                    _events.send(EditorEvent.ShowMessage("Failed to save."))
                }
        }
    }

    fun saveToGallery() {
        val current = _uiState.value as? EditorUiState.SubjectLifted ?: return
        viewModelScope.launch {
            bitmapProcessor.saveToGallery(current.subjectBitmap, "snapcut_${System.currentTimeMillis()}")
                .onSuccess {
                    _events.send(EditorEvent.ShowMessage("Saved to Gallery!"))
                }
                .onFailure {
                    _events.send(EditorEvent.ShowMessage("Failed to save to gallery."))
                }
        }
    }

    fun onLiftToggled() {
        _uiState.update { state ->
            if (state is EditorUiState.SubjectLifted) {
                state.copy(isLifted = !state.isLifted)
            } else state
        }
    }

    override fun onCleared() {
        super.onCleared()
        segmentationManager.close()
    }
}
