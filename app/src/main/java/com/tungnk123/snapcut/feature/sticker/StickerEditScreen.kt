package com.tungnk123.snapcut.feature.sticker

import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tungnk123.snapcut.core.bitmap.StickerEditParams
import kotlin.math.ceil

private val colorPresets = listOf(
    0xFFFFFFFF.toInt(),
    0xFF000000.toInt(),
    0xFFFFD700.toInt(),
    0xFFFF5722.toInt(),
    0xFFE91E63.toInt(),
    0xFF9C27B0.toInt(),
    0xFF3F51B5.toInt(),
    0xFF2196F3.toInt(),
    0xFF00BCD4.toInt(),
    0xFF4CAF50.toInt(),
    0xFFFF9800.toInt(),
    0xFFCDDC39.toInt(),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StickerEditScreen(
    stickerId: Long,
    cutImagePath: String,
    onBack: () -> Unit,
    viewModel: StickerEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(stickerId) { viewModel.init(stickerId, cutImagePath) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is StickerEditEvent.ShowMessage ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is StickerEditEvent.Share -> context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "image/webp"
                            putExtra(Intent.EXTRA_STREAM, event.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }, "Share sticker"
                    )
                )
                is StickerEditEvent.Deleted -> onBack()
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete sticker?") },
            text = { Text("This will permanently remove the sticker from your collection.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.delete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Sticker", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            when (val state = uiState) {
                is StickerEditUiState.Ready -> ActionBar(
                    onCopy = viewModel::copyToClipboard,
                    onShare = viewModel::share,
                    onSave = viewModel::saveToGallery,
                )
                else -> Unit
            }
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is StickerEditUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                    ContainedLoadingIndicator()
                }
            }
            is StickerEditUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(innerPadding).padding(32.dp), Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is StickerEditUiState.Ready -> {
                StickerEditContent(
                    state = state,
                    onParamsChanged = viewModel::updateParams,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun StickerEditContent(
    state: StickerEditUiState.Ready,
    onParamsChanged: (StickerEditParams) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Local copies of slider values for responsive dragging — only commit on drag end
    var params by remember(state.params) { mutableStateOf(state.params) }
    var outlineWidthSlider by remember(state.params.outlineWidth) { mutableIntStateOf(state.params.outlineWidth) }
    var tintAlphaSlider by remember(state.params.tintAlpha) { mutableIntStateOf(state.params.tintAlpha) }
    var shadowRadiusSlider by remember(state.params.shadowRadius) { mutableFloatStateOf(state.params.shadowRadius) }
    var shadowDxSlider by remember(state.params.shadowDx) { mutableFloatStateOf(state.params.shadowDx) }
    var shadowDySlider by remember(state.params.shadowDy) { mutableFloatStateOf(state.params.shadowDy) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Preview ───────────────────────────────────────────────────────────
        StickerPreviewBox(
            bitmap = state.styledBitmap,
            isLoading = state.isApplyingStyle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        )

        // ── Style cards ───────────────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Outline
            StyleCard(
                title = "Outline",
                subtitle = if (params.outlineEnabled) "Width ${outlineWidthSlider}px" else "Off",
                enabled = params.outlineEnabled,
                onEnabledChange = {
                    params = params.copy(outlineEnabled = it)
                    onParamsChanged(params)
                },
            ) {
                ColorPickerRow(
                    selectedColor = params.outlineColor,
                    onColorSelected = {
                        params = params.copy(outlineColor = it)
                        onParamsChanged(params)
                    },
                )
                Spacer(Modifier.height(4.dp))
                SliderRow(
                    label = "Width",
                    value = outlineWidthSlider.toFloat(),
                    valueRange = 8f..64f,
                    displayValue = "${outlineWidthSlider}px",
                    onValueChange = { outlineWidthSlider = it.toInt() },
                    onValueChangeFinished = {
                        params = params.copy(outlineWidth = outlineWidthSlider)
                        onParamsChanged(params)
                    },
                )
            }

            // Color Filter
            StyleCard(
                title = "Color Filter",
                subtitle = if (params.tintEnabled) "${(tintAlphaSlider / 255f * 100).toInt()}%" else "Off",
                enabled = params.tintEnabled,
                onEnabledChange = {
                    params = params.copy(tintEnabled = it)
                    onParamsChanged(params)
                },
            ) {
                ColorPickerRow(
                    selectedColor = params.tintColor,
                    onColorSelected = {
                        params = params.copy(tintColor = it)
                        onParamsChanged(params)
                    },
                )
                Spacer(Modifier.height(4.dp))
                SliderRow(
                    label = "Intensity",
                    value = tintAlphaSlider.toFloat(),
                    valueRange = 20f..220f,
                    displayValue = "${(tintAlphaSlider / 255f * 100).toInt()}%",
                    onValueChange = { tintAlphaSlider = it.toInt() },
                    onValueChangeFinished = {
                        params = params.copy(tintAlpha = tintAlphaSlider)
                        onParamsChanged(params)
                    },
                )
            }

            // Shadow
            StyleCard(
                title = "Shadow",
                subtitle = if (params.shadowEnabled) "r=${shadowRadiusSlider.toInt()} x=${shadowDxSlider.toInt()} y=${shadowDySlider.toInt()}" else "Off",
                enabled = params.shadowEnabled,
                onEnabledChange = {
                    params = params.copy(shadowEnabled = it)
                    onParamsChanged(params)
                },
            ) {
                SliderRow(
                    label = "Radius",
                    value = shadowRadiusSlider,
                    valueRange = 4f..60f,
                    displayValue = "${shadowRadiusSlider.toInt()}px",
                    onValueChange = { shadowRadiusSlider = it },
                    onValueChangeFinished = {
                        params = params.copy(shadowRadius = shadowRadiusSlider)
                        onParamsChanged(params)
                    },
                )
                SliderRow(
                    label = "X offset",
                    value = shadowDxSlider,
                    valueRange = -40f..40f,
                    displayValue = "${shadowDxSlider.toInt()}px",
                    onValueChange = { shadowDxSlider = it },
                    onValueChangeFinished = {
                        params = params.copy(shadowDx = shadowDxSlider)
                        onParamsChanged(params)
                    },
                )
                SliderRow(
                    label = "Y offset",
                    value = shadowDySlider,
                    valueRange = -20f..50f,
                    displayValue = "${shadowDySlider.toInt()}px",
                    onValueChange = { shadowDySlider = it },
                    onValueChangeFinished = {
                        params = params.copy(shadowDy = shadowDySlider)
                        onParamsChanged(params)
                    },
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StickerPreviewBox(
    bitmap: Bitmap,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .drawBehind {
                val sq = 20f
                val cols = ceil(size.width / sq).toInt() + 1
                val rows = ceil(size.height / sq).toInt() + 1
                for (row in 0..rows) for (col in 0..cols) {
                    drawRect(
                        color = if ((row + col) % 2 == 0) Color(0xFFDDDDDD) else Color(0xFFF5F5F5),
                        topLeft = Offset(col * sq, row * sq),
                        size = Size(sq, sq),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Sticker preview",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(16.dp),
        )
        if (isLoading) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
                modifier = Modifier.size(64.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ContainedLoadingIndicator(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerShape = CircleShape,
                    )
                }
            }
        }
    }
}

@Composable
private fun StyleCard(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            AnimatedVisibility(visible = enabled) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    content.invoke()
                }
            }
        }
    }
}

@Composable
private fun ColorPickerRow(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    var showCustomPicker by remember { mutableStateOf(false) }

    if (showCustomPicker) {
        ColorPickerDialog(
            initialColor = selectedColor,
            onDismiss = { showCustomPicker = false },
            onColorConfirmed = { color ->
                onColorSelected(color)
                showCustomPicker = false
            },
        )
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(colorPresets, key = { it }) { colorInt ->
            val isSelected = selectedColor == colorInt
            val swatchSize: Dp by animateDpAsState(
                targetValue = if (isSelected) 44.dp else 36.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessHigh,
                ),
                label = "swatchSize_$colorInt",
            )
            // Auto-pick checkmark color based on luminance
            val c = Color(colorInt)
            val luminance = 0.299f * c.red + 0.587f * c.green + 0.114f * c.blue
            val checkColor = if (luminance > 0.55f) Color.Black else Color.White

            Box(
                modifier = Modifier
                    .size(swatchSize)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) primary else outlineVariant,
                        shape = CircleShape,
                    )
                    .padding(if (isSelected) 4.dp else 2.dp)
                    .clip(CircleShape)
                    .background(Color(colorInt))
                    .clickable { onColorSelected(colorInt) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = checkColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        // "+" button opens custom RGB picker
        item(key = "custom_add") {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.5.dp, primary.copy(alpha = 0.6f), CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { showCustomPicker = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Custom color",
                    tint = primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ColorPickerDialog(
    initialColor: Int,
    onDismiss: () -> Unit,
    onColorConfirmed: (Int) -> Unit,
) {
    var r by remember { mutableIntStateOf(android.graphics.Color.red(initialColor)) }
    var g by remember { mutableIntStateOf(android.graphics.Color.green(initialColor)) }
    var b by remember { mutableIntStateOf(android.graphics.Color.blue(initialColor)) }
    val previewColor = Color(r / 255f, g / 255f, b / 255f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Color", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Live preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(previewColor),
                )
                ColorChannelSlider("R", r, Color.Red) { r = it }
                ColorChannelSlider("G", g, Color.Green.copy(green = 0.8f)) { g = it }
                ColorChannelSlider("B", b, Color.Blue) { b = it }
            }
        },
        confirmButton = {
            Button(onClick = {
                onColorConfirmed(android.graphics.Color.rgb(r, g, b))
            }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ColorChannelSlider(
    label: String,
    value: Int,
    trackColor: Color,
    onValueChange: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = trackColor,
            modifier = Modifier.width(20.dp),
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = trackColor,
                activeTrackColor = trackColor,
                inactiveTrackColor = trackColor.copy(alpha = 0.24f),
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            value.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp).padding(start = 6.dp),
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp),
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = displayValue,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp).padding(start = 8.dp),
        )
    }
}

// Connected button group shapes
private val GroupLeadingShape = RoundedCornerShape(
    topStart = 50.dp, bottomStart = 50.dp, topEnd = 8.dp, bottomEnd = 8.dp
)
private val GroupMiddleShape = RoundedCornerShape(8.dp)
private val GroupTrailingShape = RoundedCornerShape(
    topStart = 8.dp, bottomStart = 8.dp, topEnd = 50.dp, bottomEnd = 50.dp
)

@Composable
private fun ActionBar(
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
) {
    // Surface prevents the button bar from blending into the scroll content below
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            FilledTonalButton(
                onClick = onCopy,
                shape = GroupLeadingShape,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Copy")
            }
            FilledTonalButton(
                onClick = onShare,
                shape = GroupMiddleShape,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.Share, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Share")
            }
            Button(
                onClick = onSave,
                shape = GroupTrailingShape,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.SaveAlt, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save")
            }
        }
    }
}
