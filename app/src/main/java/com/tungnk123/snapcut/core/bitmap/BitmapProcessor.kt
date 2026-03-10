package com.tungnk123.snapcut.core.bitmap

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathEffect
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.tungnk123.snapcut.core.ml.SegmentationResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
class BitmapProcessor(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher
) {

    /**
     * Decodes a URI to a Bitmap, respecting the image orientation.
     * Scales down large images to [maxDimension] to avoid OOM.
     */
    suspend fun decodeBitmap(uri: Uri, maxDimension: Int = 2048): Result<Bitmap> =
        withContext(ioDispatcher) {
            runCatching {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
                options.inSampleSize = calculateInSampleSize(
                    options.outWidth,
                    options.outHeight,
                    maxDimension
                )
                options.inJustDecodeBounds = false
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                } ?: error("Cannot open URI: $uri")
            }
        }

    /**
     * Saves a transparent PNG [bitmap] to the app's private files directory.
     * Returns the absolute file path.
     */
    suspend fun saveCutBitmapToFile(bitmap: Bitmap, fileName: String): Result<String> =
        withContext(ioDispatcher) {
            runCatching {
                val dir = File(context.filesDir, "cut_subjects").apply { mkdirs() }
                val file = File(dir, "$fileName.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                file.absolutePath
            }
        }

    /**
     * Saves a cut subject bitmap to the public Gallery (MediaStore).
     * Returns the Uri of the saved image.
     */
    suspend fun saveToGallery(bitmap: Bitmap, displayName: String): Result<Uri> =
        withContext(ioDispatcher) {
            runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/SnapCut"
                    )
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                ) ?: error("Failed to create MediaStore entry")

                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                uri
            }
        }

    /**
     * Adds a solid color border/outline around the segmented subject.
     * Useful for creating sticker-style subjects with a white outline.
     */
    fun addBorderToBitmap(
        source: Bitmap,
        borderWidth: Int = 12,
        borderColor: Int = android.graphics.Color.WHITE
    ): Bitmap {
        val width = source.width + borderWidth * 2
        val height = source.height + borderWidth * 2
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = borderColor }

        // Draw a dilated silhouette as the border
        canvas.drawBitmap(source, borderWidth.toFloat(), borderWidth.toFloat(), null)
        return result
    }

    /**
     * Extracts the outline [Path] from a segmentation confidence mask.
     * Used to draw the animated shimmer outline effect.
     *
     * Coordinate mapping note: mask coordinates are in image space.
     * When drawing on a composable, scale the path to match display bounds.
     */
    fun extractOutlinePath(
        result: SegmentationResult,
        threshold: Float = 0.5f
    ): Path {
        val maskBuffer = result.confidenceMask ?: return Path()
        val width = result.maskWidth
        val height = result.maskHeight
        val path = Path()

        // Copy FloatBuffer to array for random-access indexing
        maskBuffer.rewind()
        val mask = FloatArray(maskBuffer.remaining()).also { maskBuffer.get(it) }

        // Walk the mask border pixels and build an approximate outline path
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val confidence = mask.getOrNull(idx) ?: 0f
                if (confidence >= threshold) {
                    val isBorder = listOf(
                        mask.getOrNull((y - 1) * width + x) ?: 0f,
                        mask.getOrNull((y + 1) * width + x) ?: 0f,
                        mask.getOrNull(y * width + (x - 1)) ?: 0f,
                        mask.getOrNull(y * width + (x + 1)) ?: 0f
                    ).any { it < threshold }
                    if (isBorder) {
                        path.addRect(
                            x.toFloat(), y.toFloat(),
                            (x + 1).toFloat(), (y + 1).toFloat(),
                            Path.Direction.CW
                        )
                    }
                }
            }
        }
        return path
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var inSampleSize = 1
        if (width > maxDimension || height > maxDimension) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            while (halfWidth / inSampleSize >= maxDimension ||
                halfHeight / inSampleSize >= maxDimension
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
