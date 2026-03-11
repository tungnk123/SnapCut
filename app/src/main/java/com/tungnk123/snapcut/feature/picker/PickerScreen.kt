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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tungnk123.snapcut.ui.GalleryGridSkeleton
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
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

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
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("SnapCut", fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        when {
            !uiState.hasPermission -> PermissionState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )

            uiState.isLoading -> {
                GalleryGridSkeleton(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
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
private fun PermissionState(modifier: Modifier = Modifier) {
    Box(modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(88.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Text(
                "Permission required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                "Grant storage permission to browse and pick photos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        // M3 filled search bar style
        TextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Search photos…") },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    androidx.compose.material3.IconButton(onClick = { onSearchChanged("") }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 4.dp),
        ) {
            GalleryFilter.entries.forEach { filter ->
                item(key = filter.name) {
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { onFilterChanged(filter) },
                        label = {
                            Text(
                                filter.label,
                                fontWeight = if (uiState.filter == filter) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
        }

        if (uiState.groupedItems.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Image, null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        "No photos found",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Try a different search or filter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = 12.dp,
                                            end = 12.dp,
                                            top = 14.dp,
                                            bottom = 4.dp
                                        ),
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

        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))
        )
        Box(
            Modifier
                .width(6.dp)
                .height(with(density) { thumbPx.toDp() })
                .offset(y = with(density) { thumbOffsetPx.toDp() })
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                .pointerInput(Unit) {
                    var dragOffset = 0f
                    detectDragGestures(
                        onDragStart = { dragOffset = currentThumbOffsetPx },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset =
                                (dragOffset + dragAmount.y).coerceIn(0f, currentMaxOffsetPx)
                            val fraction =
                                if (currentMaxOffsetPx > 0) dragOffset / currentMaxOffsetPx else 0f
                            val targetIndex =
                                (fraction * (currentTotalItems - currentVisibleCount)).roundToInt()
                            coroutineScope.launch { state.scrollToItem(targetIndex) }
                        }
                    )
                }
        )
    }
}
