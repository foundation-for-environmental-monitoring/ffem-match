package io.ffem.lite.helper

import android.content.Context
import android.graphics.Color
import io.ffem.lite.common.Constants.INTERPOLATION_COUNT
import io.ffem.lite.model.*
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.preference.getColorDistanceTolerance
import io.ffem.lite.util.ColorUtil
import io.ffem.lite.util.ColorUtil.getColorDistance
import io.ffem.lite.util.ColorUtil.getMaxDistance
import io.ffem.lite.util.MathUtil.round
import kotlin.math.max
import kotlin.math.min

object SwatchHelper {
    private const val MAX_DISTANCE = 999

    fun analyzeColor(
        testInfo: TestInfo,
        sampleColor: Int,
        swatches: ArrayList<ColorInfo>,
        context: Context
    ): ResultInfo {

        val gradients = generateGradient(swatches.toMutableList())

        //Find the color within the generated gradient that matches the photoColor
        val colorCompareInfo: ColorCompareInfo =
            getNearestColorFromSwatches(sampleColor, gradients, context)
        //set the result
        val resultInfo = ResultInfo(-1.00, sampleColor, 0)
        if (colorCompareInfo.result > -1) {
            resultInfo.result = colorCompareInfo.result
        }
        resultInfo.calibrationSteps = swatches.size
        resultInfo.matchedSwatch = colorCompareInfo.matchedColor
        resultInfo.distance = colorCompareInfo.distance

        testInfo.subTest().resultInfo.result = resultInfo.result

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

        var lowestDistance = getMaxDistance(getColorDistanceTolerance().toDouble())

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

    /**
     * Auto generate the color swatches for the given test type.
     *
     * @param swatches The test object
     * @return The list of generated color swatches
     */
    private fun generateGradient(swatches: MutableList<ColorInfo>): MutableList<Swatch> {
        val list: MutableList<Swatch> = ArrayList()
        if (swatches.size < 2) {
            for (swatch in swatches) {
                list.add(
                    Swatch(
                        swatch.value,
                        swatch.color
                    )
                )
            }
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
                val color = ColorUtil.getGradientColor(startColor, endColor, steps, j)
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

    fun isCalibrationEmpty(testInfo: TestInfo?): Boolean {
        if (testInfo == null) {
            return true
        }
        val calibrations = testInfo.subTest().colors
        if (calibrations.isEmpty()) {
            return true
        }

        if (calibrations.size < testInfo.subTest().values.size) {
            return true
        }

        for (swatch1 in calibrations) {
            if (swatch1.color != Color.TRANSPARENT) {
                return false
            }
        }
        return true
    }

    /**
     * Validate the color by looking for missing color, duplicate colors, color out of sequence etc...
     *
     * @param testInfo the test Information
     * @return True if valid otherwise false
     */
    fun isSwatchListValid(testInfo: TestInfo?, ignoreNotCalibrated: Boolean): Boolean {
        if (testInfo == null) {
            return false
        }
        var result = true
        val calibrations = testInfo.subTest().colors
        if (calibrations.isEmpty()) {
            return false
        }
        if (calibrations.size < testInfo.subTest().values.size) {
            return false
        }
        for (swatch1 in calibrations) {
            if (!ignoreNotCalibrated && swatch1.color == Color.TRANSPARENT) {
                // Calibration is incomplete
                result = false
                break
            }
            if (ignoreNotCalibrated && swatch1.color == Color.TRANSPARENT) {
                continue
            }
            if (swatch1.color == Color.BLACK) {
                result = false
                break
            }
            for (swatch2 in calibrations) {
                if (swatch1 != swatch2 && ColorUtil.areColorsSimilar(
                        swatch1.color,
                        swatch2.color
                    )
                ) { // Duplicate color
                    result = false
                    break
                }
            }
        }
        return result
    }

    /**
     * Validate the color by looking for missing color, duplicate colors, color out of sequence etc...
     *
     * @param testInfo the test info
     * @return True if calibration is complete
     */
    fun isCalibrationComplete(testInfo: TestInfo): Boolean {
        val calibrations = testInfo.subTest().colors
        if (calibrations.size != testInfo.subTest().values.size) {
            return false
        }
        for (swatch in calibrations) {
            if (swatch.color == Color.TRANSPARENT || swatch.color == Color.BLACK) {
                return false
            }
        }
        return true
    }

    /**
     * Get the average value from list of results.
     *
     * @param resultInfoList the result info
     * @return the average value
     */
    fun getAverageResult(resultInfoList: ArrayList<ResultInfo>, context: Context): Double {
        var result = 0.0
        for (i in resultInfoList.indices) {
            val color1 = resultInfoList[i].sampleColor
            for (j in resultInfoList.indices) {
                val color2 = resultInfoList[j].sampleColor
                if (getColorDistance(
                        color1,
                        color2
                    ) > AppPreferences.getAveragingColorDistanceTolerance(context)
                ) {
                    return (-1).toDouble()
                }
            }
        }
        for (i in resultInfoList.indices) {
            val value = resultInfoList[i].result
            result += if (value > -1) {
                value
            } else {
                return (-1).toDouble()
            }
        }
        result = try {
            (result / resultInfoList.size).round(2)
        } catch (ex: Exception) {
            -1.0
        }
        return result
    }

    /**
     * Returns an average color from a list of results.
     * If any color does not closely match the rest of the colors then it returns -1
     *
     * @param resultInfoList the list of results
     * @return the average color
     */
    fun getAverageColor(resultInfoList: ArrayList<ResultInfo>, context: Context): Int {
        var red = 0
        var green = 0
        var blue = 0
        for (i in resultInfoList.indices) {
            val color1 = resultInfoList[i].sampleColor
            //if invalid color return 0
            if (color1 == Color.TRANSPARENT) {
                return color1
            }
            //check all the colors are mostly similar otherwise return -1
            for (j in resultInfoList.indices) {
                val color2 = resultInfoList[j].sampleColor
                if (getColorDistance(
                        color1,
                        color2
                    ) > AppPreferences.getAveragingColorDistanceTolerance(context)
                ) {
                    return Color.TRANSPARENT
                }
            }
            red += Color.red(color1)
            green += Color.green(color1)
            blue += Color.blue(color1)
        }
        //return an average color
        val resultCount = resultInfoList.size
        return Color.rgb(red / resultCount, green / resultCount, blue / resultCount)
    }
}