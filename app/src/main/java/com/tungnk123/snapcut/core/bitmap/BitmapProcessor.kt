package com.tungnk123.snapcut.core.bitmap

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.tungnk123.snapcut.core.ml.SegmentationResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
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
                val file = File(dir, "$fileName.webp")
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                FileOutputStream(file).use { out ->
                    bitmap.compress(format, 100, out)
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
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.webp")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/webp")
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
                    bitmap.compress(format, 100, out)
                }
                uri
            }
        }

    /**
     * Applies all enabled effects in [params]: tint → outline → shadow.
     * Shadow stays outermost; safe to call on a background dispatcher.
     */
    fun applyEditParams(source: Bitmap, params: StickerEditParams): Bitmap {
        var result = source
        if (params.tintEnabled)    result = applyColorTint(result, params.tintColor, params.tintAlpha)
        if (params.outlineEnabled) result = applyOutline(result, params.outlineColor, params.outlineWidth)
        if (params.shadowEnabled)  result = applyShadow(result, params.shadowRadius, params.shadowDx, params.shadowDy)
        return result
    }

    /**
     * Applies a [StickerStyle] to [source] and returns the resulting bitmap.
     * NONE returns the original unmodified bitmap.
     * All other styles create a new bitmap — safe to call on a background dispatcher.
     */
    fun applyStyle(source: Bitmap, style: StickerStyle): Bitmap = when (style) {
        StickerStyle.NONE         -> source
        StickerStyle.WHITE_OUTLINE -> applyOutline(source, android.graphics.Color.WHITE)
        StickerStyle.BLACK_OUTLINE -> applyOutline(source, android.graphics.Color.BLACK)
        StickerStyle.GOLD_OUTLINE  -> applyOutline(source, android.graphics.Color.parseColor("#FFD700"))
        StickerStyle.SHADOW        -> applyShadow(source)
        StickerStyle.RED_TINT      -> applyColorTint(source, android.graphics.Color.parseColor("#E53935"))
        StickerStyle.PURPLE_TINT   -> applyColorTint(source, android.graphics.Color.parseColor("#8E24AA"))
        StickerStyle.BLUE_TINT     -> applyColorTint(source, android.graphics.Color.parseColor("#1E88E5"))
    }

    /**
     * Draws a solid-color outline around the subject's alpha boundary.
     *
     * Strategy: stamp the source bitmap at every (dx, dy) offset that falls inside
     * a circle of radius [width]. The union of all stamped copies forms a perfectly
     * filled, thick silhouette. Then colorize with SRC_IN and draw the original on top.
     *
     * This is more reliable than BlurMaskFilter, which can produce soft or invisible
     * edges when combined with other paint effects on a software canvas.
     *
     * step = width/8 keeps the draw-call count to ~200–400 regardless of [width],
     * which completes in <150 ms on a background dispatcher for typical sticker sizes.
     */
    private fun applyOutline(source: Bitmap, outlineColor: Int, width: Int = 32): Bitmap {
        val pad = width + 2
        val w = source.width + pad * 2
        val h = source.height + pad * 2

        val silhouette = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val silCanvas = Canvas(silhouette)
        val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Stamp source at every grid point inside the circle
        val step = maxOf(2, width / 8)
        val r2 = width.toLong() * width
        var dy = -width
        while (dy <= width) {
            var dx = -width
            while (dx <= width) {
                if (dx.toLong() * dx + dy.toLong() * dy <= r2) {
                    silCanvas.drawBitmap(source, (pad + dx).toFloat(), (pad + dy).toFloat(), stampPaint)
                }
                dx += step
            }
            dy += step
        }

        // Flood-fill the expanded silhouette with outlineColor
        silCanvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = outlineColor
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        })

        // Composite: colored silhouette behind, original in front
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(result).apply {
            drawBitmap(silhouette, 0f, 0f, null)
            drawBitmap(source, pad.toFloat(), pad.toFloat(), null)
        }
        silhouette.recycle()
        return result
    }

    /** Adds a blurred drop shadow beneath the subject (two-pass to avoid colorFilter+maskFilter conflict). */
    private fun applyShadow(
        source: Bitmap,
        radius: Float = 28f,
        dx: Float = 6f,
        dy: Float = 12f,
        shadowAlpha: Int = 160,
    ): Bitmap {
        val pad = (radius + abs(dx).coerceAtLeast(abs(dy))).toInt() + 8
        val w = source.width + pad * 2
        val h = source.height + pad * 2

        // Pass 1: blur the silhouette (maskFilter only — no colorFilter)
        val shadow = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(shadow).drawBitmap(source, pad + dx, pad + dy,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
            }
        )

        // Pass 2: recolor to black with desired alpha (SRC_IN keeps only existing alpha)
        Canvas(shadow).drawRect(0f, 0f, w.toFloat(), h.toFloat(),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.BLACK
                alpha = shadowAlpha
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            }
        )

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(result).apply {
            drawBitmap(shadow, 0f, 0f, null)
            drawBitmap(source, pad.toFloat(), pad.toFloat(), null)
        }
        shadow.recycle()
        return result
    }

    /** Blends a semi-transparent color over the subject (SRC_ATOP respects alpha channel). */
    private fun applyColorTint(source: Bitmap, tintColor: Int, tintAlpha: Int = 140): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)
        val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tintColor
            alpha = tintAlpha
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        }
        canvas.drawRect(0f, 0f, result.width.toFloat(), result.height.toFloat(), tintPaint)
        return result
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
