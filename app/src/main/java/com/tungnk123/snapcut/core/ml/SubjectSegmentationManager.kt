package com.tungnk123.snapcut.core.ml

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.FloatBuffer
import javax.inject.Inject
import kotlin.coroutines.resume

private const val TAG = "SnapCut.ML"

class SubjectSegmentationManager @Inject constructor() {

    private val segmenter by lazy {
        SubjectSegmentation.getClient(
            SubjectSegmenterOptions.Builder()
                .enableForegroundBitmap()
                .build()
        )
    }       

    suspend fun segmentSubject(bitmap: Bitmap): Result<SegmentationResult> =
        suspendCancellableCoroutine { continuation ->
            val input = if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap
            else bitmap.copy(Bitmap.Config.ARGB_8888, false)

            segmenter.process(InputImage.fromBitmap(input, 0))
                .addOnSuccessListener { result ->
                    val foreground = result.foregroundBitmap
                    if (foreground == null) {
                        continuation.resume(Result.failure(NoSubjectFoundException()))
                        return@addOnSuccessListener
                    }

                    val pixels = IntArray(foreground.width * foreground.height)
                    foreground.getPixels(pixels, 0, foreground.width, 0, 0, foreground.width, foreground.height)

                    if (pixels.none { (it ushr 24) > 10 }) {
                        continuation.resume(Result.failure(NoSubjectFoundException()))
                        return@addOnSuccessListener
                    }

                    // Confidence mask derived from alpha: 255=subject, 0=background
                    val confidenceMask = FloatBuffer.allocate(pixels.size)
                    pixels.forEach { confidenceMask.put((it ushr 24) / 255f) }
                    confidenceMask.rewind()

                    Log.d(TAG, "Segmented: ${foreground.width}x${foreground.height}")
                    continuation.resume(Result.success(SegmentationResult(
                        foregroundBitmap = foreground,
                        confidenceMask = confidenceMask,
                        maskWidth = foreground.width,
                        maskHeight = foreground.height
                    )))
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Segmentation failed: ${e.message}", e)
                    continuation.resume(Result.failure(e))
                }
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
    Exception("No subject detected. Try long-pressing directly on the subject you want to cut out.")
