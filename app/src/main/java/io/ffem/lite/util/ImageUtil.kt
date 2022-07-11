package io.ffem.lite.util

import android.app.Activity
import android.graphics.*
import android.view.Surface
import androidx.camera.core.ImageProxy
import io.ffem.lite.common.Constants.DECIMAL_FORMAT
import io.ffem.lite.common.Constants.DEGREES_180
import io.ffem.lite.common.Constants.DEGREES_270
import io.ffem.lite.common.Constants.DEGREES_90
import io.ffem.lite.model.ResultInfo
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.util.ColorUtil.getColorDistance
import java.io.ByteArrayOutputStream


object ImageUtil {

    fun createResultImage(
        resultInfo: ResultInfo,
        calibratedResultInfo: ResultInfo,
        maxValue: Double,
        formula: String?,
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
            if (isDiagnosticMode()) {
                val diagnosticColor = Paint()
                diagnosticColor.color = Color.RED
                diagnosticColor.textSize = textSize.toFloat()
                diagnosticColor.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                diagnosticColor.textAlign = Paint.Align.CENTER

                val valueText = getColorDistance(swatch.color, paintColor.color).toInt().toString()
                val width = textPaint.measureText(valueText) * 1.1
                if (width > swatchWidth) {
                    skip = !skip
                }
                if (!skip && swatch.value <= maxValue) {
                    canvas.drawText(
                        valueText,
                        index * swatchWidth + swatchWidth / 2.toFloat(),
                        textTop + 25,
                        diagnosticColor
                    )
                }
            }

            val valueText = DECIMAL_FORMAT.format(MathUtil.applyFormula(swatch.value, formula))
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

    fun getBitmap(bytes: ByteArray): Bitmap {
        val options = BitmapFactory.Options()
        options.inMutable = true
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    /**
     * Converts bitmap to byte array
     */
    fun bitmapToBytes(bitmap: Bitmap): ByteArray {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos)
        return bos.toByteArray()
    }

    /**
     * Crop a bitmap to a square shape with  given length.
     *
     * @param bitmap the bitmap to crop
     * @param length the length of the sides
     * @return the cropped bitmap
     */
    fun getCroppedBitmap(bitmap: Bitmap, length: Int): Bitmap {
        val pixels = IntArray(length * length)
        val centerX = bitmap.width / 2
        val centerY = bitmap.height / 2
        val point = Point(centerX, centerY)
        bitmap.getPixels(
            pixels, 0, length,
            point.x - length / 2,
            point.y - length / 2,
            length,
            length
        )
        var croppedBitmap = Bitmap.createBitmap(
            pixels, 0, length,
            length,
            length,
            Bitmap.Config.ARGB_8888
        )
        croppedBitmap = getRoundedShape(croppedBitmap, length)
        croppedBitmap.setHasAlpha(true)
//        val mutableBitmap: Bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
//        val canvas = Canvas(mutableBitmap)
//        val paint = Paint()
//        paint.isAntiAlias = true
//        paint.color = Color.GREEN
//        paint.strokeWidth = 1f
//        paint.style = Paint.Style.STROKE
//        canvas.drawBitmap(bitmap, Matrix(), null)
//        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), length / 2f, paint)
//        paint.color = Color.YELLOW
//        paint.strokeWidth = 1f
//        canvas.drawLine(0f, bitmap.height / 2f,
//                bitmap.width / 3f, bitmap.height / 2f, paint)
//        canvas.drawLine(bitmap.width - bitmap.width / 3f, bitmap.height / 2f,
//                bitmap.width.toFloat(), bitmap.height / 2f, paint)
        return croppedBitmap
    }

    fun rotateImage(activity: Activity, `in`: Bitmap): Bitmap {
        val display = activity.windowManager.defaultDisplay
        val rotation: Int = when (display.rotation) {
            Surface.ROTATION_0 -> DEGREES_90
            Surface.ROTATION_180 -> DEGREES_270
            Surface.ROTATION_270 -> DEGREES_180
            Surface.ROTATION_90 -> 0
            else -> 0
        }
        val mat = Matrix()
        mat.postRotate(rotation.toFloat())
        return Bitmap.createBitmap(`in`, 0, 0, `in`.width, `in`.height, mat, true)
    }

    /**
     * Crop bitmap image into a round shape.
     *
     * @param bitmap   the bitmap
     * @param diameter the diameter of the resulting image
     * @return the rounded bitmap
     */
    private fun getRoundedShape(bitmap: Bitmap, diameter: Int): Bitmap {
        val resultBitmap = Bitmap.createBitmap(
            diameter,
            diameter, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(resultBitmap)
        val path = Path()
        path.addCircle(
            (diameter.toFloat() - 1) / 2,
            (diameter.toFloat() - 1) / 2,
            diameter.toFloat() / 2,
            Path.Direction.CCW
        )
        canvas.clipPath(path)
        resultBitmap.setHasAlpha(true)
        canvas.drawBitmap(
            bitmap,
            Rect(0, 0, bitmap.width, bitmap.height),
            Rect(0, 0, diameter, diameter), null
        )
        return resultBitmap
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