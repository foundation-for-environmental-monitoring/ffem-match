package io.ffem.lite.util

import android.content.Context
import android.graphics.*
import android.graphics.Paint.Style
import androidx.core.content.ContextCompat.getColor
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import io.ffem.lite.R
import io.ffem.lite.camera.Utilities
import io.ffem.lite.common.Constants.CIRCLE_CUVETTE_AREA_PERCENTAGE
import io.ffem.lite.common.Constants.CIRCLE_CUVETTE_Y_OFFSET
import io.ffem.lite.common.Constants.INTERPOLATION_COUNT
import io.ffem.lite.common.Constants.MAX_COLOR_DISTANCE_RGB
import io.ffem.lite.common.Constants.MAX_DISTANCE
import io.ffem.lite.common.Constants.SWATCH_RADIUS
import io.ffem.lite.data.AppDatabase
import io.ffem.lite.data.DataHelper.getParameterValues
import io.ffem.lite.data.TestResult
import io.ffem.lite.model.*
import io.ffem.lite.model.ErrorType.*
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.preference.AppPreferences.getCalibrationColorDistanceTolerance
import io.ffem.lite.preference.AppPreferences.getColorDistanceTolerance
import io.ffem.lite.preference.getSampleTestImageNumber
import io.ffem.lite.util.ColorUtil.getAverageColor
import io.ffem.lite.util.ColorUtil.getColorDistance
import timber.log.Timber
import java.util.*
import kotlin.math.*

object ImageColorUtil {

    fun getResult(
        context: Context,
        testInfo: TestInfo? = null,
        error: ErrorType = NO_ERROR,
        bitmap: Bitmap? = null,
        colorCardType: Int
    ) {
        if (testInfo != null && bitmap != null) {
            val subTest = testInfo.subTest()
            try {
                val (extractedColors, color) = if (colorCardType == 0) {
                    extractColors(
                        bitmap,
                        testInfo,
                        context
                    )
                } else {
                    extractCircleColors(
                        bitmap,
                        testInfo,
                        context
                    )
                }

                subTest.resultInfo =
                    analyzeColor(color, extractedColors, subTest.formula, subTest.maxValue, context)
                subTest.setFinalResult(subTest.resultInfo.result * subTest.dilution)

                if (subTest.resultInfo.result > -2) {
                    var calibration: CardCalibration? = null

                    if (!AppPreferences.isCalibration()) {
                        val db = AppDatabase.getDatabase(context)
                        try {
                            calibration = db.resultDao().getCalibration(testInfo.uuid)
                        } finally {
                            db.close()
                        }
                    }

                    var sampleColor = color
                    // if calibrated then calculate also the result by adding the color differences
                    if (calibration != null) {
                        sampleColor = Color.rgb(
                            min(
                                max(0, sampleColor.red + calibration.rDiff),
                                255
                            ),
                            min(
                                max(0, sampleColor.green + calibration.gDiff),
                                255
                            ),
                            min(
                                max(0, sampleColor.blue + calibration.bDiff),
                                255
                            )
                        )

                        subTest.calibratedResult = analyzeColor(
                            sampleColor,
                            extractedColors,
                            subTest.formula,
                            subTest.maxValue,
                            context
                        )
                        subTest.calibratedResult.result =
                            subTest.calibratedResult.result * subTest.dilution
                    }
                }

                if (!AppPreferences.isCalibration()) {
                    val resultImage = ImageUtil.createResultImage(
                        subTest.resultInfo,
                        subTest.calibratedResult,
                        subTest.maxValue,
                        subTest.formula,
                        500
                    )

                    Utilities.savePng(
                        context.applicationContext, testInfo.fileName,
                        testInfo.name!!, resultImage,
                        "_result"
                    )
                }
            } catch (e: Exception) {
//                Utilities.savePicture(
//                    context.applicationContext, testInfo.fileName,
//                    testInfo.name!!, Utilities.bitmapToBytes(bitmap),
//                    "_swatch"
//                )
                subTest.error = CALIBRATION_ERROR
                return
            }

            if (!AppPreferences.isCalibration() &&
                (subTest.resultInfo.result == -1.0 && subTest.calibratedResult.result < 0)
            ) {
                subTest.error = NO_MATCH
                return
            } else {
                subTest.error = error
            }

            val db = AppDatabase.getDatabase(context)
            try {
                if (db.resultDao().getResult(testInfo.fileName) == null) {
                    if (!AppPreferences.isCalibration()) {
                        db.resultDao().insert(
                            TestResult(
                                testInfo.fileName,
                                testInfo.uuid,
                                0,
                                testInfo.name!!,
                                testInfo.sampleType.toString(),
                                Date().time,
                                -1.0,
                                subTest.maxValue,
                                0.0,
                                error = NO_ERROR
                            )
                        )

                        db.resultDao().updateResultSampleNumber(
                            testInfo.fileName,
                            getSampleTestImageNumber()
                        )
                    }
                }

                if (AppPreferences.isCalibration()) {
                    subTest.resultInfo.calibration = CardCalibration(
                        testInfo.uuid,
                        -1.0,
                        subTest.resultInfo.calibratedValue.color,
                        Color.red(subTest.resultInfo.calibratedValue.color) - Color.red(subTest.resultInfo.sampleColor),
                        Color.green(subTest.resultInfo.calibratedValue.color) - Color.green(
                            subTest.resultInfo.sampleColor
                        ),
                        Color.blue(subTest.resultInfo.calibratedValue.color) - Color.blue(subTest.resultInfo.sampleColor)
                    )
                } else {
                    db.resultDao().updateResult(
                        testInfo.fileName,
                        testInfo.uuid,
                        testInfo.name!!,
                        testInfo.sampleType.toString(),
                        subTest.getResult(),
                        subTest.getMarginOfError(),
                        subTest.error.ordinal
                    )
                }
            } finally {
                db.close()
            }
        }
    }

