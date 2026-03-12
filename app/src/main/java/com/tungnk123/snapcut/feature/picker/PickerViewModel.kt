package com.tungnk123.snapcut.feature.picker

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class GalleryImage(
    val uri: Uri,
    val dateTaken: Long,
    val displayName: String,
)

enum class GalleryFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
}

sealed class GalleryItem {
    data class Header(val label: String) : GalleryItem()
    data class Image(val image: GalleryImage) : GalleryItem()
}

data class PickerUiState(
    val groupedItems: List<GalleryItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasPermission: Boolean = false,
    val searchQuery: String = "",
    val filter: GalleryFilter = GalleryFilter.ALL,
)

@HiltViewModel
class PickerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PickerUiState())
    val uiState: StateFlow<PickerUiState> = _uiState.asStateFlow()

    private var allImages: List<GalleryImage> = emptyList()

    fun onPermissionGranted() {
        _uiState.update { it.copy(hasPermission = true) }
        if (allImages.isEmpty()) loadImages()
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(hasPermission = false) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilterAndSearch()
    }

    fun onFilterChanged(filter: GalleryFilter) {
        _uiState.update { it.copy(filter = filter) }
        applyFilterAndSearch()
    }

    private fun loadImages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            allImages = withContext(Dispatchers.IO) { queryImages() }
            applyFilterAndSearch()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun applyFilterAndSearch() {
        val query = _uiState.value.searchQuery.trim().lowercase()
        val filter = _uiState.value.filter
        val now = LocalDate.now()
        val zone = ZoneId.systemDefault()

        val filtered = allImages.filter { image ->
            val matchesSearch = query.isEmpty() || image.displayName.lowercase().contains(query)
            val imageDate = Instant.ofEpochMilli(image.dateTaken).atZone(zone).toLocalDate()
            val matchesFilter = when (filter) {
                GalleryFilter.ALL -> true
                GalleryFilter.TODAY -> imageDate == now
                GalleryFilter.THIS_WEEK -> ChronoUnit.DAYS.between(imageDate, now) in 0..6
                GalleryFilter.THIS_MONTH -> imageDate.month == now.month && imageDate.year == now.year
            }
            matchesSearch && matchesFilter
        }

        _uiState.update { it.copy(groupedItems = buildGroupedItems(filtered, now)) }
    }

    private fun buildGroupedItems(images: List<GalleryImage>, today: LocalDate): List<GalleryItem> {
        val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
        val zone = ZoneId.systemDefault()
        return images
            .groupBy { Instant.ofEpochMilli(it.dateTaken).atZone(zone).toLocalDate() }
            .entries
            .sortedByDescending { it.key }
            .flatMap { (date, imgs) ->
                val label = when {
                    date == today -> "Today"
                    date == today.minusDays(1) -> "Yesterday"
                    else -> date.format(formatter)
                }
                listOf(GalleryItem.Header(label)) + imgs.map { GalleryItem.Image(it) }
            }
    }

    private fun queryImages(): List<GalleryImage> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DISPLAY_NAME,
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        val images = mutableListOf<GalleryImage>()
        context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                images.add(
                    GalleryImage(
                        uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
                        dateTaken = cursor.getLong(dateTakenColumn),
                        displayName = cursor.getString(displayNameColumn).orEmpty(),
                    )
                )
            }
        }
        return images
    }
}
