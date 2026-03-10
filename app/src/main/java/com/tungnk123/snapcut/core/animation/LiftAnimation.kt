package com.tungnk123.snapcut.core.animation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Returns animated scale and elevation values for the subject-lift effect.
 *
 * Usage in a composable:
 * ```
 * val liftState = rememberLiftAnimationState(lifted = isLifted)
 * Image(
 *     modifier = Modifier.graphicsLayer {
 *         scaleX = liftState.scale.value
 *         scaleY = liftState.scale.value
 *         shadowElevation = liftState.elevation.value
 *         ambientShadowColor = Color.Black.copy(alpha = 0.4f).toArgb()
 *         spotShadowColor = Color.Black.copy(alpha = 0.4f).toArgb()
 *     }
 * )
 * ```
 */
data class LiftAnimationState(
    val scale: State<Float>,
    val elevation: State<Float>
)

@Composable
fun rememberLiftAnimationState(lifted: Boolean): LiftAnimationState {
    val scale = animateFloatAsState(
        targetValue = if (lifted) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "liftScale"
    )
    val elevation = animateFloatAsState(
        targetValue = if (lifted) 24f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "liftElevation"
    )
    return LiftAnimationState(scale, elevation)
}
