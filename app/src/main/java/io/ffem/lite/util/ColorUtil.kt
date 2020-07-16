@file:Suppress("SpellCheckingInspection")

package io.ffem.lite.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.Paint.Style
import android.os.Environment
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import io.ffem.lite.app.App
import io.ffem.lite.app.App.Companion.DEFAULT_TEST_UUID
import io.ffem.lite.app.App.Companion.TEST_ID_KEY
import io.ffem.lite.app.App.Companion.TEST_VALUE_KEY
import io.ffem.lite.app.App.Companion.getCardColors
import io.ffem.lite.app.App.Companion.getTestInfo
import io.ffem.lite.app.AppDatabase
import io.ffem.lite.camera.MAX_ANGLE
import io.ffem.lite.camera.Utilities
import io.ffem.lite.model.*
import io.ffem.lite.model.ErrorType.*
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.preference.getCalibrationColorDistanceTolerance
import io.ffem.lite.preference.getColorDistanceTolerance
import io.ffem.lite.preference.getSampleTestImageNumber
import timber.log.Timber
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.*

const val IMAGE_THRESHOLD = 130
const val MAX_COLOR_DISTANCE_RGB = 60
const val MAX_COLOR_DISTANCE_CALIBRATION = 80
const val INTERPOLATION_COUNT = 100.0
const val MAX_DISTANCE = 999

/**
 * Computes the Euclidean distance between the two colors
 *
 * @param color1 the first color
 * @param color2 the color to compare with
 * @return the distance between the two colors
 */
fun getColorDistance(color1: Int, color2: Int): Double {
    val r: Double = (Color.red(color2) - Color.red(color1)).toDouble().pow(2.0)
    val g: Double = (Color.green(color2) - Color.green(color1)).toDouble().pow(2.0)
    val b: Double = (Color.blue(color2) - Color.blue(color1)).toDouble().pow(2.0)
    return sqrt(b + g + r)
}

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

    return (r / pixels.size) < 150
}


fun isDark(pixels: IntArray): Boolean {
    var r = 0

    if (pixels.isEmpty()) {
        return false
    }

    for (element in pixels) {
        r += element.red
    }

    return (r / pixels.size) < 140
}

fun isNotBright(pixels: IntArray): Boolean {
    var r = 0

    if (pixels.isEmpty()) {
        return false
    }

    for (element in pixels) {
        r += element.red
    }

    return (r / pixels.size) < 110
}


fun isWhite(pixels: IntArray): Boolean {
    var r = 0

    if (pixels.isEmpty()) {
        return false
    }

    for (element in pixels) {
        r += element.red
    }

    return (r / pixels.size) > 240
}

fun getAverageColor(pixels: IntArray): Int {
    var list = pixels.toCollection(ArrayList())
    for (i in 40 downTo 10 step 3) {
        list = removeOutliers(list, i)
    }

    // Reject if too few pixels remaining after removing outliers
    if (list.size < pixels.size / 10) {
        return Color.TRANSPARENT
    }
    return getMean(list)
}

fun removeOutliers(pixels: ArrayList<Int>, distance: Int): ArrayList<Int> {
    val meanColor = getMean(pixels)
    val newList = ArrayList<Int>()
    for (element in pixels) {
        if (getColorDistance(
                meanColor,
                Color.rgb(element.red, element.green, element.blue)
            ) < distance
        ) {
            newList.add(element)
        }
    }
    return newList
}