    private fun extractColors(
        bitmap: Bitmap,
        testInfo: TestInfo,
        context: Context
    ): Pair<ArrayList<ColorInfo>, Int> {

        val greenPaint = Paint()
        greenPaint.style = Style.STROKE
        greenPaint.color = getColor(context, R.color.bright_green)
        greenPaint.strokeWidth = 3f

        val blackPaint = Paint()
        blackPaint.style = Style.STROKE
        blackPaint.color = Color.BLACK
        blackPaint.strokeWidth = 1f

        val parameterValues: List<CalibrationValue> = getParameterValues(testInfo.uuid, context)

        val intervals = parameterValues.size / 2
        val squareLeft = bitmap.width / intervals
        val padding = squareLeft / 7
        var calibrationIndex = 0

        val points = getMarkers(bitmap)

        // Column 2 color squares
        val row2Top = points.y
        for (i in 0 until intervals) {
            val rectangle = Rect(
                max(1, ((squareLeft * i) + (squareLeft / 2.0) - padding).toInt()),
                max(1, row2Top - padding),
                min(bitmap.width, ((squareLeft * i) + (squareLeft / 2.0) + padding).toInt()),
                min(bitmap.height, row2Top + padding),
            )

            val pixels = getBitmapPixels(bitmap, rectangle)

            val cal = parameterValues[calibrationIndex]
            calibrationIndex++
            cal.color = getAverageColor(pixels, false)

            val canvas = Canvas(bitmap)
            canvas.drawRect(rectangle, greenPaint)
            canvas.drawRect(rectangle, blackPaint)
        }

        // Column 1 color squares
        val row1Top = points.x
        for (i in 0 until intervals) {
            val rectangle = Rect(
                max(1, ((squareLeft * i) + (squareLeft / 2.0) - padding).toInt()),
                max(1, row1Top - padding),
                min(bitmap.width, ((squareLeft * i) + (squareLeft / 2.0) + padding).toInt()),
                min(bitmap.height, row1Top + padding)
            )

            val pixels = getBitmapPixels(bitmap, rectangle)

            val cal = parameterValues[calibrationIndex]
            calibrationIndex++
            cal.color = getAverageColor(pixels, false)

            val canvas = Canvas(bitmap)
            canvas.drawRect(rectangle, greenPaint)
            canvas.drawRect(rectangle, blackPaint)
        }

        // Cuvette area
        val y1 = ((row2Top - row1Top) / 2) + row1Top
        val x1 = ((bitmap.width) / 2) + (bitmap.width * 0.12).toInt()
        val areaHeight = (bitmap.width * 0.035).toInt()
        val rectangle = Rect(
            x1 - (bitmap.width * 0.04).toInt(),
            y1 - areaHeight,
            x1 + (bitmap.width * 0.12).toInt(),
            y1 + areaHeight
        )
        val pixels = getBitmapPixels(bitmap, rectangle)

        val cuvetteColor = getAverageColor(pixels, true)

        val swatches: ArrayList<ColorInfo> = ArrayList()
        val canvas = Canvas(bitmap)
        canvas.drawRect(rectangle, greenPaint)
        canvas.drawRect(rectangle, blackPaint)

        Utilities.savePicture(
            context.applicationContext, testInfo.fileName,
            testInfo.name!!, Utilities.bitmapToBytes(bitmap),
            "_swatch"
        )

        for (cal in parameterValues) {
            if (swatches.size >= parameterValues.size / 2) {
                break
            }
            swatches.add(getCalibrationColor(cal.value, parameterValues, testInfo.subtype, context))
        }

        return Pair(swatches, cuvetteColor)
    }

