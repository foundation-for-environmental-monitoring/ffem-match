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
import io.ffem.lite.camera.MAX_ANGLE
import io.ffem.lite.camera.Utilities
import io.ffem.lite.model.*
import io.ffem.lite.preference.getCalibrationColorDistanceTolerance
import io.ffem.lite.preference.getColorDistanceTolerance
import timber.log.Timber
import java.util.*
import kotlin.math.*

const val MAX_COLOR_DISTANCE_RGB = 80
const val MAX_COLOR_DISTANCE_CALIBRATION = 40
const val INTERPOLATION_COUNT = 100.0
const val MAX_DISTANCE = 999
const val MIN_BRIGHTNESS = 20

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

fun hasBlackPixelsOnLine(bitmap: Bitmap, row: Int): Boolean {
    var total = 0

    val pixels = getBitmapPixels(
        bitmap,
        Rect(0, row, bitmap.width, row + 1)
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

    private var interval = 0

    fun extractImage(context: Context, id: String, bitmapImage: Bitmap) {

        val detector: FirebaseVisionBarcodeDetector by lazy {

            val options = FirebaseVisionBarcodeDetectorOptions.Builder()
                .setBarcodeFormats(
                    FirebaseVisionBarcode.FORMAT_CODE_128
                )
                .build()
            FirebaseVision.getInstance().getVisionBarcodeDetector(options)
        }

        val bitmap = Utilities.rotateImage(bitmapImage, 90)

        val leftBarcodeBitmap = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height / 2
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

//                            if (barcode.boundingBox!!.width() > bitmap.width * .44 &&
//                                barcode.boundingBox!!.width() < bitmap.width * .48
//                            ) {
                            try {
                                cropTop =
                                    (bitmap.width - barcode.boundingBox!!.right - barcode.boundingBox!!.left)
                                cropBottom = (bitmap.width - cropTop)
                                cropLeft = barcode.boundingBox!!.bottom + 5

                                for (i in cropLeft..barcode.boundingBox!!.bottom + 100) {
                                    if (!hasBlackPixelsOnLine(leftBarcodeBitmap, i)) {
                                        cropLeft = i + 5
                                        break
                                    }
                                }

                                leftBarcodeBitmap.recycle()

                                val rightBarcodeBitmap =
                                    Bitmap.createBitmap(
                                        bitmap,
                                        0,
                                        bitmap.height / 2,
                                        bitmap.width,
                                        bitmap.height / 2
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

                                                // Check if image angle is ok
                                                if (abs(
                                                        barcode.boundingBox!!.left
                                                                - barcode2.boundingBox!!.left
                                                    ) > MAX_ANGLE ||
                                                    abs(
                                                        barcode.boundingBox!!.right
                                                                - barcode2.boundingBox!!.right
                                                    ) > MAX_ANGLE
                                                ) {
                                                    returnResult(context, id)
                                                }

                                                cropRight = barcode2.boundingBox!!.top - 5

                                                for (i in cropRight downTo cropRight - 100) {
                                                    if (!hasBlackPixelsOnLine(
                                                            rightBarcodeBitmap,
                                                            i
                                                        )
                                                    ) {
                                                        cropRight = i - 5
                                                        break
                                                    }
                                                }

                                                rightBarcodeBitmap.recycle()
                                                analyzeBarcode(context, id, bitmap, result)
                                            }

                                        }
                                    )
                            } catch (ignored: Exception) {
                                returnResult(context, id)
                            }
//                            } else {
//                                returnResult(context, id)
//                            }
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
//                if (barcode2.boundingBox!!.width() > bitmap.width * .44 &&
//                    barcode2.boundingBox!!.width() < bitmap.width * .48
//                ) {

                val input = context.resources.openRawResource(R.raw.calibration)
                val content = FileUtil.readTextFile(input)
                val testConfig = Gson().fromJson(content, TestConfig::class.java)

                var testName = ""
                for (test in testConfig.tests) {
                    if (test.uuid == result[0].displayValue!!) {
                        testName = test.name!!
                        break
                    }
                }

                if (testName.isEmpty()) {
                    returnResult(context, id)
                }

                var bitmapRotated = Utilities.rotateImage(bitmap, 270)

                cropTop = max(0, cropTop - 10)
                val height = min(
                    max(1, cropBottom - cropTop + 10),
                    bitmapRotated.height - cropTop
                )

                bitmapRotated = Bitmap.createBitmap(
                    bitmapRotated, 0, cropTop,
                    bitmapRotated.width, height
                )

                val croppedBitmap = Bitmap.createBitmap(
                    bitmapRotated, max(1, cropLeft), 0,
                    max(1, cropRight + ((bitmapRotated.width / 2) - cropLeft)),
                    bitmapRotated.height
                )

                bitmap.recycle()

                val resultDetail = extractColors(
                    context,
                    croppedBitmap, result[0].displayValue!!
                )

                Utilities.savePicture(
                    context.applicationContext, id,
                    "", Utilities.bitmapToBytes(croppedBitmap)
                )
                croppedBitmap.recycle()

                bitmapRotated.recycle()

                returnResult(context, id, testName, resultDetail)
//                } else {
//                    returnResult(context, id)
//                }

            } else {
                returnResult(context, id)
            }
        }
    }

    private fun returnResult(
        context: Context, id: String, testName: String = "",
        resultDetail: ResultDetail = ResultDetail((-1).toDouble(), 0)
    ) {
        val intent = Intent(App.LOCAL_RESULT_EVENT)
        intent.putExtra(App.TEST_ID_KEY, id)
        intent.putExtra(App.TEST_NAME_KEY, testName)

        var result = (round(resultDetail.result * 100) / 100.0).toString()
        if (resultDetail.result == -1.0) {
            result = "Error"
        }

        intent.putExtra(App.TEST_RESULT, result)

        val localBroadcastManager = LocalBroadcastManager.getInstance(context)
        localBroadcastManager.sendBroadcast(intent)
    }

    private fun extractColors(
        context: Context,
        image: Bitmap,
        id: String
    ): ResultDetail {
        try {
            val input = context.resources.openRawResource(R.raw.calibration)
            val paint = Paint()
            paint.style = Style.STROKE
            paint.color = Color.WHITE
            paint.strokeWidth = 4f
            paint.isAntiAlias = true

            val content = FileUtil.readTextFile(input)
            val testConfig = gson.fromJson(content, TestConfig::class.java)

            var calibration: List<CalibrationValue> = testConfig.tests[0].values
            for (test in testConfig.tests) {
                if (test.uuid == id) {
                    calibration = test.values
                    break
                }
            }

            val canvas = Canvas(image)

            val centerPointLeft = getSquareCenter(
                image,
                calibration.size / 2, (image.width * 0.12).toInt()
            )

            interval += (image.height * 0.01).toInt()

            interval = min(
                interval, image.height / (calibration.size / 2)
            )

            val padding = interval / 10

            for (i in 0 until (calibration.size / 2)) {

                val cal = calibration[i]
                val rectangle = Rect(
                    max(1, centerPointLeft.x - padding),
                    max(1, centerPointLeft.y - padding),
                    min(image.width, centerPointLeft.x + padding),
                    min(image.height, centerPointLeft.y + padding)
                )

                centerPointLeft.y += interval

                val pixels = getBitmapPixels(image, rectangle)

                cal.color = getAverageColor(pixels)
                canvas.drawRect(rectangle, paint)
            }

            val centerPointRight = getSquareCenter(
                image, calibration.size / 2, (image.width * 0.88).toInt()
            )

            interval += (image.height * 0.01).toInt()

            interval = min(
                interval, image.height / (calibration.size / 2)
            )

            for (i in calibration.size / 2 until calibration.size) {

                val cal = calibration[i]
                val rectangle = Rect(
                    centerPointRight.x - padding, centerPointRight.y - padding,
                    centerPointRight.x + padding, centerPointRight.y + padding
                )

                centerPointRight.y += interval

                val pixels = getBitmapPixels(image, rectangle)

                cal.color = getAverageColor(pixels)
                canvas.drawRect(rectangle, paint)
            }

            val x1 = ((centerPointRight.x - centerPointLeft.x) / 2) + centerPointLeft.x
            val y1 = ((image.height) / 2) + 80
            val rectangle = Rect(x1 - 20, y1 - 27, x1 + 20, y1 + 35)
            val pixels = getBitmapPixels(image, rectangle)
            val colorInfo = ColorInfo(getAverageColor(pixels))

            canvas.drawRect(rectangle, paint)

            val swatches: ArrayList<Swatch> = ArrayList()

            for (cal in calibration) {
                if (swatches.size >= calibration.size / 2) {
                    break
                }

                if (cal.value >= 0) {
                    swatches.add(
                        Swatch(
                            cal.value.toDouble(),
                            getCalibrationColor(cal.value, calibration)
                        )
                    )
                }
            }

            return analyzeColor(swatches.size, colorInfo, generateGradient(swatches))

        } catch (e: Exception) {
            Timber.e(e)
        }
        return ResultDetail((-1).toDouble(), 0)
    }

    private fun getSquareCenter(image: Bitmap, intervals: Int, tempLeft: Int): Point {
        val tempInterval = image.height / intervals
        var top = image.height / intervals
        var bottom = top
        var left = tempLeft
        var right = left

        val colorDifference = 60

        val refPixel = image.getPixel(left, top)
        for (y in top downTo 0) {
            val pixel = image.getPixel(left, y)

            if ((refPixel.red - pixel.red) > colorDifference ||
                (refPixel.green - pixel.green) > colorDifference ||
                (refPixel.blue - pixel.blue) > colorDifference
            ) {
                top = y
                break
            }
        }

        for (y in bottom until bottom + tempInterval + 10) {
            val pixel = image.getPixel(left, y)

            if ((refPixel.red - pixel.red) > colorDifference ||
                (refPixel.green - pixel.green) > colorDifference ||
                (refPixel.blue - pixel.blue) > colorDifference
            ) {
                bottom = y
                break
            }
        }

        val centerY = ((bottom - top) / 2) + top

        for (x in left downTo 0) {
            val pixel = image.getPixel(x, centerY)

            if ((refPixel.red - pixel.red) > colorDifference ||
                (refPixel.green - pixel.green) > colorDifference ||
                (refPixel.blue - pixel.blue) > colorDifference
            ) {
                left = x
                break
            }
        }

        for (x in right until right + tempInterval + 10) {
            val pixel = image.getPixel(x, centerY)

            if ((refPixel.red - pixel.red) > colorDifference ||
                (refPixel.green - pixel.green) > colorDifference ||
                (refPixel.blue - pixel.blue) > colorDifference
            ) {
                right = x
                break
            }
        }

        val centerX = ((right - left) / 2) + left

        interval = (bottom - top)

        return Point(centerX, centerY)
    }

    private fun getCalibrationColor(pointValue: Float, calibration: List<CalibrationValue>): Int {
        var red = 0
        var green = 0
        var blue = 0

        var count = 0

        val filteredCalibrations = ArrayList<CalibrationValue>()

        for (i in calibration) {
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
