package com.tungnk123.snapcut.core.ml

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.FloatBuffer
import javax.inject.Inject
import kotlin.coroutines.resume

class SubjectSegmentationManager @Inject constructor(
    private val context: Context
) {
    private val segmenter = SubjectSegmentation.getClient(
        SubjectSegmenterOptions.Builder()
            // enableForegroundBitmap() populates result.foregroundBitmap —
            // the composite of all detected subjects with transparent background.
            // subject.bitmap (per-subject) requires SubjectResultOptions.enableSubjectBitmap()
            // and is separate. We use the foreground bitmap as it's what we need.
            .enableForegroundBitmap()
            .enableForegroundConfidenceMask()
            .build()
    )

    /**
     * Segments subjects from [bitmap].
     * Returns [SegmentationResult] with the foreground bitmap (all subjects,
     * transparent background) and confidence mask sized to the input image.
     */
    suspend fun segmentSubject(bitmap: Bitmap): Result<SegmentationResult> =
        suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            segmenter.process(image)
                .addOnSuccessListener { result ->
                    // Use foregroundBitmap — populated by enableForegroundBitmap()
                    val foreground = result.foregroundBitmap
                    if (foreground != null) {
                        continuation.resume(
                            Result.success(
                                SegmentationResult(
                                    foregroundBitmap = foreground,
                                    confidenceMask = result.foregroundConfidenceMask,
                                    // Mask is always the same size as the input image
                                    maskWidth = bitmap.width,
                                    maskHeight = bitmap.height
                                )
                            )
                        )
                    } else {
                        continuation.resume(Result.failure(NoSubjectFoundException()))
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resume(Result.failure(e))
                }

            continuation.invokeOnCancellation { segmenter.close() }
        }

    fun close() = segmenter.close()
}

data class SegmentationResult(
    val foregroundBitmap: Bitmap,
    val confidenceMask: FloatBuffer?,
    val maskWidth: Int,
    val maskHeight: Int
)

class NoSubjectFoundException : Exception("No subject detected in the image")
