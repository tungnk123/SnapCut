package com.tungnk123.snapcut.feature.sticker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tungnk123.snapcut.core.bitmap.BitmapProcessor
import com.tungnk123.snapcut.data.model.CutSubject
import com.tungnk123.snapcut.data.repository.StickerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface StickerUiState {
    data object Loading : StickerUiState
    data class Success(val stickers: List<CutSubject>) : StickerUiState
    data object Empty : StickerUiState
}

sealed interface StickerEvent {
    data class Share(val uri: Uri) : StickerEvent
    data class ShowMessage(val message: String) : StickerEvent
}

@HiltViewModel
class StickerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stickerRepository: StickerRepository,
    private val bitmapProcessor: BitmapProcessor,
) : ViewModel() {

    val uiState: StateFlow<StickerUiState> = stickerRepository.observeHistory()
        .map { stickers ->
            if (stickers.isEmpty()) StickerUiState.Empty
            else StickerUiState.Success(stickers)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = StickerUiState.Loading
        )

    private val _events = Channel<StickerEvent>()
    val events = _events.receiveAsFlow()

    fun deleteSticker(id: Long) {
        viewModelScope.launch {
            stickerRepository.deleteCutSubject(id)
        }
    }

    fun shareSticker(sticker: CutSubject) {
        viewModelScope.launch {
            runCatching {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    File(sticker.cutImagePath)
                )
                _events.send(StickerEvent.Share(uri))
            }.onFailure {
                _events.send(StickerEvent.ShowMessage("Failed to share sticker."))
            }
        }
    }

    fun copyToClipboard(sticker: CutSubject) {
        viewModelScope.launch {
            runCatching {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    File(sticker.cutImagePath)
                )
                val clip = ClipData.newUri(context.contentResolver, "Sticker", uri)
                context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip)
                _events.send(StickerEvent.ShowMessage("Copied to clipboard!"))
            }.onFailure {
                _events.send(StickerEvent.ShowMessage("Failed to copy."))
            }
        }
    }

    fun saveToGallery(sticker: CutSubject) {
        viewModelScope.launch {
            val bitmap = BitmapFactory.decodeFile(sticker.cutImagePath)
            if (bitmap == null) {
                _events.send(StickerEvent.ShowMessage("Failed to read sticker file."))
                return@launch
            }
            bitmapProcessor.saveToGallery(bitmap, "snapcut_${System.currentTimeMillis()}")
                .onSuccess { _events.send(StickerEvent.ShowMessage("Saved to Gallery!")) }
                .onFailure { _events.send(StickerEvent.ShowMessage("Failed to save to gallery.")) }
        }
    }
}