    private fun getMarkers(
        bitmap: Bitmap
    ): Point {
        val leftSquareCenter = (bitmap.height * 0.11).toInt()
        val rightSquareCenter = bitmap.height - (bitmap.height * 0.11).toInt()

        return Point(leftSquareCenter, rightSquareCenter)
    }

    private fun extractCircleColors(
        bmp: Bitmap,
        testInfo: TestInfo,
        context: Context
    ): Pair<ArrayList<ColorInfo>, Int> {

        val bitmap = Utilities.rotateImage(bmp, 90)

        val greenPaint = Paint()
        greenPaint.style = Style.STROKE
        greenPaint.color = getColor(context, R.color.bright_green)
        greenPaint.strokeWidth = 3f

        val blackPaint = Paint()
        blackPaint.style = Style.STROKE
        blackPaint.color = Color.BLACK
        blackPaint.strokeWidth = 1f

        val cardColors: List<CalibrationValue> = getParameterValues(testInfo.uuid, context)

        val center = Point(bitmap.width / 2, bitmap.height / 2)
        val padding = (bitmap.width * 0.031).toInt()
        val canvas = Canvas(bitmap)

        val radius = (bitmap.width * SWATCH_RADIUS)

        val slice = 2 * Math.PI / cardColors.size
        for ((index, i) in (cardColors.size downTo 1).withIndex()) {
            val angle = if ((cardColors.size / 2) % 2 == 0) {
                (slice * (i - (cardColors.size / 4))) - (slice / 2)
            } else {
                (slice * (i - (cardColors.size / 4))) - slice
            }
            val newX = (center.x + radius * cos(angle)).toInt()
            val newY = (center.y + radius * sin(angle)).toInt()
            val p = Point(newX, newY)

            val rectangle = Rect(
                p.x - padding, p.y - padding, p.x + padding, p.y + padding
            )
            val pixels = getBitmapPixels(bitmap, rectangle)
            cardColors[index].color = getAverageColor(pixels, false)

            canvas.drawRect(rectangle, blackPaint)
        }

        // Cuvette area
        val areaHeight = (bitmap.width * CIRCLE_CUVETTE_AREA_PERCENTAGE).toInt()

        val cuvetteArea = Rect(
            center.x - areaHeight,
            center.y - areaHeight + CIRCLE_CUVETTE_Y_OFFSET,
            center.x + areaHeight,
            center.y + areaHeight + CIRCLE_CUVETTE_Y_OFFSET
        )
        val pixels = getBitmapPixels(bitmap, cuvetteArea)

        val cuvetteColor = getAverageColor(pixels, true)

        val swatches: ArrayList<ColorInfo> = ArrayList()
        canvas.drawRect(cuvetteArea, blackPaint)

        Utilities.savePicture(
            context.applicationContext, testInfo.fileName,
            testInfo.name!!, Utilities.bitmapToBytes(bitmap),
            "_swatch"
        )

        bitmap.recycle()
        bmp.recycle()

        for (cal in cardColors) {
            if (swatches.size >= cardColors.size / 2) {
                break
            }
            swatches.add(getCalibrationColor(cal.value, cardColors, testInfo.subtype, context))
        }
        return Pair(swatches, cuvetteColor)
    }

