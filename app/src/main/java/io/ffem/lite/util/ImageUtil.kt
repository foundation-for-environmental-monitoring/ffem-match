package io.ffem.lite.util

import android.graphics.*


object ImageUtil {

    private fun createGreyMatrix(): ColorMatrix {
        return ColorMatrix(
            floatArrayOf(
                0.2989f, 0.5870f, 0.1140f, 0f, 0f,
                0.2989f, 0.5870f, 0.1140f, 0f, 0f,
                0.2989f, 0.5870f, 0.1140f, 0f, 0f, 0f, 0f, 0f, 1f, 0f
            )
        )
    }

    private fun createThresholdMatrix(threshold: Int): ColorMatrix {
        return ColorMatrix(
            floatArrayOf(
                85f, 85f, 85f, 0f, -255f * threshold,
                85f, 85f, 85f, 0f, -255f * threshold,
                85f, 85f, 85f, 0f, -255f * threshold,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

//    fun toGrayscale(src: Bitmap): Bitmap {
//        val dest =
//            Bitmap.createBitmap(src.width, src.height, src.config)
//        val canvas = Canvas(dest)
//        val paint = Paint()
//        val cm = ColorMatrix()
//        cm.setSaturation(0f)
//        paint.colorFilter = ColorMatrixColorFilter(cm)
//        canvas.drawBitmap(src, 0f, 0f, paint)
//        return dest
//    }

    // https://codeday.me/en/qa/20190310/15630.html
    fun toBlackAndWhite(src: Bitmap, threshold: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inScaled = false

        val bitmapPaint = Paint()

        val result = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(result)

        // convert bitmap to grey scale:
        bitmapPaint.colorFilter = ColorMatrixColorFilter(createGreyMatrix())
        c.drawBitmap(src, 0f, 0f, bitmapPaint)

        // to black and white using threshold matrix
        bitmapPaint.colorFilter = ColorMatrixColorFilter(createThresholdMatrix(threshold))
        c.drawBitmap(result, 0f, 0f, bitmapPaint)

        return result
    }
}