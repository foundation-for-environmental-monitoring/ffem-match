package io.ffem.lite.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import io.ffem.lite.common.Constants
import io.ffem.lite.common.Constants.QR_TO_COLOR_AREA_DISTANCE_PERCENTAGE
import io.ffem.lite.scanner.zxing.qrcode.detector.FinderPatternInfo
import kotlin.math.max

class ColorCardAnalyzer(context: Context) : ColorCardAnalyzerBase(context) {

    //https://stackoverflow.com/questions/13161628/cropping-a-perspective-transformation-of-image-on-android
    override fun perspectiveTransform(bitmap: Bitmap, pattern: FinderPatternInfo): Bitmap {
        val matrix = Matrix()
        val width = max(
            pattern.topRight.x - pattern.topLeft.x,
            pattern.bottomRight.x - pattern.bottomLeft.x
        )
        val height = (width * 58) / 40

        val dst = floatArrayOf(
            0f, 0f,
            width, 0f,
            width,
            height, 0f,
            height
        )
        val src = floatArrayOf(
            pattern.topLeft.x,
            pattern.topLeft.y,
            pattern.topRight.x,
            pattern.topRight.y,
            pattern.bottomRight.x,
            pattern.bottomRight.y,
            pattern.bottomLeft.x,
            pattern.bottomLeft.y
        )
        matrix.setPolyToPoly(src, 0, dst, 0, src.size shr 1)
        val mappedTL = floatArrayOf(0f, 0f)
        matrix.mapPoints(mappedTL)

        val mappedTR = floatArrayOf(bitmap.width.toFloat(), 0f)
        matrix.mapPoints(mappedTR)

        val mappedLL = floatArrayOf(0f, bitmap.height.toFloat())
        matrix.mapPoints(mappedLL)

        val correctedBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val p = getPatternFromBitmap(correctedBitmap)

        return if (p != null) {
            val finalBitmap = Bitmap.createBitmap(
                correctedBitmap,
                p.topLeft.x.toInt(),
                p.topLeft.y.toInt(),
                (p.topRight.x - p.topLeft.x).toInt(),
                (p.bottomRight.y - p.topRight.y).toInt(),
                null,
                true
            )
            correctedBitmap.recycle()

            val shiftX =
                (finalBitmap.width * Constants.CALIBRATION_COLOR_AREA_WIDTH_PERCENTAGE).toInt()
            val shiftY =
                (finalBitmap.height * QR_TO_COLOR_AREA_DISTANCE_PERCENTAGE).toInt()

            val centerX = finalBitmap.width / 2
            val centerY = finalBitmap.height / 2

            Bitmap.createBitmap(
                finalBitmap,
                centerX - shiftX,
                centerY - shiftY,
                shiftX * 2,
                shiftY * 2,
                null,
                true
            )
        } else {
            correctedBitmap
        }
    }
}
