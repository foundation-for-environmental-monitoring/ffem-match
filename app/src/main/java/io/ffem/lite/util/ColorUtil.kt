package io.ffem.lite.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.Paint.Style
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.gson.Gson
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.camera.Utilities
import io.ffem.lite.model.*
import io.ffem.lite.preference.getCalibrationColorDistanceTolerance
import io.ffem.lite.preference.getColorDistanceTolerance
import java.util.*
import kotlin.math.*

const val MAX_COLOR_DISTANCE_RGB = 80
const val MAX_COLOR_DISTANCE_CALIBRATION = 40
const val INTERPOLATION_COUNT = 100.0
const val MAX_DISTANCE = 999
const val MIN_BRIGHTNESS = 20
const val MIN_LINE_BRIGHTNESS = 100

fun getBitmapPixels(bitmap: Bitmap, rect: Rect): IntArray {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(
        pixels, 0, bitmap.width, rect.left, rect.top,
        rect.width(), rect.height()
    )
    val subsetPixels = IntArray(rect.width() * rect.height())
    for (row in 0 until rect.height()) {
        System.arraycopy(
            pixels, row * bitmap.width,
            subsetPixels, row * rect.width(), rect.width()
        )
    }
    return subsetPixels
}

fun getAverageColor(pixels: IntArray): Int {

    var r = 0
    var g = 0
    var b = 0
    var total = 0

    for (element in pixels) {
        r += element.red
        g += element.green
        b += element.blue
        total++
    }

    r /= total
    g /= total
    b /= total

    return Color.argb(255, r, g, b)
}

fun hasBlackPixelsOnBottomEdge(bitmap: Bitmap, left: Int, width: Int): Boolean {
    var total = 0

    val pixels = getBitmapPixels(
        bitmap,
        Rect(
            left, bitmap.height - 5,
            width, bitmap.height
        )
    )

    for (element in pixels) {
        if (element.red < MIN_BRIGHTNESS &&
            element.green < MIN_BRIGHTNESS &&
            element.blue < MIN_BRIGHTNESS
        ) {
            total++
            if (total > 50) {
                return true
            }
        }
    }

    return false
}

fun hasBlackPixelsOnTopEdge(bitmap: Bitmap, left: Int, width: Int): Boolean {
    var total = 0

    val pixels = getBitmapPixels(
        bitmap,
        Rect(left, 0, width, 5)
    )

    for (element in pixels) {
        if (element.red < MIN_BRIGHTNESS &&
            element.green < MIN_BRIGHTNESS &&
            element.blue < MIN_BRIGHTNESS
        ) {
            total++
            if (total > 50) {
                return true
            }
        }
    }

    return false
}

object ColorUtil {

    private var gson = Gson()

    private var cropLeft = 0
    private var cropRight = 0
    private var cropTop = 0
    private var cropBottom = 0

