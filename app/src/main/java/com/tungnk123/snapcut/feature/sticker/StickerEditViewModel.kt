package com.tungnk123.snapcut.feature.sticker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tungnk123.snapcut.core.bitmap.BitmapProcessor
import com.tungnk123.snapcut.core.bitmap.StickerEditParams
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

sealed interface StickerEditUiState {
    data object Loading : StickerEditUiState
    data class Ready(
        val rawBitmap: Bitmap,
        val styledBitmap: Bitmap,
        val params: StickerEditParams,
        val isApplyingStyle: Boolean = false,
    ) : StickerEditUiState
    data class Error(val message: String) : StickerEditUiState
}

sealed interface StickerEditEvent {
    data class ShowMessage(val message: String) : StickerEditEvent
    data class Share(val uri: Uri) : StickerEditEvent
    data object Deleted : StickerEditEvent
}

@HiltViewModel
class StickerEditViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bitmapProcessor: BitmapProcessor,
    private val stickerRepository: StickerRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow<StickerEditUiState>(StickerEditUiState.Loading)
    val uiState: StateFlow<StickerEditUiState> = _uiState.asStateFlow()

    private val _events = Channel<StickerEditEvent>(Channel.BUFFERED)
    val events: Flow<StickerEditEvent> = _events.receiveAsFlow()

    private var stickerId = -1L

    fun init(id: Long, path: String) {
        if (stickerId == id) return
        stickerId = id
        viewModelScope.launch {
            val bitmapResult = withContext(defaultDispatcher) {
                runCatching { BitmapFactory.decodeFile(path) ?: error("Cannot decode: $path") }
            }
            bitmapResult.fold(
                onSuccess = { bitmap ->
                    val defaultParams = StickerEditParams()
                    val styled = withContext(defaultDispatcher) {
                        bitmapProcessor.applyEditParams(bitmap, defaultParams)
                    }
                    _uiState.update { StickerEditUiState.Ready(bitmap, styled, defaultParams) }
                },
                onFailure = { e ->
                    _uiState.update { StickerEditUiState.Error(e.message ?: "Failed to load sticker") }
                }
            )
        }
    }

    fun updateParams(params: StickerEditParams) {
        val current = _uiState.value as? StickerEditUiState.Ready ?: return
        if (current.params == params) return
        _uiState.update { current.copy(params = params, isApplyingStyle = true) }
        viewModelScope.launch {
            val styled = withContext(defaultDispatcher) {
                bitmapProcessor.applyEditParams(current.rawBitmap, params)
            }
            _uiState.update { s ->
                if (s is StickerEditUiState.Ready) s.copy(styledBitmap = styled, params = params, isApplyingStyle = false)
                else s
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            stickerRepository.deleteCutSubject(stickerId)
            _events.send(StickerEditEvent.Deleted)
        }
    }

    fun copyToClipboard() {
        val state = _uiState.value as? StickerEditUiState.Ready ?: return
        viewModelScope.launch {
            bitmapProcessor.saveCutBitmapToFile(state.styledBitmap, "copy_${System.currentTimeMillis()}")
                .onSuccess { path ->
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))
                    context.getSystemService(ClipboardManager::class.java)
                        ?.setPrimaryClip(ClipData.newUri(context.contentResolver, "Sticker", uri))
                    _events.send(StickerEditEvent.ShowMessage("Copied!"))
                }
                .onFailure { _events.send(StickerEditEvent.ShowMessage("Failed to copy")) }
        }
    }

    fun share() {
        val state = _uiState.value as? StickerEditUiState.Ready ?: return
        viewModelScope.launch {
            bitmapProcessor.saveCutBitmapToFile(state.styledBitmap, "share_${System.currentTimeMillis()}")
                .onSuccess { path ->
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))
                    _events.send(StickerEditEvent.Share(uri))
                }
                .onFailure { _events.send(StickerEditEvent.ShowMessage("Failed to share")) }
        }
    }

    fun saveToGallery() {
        val state = _uiState.value as? StickerEditUiState.Ready ?: return
        viewModelScope.launch {
            bitmapProcessor.saveToGallery(state.styledBitmap, "snapcut_${System.currentTimeMillis()}")
                .onSuccess { _events.send(StickerEditEvent.ShowMessage("Saved to Gallery!")) }
                .onFailure { _events.send(StickerEditEvent.ShowMessage("Failed to save")) }
        }
    }
}
