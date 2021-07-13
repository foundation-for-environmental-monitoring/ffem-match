package io.ffem.lite.util

import android.graphics.*
import androidx.camera.core.ImageProxy
import io.ffem.lite.model.ResultInfo
import io.ffem.lite.preference.isDiagnosticMode
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*


object ImageUtil {
    private val decimalFormat = DecimalFormat("#.###", DecimalFormatSymbols(Locale.US))

    fun createResultImage(
        resultInfo: ResultInfo,
        calibratedResultInfo: ResultInfo,
        maxValue: Double,
        formula: String,
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
        result.eraseColor(Color.TRANSPARENT)

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
        textPaint.color = Color.GRAY
        textPaint.textSize = textSize.toFloat()
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.CENTER

        var index = -1
        var skip = false
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
            val valueText = decimalFormat.format(MathUtil.applyFormula(swatch.value, formula))
            val width = textPaint.measureText(valueText) * 1.1
            if (width > swatchWidth) {
                skip = !skip
            }
            if (!skip && swatch.value <= maxValue) {
                canvas.drawText(
                    valueText,
                    index * swatchWidth + swatchWidth / 2.toFloat(),
                    textTop,
                    textPaint
                )
            }
        }

        return result
    }

    fun ImageProxy.toBitmap(): Bitmap {
        val nv21 = yuv420888ToNv21(this)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        return yuvImage.toBitmap()!!
    }

    private fun YuvImage.toBitmap(): Bitmap? {
        val out = ByteArrayOutputStream()
        if (!compressToJpeg(Rect(0, 0, width, height), 100, out))
            return null
        val imageBytes: ByteArray = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
        val pixelCount = image.cropRect.width() * image.cropRect.height()
        val pixelSizeBits = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)
        val outputBuffer = ByteArray(pixelCount * pixelSizeBits / 8)
        imageToByteBuffer(image, outputBuffer, pixelCount)
        return outputBuffer
    }

    private fun imageToByteBuffer(image: ImageProxy, outputBuffer: ByteArray, pixelCount: Int) {
//        assert(image.format == ImageFormat.YUV_420_888)

        val imageCrop = image.cropRect
        val imagePlanes = image.planes

        imagePlanes.forEachIndexed { planeIndex, plane ->
            // How many values are read in input for each output value written
            // Only the Y plane has a value for every pixel, U and V have half the resolution i.e.
            //
            // Y Plane            U Plane    V Plane
            // ===============    =======    =======
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            val outputStride: Int

            // The index in the output buffer the next value will be written at
            // For Y it's zero, for U and V we start at the end of Y and interleave them i.e.
            //
            // First chunk        Second chunk
            // ===============    ===============
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            var outputOffset: Int

            when (planeIndex) {
                0 -> {
                    outputStride = 1
                    outputOffset = 0
                }
                1 -> {
                    outputStride = 2
                    // For NV21 format, U is in odd-numbered indices
                    outputOffset = pixelCount + 1
                }
                2 -> {
                    outputStride = 2
                    // For NV21 format, V is in even-numbered indices
                    outputOffset = pixelCount
                }
                else -> {
                    // Image contains more than 3 planes, something strange is going on
                    return@forEachIndexed
                }
            }

            val planeBuffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride

            // We have to divide the width and height by two if it's not the Y plane
            val planeCrop = if (planeIndex == 0) {
                imageCrop
            } else {
                Rect(
                    imageCrop.left / 2,
                    imageCrop.top / 2,
                    imageCrop.right / 2,
                    imageCrop.bottom / 2
                )
            }

            val planeWidth = planeCrop.width()
            val planeHeight = planeCrop.height()

            // Intermediate buffer used to store the bytes of each row
            val rowBuffer = ByteArray(plane.rowStride)

            // Size of each row in bytes
            val rowLength = if (pixelStride == 1 && outputStride == 1) {
                planeWidth
            } else {
                // Take into account that the stride may include data from pixels other than this
                // particular plane and row, and that could be between pixels and not after every
                // pixel:
                //
                // |---- Pixel stride ----|                    Row ends here --> |
                // | Pixel 1 | Other Data | Pixel 2 | Other Data | ... | Pixel N |
                //
                // We need to get (N-1) * (pixel stride bytes) per row + 1 byte for the last pixel
                (planeWidth - 1) * pixelStride + 1
            }

            for (row in 0 until planeHeight) {
                // Move buffer position to the beginning of this row
                planeBuffer.position(
                    (row + planeCrop.top) * rowStride + planeCrop.left * pixelStride
                )

                if (pixelStride == 1 && outputStride == 1) {
                    // When there is a single stride value for pixel and output, we can just copy
                    // the entire row in a single step
                    planeBuffer.get(outputBuffer, outputOffset, rowLength)
                    outputOffset += rowLength
                } else {
                    // When either pixel or output have a stride > 1 we must copy pixel by pixel
                    planeBuffer.get(rowBuffer, 0, rowLength)
                    for (col in 0 until planeWidth) {
                        outputBuffer[outputOffset] = rowBuffer[col * pixelStride]
                        outputOffset += outputStride
                    }
                }
            }
        }
    }
}