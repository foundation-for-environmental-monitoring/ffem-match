package io.ffem.lite.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.Paint.Style
import android.os.Environment
import androidx.core.content.ContextCompat.getColor
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.Barcode.FORMAT_CODE_128
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import io.ffem.lite.R
import io.ffem.lite.app.App.Companion.getCardColors
import io.ffem.lite.app.App.Companion.getTestInfo
import io.ffem.lite.camera.MAX_ANGLE
import io.ffem.lite.camera.Utilities
import io.ffem.lite.common.*
import io.ffem.lite.common.Constants.IMAGE_THRESHOLD
import io.ffem.lite.common.Constants.INTERPOLATION_COUNT
import io.ffem.lite.common.Constants.MAX_COLOR_DISTANCE_RGB
import io.ffem.lite.common.Constants.MAX_DISTANCE
import io.ffem.lite.data.AppDatabase
import io.ffem.lite.data.Calibration
import io.ffem.lite.data.TestResult
import io.ffem.lite.model.*
import io.ffem.lite.model.ErrorType.*
import io.ffem.lite.preference.*
import timber.log.Timber
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

@Deprecated("User QR color card instead")
object BarcodeColorUtil {

    fun extractImage(context: Context, bitmapImage: Bitmap) {

        val detector: BarcodeScanner by lazy {
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    FORMAT_CODE_128
                ).build()
            BarcodeScanning.getClient(options)
        }

        val bitmap = Utilities.rotateImage(bitmapImage, 90)

        var badLighting = false

        val barcodeHeight = ((bitmap.height / 2) - (.20 * bitmap.height / 2)).toInt()