    fun extractImage(context: Context, id: String, bitmap: Bitmap) {

        val detector: FirebaseVisionBarcodeDetector by lazy {

            val options = FirebaseVisionBarcodeDetectorOptions.Builder()
                .setBarcodeFormats(
                    FirebaseVisionBarcode.FORMAT_CODE_128
                )
                .build()
            FirebaseVision.getInstance().getVisionBarcodeDetector(options)
        }

        val leftBarcodeBitmap = Utilities.rotateImage(
            Bitmap.createBitmap(
                bitmap, 0, 0,
                (bitmap.width * 0.15).toInt(), bitmap.height
            ), 90
        )

        detector.detectInImage(FirebaseVisionImage.fromBitmap(leftBarcodeBitmap))
            .addOnFailureListener(
                fun(_: Exception) {
                    returnResult(context, id)
                }
            )
            .addOnSuccessListener(
                fun(result: List<FirebaseVisionBarcode>) {
                    if (result.isEmpty()) {
                        returnResult(context, id)
                    }
                    for (barcode in result) {
                        if (!barcode.rawValue.isNullOrEmpty()) {

                            val width = barcode.boundingBox!!.width()
                            val height = barcode.boundingBox!!.height()

                            if (height > bitmap.width * .050
                                && width > bitmap.height * .70
                            ) {
                                try {
                                    cropTop = barcode.boundingBox!!.left
                                    cropBottom = barcode.boundingBox!!.right
                                    cropLeft = barcode.boundingBox!!.bottom + 10

                                    for (i in barcode.boundingBox!!.bottom + 10
                                            until leftBarcodeBitmap.height) {
                                        val pixel = leftBarcodeBitmap.getPixel(
                                            leftBarcodeBitmap.width / 2, i
                                        )
                                        if (pixel.red < MIN_LINE_BRIGHTNESS &&
                                            pixel.green < MIN_LINE_BRIGHTNESS &&
                                            pixel.blue < MIN_LINE_BRIGHTNESS
                                        ) {
                                            cropLeft = i
                                            break
                                        }
                                    }

                                    leftBarcodeBitmap.recycle()

                                    val rightBarcodeBitmap = Utilities.rotateImage(
                                        Bitmap.createBitmap(
                                            bitmap,
                                            (bitmap.width * 0.85).toInt(),
                                            0,
                                            (bitmap.width * 0.15).toInt(),
                                            bitmap.height
                                        ), 270
                                    )

                                    detector.detectInImage(
                                        FirebaseVisionImage.fromBitmap(rightBarcodeBitmap)
                                    )
                                        .addOnFailureListener(fun(_: Exception) {
                                            returnResult(context, id)
                                        })
                                        .addOnSuccessListener(
                                            fun(result: List<FirebaseVisionBarcode>) {

                                                for (barcode2 in result) {
                                                    cropRight = barcode2.boundingBox!!.bottom + 10
                                                    for (i in barcode2.boundingBox!!.bottom + 10
                                                            until rightBarcodeBitmap.height) {
                                                        val pixel = rightBarcodeBitmap.getPixel(
                                                            rightBarcodeBitmap.width / 2, i
                                                        )
                                                        if (pixel.red < MIN_LINE_BRIGHTNESS &&
                                                            pixel.green < MIN_LINE_BRIGHTNESS &&
                                                            pixel.blue < MIN_LINE_BRIGHTNESS
                                                        ) {
                                                            cropRight = bitmap.width - i
                                                            break
                                                        }
                                                    }
                                                }

                                                rightBarcodeBitmap.recycle()
                                                analyzeBarcode(context, id, bitmap, result)
                                            }
                                        )
                                } catch (ignored: Exception) {
                                    returnResult(context, id)
                                }
                            } else {
                                returnResult(context, id)
                            }
                        } else {
                            returnResult(context, id)
                        }
                    }
                }
            )
    }

    private fun analyzeBarcode(
        context: Context, id: String,
        bitmap: Bitmap, result: List<FirebaseVisionBarcode>
    ) {
        if (result.isEmpty()) {
            returnResult(context, id)
        }
        for (barcode2 in result) {
            if (!barcode2.rawValue.isNullOrEmpty()) {
                if (barcode2.boundingBox!!.height() > bitmap.width * .050
                    && barcode2.boundingBox!!.width() > bitmap.height * .70
                ) {
                    cropTop = min(barcode2.boundingBox!!.left, cropTop)
                    cropBottom = max(barcode2.boundingBox!!.right, cropBottom)

                    val croppedBitmap = Bitmap.createBitmap(
                        bitmap, cropLeft, cropTop,
                        max(1, cropRight - cropLeft),
                        max(1, cropBottom - cropTop)
                    )

                    bitmap.recycle()

                    val resultDetail = extractColors(context, croppedBitmap)

                    Utilities.savePicture(
                        context.applicationContext, id,
                        "", Utilities.bitmapToBytes(croppedBitmap)
                    )
                    croppedBitmap.recycle()

                    returnResult(context, id, resultDetail)
                } else {
                    returnResult(context, id)
                }

            } else {
                returnResult(context, id)
            }
        }
    }

    private fun returnResult(
        context: Context, id: String,
        resultDetail: ResultDetail = ResultDetail((-1).toDouble(), 0)
    ) {
        val intent = Intent(App.LOCAL_RESULT_EVENT)
        intent.putExtra(App.TEST_ID_KEY, id)

        var result = (round(resultDetail.result * 100) / 100.0).toString()
        if (resultDetail.result == -1.0) {
            result = "Error"
        }

        intent.putExtra(App.TEST_RESULT, result)

        val localBroadcastManager = LocalBroadcastManager.getInstance(context)
        localBroadcastManager.sendBroadcast(intent)
    }

