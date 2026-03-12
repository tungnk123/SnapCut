package com.tungnk123.snapcut.feature.editor.components

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tungnk123.snapcut.core.bitmap.StickerStyle
import kotlin.math.ceil

private data class StylePreset(
    val style: StickerStyle,
    val swatchColor: Color,
    val swatchBorder: Color? = null,
)

private val stylePresets = listOf(
    StylePreset(StickerStyle.NONE,          Color.Transparent,  swatchBorder = Color(0xFFCAC4D0)),
    StylePreset(StickerStyle.WHITE_OUTLINE, Color.White,        swatchBorder = Color(0xFFCAC4D0)),
    StylePreset(StickerStyle.BLACK_OUTLINE, Color(0xFF1A1A1A)),
    StylePreset(StickerStyle.GOLD_OUTLINE,  Color(0xFFFFD700)),
    StylePreset(StickerStyle.SHADOW,        Color(0xFF607D8B)),
    StylePreset(StickerStyle.RED_TINT,      Color(0xFFE53935)),
    StylePreset(StickerStyle.PURPLE_TINT,   Color(0xFF8E24AA)),
    StylePreset(StickerStyle.BLUE_TINT,     Color(0xFF1E88E5)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSheet(
    subjectBitmap: Bitmap,
    selectedStyle: StickerStyle,
    onStyleSelected: (StickerStyle) -> Unit,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val aspectRatio = subjectBitmap.width.toFloat() / subjectBitmap.height.toFloat()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Style",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(Modifier.height(12.dp))

            // ── Style picker ──────────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(stylePresets, key = { it.style.name }) { preset ->
                    StyleChip(
                        preset = preset,
                        isSelected = selectedStyle == preset.style,
                        onClick = { onStyleSelected(preset.style) },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Sticker preview ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .aspectRatio(aspectRatio.coerceIn(0.4f, 2.5f))
                    .clip(RoundedCornerShape(16.dp))
                    .drawBehind {
                        val sq = 18f
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
                    bitmap = subjectBitmap.asImageBitmap(),
                    contentDescription = "Sticker preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Action buttons ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ActionButton(icon = Icons.Default.ContentCopy, label = "Copy",  onClick = onCopy)
                ActionButton(icon = Icons.Default.Share,       label = "Share", onClick = onShare)
                ActionButton(icon = Icons.Default.Download,    label = "Save",  onClick = onSave)
            }
        }
    }
}

@Composable
private fun StyleChip(
    preset: StylePreset,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) primary
                            else (preset.swatchBorder ?: preset.swatchColor.copy(alpha = 0.4f)),
                    shape = CircleShape,
                )
                .padding(if (isSelected) 3.dp else 2.dp)
                .clip(CircleShape)
                .then(
                    if (preset.style == StickerStyle.NONE) Modifier.drawBehind {
                        val sq = size.width / 4f
                        for (row in 0..4) for (col in 0..4) {
                            drawRect(
                                color = if ((row + col) % 2 == 0) Color(0xFFDDDDDD) else Color(0xFFF5F5F5),
                                topLeft = Offset(col * sq, row * sq),
                                size = Size(sq, sq),
                            )
                        }
                    }
                    else Modifier.background(color = preset.swatchColor, shape = CircleShape)
                )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = preset.style.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.size(width = 96.dp, height = 72.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
