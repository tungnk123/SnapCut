package com.tungnk123.snapcut.core.animation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

/**
 * Draws an animated dashed shimmer outline around a segmented subject.
 *
 * [pathScale], [pathOffsetX], [pathOffsetY] must be computed from ContentScale.Fit
 * so the path (which is in bitmap/image space) aligns with the letterboxed image.
 *
 * Use [computeFitTransform] in the composable to get these values.
 */
@Composable
fun Modifier.shimmerOutline(
    outlinePath: Path,
    pathScale: Float,
    pathOffsetX: Float,
    pathOffsetY: Float,
    visible: Boolean,
    color: Color = Color.White,
    strokeWidth: Float = 6f,
    dashLength: Float = 20f,
    gapLength: Float = 10f
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = dashLength + gapLength,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerPhase"
    )

    return this.drawWithContent {
        drawContent()
        if (visible && !outlinePath.isEmpty) {
            // Translate to letterbox offset first, then scale from origin (Offset.Zero)
            // so that path point (0,0) maps to the top-left of the rendered image.
            translate(left = pathOffsetX, top = pathOffsetY) {
                scale(scaleX = pathScale, scaleY = pathScale, pivot = Offset.Zero) {
                    drawPath(
                        path = outlinePath,
                        color = color,
                        style = Stroke(
                            width = strokeWidth / pathScale,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = PathEffect.dashPathEffect(
                                intervals = floatArrayOf(dashLength, gapLength),
                                phase = phase
                            )
                        )
                    )
                }
            }
        }
    }
}

/**
 * Computes the uniform scale and letterbox offsets for ContentScale.Fit.
 * Returns Triple(scale, offsetX, offsetY).
 */
fun computeFitTransform(
    bitmapWidth: Int,
    bitmapHeight: Int,
    boxWidth: Float,
    boxHeight: Float
): Triple<Float, Float, Float> {
    val scaleX = boxWidth / bitmapWidth
    val scaleY = boxHeight / bitmapHeight
    val scale = minOf(scaleX, scaleY)
    val offsetX = (boxWidth - bitmapWidth * scale) / 2f
    val offsetY = (boxHeight - bitmapHeight * scale) / 2f
    return Triple(scale, offsetX, offsetY)
}
