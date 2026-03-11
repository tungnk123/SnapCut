package com.tungnk123.snapcut.feature.picker

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerScreen(
    onImagePicked: (Uri) -> Unit,
    viewModel: PickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onPermissionGranted() else viewModel.onPermissionDenied()
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.onPermissionGranted()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("SnapCut") }) }
    ) { innerPadding ->
        when {
            !uiState.hasPermission -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Storage permission is required to load images")
                }
            }

            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                GalleryContent(
                    uiState = uiState,
                    onImagePicked = onImagePicked,
                    onSearchChanged = viewModel::onSearchQueryChanged,
                    onFilterChanged = viewModel::onFilterChanged,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun GalleryContent(
    uiState: PickerUiState,
    onImagePicked: (Uri) -> Unit,
    onSearchChanged: (String) -> Unit,
    onFilterChanged: (GalleryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val context = LocalContext.current

    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Search images...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            GalleryFilter.entries.forEach { filter ->
                item(key = filter.name) {
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { onFilterChanged(filter) },
                        label = { Text(filter.label) },
                    )
                }
            }
        }

        if (uiState.groupedItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No images found", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 14.dp),
                ) {
                    items(
                        items = uiState.groupedItems,
                        key = { item ->
                            when (item) {
                                is GalleryItem.Header -> "h_${item.label}"
                                is GalleryItem.Image -> item.image.uri.toString()
                            }
                        },
                        span = { item ->
                            when (item) {
                                is GalleryItem.Header -> GridItemSpan(maxLineSpan)
                                is GalleryItem.Image -> GridItemSpan(1)
                            }
                        },
                    ) { item ->
                        when (item) {
                            is GalleryItem.Header -> {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                            is GalleryItem.Image -> {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(item.image.uri)
                                        .size(200, 200)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = item.image.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clickable { onImagePicked(item.image.uri) },
                                )
                            }
                        }
                    }
                }

                VerticalScrollbar(
                    state = gridState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun VerticalScrollbar(
    state: LazyGridState,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val totalItems by remember { derivedStateOf { state.layoutInfo.totalItemsCount } }
    val visibleCount by remember { derivedStateOf { state.layoutInfo.visibleItemsInfo.size } }
    val firstVisible by remember { derivedStateOf { state.firstVisibleItemIndex } }

    if (totalItems == 0 || visibleCount >= totalItems) return

    val thumbFraction = visibleCount.toFloat() / totalItems
    val scrollFraction = (firstVisible.toFloat() / (totalItems - visibleCount).coerceAtLeast(1)).coerceIn(0f, 1f)

    val currentScrollFraction by rememberUpdatedState(scrollFraction)
    val currentTotalItems by rememberUpdatedState(totalItems)
    val currentVisibleCount by rememberUpdatedState(visibleCount)

    BoxWithConstraints(modifier.width(6.dp)) {
        val trackPx = constraints.maxHeight.toFloat()
        val thumbPx = (trackPx * thumbFraction).coerceAtLeast(48f).coerceAtMost(trackPx)
        val maxOffsetPx = (trackPx - thumbPx).coerceAtLeast(0f)
        val thumbOffsetPx = currentScrollFraction * maxOffsetPx

        val currentMaxOffsetPx by rememberUpdatedState(maxOffsetPx)
        val currentThumbOffsetPx by rememberUpdatedState(thumbOffsetPx)

        // Track
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(3.dp)
                )
        )
        // Thumb
        Box(
            Modifier
                .width(6.dp)
                .height(with(density) { thumbPx.toDp() })
                .offset(y = with(density) { thumbOffsetPx.toDp() })
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                .pointerInput(Unit) {
                    var dragOffset = 0f
                    detectDragGestures(
                        onDragStart = { dragOffset = currentThumbOffsetPx },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset = (dragOffset + dragAmount.y).coerceIn(0f, currentMaxOffsetPx)
                            val fraction = if (currentMaxOffsetPx > 0) dragOffset / currentMaxOffsetPx else 0f
                            val targetIndex = (fraction * (currentTotalItems - currentVisibleCount)).roundToInt()
                            coroutineScope.launch { state.scrollToItem(targetIndex) }
                        }
                    )
                }
        )
    }
}
