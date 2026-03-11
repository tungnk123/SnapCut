package com.tungnk123.snapcut.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tungnk123.snapcut.data.repository.StickerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsEvent {
    data object StickersCleared : SettingsEvent
    data object ClearFailed : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val stickerRepository: StickerRepository
) : ViewModel() {

    // Channel for one-shot events (navigation, snackbar) — never SharedFlow(replay=0)
    private val _events = Channel<SettingsEvent>()
    val events = _events.receiveAsFlow()

    fun clearAllStickers() {
        viewModelScope.launch {
            stickerRepository.deleteAll().fold(
                onSuccess = { _events.send(SettingsEvent.StickersCleared) },
                onFailure = { _events.send(SettingsEvent.ClearFailed) }
            )
        }
    }
}
