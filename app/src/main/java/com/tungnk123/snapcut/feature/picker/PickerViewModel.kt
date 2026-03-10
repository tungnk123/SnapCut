package com.tungnk123.snapcut.feature.picker

import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

sealed interface PickerUiState {
    data object Idle : PickerUiState
    data class ImageSelected(val uri: Uri) : PickerUiState
}

@HiltViewModel
class PickerViewModel @Inject constructor() : ViewModel() {

    // Always expose immutable StateFlow — keep MutableStateFlow private
    private val _uiState = MutableStateFlow<PickerUiState>(PickerUiState.Idle)
    val uiState: StateFlow<PickerUiState> = _uiState.asStateFlow()

    fun onImageSelected(uri: Uri) {
        _uiState.update { PickerUiState.ImageSelected(uri) }
    }

    fun onSelectionCleared() {
        _uiState.update { PickerUiState.Idle }
    }
}