    private fun extractColors(context: Context, image: Bitmap): ResultDetail {
        try {
            val input = context.resources.openRawResource(R.raw.calibration)
            val paint = Paint()
            paint.style = Style.STROKE
            paint.color = Color.WHITE
            paint.strokeWidth = 4f
            paint.isAntiAlias = true

            val content = FileUtil.readTextFile(input)
            val calibration = gson.fromJson(content, Calibration::class.java)

            val canvas = Canvas(image)
            val padding = 8.0

            for (i in calibration.values) {
                val x = (i.x * image.width) / calibration.width
                val y = (i.y * image.height) / calibration.height
                val rectangle = Rect(
                    (x - padding).toInt(), (y - padding).toInt(),
                    (x + padding).toInt(), (y + padding).toInt()
                )

                val pixels = getBitmapPixels(image, rectangle)

                if (i.value != -1.0f) {
                    i.color = getAverageColor(pixels)
                    canvas.drawRect(rectangle, paint)
                }
            }

            val x1 = (410 * image.width) / calibration.width
            val y1 = (200 * image.height) / calibration.height
            val rectangle = Rect(x1 - 20, y1 - 27, x1 + 20, y1 + 27)
            val pixels = getBitmapPixels(image, rectangle)
            val colorInfo = ColorInfo(getAverageColor(pixels))

            canvas.drawRect(rectangle, paint)

            val swatches: ArrayList<Swatch> = ArrayList()
            swatches.add(Swatch(0.0, getCalibrationColor(0f, calibration)))
            swatches.add(Swatch(0.5, getCalibrationColor(.5f, calibration)))
            swatches.add(Swatch(1.0, getCalibrationColor(1f, calibration)))
            swatches.add(Swatch(1.5, getCalibrationColor(1.5f, calibration)))
            swatches.add(Swatch(2.0, getCalibrationColor(2f, calibration)))

            return analyzeColor(5, colorInfo, generateGradient(swatches))

        } catch (e: Exception) {
        }
        return ResultDetail((-1).toDouble(), 0)
    }

    private fun getCalibrationColor(pointValue: Float, calibration: Calibration): Int {
        var red = 0
        var green = 0
        var blue = 0

        var count = 0

        val filteredCalibrations = ArrayList<CalibrationValue>()

        for (i in calibration.values) {
            if (i.value == pointValue) {
                filteredCalibrations.add(i)
            }
        }

        val maxAllowedDistance = getCalibrationColorDistanceTolerance()
        for (i in filteredCalibrations) {
            for (j in filteredCalibrations) {
                if (getColorDistance(i.color, j.color) > maxAllowedDistance) {
                    throw Exception()
                }
            }
        }

        for (i in filteredCalibrations) {
            count += 1
            red += i.color.red
            green += i.color.green
            blue += i.color.blue
        }

        return Color.rgb(
            red / count, green / count, blue / count
        )
    }

    /**
     * Get the color that lies in between two colors
     *
     * @param startColor The first color
     * @param endColor   The last color
     * @param n          Number of steps between the two colors
     * @param i          The index at which the color is to be calculated
     * @return The newly generated color
     */
    private fun getGradientColor(startColor: Int, endColor: Int, n: Int, i: Int): Int {
        return Color.rgb(
            interpolate(startColor.red, endColor.red, n, i),
            interpolate(startColor.green, endColor.green, n, i),
            interpolate(startColor.blue, endColor.blue, n, i)
        )
    }

    /**
     * Get the color component that lies between the two color component points
     *
     * @param start The first color component value
     * @param end   The last color component value
     * @param n     Number of steps between the two colors
     * @param i     The index at which the color is to be calculated
     * @return The calculated color component
     */
    private fun interpolate(start: Int, end: Int, n: Int, i: Int): Int {
        return (start.toFloat() + (end.toFloat() - start.toFloat()) / n * i).toInt()
    }

    /**
     * Auto generate the color swatches for the given test type.
     *
     * @param swatches The test object
     * @return The list of generated color swatches
     */
    private fun generateGradient(swatches: ArrayList<Swatch>): ArrayList<Swatch> {

        val list = ArrayList<Swatch>()

        if (swatches.size < 2) {
            return list
        }

        // Predict 2 more points in the calibration list to account for high levels of contamination
        val swatch1 = swatches[swatches.size - 2]
        val swatch2 = swatches[swatches.size - 1]

        swatches.add(predictNextColor(swatch1, swatch2))
        swatches.add(predictNextColor(swatch2, swatches[swatches.size - 1]))

        for (i in 0 until swatches.size - 1) {

            val startColor = swatches[i].color
            val endColor = swatches[i + 1].color
            val startValue = swatches[i].value
            val endValue = swatches[i + 1].value
            val increment = (endValue - startValue) / INTERPOLATION_COUNT
            val steps = ((endValue - startValue) / increment).toInt()

            for (j in 0 until steps) {
                val color = getGradientColor(startColor, endColor, steps, j)
                list.add(Swatch(startValue + j * increment, color))
            }
        }

        list.add(
            Swatch(
                swatches[swatches.size - 1].value,
                swatches[swatches.size - 1].color
            )
        )

        return list
    }