    private fun getCalibrationColor(
        pointValue: Double,
        calibration: List<CalibrationValue>,
        testType: TestType,
        context: Context
    ): ColorInfo {
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
        val maxAllowedDistance = getCalibrationColorDistanceTolerance(context, testType)
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

        return ColorInfo(pointValue, color, distance / filteredCalibrations.size)
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
    private fun generateGradient(swatches: ArrayList<ColorInfo>): ArrayList<Swatch> {

        val list = ArrayList<Swatch>()

        if (swatches.size < 2) {
            return list
        }

        if (swatches[0].value == -1.0) {
            swatches.removeAt(0)
        }

        // Predict more points in the calibration list to account for high levels of contamination
        swatches.add(predictNextColor(swatches[swatches.size - 2], swatches[swatches.size - 1]))
        swatches.add(predictNextColor(swatches[swatches.size - 2], swatches[swatches.size - 1]))
        swatches.add(predictNextColor(swatches[swatches.size - 2], swatches[swatches.size - 1]))
        swatches.add(predictNextColor(swatches[swatches.size - 2], swatches[swatches.size - 1]))
        swatches.add(predictNextColor(swatches[swatches.size - 2], swatches[swatches.size - 1]))
        swatches.add(predictNextColor(swatches[swatches.size - 2], swatches[swatches.size - 1]))
        swatches.add(predictNextColor(swatches[swatches.size - 2], swatches[swatches.size - 1]))

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

    private fun predictNextColor(swatch1: ColorInfo, swatch2: ColorInfo): ColorInfo {

        val valueDiff = swatch2.value - swatch1.value

        val color1 = swatch1.color
        val color2 = swatch2.color
        val r = getNextLinePoint(Color.red(color1), Color.red(color2))
        val g = getNextLinePoint(Color.green(color1), Color.green(color2))
        val b = getNextLinePoint(Color.blue(color1), Color.blue(color2))

        return ColorInfo(swatch2.value + valueDiff, Color.rgb(r, g, b))
    }

    private fun getNextLinePoint(y: Int, y2: Int): Int {
        val diff = y2 - y
        return min(255, max(0, y2 + diff))
    }

    /**
     * Analyzes the color and returns a result info.
     */
    @Suppress("SameParameterValue")
    fun analyzeColor(
        sampleColor: Int, swatches: ArrayList<ColorInfo>, formula: String?,
        maxValue: Double, context: Context
    ): ResultInfo {

        val maxRange = swatches[swatches.size - 1].value
        val defaultSwatchSize = swatches.size
        val gradientList = generateGradient(swatches)

        //Find the color within the generated gradient that matches the sampleColor
        val colorCompareInfo: ColorCompareInfo =
            getNearestColorFromSwatches(sampleColor, gradientList, context)

        //set the result
        val resultInfo = ResultInfo(
            sampleColor = sampleColor,
            matchedSwatch = colorCompareInfo.matchedColor,
            distance = colorCompareInfo.distance,
            swatches = swatches
        )

        if (colorCompareInfo.result > -1) {
            resultInfo.result = (round(colorCompareInfo.result * 100) / 100.0)
        }

        var distanceSum = 0.0
        for (swatch in gradientList) {
            distanceSum += swatch.distance
        }
        resultInfo.swatchDistance = distanceSum / gradientList.size

        if (resultInfo.result > maxRange - (maxRange * 0.1)) {
            var lastSwatchPosition = 2
            for (x in 2..swatches.size) {
                if (swatches[x].value > resultInfo.result) {
                    lastSwatchPosition = x
                    break
                }
            }

            lastSwatchPosition = max(lastSwatchPosition, defaultSwatchSize)

            for (x in swatches.size - 1 downTo lastSwatchPosition + 1) {
                swatches.removeAt(x)
            }
        } else {
            for (x in swatches.size - 1 downTo 2) {
                if (swatches.size <= defaultSwatchSize) {
                    break
                }
                if (swatches[x].value > maxRange) {
                    swatches.removeAt(x)
                }
            }
        }

        resultInfo.result = MathUtil.applyFormula(resultInfo.result, formula)
        if (resultInfo.result < maxValue && resultInfo.result > maxValue - (maxValue * 0.1)) {
            resultInfo.result = maxValue
        }

        resultInfo.matchedPosition =
            (colorCompareInfo.matchedIndex.toFloat() * 100 /
                    ((swatches.size - 1) * INTERPOLATION_COUNT)).toFloat()

        resultInfo.result = (round(resultInfo.result * 100) / 100.0)

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
        colorToFind: Int, swatches: List<Swatch>, context: Context
    ): ColorCompareInfo {

        var lowestDistance = getMaxDistance(getColorDistanceTolerance(context).toDouble())

        var resultValue = -1.0
        var matchedColor = -1
        var newDistance: Double
        var nearestDistance = MAX_DISTANCE.toDouble()
        var nearestMatchedColor = -1
        var matchedIndex = 0

        for (i in swatches.indices) {
            val tempColor = swatches[i].color

            newDistance = getColorDistance(tempColor, colorToFind)
            if (nearestDistance > newDistance) {
                nearestDistance = newDistance
                nearestMatchedColor = tempColor
            }

            if (newDistance == 0.0) {
                resultValue = swatches[i].value
                matchedColor = swatches[i].color
                matchedIndex = i
                break
            } else if (newDistance < lowestDistance) {
                lowestDistance = newDistance
                resultValue = swatches[i].value
                matchedColor = swatches[i].color
                matchedIndex = i
            }
        }

        //if no result was found add some diagnostic info
        if (resultValue == -1.0) {
            lowestDistance = nearestDistance
            matchedColor = nearestMatchedColor
        }
        return ColorCompareInfo(
            resultValue,
            colorToFind,
            matchedColor,
            lowestDistance,
            matchedIndex
        )
    }

    private fun getMaxDistance(defaultValue: Double): Double {
        return if (defaultValue > 0) {
            defaultValue
        } else {
            MAX_COLOR_DISTANCE_RGB.toDouble()
        }
    }
}
