package io.ffem.lite.util

import android.graphics.*

object ImageUtil {
    //Custom color matrix to convert to GrayScale
    private val MATRIX = floatArrayOf(
        0.3f, 0.59f, 0.11f, 0f, 0f,
        0.3f, 0.59f, 0.11f, 0f, 0f,
        0.3f, 0.59f, 0.11f, 0f, 0f, 0f, 0f, 0f, 1f, 0f
    )

    fun createGreyMatrix(): ColorMatrix {
        return ColorMatrix(
            floatArrayOf(
                0.2989f, 0.5870f, 0.1140f, 0f, 0f,
                0.2989f, 0.5870f, 0.1140f, 0f, 0f,
                0.2989f, 0.5870f, 0.1140f, 0f, 0f, 0f, 0f, 0f, 1f, 0f
            )
        )
    }

    fun createThresholdMatrix(threshold: Int): ColorMatrix {
        return ColorMatrix(
            floatArrayOf(
                85f, 85f, 85f, 0f, -255f * threshold,
                85f, 85f, 85f, 0f, -255f * threshold,
                85f, 85f, 85f, 0f, -255f * threshold,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    fun getGrayscale(src: Bitmap): Bitmap {
        val dest = Bitmap.createBitmap(
            src.width,
            src.height,
            src.config
        )
        val canvas = Canvas(dest)
        val paint = Paint()
        val filter =
            ColorMatrixColorFilter(MATRIX)
        paint.colorFilter = filter
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }
}