fun getMean(pixels: ArrayList<Int>): Int {
    var r = 0
    var g = 0
    var b = 0

    for (element in pixels) {
        r += element.red
        g += element.green
        b += element.blue
    }

    return Color.rgb(r / pixels.size, g / pixels.size, b / pixels.size)
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

        var bitmap: Bitmap
        bitmap = when {
            bitmapImage.width.toDouble() / bitmapImage.height > 1.58 -> {
                cropImageToHalfSize(bitmapImage)
            }
            bitmapImage.height.toDouble() / bitmapImage.width > 1.58 -> {
                bitmap = Utilities.rotateImage(bitmapImage, 90)
                cropImageToHalfSize(bitmap)
            }
            else -> {
                bitmapImage
            }
        }

        if (bitmap.width > bitmap.height) {
            bitmap = Utilities.rotateImage(bitmap, 90)
        }

        var badLighting = false

        val leftBarcodeBitmap = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height / 2
        )

        detector.detectInImage(FirebaseVisionImage.fromBitmap(leftBarcodeBitmap))
            .addOnFailureListener(
                fun(_: Exception) {
                    returnResult(context)
                }
            )
            .addOnSuccessListener(
                fun(result: List<FirebaseVisionBarcode>) {
                    if (result.isEmpty()) {
                        returnResult(context, getTestInfo(DEFAULT_TEST_UUID), BAD_LIGHTING, bitmap)
                    }
                    for (leftBarcode in result) {
                        if (!leftBarcode.rawValue.isNullOrEmpty()) {
                            try {
                                val testInfo = getTestInfo(result[0].displayValue!!)

                                val leftBoundingBox = fixBoundary(
                                    leftBarcode,
                                    leftBarcodeBitmap,
                                    ImageEdgeType.WhiteTop
                                )

                                if (!isBarcodeValid(
                                        leftBarcodeBitmap,
                                        leftBoundingBox,
                                        ImageEdgeType.WhiteTop
                                    )
                                ) {
                                    badLighting = true
                                }

                                val rightBarcodeBitmap = Bitmap.createBitmap(
                                    bitmap, 0, bitmap.height / 2,
                                    bitmap.width, bitmap.height / 2
                                )

                                detector.detectInImage(
                                    FirebaseVisionImage.fromBitmap(rightBarcodeBitmap)
                                )
                                    .addOnFailureListener(fun(_: Exception) {
                                        returnResult(
                                            context,
                                            testInfo,
                                            BAD_LIGHTING,
                                            bitmap
                                        )
                                    })
                                    .addOnSuccessListener(
                                        fun(result: List<FirebaseVisionBarcode>) {
                                            if (result.isEmpty()) {
                                                returnResult(
                                                    context,
                                                    testInfo,
                                                    BAD_LIGHTING,
                                                    bitmap
                                                )
                                                return
                                            }

                                            for (rightBarcode in result) {

                                                val rightBoundingBox =
                                                    fixBoundary(
                                                        rightBarcode,
                                                        rightBarcodeBitmap,
                                                        ImageEdgeType.WhiteDown
                                                    )

                                                if (isTilted(leftBoundingBox, rightBoundingBox)) {
                                                    returnResult(
                                                        context, testInfo,
                                                        IMAGE_TILTED, bitmap
                                                    )
                                                    return
                                                }

                                                if (badLighting || !isBarcodeValid(
                                                        rightBarcodeBitmap,
                                                        rightBoundingBox,
                                                        ImageEdgeType.WhiteDown
                                                    )
                                                ) {
                                                    returnResult(
                                                        context, testInfo,
                                                        BAD_LIGHTING, bitmap
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
                                returnResult(context)
                            }
                        } else {
                            returnResult(context)
                            return
                        }
                    }
                }
            )
    }

    private fun cropImageToHalfSize(bitmap: Bitmap): Bitmap {
        return Bitmap.createBitmap(
            bitmap, bitmap.width / 2, 0,
            bitmap.width / 2,
            bitmap.height
        )
    }

    private fun analyzeBarcode(
        context: Context, id: String, bitmap: Bitmap, rightBarcode: FirebaseVisionBarcode,
        rightBoundingBox: Rect, leftBoundingBox: Rect
    ) {

        if (!rightBarcode.rawValue.isNullOrEmpty()) {
            val testInfo = getTestInfo(rightBarcode.displayValue!!)
            if (testInfo == null) {
                returnResult(context, testInfo, INVALID_BARCODE, bitmap)
                return
            }

            val requestedTestId = PreferencesUtil.getString(context, TEST_ID_KEY, "")
            if ((requestedTestId!!.isNotEmpty() && testInfo.uuid != requestedTestId)) {
                returnResult(context, testInfo, WRONG_CARD, bitmap)
                return
            }

            val cropLeft = max(leftBoundingBox.left - 20, 0)
            val cropWidth = min(
                leftBoundingBox.right - cropLeft + 40,
                bitmap.width - cropLeft
            )

            val cropTop = max(leftBoundingBox.bottom + 2, 0)
            val cropBottom = (bitmap.height / 2) + rightBoundingBox.top
            val cropHeight = min(cropBottom - cropTop, bitmap.height - cropTop)

            val finalBitmap = Bitmap.createBitmap(
                bitmap, cropLeft, cropTop, cropWidth, cropHeight
            )

            val croppedBitmap1 = Utilities.rotateImage(finalBitmap, 270)

            finalBitmap.recycle()

            val bwCroppedBitmap1 =
                ImageUtil.toBlackAndWhite(
                    croppedBitmap1, IMAGE_THRESHOLD, ImageEdgeType.WhiteTop,
                    0, croppedBitmap1.width
                )
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

            croppedBitmap1.recycle()

            val bwCroppedBitmap2 =
                ImageUtil.toBlackAndWhite(
                    croppedBitmap2, IMAGE_THRESHOLD, ImageEdgeType.WhiteTop,
                    0, croppedBitmap2.width
                )
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

            val extractedBitmap = Bitmap.createBitmap(
                croppedBitmap2, 0, top,
                croppedBitmap2.width,
                max(1, bottom - top)
            )
            val gsExtractedBitmap = ImageUtil.toGrayscale(extractedBitmap)

            val bwBitmap = ImageUtil.toBlackAndWhite(
                extractedBitmap, IMAGE_THRESHOLD, ImageEdgeType.WhiteTop, 0, extractedBitmap.width
            )

            croppedBitmap2.recycle()

            testInfo.fileName = id
            var error = NO_ERROR
            val db = AppDatabase.getDatabase(context)
            var calibration: Calibration? = null
            if (!AppPreferences.isCalibration()) {
                calibration = db.resultDao().getCalibration(testInfo.uuid)
            }

            try {
                val extractedColors = extractColors(
                    gsExtractedBitmap,
                    bwBitmap,
                    rightBarcode.displayValue!!,
                    calibration
                )

                testInfo.resultInfoGrayscale = analyzeColor(extractedColors)

//                if (testInfo.resultInfoGrayscale.result < 0) {
//                    error = NO_MATCH
//                }
            } catch (e: Exception) {
//                error = CALIBRATION_ERROR
            }

            Utilities.savePicture(
                context.applicationContext, id,
                testInfo.name!!, Utilities.bitmapToBytes(gsExtractedBitmap),
                isExtract = true,
                isGrayscale = true
            )
            gsExtractedBitmap.recycle()

            try {
                val extractedColors = extractColors(
                    extractedBitmap,
                    bwBitmap,
                    rightBarcode.displayValue!!,
                    calibration
                )

                testInfo.resultInfo = analyzeColor(extractedColors)

                if (testInfo.resultInfo.result < 0) {
                    error = NO_MATCH
                }
            } catch (e: Exception) {
                error = CALIBRATION_ERROR
            }

            Utilities.savePicture(
                context.applicationContext, id,
                testInfo.name!!, Utilities.bitmapToBytes(extractedBitmap),
                isExtract = true,
                isGrayscale = false
            )

            extractedBitmap.recycle()
            bwBitmap.recycle()

            returnResult(context, testInfo, error, bitmap)
        } else {
            returnResult(context)
            return
        }
    }

    private fun returnResult(
        context: Context,
        testInfo: TestInfo? = null,
        error: ErrorType = NO_ERROR,
        bitmap: Bitmap? = null
    ) {
        val intent = Intent(App.LOCAL_RESULT_EVENT)
        if (testInfo != null) {
            testInfo.error = error

            val db = AppDatabase.getDatabase(context)

            if (db.resultDao().getResult(testInfo.fileName) == null) {
                if (bitmap != null) {
                    val bitmapRotated = Utilities.rotateImage(bitmap, 270)
                    Utilities.savePicture(
                        context,
                        testInfo.fileName,
                        testInfo.name!!,
                        Utilities.bitmapToBytes(bitmapRotated),
                        isExtract = false, isGrayscale = false
                    )

                    bitmap.recycle()
                    bitmapRotated.recycle()
                }

                if (!AppPreferences.isCalibration()) {
                    db.resultDao().insert(
                        TestResult(
                            testInfo.fileName, testInfo.uuid!!, 0, testInfo.name!!,
                            Date().time, -1.0, -1.0, NO_ERROR,
                            getSampleTestImageNumber()
                        )
                    )
                }
            }

            val path = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() +
                    File.separator + "captures" + File.separator

            val basePath = File(path)
            if (!basePath.exists())
                Timber.d(if (basePath.mkdirs()) "Success" else "Failed")

            if (AppPreferences.isCalibration()) {
                testInfo.resultInfo.calibration = Calibration(
                    testInfo.uuid!!,
                    -1.0,
                    Color.red(testInfo.resultInfo.calibrationColor) - Color.red(testInfo.resultInfo.color),
                    Color.green(testInfo.resultInfo.calibrationColor) - Color.green(testInfo.resultInfo.color),
                    Color.blue(testInfo.resultInfo.calibrationColor) - Color.blue(testInfo.resultInfo.color)
                )
            } else {
                db.resultDao().updateResult(
                    testInfo.fileName,
                    testInfo.name!!,
                    testInfo.resultInfo.result,
                    testInfo.resultInfoGrayscale.result,
                    testInfo.error.ordinal
                )
            }

            intent.putExtra(App.TEST_INFO_KEY, testInfo)
            intent.putExtra(TEST_VALUE_KEY, testInfo.resultInfo.result)
        }

        val localBroadcastManager = LocalBroadcastManager.getInstance(context)
        localBroadcastManager.sendBroadcast(intent)
    }

    private fun extractColors(
        bitmap: Bitmap,
        bwBitmap: Bitmap,
        barcodeValue: String,
        calibration: Calibration?
    ): ColorInfo {

        val paint = Paint()
        paint.style = Style.STROKE
        paint.color = Color.WHITE
        paint.strokeWidth = 2f
        paint.isAntiAlias = true

        val cardColors: List<CalibrationValue> = getCardColors(barcodeValue)

        val intervals = cardColors.size / 2
        val commonTop = bwBitmap.height / intervals
        val padding = commonTop / 7
        var calibrationIndex = 0

        val points = getMarkers(bwBitmap)

        val commonLeft = points.x
        for (i in 0 until intervals) {
            val rectangle = Rect(
                max(1, commonLeft - padding),
                max(1, (commonTop * i) + (commonTop / 2) - padding),
                min(bwBitmap.width, commonLeft + padding),
                min(bwBitmap.height, (commonTop * i) + (commonTop / 2) + padding)
            )

            val pixels = getBitmapPixels(bitmap, rectangle)

            val cal = cardColors[calibrationIndex]
            calibrationIndex++
            cal.color = getAverageColor(pixels)

            val canvas = Canvas(bitmap)
            canvas.drawRect(rectangle, paint)
        }

        val commonRight = points.y
        for (i in 0 until intervals) {
            val rectangle = Rect(
                max(1, commonRight - padding),
                max(1, (commonTop * i) + (commonTop / 2) - padding),
                min(bwBitmap.width, commonRight + padding),
                min(bwBitmap.height, (commonTop * i) + (commonTop / 2) + padding)
            )

            val pixels = getBitmapPixels(bitmap, rectangle)

            val cal = cardColors[calibrationIndex]
            calibrationIndex++
            cal.color = getAverageColor(pixels)

            val canvas = Canvas(bitmap)
            canvas.drawRect(rectangle, paint)
        }

        val x1 = ((commonRight - commonLeft) / 2) + commonLeft
        val y1 = ((bwBitmap.height) / 2) + (bwBitmap.height * 0.1).toInt()
        val rectangle = Rect(x1 - 17, y1 - 27, x1 + 17, y1 + 35)
        val pixels = getBitmapPixels(bitmap, rectangle)

        var cuvetteColor = getAverageColor(pixels)
        if (calibration != null) {
            cuvetteColor = Color.rgb(
                cuvetteColor.red + calibration.rDiff,
                cuvetteColor.green + calibration.gDiff,
                cuvetteColor.blue + calibration.bDiff
            )
        }

        val swatches: ArrayList<Swatch> = ArrayList()
        val colorInfo = ColorInfo(cuvetteColor, swatches)
        val canvas = Canvas(bitmap)
        canvas.drawRect(rectangle, paint)

        for (cal in cardColors) {
            if (swatches.size >= cardColors.size / 2) {
                break
            }
            swatches.add(getCalibrationColor(cal.value, cardColors))
        }
        return colorInfo
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

    fun fixBoundary(
        barcode: FirebaseVisionBarcode,
        barcodeBitmap: Bitmap,
        imageEdgeSide: ImageEdgeType
    ): Rect {

        var top = barcode.boundingBox!!.top
        var left = barcode.boundingBox!!.left
        val right = barcode.boundingBox!!.right
        var bottom = barcode.boundingBox!!.bottom
        val midY = ((bottom - top) / 2) + top

        val bwBitmap = ImageUtil.toBlackAndWhite(
            barcodeBitmap, IMAGE_THRESHOLD, imageEdgeSide, left, right
        )

        for (x in left until left + 50) {
            val pixel = bwBitmap.getPixel(x, midY)
            if (isDarkPixel(pixel)) {
                left = min(x, barcode.boundingBox!!.left)
                break
            }
        }

        for (y in midY downTo 1) {
            top = y
            var isClear = true
            for (x in 0 until 20) {
                val pixel = bwBitmap.getPixel(left + x, top)
                if (isDarkPixel(pixel)) {
                    isClear = false
                    break
                }
            }
            if (isClear) {
                top = min(top, barcode.boundingBox!!.top)
                break
            }
        }

        for (y in midY until min(midY + 150, bwBitmap.height)) {
            bottom = y
            var isClear = true
            for (x in 0 until 20) {
                val pixel = bwBitmap.getPixel(left + x, bottom)
                if (isDarkPixel(pixel)) {
                    isClear = false
                    break
                }
            }
            if (isClear) {
                bottom = max(bottom, barcode.boundingBox!!.bottom)
                break
            }
        }

        return Rect(left, top, right, bottom)
    }

    fun isBarcodeValid(
        barcodeBitmap: Bitmap, barcode: Rect, imageEdgeSide: ImageEdgeType
    ): Boolean {

        var valid = true
        val bwBitmap = ImageUtil.toBlackAndWhite(
            barcodeBitmap, IMAGE_THRESHOLD, imageEdgeSide, 0, barcodeBitmap.width
        )

        val top = barcode.top
        val left = barcode.left
        val right = barcode.right
        val bottom = barcode.bottom

        val margin1 = 5
        val margin2 = 7

        if (top < margin1 || left < margin1 ||
            right > bwBitmap.width - margin2 || bottom > bwBitmap.height - margin2
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

    private fun getCalibrationColor(
        pointValue: Double,
        calibration: List<CalibrationValue>
    ): Swatch {
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

        var distance = 0.0
        val maxAllowedDistance = getCalibrationColorDistanceTolerance()
        for (i in filteredCalibrations) {
            for (j in filteredCalibrations) {
                if (getColorDistance(i.color, j.color) > maxAllowedDistance) {
                    throw Exception()
                }
                distance += getColorDistance(i.color, j.color)
            }
        }

        for (i in filteredCalibrations) {
            count += 1
            red += i.color.red
            green += i.color.green
            blue += i.color.blue
        }

        return Swatch(
            pointValue, Color.rgb(red / count, green / count, blue / count),
            distance / filteredCalibrations.size
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

        if (swatches[0].value == -1.0) {
            swatches.removeAt(0)
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
                list.add(Swatch(startValue + j * increment, color, swatches[i].distance))
            }
        }

        list.add(
            Swatch(
                swatches[swatches.size - 1].value,
                swatches[swatches.size - 1].color,
                swatches[swatches.size - 1].distance
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
     */
    @Suppress("SameParameterValue")
    private fun analyzeColor(
        photoColor: ColorInfo
    ): ResultInfo {

        val gradientList = generateGradient(photoColor.swatches)

        //Find the color within the generated gradient that matches the photoColor
        val colorCompareInfo: ColorCompareInfo =
            getNearestColorFromSwatches(photoColor.color, gradientList)

        //set the result
        val resultInfo = ResultInfo(color = photoColor.color)
        if (colorCompareInfo.result > -1) {
            resultInfo.result = (round(colorCompareInfo.result * 100) / 100.0)
        }
        resultInfo.matchedColor = colorCompareInfo.matchedColor
        resultInfo.distance = colorCompareInfo.distance

        var calibrationDistance = 0.0
        for (swatch in gradientList) {
            calibrationDistance += swatch.distance
        }
        resultInfo.calibrationDistance = calibrationDistance / gradientList.size
        resultInfo.swatches = photoColor.swatches

        return resultInfo
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
}
