package com.tungnk123.snapcut.feature.editor.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import com.tungnk123.snapcut.core.animation.computeFitTransform
import com.tungnk123.snapcut.core.animation.rememberLiftAnimationState
import com.tungnk123.snapcut.core.animation.shimmerOutline

@Composable
fun SubjectLiftCanvas(
    sourceBitmap: Bitmap,
    subjectBitmap: Bitmap?,
    outlinePath: Path,
    isLifted: Boolean,
    // Receives tap position already converted to image pixel coordinates.
    onLongPress: (imageX: Float, imageY: Float) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val liftState = rememberLiftAnimationState(lifted = isLifted && subjectBitmap != null)
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val (pathScale, pathOffsetX, pathOffsetY) = remember(sourceBitmap, widthPx, heightPx) {
            computeFitTransform(sourceBitmap.width, sourceBitmap.height, widthPx, heightPx)
        }

        Image(
            bitmap = sourceBitmap.asImageBitmap(),
            contentDescription = "Source image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(pathScale, pathOffsetX, pathOffsetY) {
                    detectTapGestures(
                        onLongPress = { offset ->
                            // Convert screen offset → image pixel coordinates using inverse
                            // of the ContentScale.Fit transform. This ensures the tapped
                            // position maps to the correct pixel in the source bitmap,
                            // regardless of image aspect ratio or device screen size.
                            val imageX = (offset.x - pathOffsetX) / pathScale
                            val imageY = (offset.y - pathOffsetY) / pathScale
                            onLongPress(imageX, imageY)
                        },
                        onTap = { onTap() }
                    )
                }
        )

        if (subjectBitmap != null) {
            val liftProgress = ((liftState.scale.value - 1f) / 0.08f).coerceIn(0f, 1f)

            // Shadow: black silhouette of subject, offset down, fades in with lift
            Image(
                bitmap = subjectBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.colorMatrix(
                    ColorMatrix().apply { setToScale(0f, 0f, 0f, 1f) }
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = liftState.scale.value
                        scaleY = liftState.scale.value
                        translationY = 28f * liftProgress
                        alpha = 0.35f * liftProgress
                    }
            )

            // Subject layer on top with shimmer outline
            Image(
                bitmap = subjectBitmap.asImageBitmap(),
                contentDescription = "Lifted subject",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = liftState.scale.value
                        scaleY = liftState.scale.value
                    }
                    .shimmerOutline(
                        outlinePath = outlinePath,
                        pathScale = pathScale,
                        pathOffsetX = pathOffsetX,
                        pathOffsetY = pathOffsetY,
                        visible = isLifted,
                        color = Color.White
                    )
            )
        }
    }
}
