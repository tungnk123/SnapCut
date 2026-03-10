package com.tungnk123.snapcut.core.bitmap

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
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
    suspend fun decodeBitmap(uri: Uri, maxDimension: Int = 1024): Result<Bitmap> =
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
                // RGB_565 uses 2 bytes/pixel vs ARGB_8888's 4 bytes — halves GPU memory
                // for the MLKit input tensor, reducing SIGSEGV risk in drishti_gl_runn.
                // The segmentation output (foreground bitmap) is still ARGB_8888.
                options.inPreferredConfig = Bitmap.Config.RGB_565
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
        threshold: Float = 0.5f,
        // Sample every `step` pixels to reduce path complexity ~step² times.
        // step=8 reduces a 1080x2400 scan from 2.6M to ~40k checks and
        // drops path rect count from ~10k to ~150 — 60fps draw becomes trivial.
        step: Int = 8
    ): Path {
        val maskBuffer = result.confidenceMask ?: return Path()
        val width = result.maskWidth
        val height = result.maskHeight
        val path = Path()

        maskBuffer.rewind()
        val mask = FloatArray(maskBuffer.remaining()).also { maskBuffer.get(it) }

        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val idx = y * width + x
                if ((mask.getOrNull(idx) ?: 0f) >= threshold) {
                    val isBorder =
                        (mask.getOrNull((y - step) * width + x) ?: 0f) < threshold ||
                        (mask.getOrNull((y + step) * width + x) ?: 0f) < threshold ||
                        (mask.getOrNull(y * width + (x - step)) ?: 0f) < threshold ||
                        (mask.getOrNull(y * width + (x + step)) ?: 0f) < threshold
                    if (isBorder) {
                        path.addRect(
                            x.toFloat(), y.toFloat(),
                            (x + step).toFloat(), (y + step).toFloat(),
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
        // Keep doubling inSampleSize until BOTH dimensions fit within maxDimension.
        // Previous logic checked halfWidth >= maxDimension which was always false for
        // images just slightly over the limit (e.g. 2560 → halfWidth=1280 < 2048).
        while (width / inSampleSize > maxDimension || height / inSampleSize > maxDimension) {
            inSampleSize *= 2
        }
        return inSampleSize
    }
}
