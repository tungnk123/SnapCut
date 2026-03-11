package com.tungnk123.snapcut.feature.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tungnk123.snapcut.core.bitmap.BitmapProcessor
import com.tungnk123.snapcut.core.ml.SubjectSegmentationManager
import com.tungnk123.snapcut.data.repository.StickerRepository
import com.tungnk123.snapcut.di.DefaultDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

private const val TAG = "SnapCut.Editor"

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
    @ApplicationContext private val context: Context,
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
        Log.d(TAG, "loadImage: $uri")
        viewModelScope.launch {
            bitmapProcessor.decodeBitmap(uri)
                .onSuccess { bitmap ->
                    Log.d(TAG, "Image decoded: ${bitmap.width}x${bitmap.height} config=${bitmap.config}")
                    _uiState.update { EditorUiState.ImageLoaded(bitmap) }
                    // Auto-segment at center immediately after load (test mode)
                    onLongPressDetected(bitmap.width / 2f, bitmap.height / 2f)
                }
                .onFailure { e ->
                    Log.e(TAG, "decodeBitmap failed: ${e.message}", e)
                    _uiState.update { EditorUiState.Error("Failed to load image: ${e.message}") }
                }
        }
    }

    /**
     * [tapImageX] / [tapImageY]: long-press position mapped to image pixel coordinates.
     * Used to select the specific subject the user tapped on when multiple subjects exist.
     */
    fun onLongPressDetected(tapImageX: Float, tapImageY: Float) {
        val current = _uiState.value
        Log.d(TAG, "onLongPressDetected: tap=(${tapImageX},${tapImageY}) state=${current::class.simpleName}")
        if (current !is EditorUiState.ImageLoaded) return

        viewModelScope.launch {
            _uiState.update { EditorUiState.Segmenting }
            segmentationManager.segmentSubject(current.sourceBitmap)
                .onSuccess { result ->
                    Log.d(TAG, "Segmentation success: foreground=${result.foregroundBitmap.width}x${result.foregroundBitmap.height}")
                    val outlinePath = withContext(defaultDispatcher) {
                        bitmapProcessor.extractOutlinePath(result)
                    }
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
                    Log.e(TAG, "Segmentation failed: ${e.javaClass.simpleName}: ${e.message}", e)
                    _uiState.update { EditorUiState.ImageLoaded(current.sourceBitmap) }
                    _events.send(EditorEvent.ShowMessage(
                        e.message ?: "No subject found. Try long-pressing on a person or object."
                    ))
                }
        }
    }

    fun copySubjectToClipboard() {
        val current = _uiState.value as? EditorUiState.SubjectLifted ?: return
        viewModelScope.launch {
            val fileName = "copy_${System.currentTimeMillis()}"
            bitmapProcessor.saveCutBitmapToFile(current.subjectBitmap, fileName)
                .onSuccess { path ->
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(path)
                    )
                    val clip = ClipData.newUri(context.contentResolver, "Copied subject", uri)
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(clip)
                    _events.send(EditorEvent.ShowMessage("Copied!"))
                }
                .onFailure {
                    _events.send(EditorEvent.ShowMessage("Failed to copy."))
                }
        }
    }

    fun shareSubject() {
        val current = _uiState.value as? EditorUiState.SubjectLifted ?: return
        viewModelScope.launch {
            val fileName = "share_${System.currentTimeMillis()}"
            bitmapProcessor.saveCutBitmapToFile(current.subjectBitmap, fileName)
                .onSuccess { path ->
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(path)
                    )
                    _events.send(EditorEvent.SubjectShared(uri))
                }
                .onFailure {
                    _events.send(EditorEvent.ShowMessage("Failed to share."))
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

    // Do NOT close segmentationManager here — it is @Singleton and shared across
    // all EditorViewModel instances. Closing it in onCleared() permanently kills
    // the MLKit segmenter for the app's lifetime until cache is cleared.
}
