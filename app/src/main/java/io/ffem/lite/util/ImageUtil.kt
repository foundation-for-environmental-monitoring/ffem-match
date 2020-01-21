package io.ffem.lite.util

import android.graphics.*
import io.ffem.lite.model.ImageEdgeType


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
    fun toBlackAndWhite(
        src: Bitmap,
        threshold: Int,
        imageEdgeSide: ImageEdgeType,
        left: Int,
        right: Int
    ): Bitmap {

        val width = right - left
        var thresholdValue = threshold

        val options = BitmapFactory.Options()
        options.inScaled = false
        val bitmapPaint = Paint()
        var result = Bitmap.createBitmap(width, src.height, Bitmap.Config.ARGB_8888)

        val srcRect = Rect(left, 0, right, src.height)
        val dstRect = Rect(0, 0, width, src.height)

        var c = Canvas(result)
        // convert bitmap to grey scale:
        bitmapPaint.colorFilter = ColorMatrixColorFilter(createGreyMatrix())
        c.drawBitmap(src, srcRect, dstRect, bitmapPaint)

        bitmapPaint.colorFilter = ColorMatrixColorFilter(createThresholdMatrix(threshold))
        c.drawBitmap(result, 0f, 0f, bitmapPaint)

        var top = 0
        var bottom = 4

        if (imageEdgeSide == ImageEdgeType.WhiteDown) {
            top = result.height - 4
            bottom = result.height
        }

        var rect = Rect(left, top, result.width, bottom)
        var pixels = getBitmapPixels(result, rect)
        while (!isWhite(pixels)) {
            result.recycle()
            thresholdValue -= 5
            result = Bitmap.createBitmap(width, src.height, Bitmap.Config.ARGB_8888)

            c = Canvas(result)

            // convert bitmap to grey scale:
            bitmapPaint.colorFilter = ColorMatrixColorFilter(createGreyMatrix())
            c.drawBitmap(src, srcRect, dstRect, bitmapPaint)

            bitmapPaint.colorFilter = ColorMatrixColorFilter(createThresholdMatrix(thresholdValue))
            c.drawBitmap(result, 0f, 0f, bitmapPaint)

            rect = Rect(left, top, result.width, bottom)
            pixels = getBitmapPixels(result, rect)
        }

        result.recycle()
        result = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)

        c = Canvas(result)

        // convert bitmap to grey scale:
        bitmapPaint.colorFilter = ColorMatrixColorFilter(createGreyMatrix())
        c.drawBitmap(src, 0f, 0f, bitmapPaint)

        bitmapPaint.colorFilter = ColorMatrixColorFilter(createThresholdMatrix(thresholdValue))
        c.drawBitmap(result, 0f, 0f, bitmapPaint)

        return result
    }
}