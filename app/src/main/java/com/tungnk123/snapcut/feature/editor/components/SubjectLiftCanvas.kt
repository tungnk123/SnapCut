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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import com.tungnk123.snapcut.core.animation.computeFitTransform
import com.tungnk123.snapcut.core.animation.shimmerOutline

@Composable
fun SubjectLiftCanvas(
    sourceBitmap: Bitmap,
    subjectBitmap: Bitmap?,
    outlinePath: Path = Path(),
    isLifted: Boolean,
    onLongPress: (imageX: Float, imageY: Float) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val widthPx  = with(density) { maxWidth.toPx() }
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
                            onLongPress(
                                (offset.x - pathOffsetX) / pathScale,
                                (offset.y - pathOffsetY) / pathScale
                            )
                        },
                        onTap = { onTap() }
                    )
                }
                .shimmerOutline(
                    outlinePath = outlinePath,
                    pathScale = pathScale,
                    pathOffsetX = pathOffsetX,
                    pathOffsetY = pathOffsetY,
                    visible = isLifted && subjectBitmap != null,
                    color = Color.White
                )
        )
    }
}
