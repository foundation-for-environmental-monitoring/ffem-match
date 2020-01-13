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
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.app.App.Companion.getCalibration
import io.ffem.lite.app.App.Companion.getTestName
import io.ffem.lite.app.AppDatabase
import io.ffem.lite.camera.MAX_ANGLE
import io.ffem.lite.camera.Utilities
import io.ffem.lite.model.*
import io.ffem.lite.preference.getCalibrationColorDistanceTolerance
import io.ffem.lite.preference.getColorDistanceTolerance
import java.util.*
import kotlin.math.*

const val MAX_COLOR_DISTANCE_RGB = 80
const val MAX_COLOR_DISTANCE_CALIBRATION = 80
const val INTERPOLATION_COUNT = 100.0
const val MAX_DISTANCE = 999
//const val MIN_BRIGHTNESS = 30

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

fun isDarkLine(pixels: IntArray): Boolean {
    var r = 0

    if (pixels.isEmpty()) {
        return false
    }

    for (element in pixels) {
        r += element.red
    }

    return (r / pixels.size) < 180
}


fun isDark(pixels: IntArray): Boolean {
    var r = 0

    if (pixels.isEmpty()) {
        return false
    }

    for (element in pixels) {
        r += element.red
    }

    return (r / pixels.size) < 170
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

object ColorUtil {

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

        var badLighting = false

        val leftBarcodeBitmap = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height / 2
        )

//        val leftBarcodeBitmap = ImageUtil.toBlackAndWhite(leftBarcodeBitmapColor, 110)
//        leftBarcodeBitmapColor.recycle()

        detector.detectInImage(FirebaseVisionImage.fromBitmap(leftBarcodeBitmap))
            .addOnFailureListener(
                fun(_: Exception) {
                    returnResult(context, id)
                }
            )
            .addOnSuccessListener(
                fun(result: List<FirebaseVisionBarcode>) {
                    if (result.isEmpty()) {
                        returnResult(context, id, R.string.bad_lighting, bitmap)
                    }
                    for (leftBarcode in result) {
                        if (!leftBarcode.rawValue.isNullOrEmpty()) {
//                            if (leftBarcode.boundingBox!!.width() > bitmap.width * .44) {
                            try {

                                val testName = getTestName(result[0].displayValue!!)

                                val leftBoundingBox = fixBoundary(leftBarcode, leftBarcodeBitmap)

                                if (!isBarcodeValid(leftBarcodeBitmap, leftBoundingBox, true)) {
                                    badLighting = true
                                }

                                val rightBarcodeBitmap = Bitmap.createBitmap(
                                    bitmap, 0, bitmap.height / 2,
                                    bitmap.width, bitmap.height / 2
                                )

//                                val rightBarcodeBitmap =
//                                    ImageUtil.toBlackAndWhite(rightBarcodeBitmapColor, 110)
//                                rightBarcodeBitmapColor.recycle()

                                detector.detectInImage(
                                    FirebaseVisionImage.fromBitmap(rightBarcodeBitmap)
                                )
                                    .addOnFailureListener(fun(_: Exception) {
                                        returnResult(
                                            context,
                                            id,
                                            R.string.bad_lighting,
                                            bitmap,
                                            testName
                                        )
                                    })
                                    .addOnSuccessListener(
                                        fun(result: List<FirebaseVisionBarcode>) {
                                            if (result.isEmpty()) {
                                                returnResult(
                                                    context,
                                                    id,
                                                    R.string.bad_lighting,
                                                    bitmap,
                                                    testName
                                                )
                                                return
                                            }

                                            for (rightBarcode in result) {

                                                val rightBoundingBox =
                                                    fixBoundary(rightBarcode, rightBarcodeBitmap)

                                                if (isTilted(leftBoundingBox, rightBoundingBox)) {
                                                    returnResult(
                                                        context, id,
                                                        R.string.image_tilted, bitmap, testName
                                                    )
                                                    return
                                                }

                                                if (badLighting || !isBarcodeValid(
                                                        rightBarcodeBitmap, rightBoundingBox, false
                                                    )
                                                ) {
                                                    returnResult(
                                                        context, id,
                                                        R.string.bad_lighting, bitmap, testName
                                                    )
                                                    return
                                                }

                                                analyzeBarcode(
                                                    context, id, bitmap, rightBarcode,
                                                    rightBoundingBox, leftBoundingBox
                                                )
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
                            return
                        }
                    }
                }
            )
    }

    private fun analyzeBarcode(
        context: Context, id: String, bitmap: Bitmap, rightBarcode: FirebaseVisionBarcode,
        rightBoundingBox: Rect, leftBoundingBox: Rect
    ) {

        if (!rightBarcode.rawValue.isNullOrEmpty()) {
//                if (barcode2.boundingBox!!.width() > bitmap.width * .44) {

            val testName = getTestName(rightBarcode.displayValue!!)
            if (testName.isEmpty()) {
                returnResult(context, id, R.string.invalid_barcode, bitmap)
                return
            }

            val cropLeft = max(leftBoundingBox.left - 20, 0)
            val cropWidth = min(
                leftBoundingBox.right - cropLeft + 40,
                bitmap.width - cropLeft
            )

            val cropTop = leftBoundingBox.bottom + 2
            val cropHeight = min(
                rightBoundingBox.top - leftBoundingBox.bottom + (bitmap.height / 2) - 4,
                bitmap.height - cropTop
            )

            val finalBitmap = Bitmap.createBitmap(
                bitmap, cropLeft, cropTop, cropWidth, cropHeight
            )

            val croppedBitmap1 = Utilities.rotateImage(finalBitmap, 270)

            val bwCroppedBitmap1 = ImageUtil.toBlackAndWhite(croppedBitmap1, 100)
            var top = 0
            var bottom = 0

            var left = 0
            for (x in 1 until 100) {
                val rectangle = Rect(x, 10, x + 1, bwCroppedBitmap1.height - 10)
                val pixels = getBitmapPixels(bwCroppedBitmap1, rectangle)
                if (isDarkLine(pixels)) {
                    left = x
                    break
                }
            }

            var right = 0
            for (x in bwCroppedBitmap1.width - 1 downTo bwCroppedBitmap1.width - 100) {
                val rectangle = Rect(x - 1, 10, x, bwCroppedBitmap1.height - 10)
                val pixels = getBitmapPixels(bwCroppedBitmap1, rectangle)
                if (isDarkLine(pixels)) {
                    right = x
                    break
                }
            }

            val croppedBitmap2 = Bitmap.createBitmap(
                croppedBitmap1, left, 0,
                right - left,
                croppedBitmap1.height
            )

            val bwCroppedBitmap2 = ImageUtil.toBlackAndWhite(croppedBitmap2, 100)
            bwCroppedBitmap1.recycle()

            for (y in 1 until 100) {
                val rectangle = Rect(3, y, 70, y + 3)
                val rectangleRight =
                    Rect(bwCroppedBitmap2.width - 70, y, bwCroppedBitmap2.width - 1, y + 3)
                val pixels = getBitmapPixels(bwCroppedBitmap2, rectangle)
                val pixelsRight = getBitmapPixels(bwCroppedBitmap2, rectangleRight)
                if (isDarkLine(pixels) && isDarkLine(pixelsRight)) {
                    top = y
                    break
                }
            }

            for (y in bwCroppedBitmap2.height - 3 downTo 0) {
                val pixel =
                    bwCroppedBitmap2.getPixel((bwCroppedBitmap2.width * 0.15).toInt(), y)
                if (isDarkPixel(pixel)) {
                    bottom = y
                    break
                }
            }

            bwCroppedBitmap2.recycle()

            val croppedBitmap = Bitmap.createBitmap(
                croppedBitmap2, 0, top,
                croppedBitmap2.width,
                max(1, bottom - top)
            )

            var error = -1
            var resultDetail = ResultDetail(-1.0, 0)
            try {
                resultDetail = extractColors(croppedBitmap, rightBarcode.displayValue!!)
            } catch (e: Exception) {
                error = R.string.calibration_error
            }

            Utilities.savePicture(
                context.applicationContext, id,
                testName + "_swatch", Utilities.bitmapToBytes(croppedBitmap)
            )
            croppedBitmap.recycle()

//            val db = AppDatabase.getDatabase(context)
//            if (db.resultDao().getResult(id) == null) {
//                Utilities.savePicture(
//                    context,
//                    id,
//                    testName,
//                    Utilities.bitmapToBytes(bitmap)
//                )
//
//
//            }

            croppedBitmap1.recycle()
            finalBitmap.recycle()

            returnResult(context, id, error, bitmap, testName, resultDetail)
//                } else {
//                    returnResult(context, id)
//                }

        } else {
            returnResult(context, id)
            return
        }

    }

    private fun returnResult(
        context: Context,
        id: String,
        error: Int = R.string.error,
        bitmap: Bitmap? = null,
        testName: String = "Unknown",
        resultDetail: ResultDetail = ResultDetail((-1).toDouble(), 0)
    ) {
        val intent = Intent(App.LOCAL_RESULT_EVENT)
        intent.putExtra(App.TEST_ID_KEY, id)
        intent.putExtra(App.TEST_NAME_KEY, testName)

        var result = (round(resultDetail.result * 100) / 100.0).toString()
        if (resultDetail.result < 0) {
            result = if (error == -1) {
                "No match"
            } else {
                context.getString(error)
            }
        }

        intent.putExtra(App.TEST_RESULT, result)

        val db = AppDatabase.getDatabase(context)
        if (db.resultDao().getResult(id) == null) {
            if (bitmap != null) {
                val bitmapRotated = Utilities.rotateImage(bitmap, 270)
                Utilities.savePicture(
                    context,
                    id,
                    testName,
                    Utilities.bitmapToBytes(bitmapRotated)
                )

                val expectedValue = PreferencesUtil
                    .getString(context, R.string.expectedValueKey, "")

                db.resultDao().insert(
                    TestResult(
                        id, 0, testName,
                        Date().time, Date().time, "", "",
                        expectedValue, context.getString(R.string.outbox)
                    )
                )

                bitmap.recycle()
                bitmapRotated.recycle()
            }

            val expectedValue = PreferencesUtil
                .getString(context, R.string.expectedValueKey, "")

            db.resultDao().insert(
                TestResult(
                    id, 0, testName,
                    Date().time, Date().time, "", "",
                    expectedValue, ""
                )
            )
        }

        val localBroadcastManager = LocalBroadcastManager.getInstance(context)
        localBroadcastManager.sendBroadcast(intent)
    }

    private fun extractColors(image: Bitmap, barcodeValue: String): ResultDetail {

        val bitmap = ImageUtil.toBlackAndWhite(image, 100)

        val paint = Paint()
        paint.style = Style.STROKE
        paint.color = Color.WHITE
        paint.strokeWidth = 2f
        paint.isAntiAlias = true

        val calibration: List<CalibrationValue> = getCalibration(barcodeValue)

        val intervals = calibration.size / 2
        val commonTop = bitmap.height / intervals
        val padding = commonTop / 7
        var calibrationIndex = 0

        val points = getMarkers(bitmap)

        val commonLeft = points.x
        for (i in 0 until intervals) {
            val rectangle = Rect(
                max(1, commonLeft - padding),
                max(1, (commonTop * i) + (commonTop / 2) - padding),
                min(bitmap.width, commonLeft + padding),
                min(bitmap.height, (commonTop * i) + (commonTop / 2) + padding)
            )

            val pixels = getBitmapPixels(image, rectangle)

            val cal = calibration[calibrationIndex]
            calibrationIndex++
            cal.color = getAverageColor(pixels)

            val canvas = Canvas(image)
            canvas.drawRect(rectangle, paint)
        }

        val commonRight = points.y
        for (i in 0 until intervals) {
            val rectangle = Rect(
                max(1, commonRight - padding),
                max(1, (commonTop * i) + (commonTop / 2) - padding),
                min(bitmap.width, commonRight + padding),
                min(bitmap.height, (commonTop * i) + (commonTop / 2) + padding)
            )

            val pixels = getBitmapPixels(image, rectangle)

            val cal = calibration[calibrationIndex]
            calibrationIndex++
            cal.color = getAverageColor(pixels)

            val canvas = Canvas(image)
            canvas.drawRect(rectangle, paint)
        }

        val x1 = ((commonRight - commonLeft) / 2) + commonLeft
        val y1 = ((bitmap.height) / 2) + (bitmap.height * 0.1).toInt()
        val rectangle = Rect(x1 - 20, y1 - 27, x1 + 20, y1 + 35)
        val pixels = getBitmapPixels(image, rectangle)
        val colorInfo = ColorInfo(getAverageColor(pixels))

        val canvas = Canvas(image)
        canvas.drawRect(rectangle, paint)

        val swatches: ArrayList<Swatch> = ArrayList()

        for (cal in calibration) {
            if (swatches.size >= calibration.size / 2) {
                break
            }

            swatches.add(
                Swatch(
                    cal.value.toDouble(),
                    getCalibrationColor(cal.value, calibration)
                )
            )
        }

        return analyzeColor(swatches.size, colorInfo, generateGradient(swatches))
    }

    fun isTilted(
        barcode: Rect,
        otherBarcode: Rect
    ): Boolean {
        val left = barcode.left
        val right = barcode.right
        if (abs(left - otherBarcode.left) > MAX_ANGLE ||
            abs(right - otherBarcode.right) > MAX_ANGLE
        ) {
            return true
        }
        return false
    }

    fun fixBoundary(barcode: FirebaseVisionBarcode, barcodeBitmap: Bitmap): Rect {
        val bwBitmap = ImageUtil.toBlackAndWhite(barcodeBitmap, 100)

        var top = barcode.boundingBox!!.top
        var left = barcode.boundingBox!!.left
        val right = barcode.boundingBox!!.right
        var bottom = barcode.boundingBox!!.bottom
        val midY = ((bottom - top) / 2) + top

        for (x in left until left + 50) {
            val pixel = bwBitmap.getPixel(x, midY)
            if (isDarkPixel(pixel)) {
                left = min(x, barcode.boundingBox!!.left) + 2
                break
            }
        }

        for (y in midY downTo 1) {
            top = y
            val pixel = bwBitmap.getPixel(left, top)
            if (!isDarkPixel(pixel)) {
                top = min(top, barcode.boundingBox!!.top)
                break
            }
        }

        for (y in midY until min(midY + 150, bwBitmap.height)) {
            bottom = y
            val pixel = bwBitmap.getPixel(left, bottom)
            if (!isDarkPixel(pixel)) {
                bottom = max(bottom, barcode.boundingBox!!.bottom)
                break
            }
        }

        return Rect(left, top, right, bottom)
    }

    fun isBarcodeValid(
        barcodeBitmap: Bitmap, barcode: Rect, isLeft: Boolean
    ): Boolean {

        var valid = true
        val bwBitmap = ImageUtil.toBlackAndWhite(barcodeBitmap, 110)

        val top = barcode.top
        val left = barcode.left
        val right = barcode.right
        val bottom = barcode.bottom

        val margin1 = 5
        val margin2 = 7

        if (top < margin1 || left < margin1 ||
            right > bwBitmap.width - margin2 || bottom > bwBitmap.height - (margin2 * 2)
        ) {
            valid = false
        }

        var rect: Rect
        var pixels: IntArray

        try {

            if (valid) {
                rect = Rect(left - margin2, top, left - margin1, bottom)
                pixels = getBitmapPixels(bwBitmap, rect)
                valid = !isDark(pixels)
            }

            if (valid) {
                rect = Rect(right + margin1, top, right + margin2, bottom)
                pixels = getBitmapPixels(bwBitmap, rect)
                valid = !isDark(pixels)
            }

            var margin3 = 0
            if (isLeft) {
                margin3 = 10
            }

            if (valid) {
                rect = Rect(left, top - margin2 - margin3, right, top - margin1 - margin3)
                pixels = getBitmapPixels(bwBitmap, rect)
                valid = !isDark(pixels)
            }

            margin3 = if (!isLeft) {
                10
            } else {
                0
            }

            if (valid) {
                rect = Rect(left, bottom + margin1 + margin3, right, bottom + margin2 + margin3)
                pixels = getBitmapPixels(bwBitmap, rect)
                valid = !isDark(pixels)
            }

            bwBitmap.recycle()
        } catch (ex: Exception) {
            valid = false
        }

        return valid
    }

    private fun getMarkers(
        bitmap: Bitmap
    ): Point {
        val leftSquareCenter = (bitmap.width * 0.12).toInt()
        val rightSquareCenter = bitmap.width - (bitmap.width * 0.12).toInt()

        return Point(leftSquareCenter, rightSquareCenter)
    }

    private fun isDarkPixel(pixel: Int): Boolean {
        return pixel.red < 90 || pixel.green < 90
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