    private fun predictNextColor(swatch1: Swatch, swatch2: Swatch): Swatch {

        val valueDiff = swatch2.value - swatch1.value

        val color1 = swatch1.color
        val color2 = swatch2.color
        val r = getNextLinePoint(Color.red(color1), Color.red(color2))
        val g = getNextLinePoint(Color.green(color1), Color.green(color2))
        val b = getNextLinePoint(Color.blue(color1), Color.blue(color2))

        return Swatch(swatch2.value + valueDiff, Color.rgb(r, g, b))
    }

    private fun getNextLinePoint(y: Int, y2: Int): Int {
        val diff = y2 - y
        return min(255, max(0, y2 + diff))
    }

    /**
     * Analyzes the color and returns a result info.
     *
     * @param photoColor The color to compare
     * @param swatches   The range of colors to compare against
     */
    @Suppress("SameParameterValue")
    private fun analyzeColor(
        steps: Int,
        photoColor: ColorInfo,
        swatches: List<Swatch>
    ): ResultDetail {

        val colorCompareInfo: ColorCompareInfo =
            getNearestColorFromSwatches(photoColor.color, swatches)

        //Find the color within the generated gradient that matches the photoColor

        //set the result
        val resultDetail = ResultDetail((-1).toDouble(), photoColor.color)
        if (colorCompareInfo.result > -1) {
            resultDetail.result = colorCompareInfo.result
        }
        resultDetail.calibrationSteps = steps
        resultDetail.matchedColor = colorCompareInfo.matchedColor
        resultDetail.distance = colorCompareInfo.distance

        return resultDetail
    }

    /**
     * Compares the colorToFind to all colors in the color range and finds the nearest matching color.
     *
     * @param colorToFind The colorToFind to compare
     * @param swatches    The range of colors from which to return the nearest colorToFind
     * @return details of the matching color with its corresponding value
     */
    private fun getNearestColorFromSwatches(
        colorToFind: Int, swatches: List<Swatch>
    ): ColorCompareInfo {

        var distance = getMaxDistance(getColorDistanceTolerance().toDouble())

        var resultValue = -1.0
        var matchedColor = -1
        var tempDistance: Double
        var nearestDistance = MAX_DISTANCE.toDouble()
        var nearestMatchedColor = -1

        for (i in swatches.indices) {
            val tempColor = swatches[i].color

            tempDistance = getColorDistance(tempColor, colorToFind)
            if (nearestDistance > tempDistance) {
                nearestDistance = tempDistance
                nearestMatchedColor = tempColor
            }

            if (tempDistance == 0.0) {
                resultValue = swatches[i].value
                matchedColor = swatches[i].color
                break
            } else if (tempDistance < distance) {
                distance = tempDistance
                resultValue = swatches[i].value
                matchedColor = swatches[i].color
            }
        }

        //if no result was found add some diagnostic info
        if (resultValue == -1.0) {
            distance = nearestDistance
            matchedColor = nearestMatchedColor
        }
        return ColorCompareInfo(resultValue, colorToFind, matchedColor, distance)
    }

    private fun getMaxDistance(defaultValue: Double): Double {
        return if (defaultValue > 0) {
            defaultValue
        } else {
            MAX_COLOR_DISTANCE_RGB.toDouble()
        }
    }

    /**
     * Computes the Euclidean distance between the two colors
     *
     * @param color1 the first color
     * @param color2 the color to compare with
     * @return the distance between the two colors
     */
    private fun getColorDistance(color1: Int, color2: Int): Double {
        val r: Double = (Color.red(color2) - Color.red(color1)).toDouble().pow(2.0)
        val g: Double = (Color.green(color2) - Color.green(color1)).toDouble().pow(2.0)
        val b: Double = (Color.blue(color2) - Color.blue(color1)).toDouble().pow(2.0)

        return sqrt(b + g + r)
    }
}