        val leftBarcodeBitmap = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, barcodeHeight
        )

        detector.process(InputImage.fromBitmap(leftBarcodeBitmap, 0))
            .addOnFailureListener(
                fun(_: Exception) {
                    returnResult(context)
                }
            )
            .addOnSuccessListener(
                fun(result: List<Barcode>) {
                    if (result.isEmpty()) {
                        returnResult(context, getTestInfo(DEFAULT_TEST_UUID), BAD_LIGHTING, bitmap)
                    }
                    if (result.isNotEmpty()) {
                        val leftBarcode = result[0]
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
                                    bitmap, 0, bitmap.height - barcodeHeight,
                                    bitmap.width, barcodeHeight
                                )

                                detector.process(
                                    InputImage.fromBitmap(rightBarcodeBitmap, 0)
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
                                        fun(result: List<Barcode>) {
                                            if (result.isEmpty()) {
                                                returnResult(
                                                    context,
                                                    testInfo,
                                                    BAD_LIGHTING,
                                                    bitmap
                                                )
                                                return
                                            }

                                            if (result.isNotEmpty()) {
                                                val rightBarcode = result[0]
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
                                                    context, bitmap, rightBarcode,
                                                    rightBoundingBox, leftBoundingBox, barcodeHeight
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

    private fun analyzeBarcode(
        context: Context, bitmap: Bitmap, rightBarcode: Barcode,
        rightBoundingBox: Rect, leftBoundingBox: Rect, barcodeHeight: Int
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
            var cropWidth = min(
                leftBoundingBox.right - cropLeft + 40,
                bitmap.width - cropLeft
            )
            if (cropWidth < (0.6 * bitmap.width)) {
                cropWidth = bitmap.width - cropLeft
            }

            val cropTop = max(leftBoundingBox.bottom + 2, 0)
            val cropBottom = (bitmap.height - barcodeHeight) + rightBoundingBox.top
            var cropHeight = min(cropBottom - cropTop, bitmap.height - cropTop)
            if (cropHeight < (0.35 * bitmap.height)) {
                cropHeight = bitmap.height - cropTop
            }

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

            val bwBitmap = ImageUtil.toBlackAndWhite(
                extractedBitmap, IMAGE_THRESHOLD, ImageEdgeType.WhiteTop, 0, extractedBitmap.width
            )

            croppedBitmap2.recycle()

            var error = NO_ERROR
            var calibration: Calibration? = null
            if (!AppPreferences.isCalibration()) {
                val db = AppDatabase.getDatabase(context)
                try {
                    calibration = db.resultDao().getCalibration(testInfo.uuid)
                } finally {
                    db.close()
                }
            }

            // Calculate grayscale image result
            if (isDiagnosticMode()) {
                val gsExtractedBitmap = ImageUtil.toGrayscale(extractedBitmap)

                try {
                    val extractedColors = extractColors(
                        gsExtractedBitmap,
                        bwBitmap,
                        rightBarcode.displayValue!!,
                        context
                    )

                    testInfo.resultInfoGrayscale = analyzeColor(extractedColors)
                } catch (e: Exception) {
                }

                Utilities.savePicture(
                    context.applicationContext, testInfo.fileName,
                    testInfo.name!!, Utilities.bitmapToBytes(gsExtractedBitmap),
                    "_swatch_gs"
                )
                gsExtractedBitmap.recycle()
            }

            try {
                val extractedColors = extractColors(
                    extractedBitmap,
                    bwBitmap,
                    rightBarcode.displayValue!!,
                    context
                )

                testInfo.resultInfo = analyzeColor(extractedColors)

                // if calibrated then calculate also the result by adding the color differences
                if (calibration != null) {
                    extractedColors.swatches.removeAt(extractedColors.swatches.size - 1)
                    extractedColors.swatches.removeAt(extractedColors.swatches.size - 1)
                    extractedColors.sampleColor = Color.rgb(
                        min(max(0, extractedColors.sampleColor.red + calibration.rDiff), 255),
                        min(max(0, extractedColors.sampleColor.green + calibration.gDiff), 255),
                        min(max(0, extractedColors.sampleColor.blue + calibration.bDiff), 255)
                    )

                    testInfo.calibratedResultInfo = analyzeColor(extractedColors)
                }

                val resultImage = ImageUtil.createResultImage(
                    testInfo.resultInfo,
                    testInfo.calibratedResultInfo,
                    500
                )

                Utilities.savePicture(
                    context.applicationContext, testInfo.fileName,
                    testInfo.name!!, Utilities.bitmapToBytes(resultImage),
                    "_result"
                )

                if (testInfo.resultInfo.result < 0) {
                    error = NO_MATCH
                }
            } catch (e: Exception) {
                error = CALIBRATION_ERROR
            }

            Utilities.savePicture(
                context.applicationContext, testInfo.fileName,
                testInfo.name!!, Utilities.bitmapToBytes(extractedBitmap),
                "_swatch"
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
        val intent = Intent(RESULT_EVENT_BROADCAST)
        if (testInfo != null) {
            testInfo.error = error

            val db = AppDatabase.getDatabase(context)
            try {
                if (db.resultDao().getResult(testInfo.fileName) == null) {
                    if (bitmap != null) {
                        val bitmapRotated = Utilities.rotateImage(bitmap, 270)
                        Utilities.savePicture(
                            context,
                            testInfo.fileName,
                            testInfo.name!!,
                            Utilities.bitmapToBytes(bitmapRotated), ""
                        )

                        bitmap.recycle()
                        bitmapRotated.recycle()
                    }

                    if (!AppPreferences.isCalibration()) {
                        db.resultDao().insert(
                            TestResult(
                                testInfo.fileName, testInfo.uuid!!, 0, testInfo.name!!,
                                Date().time, -1.0, -1.0, 0.0, error = NO_ERROR
                            )
                        )

                        db.resultDao().updateResultSampleNumber(
                            testInfo.fileName,
                            getSampleTestImageNumber()
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
                        Color.red(testInfo.resultInfo.calibratedValue.color) - Color.red(testInfo.resultInfo.sampleColor),
                        Color.green(testInfo.resultInfo.calibratedValue.color) - Color.green(
                            testInfo.resultInfo.sampleColor
                        ),
                        Color.blue(testInfo.resultInfo.calibratedValue.color) - Color.blue(testInfo.resultInfo.sampleColor)
                    )
                } else {
                    db.resultDao().updateResult(
                        testInfo.fileName,
                        testInfo.uuid!!,
                        testInfo.name!!,
                        testInfo.getResult(),
                        testInfo.resultInfoGrayscale.result,
                        testInfo.getMarginOfError(),
                        testInfo.error.ordinal
                    )
                }
            } finally {
                db.close()
            }
            intent.putExtra(TEST_INFO_KEY, testInfo)
            intent.putExtra(TEST_VALUE_KEY, testInfo.resultInfo.result)
        }

        val localBroadcastManager = LocalBroadcastManager.getInstance(context)
        localBroadcastManager.sendBroadcast(intent)
    }

    private fun extractColors(
        bitmap: Bitmap,
        bwBitmap: Bitmap,
        barcodeValue: String,
        context: Context
    ): ColorInfo {

        val paint = Paint()
        paint.style = Style.STROKE
        paint.color = getColor(context, R.color.bright_green)
        paint.strokeWidth = 3f

        val paint1 = Paint()
        paint1.style = Style.STROKE
        paint1.color = Color.BLACK
        paint1.strokeWidth = 1f

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
            cal.color = getAverageColor(pixels, false)

            val canvas = Canvas(bitmap)
            canvas.drawRect(rectangle, paint)
            canvas.drawRect(rectangle, paint1)
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
            cal.color = getAverageColor(pixels, false)

            val canvas = Canvas(bitmap)
            canvas.drawRect(rectangle, paint)
            canvas.drawRect(rectangle, paint1)
        }

        val x1 = ((commonRight - commonLeft) / 2) + commonLeft
        val y1 = ((bwBitmap.height) / 2) + (bwBitmap.height * 0.1).toInt()
        val areaWidth = (bitmap.height * 0.04).toInt()
        val rectangle = Rect(
            x1 - areaWidth,
            y1 - (bitmap.height * 0.04).toInt(),
            x1 + areaWidth,
            y1 + (bitmap.height * 0.1).toInt()
        )
        val pixels = getBitmapPixels(bitmap, rectangle)

        val cuvetteColor = getAverageColor(pixels, true)

        val swatches: ArrayList<Swatch> = ArrayList()
        val colorInfo = ColorInfo(cuvetteColor, swatches)
        val canvas = Canvas(bitmap)
        canvas.drawRect(rectangle, paint)
        canvas.drawRect(rectangle, paint1)

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

    fun isBarcodeTilted(
        corners: Array<Point>?
    ): Boolean {
        if (corners == null) {
            return true
        }
        if (abs(corners[0].y - corners[1].y) < MAX_ANGLE) {
            return false
        }
        if (abs(corners[0].y - corners[2].y) < MAX_ANGLE) {
            return false
        }
        if (abs(corners[0].y - corners[3].y) < MAX_ANGLE) {
            return false
        }
        return true
    }

    fun fixBoundary(
        barcode: Barcode,
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

        val my = min(bwBitmap.height - 1, midY)
        for (x in left until left + 50) {
            val pixel = bwBitmap.getPixel(x, my)
            if (isDarkPixel(pixel)) {
                left = min(x, barcode.boundingBox!!.left)
                break
            }
        }

        for (y in midY downTo 1) {
            top = min(y, bwBitmap.height - 1)
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
            bottom = min(y, bwBitmap.height - 1)
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

        val color = Color.rgb(red / count, green / count, blue / count)

        if (getColorDistance(color, Color.BLACK) < 10) {
            Timber.e("Card color is too dark")
            throw Exception()
        }

        return Swatch(pointValue, color, distance / filteredCalibrations.size)
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
     * @param colorInfo The color to compare
     */
    @Suppress("SameParameterValue")
    private fun analyzeColor(
        colorInfo: ColorInfo
    ): ResultInfo {

        val gradientList = generateGradient(colorInfo.swatches)

        //Find the color within the generated gradient that matches the sampleColor
        val colorCompareInfo: ColorCompareInfo =
            getNearestColorFromSwatches(colorInfo.sampleColor, gradientList)

        //set the result
        val resultInfo = ResultInfo(
            sampleColor = colorInfo.sampleColor,
            matchedSwatch = colorCompareInfo.matchedColor,
            distance = colorCompareInfo.distance,
            swatches = colorInfo.swatches
        )

        if (colorCompareInfo.result > -1) {
            resultInfo.result = (round(colorCompareInfo.result * 100) / 100.0)
        }

        var distanceSum = 0.0
        for (swatch in gradientList) {
            distanceSum += swatch.distance
        }
        resultInfo.swatchDistance = distanceSum / gradientList.size
        resultInfo.matchedPosition =
            colorCompareInfo.matchedIndex.toFloat() * 100 / gradientList.size

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
        var matchedIndex = 0

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
                matchedIndex = i
                break
            } else if (tempDistance < distance) {
                distance = tempDistance
                resultValue = swatches[i].value
                matchedColor = swatches[i].color
                matchedIndex = i
            }
        }

        //if no result was found add some diagnostic info
        if (resultValue == -1.0) {
            distance = nearestDistance
            matchedColor = nearestMatchedColor
        }
        return ColorCompareInfo(resultValue, colorToFind, matchedColor, distance, matchedIndex)
    }

    private fun getMaxDistance(defaultValue: Double): Double {
        return if (defaultValue > 0) {
            defaultValue
        } else {
            MAX_COLOR_DISTANCE_RGB.toDouble()
        }
    }
}