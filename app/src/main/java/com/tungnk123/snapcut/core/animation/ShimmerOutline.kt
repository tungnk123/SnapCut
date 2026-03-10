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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.toArgb

/**
 * Draws an animated dashed shimmer outline around a segmented subject.
 *
 * The outline path must be in image space. [scaleX] and [scaleY] map it
 * from image coordinates to composable display coordinates.
 *
 * Usage:
 * ```
 * Image(
 *     modifier = Modifier.shimmerOutline(outlinePath, scaleX, scaleY, visible = isSegmented)
 * )
 * ```
 */
@Composable
fun Modifier.shimmerOutline(
    outlinePath: Path,
    scaleX: Float,
    scaleY: Float,
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
            scale(scaleX = scaleX, scaleY = scaleY) {
                drawPath(
                    path = outlinePath,
                    color = color,
                    style = Stroke(
                        width = strokeWidth / scaleX,
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
