package com.tungnk123.snapcut.feature.editor.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import com.tungnk123.snapcut.core.animation.rememberLiftAnimationState
import com.tungnk123.snapcut.core.animation.shimmerOutline

/**
 * Renders the source image with an animated subject overlay.
 *
 * Coordinate mapping:
 * BoxWithConstraints gives us the composable dimensions in Dp → converted to px.
 * We compute scaleX/scaleY as (composable_px / bitmap_px) so the shimmer outline path
 * (which is in bitmap/image space) maps correctly to screen space.
 * This avoids the classic coordinate mismatch when the image is padded/letterboxed.
 */
@Composable
fun SubjectLiftCanvas(
    sourceBitmap: Bitmap,
    subjectBitmap: Bitmap?,
    outlinePath: Path,
    isLifted: Boolean,
    onLongPress: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val liftState = rememberLiftAnimationState(lifted = isLifted && subjectBitmap != null)
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onTap = { onTap() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        // Scale factors: map from bitmap coordinate space → composable display space
        var scaleX = remember(sourceBitmap, widthPx) {
            if (sourceBitmap.width > 0) widthPx / sourceBitmap.width else 1f
        }
        var scaleY = remember(sourceBitmap, heightPx) {
            if (sourceBitmap.height > 0) heightPx / sourceBitmap.height else 1f
        }

        // Background source image
        Image(
            bitmap = sourceBitmap.asImageBitmap(),
            contentDescription = "Source image",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // Subject overlay — lifted with scale + drop shadow animation
        if (subjectBitmap != null) {
            Image(
                bitmap = subjectBitmap.asImageBitmap(),
                contentDescription = "Lifted subject",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = liftState.scale.value
                        scaleY = liftState.scale.value
                        shadowElevation = liftState.elevation.value
                        ambientShadowColor = Color.Black.copy(alpha = 0.4f)
                        spotShadowColor = Color.Black.copy(alpha = 0.4f)
                    }
                    .shimmerOutline(
                        outlinePath = outlinePath,
                        scaleX = scaleX,
                        scaleY = scaleY,
                        visible = isLifted,
                        color = Color.White
                    )
            )
        }
    }
}
