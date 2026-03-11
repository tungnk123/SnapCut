package com.tungnk123.snapcut.core.animation

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

fun Modifier.shimmerOutline(
    outlinePath: Path,
    pathScale: Float,
    pathOffsetX: Float,
    pathOffsetY: Float,
    visible: Boolean,
    color: Color = Color.White
): Modifier = this.drawWithContent {
    drawContent()
    if (!visible || outlinePath.isEmpty) return@drawWithContent

    translate(left = pathOffsetX, top = pathOffsetY) {
        scale(scaleX = pathScale, scaleY = pathScale, pivot = Offset.Zero) {
            val s = 1f / pathScale

            drawPath(
                path = outlinePath,
                color = color.copy(alpha = 0.08f),
                style = Stroke(width = 20f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = outlinePath,
                color = color.copy(alpha = 0.25f),
                style = Stroke(width = 8f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = outlinePath,
                color = color,
                style = Stroke(width = 2.5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

fun computeFitTransform(
    bitmapWidth: Int,
    bitmapHeight: Int,
    boxWidth: Float,
    boxHeight: Float
): Triple<Float, Float, Float> {
    val scale = minOf(boxWidth / bitmapWidth, boxHeight / bitmapHeight)
    val offsetX = (boxWidth - bitmapWidth * scale) / 2f
    val offsetY = (boxHeight - bitmapHeight * scale) / 2f
    return Triple(scale, offsetX, offsetY)
}
