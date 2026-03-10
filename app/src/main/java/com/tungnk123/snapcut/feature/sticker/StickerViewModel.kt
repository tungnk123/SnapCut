package com.tungnk123.snapcut.feature.sticker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tungnk123.snapcut.data.model.CutSubject
import com.tungnk123.snapcut.data.repository.StickerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface StickerUiState {
    data object Loading : StickerUiState
    data class Success(val stickers: List<CutSubject>) : StickerUiState
    data object Empty : StickerUiState
}

@HiltViewModel
class StickerViewModel @Inject constructor(
    private val stickerRepository: StickerRepository
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

    fun deleteSticker(id: Long) {
        viewModelScope.launch {
            stickerRepository.deleteCutSubject(id)
        }
    }
}
