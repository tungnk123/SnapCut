package com.tungnk123.snapcut.core.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.FloatBuffer
import javax.inject.Inject
import kotlin.coroutines.resume
import com.google.mediapipe.tasks.core.Delegate

private const val TAG = "SnapCut.ML"
private const val MODEL_FILE = "selfie_segmenter.tflite"

/**
 * Uses MediaPipe ImageSegmenter with explicit CPU delegate.
 *
 * Why MediaPipe instead of MLKit:
 * - play-services-mlkit-subject-segmentation:16.0.0-beta1 → SIGSEGV on Android 16 (MTE/GPU)
 * - segmentation-selfie (MLKit) → poor quality, designed for 30fps video not still photos
 * - MediaPipe selfie_segmenter.tflite → higher quality model, CPU-only, no GPU crash
 *
 * Model setup: download selfie_segmenter.tflite and place in app/src/main/assets/
 * Download: https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_segmenter/float16/latest/selfie_segmenter.tflite
 */
class SubjectSegmentationManager @Inject constructor(
    private val context: Context
) {
    private val segmenter: ImageSegmenter by lazy {
        Log.d(TAG, "Creating MediaPipe ImageSegmenter (CPU)")
        ImageSegmenter.createFromOptions(
            context,
            ImageSegmenter.ImageSegmenterOptions.builder()
                .setBaseOptions(
                    BaseOptions.builder()
                        .setDelegate(Delegate.CPU)
                        .setModelAssetPath(MODEL_FILE)
                        .build()
                )
                .setRunningMode(RunningMode.IMAGE)
                .setOutputCategoryMask(true)
                .build()
        )
    }

    /**
     * Segments the subject (person) from [bitmap].
     * Returns a full-size ARGB_8888 bitmap with the background removed.
     *
     * The [tapX]/[tapY] parameters are retained for API compatibility
     * (MediaPipe segments the full frame, tap position is not used for selection).
     */
    suspend fun segmentSubject(
        bitmap: Bitmap,
        tapX: Float = bitmap.width / 2f,
        tapY: Float = bitmap.height / 2f
    ): Result<SegmentationResult> =
        suspendCancellableCoroutine { continuation ->
            try {
                Log.d(TAG, "segmentSubject: ${bitmap.width}x${bitmap.height}")

                // Ensure ARGB_8888 — MediaPipe requires it
                val inputBitmap = if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap
                else bitmap.copy(Bitmap.Config.ARGB_8888, false)

                val mpImage = BitmapImageBuilder(inputBitmap).build()
                val result = segmenter.segment(mpImage)

                val categoryMaskImage = result.categoryMask().get()

                // ByteBuffer: each byte = category index (0=background, 1=person)
                val byteBuffer = ByteBufferExtractor.extract(categoryMaskImage)
                byteBuffer.rewind()
                val categories = ByteArray(byteBuffer.remaining()).also { byteBuffer.get(it) }

                Log.d(TAG, "Category mask: ${categories.size} pixels")

                // Apply mask to source bitmap
                val foreground = applyMaskToBitmap(inputBitmap, categories)
                if (foreground == null) {
                    Log.w(TAG, "No person detected")
                    continuation.resume(Result.failure(NoSubjectFoundException()))
                    return@suspendCancellableCoroutine
                }

                // Build a FloatBuffer confidence proxy for shimmer outline
                // (1.0f for person pixels, 0.0f for background)
                val floatMask = FloatBuffer.allocate(categories.size)
                categories.forEach { floatMask.put(if (it.toInt() != 0) 1f else 0f) }
                floatMask.rewind()

                Log.d(TAG, "Foreground: ${foreground.width}x${foreground.height}")
                continuation.resume(
                    Result.success(
                        SegmentationResult(
                            foregroundBitmap = foreground,
                            confidenceMask = floatMask,
                            maskWidth = categoryMaskImage.width,
                            maskHeight = categoryMaskImage.height
                        )
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Segmentation error: ${e.javaClass.simpleName}: ${e.message}", e)
                continuation.resume(Result.failure(e))
            }
        }

    private fun applyMaskToBitmap(
        source: Bitmap,
        categories: ByteArray,
        personCategory: Int = 1
    ): Bitmap? {
        val pixels = IntArray(source.width * source.height)
        source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)

        var hasSubject = false
        for (i in pixels.indices) {
            val category = categories.getOrNull(i)?.toInt() ?: 0
            if (category == personCategory) {
                hasSubject = true
            } else {
                pixels[i] = 0 // transparent
            }
        }
        if (!hasSubject) return null

        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
        return result
    }

    fun close() = segmenter.close()
}

data class SegmentationResult(
    val foregroundBitmap: Bitmap,
    val confidenceMask: FloatBuffer?,
    val maskWidth: Int,
    val maskHeight: Int
)

class NoSubjectFoundException :
    Exception("No person detected. Try a photo with a person clearly visible.")
