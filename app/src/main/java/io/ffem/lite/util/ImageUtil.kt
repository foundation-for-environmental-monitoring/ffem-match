package io.ffem.lite.util

import android.graphics.*
import io.ffem.lite.model.ImageEdgeType
import io.ffem.lite.model.ResultInfo
import io.ffem.lite.preference.isDiagnosticMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*


object ImageUtil {
    private val decimalFormat = DecimalFormat("#.###", DecimalFormatSymbols(Locale.US))

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

    fun toGrayscale(src: Bitmap): Bitmap {
        val dest =
            Bitmap.createBitmap(src.width, src.height, src.config)
        val canvas = Canvas(dest)
        val paint = Paint()
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }

    // https://codeday.me/en/qa/20190310/15630.html
    fun toBlackAndWhite(
        src: Bitmap, threshold: Int, imageEdgeSide: ImageEdgeType, left: Int, right: Int
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

        var top = 3
        var bottom = 7

        if (imageEdgeSide == ImageEdgeType.WhiteDown) {
            top = result.height - 7
            bottom = result.height - 3
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

    fun createResultImage(
        resultInfo: ResultInfo,
        calibratedResultInfo: ResultInfo,
        imageWidth: Int
    ): Bitmap {

        val sampleHeight = 20f
        val swatchTop = 40f
        val swatchHeight = 50
        val swatchBottom = swatchTop + swatchHeight
        val swatchWidth =
            (imageWidth - resultInfo.swatches!!.size - 1) / resultInfo.swatches!!.size.toFloat()

        var textTop = swatchBottom + sampleHeight + 10
        val textHeight = 25
        val textSize = 20

        if (isDiagnosticMode() && calibratedResultInfo.result > -1) {
            textTop += 40
        }

        val result = Bitmap.createBitmap(
            imageWidth,
            (textTop + textHeight).toInt(),
            Bitmap.Config.ARGB_8888
        )
        result.eraseColor(Color.WHITE)

        val canvas = Canvas(result)

        val paintColor = Paint()
        paintColor.style = Paint.Style.FILL

        val pointerTop: Float
        val factor = (imageWidth - swatchWidth) / 100
        if (calibratedResultInfo.result > -1) {
            paintColor.color = calibratedResultInfo.matchedSwatch
            pointerTop = (calibratedResultInfo.matchedPosition * factor) + swatchWidth / 2 - 4

            if (isDiagnosticMode() && resultInfo.result > -1) {
                val pathBottom = Path()
                val cPaintColor = Paint()
                cPaintColor.style = Paint.Style.FILL
                cPaintColor.color = resultInfo.matchedSwatch
                val pointerBottom =
                    (resultInfo.matchedPosition * factor) + swatchWidth / 2 - 4
                pathBottom.moveTo(pointerBottom - sampleHeight, swatchBottom + 40)
                pathBottom.lineTo(pointerBottom + sampleHeight, swatchBottom + 40)
                pathBottom.lineTo(pointerBottom, swatchBottom)
                pathBottom.close()
                canvas.drawPath(pathBottom, cPaintColor)
            }
        } else {
            paintColor.color = resultInfo.matchedSwatch
            pointerTop = (resultInfo.matchedPosition * factor) + swatchWidth / 2 - 4
        }

        val path = Path()
        path.moveTo(pointerTop - sampleHeight, 0f)
        path.lineTo(pointerTop + sampleHeight, 0f)
        path.lineTo(pointerTop, swatchTop)
        path.close()
        canvas.drawPath(path, paintColor)

        if (getColorDistance(Color.WHITE, paintColor.color) < 40) {
            val strokeColor = Paint()
            strokeColor.style = Paint.Style.STROKE
            strokeColor.strokeWidth = 2f
            strokeColor.isAntiAlias = true
            strokeColor.color = Color.BLACK
            canvas.drawPath(path, strokeColor)
        }

        // draw the swatches
        val swatchPaint = Paint()
        swatchPaint.style = Paint.Style.FILL

        val textPaint = Paint()
        textPaint.color = Color.BLACK
        textPaint.textSize = textSize.toFloat()
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.CENTER

        var index = -1
        for (swatch in resultInfo.swatches!!) {
            index++
            swatchPaint.color = swatch.color
            canvas.drawRect(
                index * swatchWidth,
                swatchTop,
                index * swatchWidth + swatchWidth,
                swatchTop + swatchHeight,
                swatchPaint
            )
            canvas.drawText(
                decimalFormat.format(swatch.value),
                index * swatchWidth + swatchWidth / 2.toFloat(),
                textTop,
                textPaint
            )
        }

        return result
    }
}