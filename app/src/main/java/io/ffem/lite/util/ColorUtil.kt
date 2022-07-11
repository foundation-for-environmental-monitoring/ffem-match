package io.ffem.lite.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.SparseIntArray
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import io.ffem.lite.common.Constants.MAX_COLOR_DISTANCE_RGB
import io.ffem.lite.common.Constants.SENSOR_INTERPOLATION_COUNT
import io.ffem.lite.model.ColorInfo
import io.ffem.lite.model.PulseWidth
import timber.log.Timber
import java.util.*
import kotlin.math.*

/**
 * The minimum color distance at which the colors are considered equivalent
 */
const val MIN_DISTANCE = 6.0

/**
 * Set of utility functions for color calculations and analysis
 */
object ColorUtil {

    fun getMaxDistance(defaultValue: Double): Double {
        return if (defaultValue > 0) {
            defaultValue
        } else {
            MAX_COLOR_DISTANCE_RGB.toDouble()
        }
    }

    /**
     * Get the most common color from the bitmap
     *
     * @param bitmap       The bitmap from which to extract the color
     * @param sampleLength The max length of the image to traverse
     * @return The extracted color information
     */
    fun getColorFromBitmap(bitmap: Bitmap, sampleLength: Int): ColorInfo {
        var highestCount = 0
        var commonColor = -1
        var counter: Int
        var goodPixelCount = 0
        var totalPixels = 0
        val colorsFound: Int
        try {
            val m = SparseIntArray()
            for (i in 0 until min(bitmap.width, sampleLength)) {
                for (j in 0 until min(bitmap.height, sampleLength)) {
                    val color = bitmap.getPixel(i, j)
                    if (color != Color.TRANSPARENT) {
                        totalPixels++
                        counter = m[color]
                        counter++
                        m.put(color, counter)
                        if (counter > highestCount) {
                            commonColor = color
                            highestCount = counter
                        }
                    }
                }
            }

            // check the quality of the photo
            colorsFound = m.size()
            var goodColors = 0
            for (i in 0 until colorsFound) {
                if (areColorsSimilar(commonColor, m.keyAt(i))) {
                    goodColors++
                    goodPixelCount += m.valueAt(i)
                }
            }
            m.clear()
        } catch (e: Exception) {
            Timber.e(e)
        }
        return ColorInfo(0.0, commonColor)
    }

    /**
     * Get the brightness of a given color
     *
     * @param color The color
     * @return The brightness value
     */
    fun getBrightness(color: Int): Int {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return sqrt(r * r * .241 + g * g * .691 + b * b * .068).toInt()
    }

    /**
     * Computes the Euclidean distance between the two colors
     *
     * @param color1 the first color
     * @param color2 the color to compare with
     * @return the distance between the two colors
     */
    fun getColorDistance(color1: Int, color2: Int): Double {
        val r: Double = (Color.red(color2) - Color.red(color1).toDouble()).pow(2.0)
        val g: Double = (Color.green(color2) - Color.green(color1).toDouble()).pow(2.0)
        val b: Double = (Color.blue(color2) - Color.blue(color1).toDouble()).pow(2.0)
        return sqrt(b + g + r)
    }

    fun getDistance(color1: PulseWidth, color2: PulseWidth): Double {
        val r: Double = (color2.a[0] - color1.a[0]).toDouble().pow(2.0)
        val g: Double = (color2.a[1] - color1.a[1]).toDouble().pow(2.0)
        val b: Double = (color2.a[2] - color1.a[2]).toDouble().pow(2.0)
        val c: Double = (color2.a[3] - color1.a[3]).toDouble().pow(2.0)
        return sqrt(r + g + b + c)
    }

    //    public static boolean areColorsTooDissimilar(int color1, int color2) {
    //        return getColorDistanceRgb(color1, color2) > MAX_SAMPLING_COLOR_DISTANCE_RGB;
    //    }
    fun areColorsSimilar(color1: Int, color2: Int): Boolean {
        return getColorDistance(color1, color2) < MIN_DISTANCE
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
    @JvmStatic
    fun getGradientColor(startColor: Int, endColor: Int, n: Int, i: Int): Int {
        return Color.rgb(
            interpolate(Color.red(startColor), Color.red(endColor), n, i),
            interpolate(Color.green(startColor), Color.green(endColor), n, i),
            interpolate(Color.blue(startColor), Color.blue(endColor), n, i)
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

    fun interpolate(start: PulseWidth, end: PulseWidth): ArrayList<PulseWidth> {
        val array = ArrayList<PulseWidth>()
        val interval = (end.value - start.value) / SENSOR_INTERPOLATION_COUNT
        for (i in 0..SENSOR_INTERPOLATION_COUNT) {
            val pulse = ArrayList<Int>()
            pulse.add(start.a[0] + i * (end.a[0] - start.a[0]) / SENSOR_INTERPOLATION_COUNT)
            pulse.add(start.a[1] + i * (end.a[1] - start.a[1]) / SENSOR_INTERPOLATION_COUNT)
            pulse.add(start.a[2] + i * (end.a[2] - start.a[2]) / SENSOR_INTERPOLATION_COUNT)
            pulse.add(start.a[3] + i * (end.a[3] - start.a[3]) / SENSOR_INTERPOLATION_COUNT)
            array.add(PulseWidth(start.value + (interval * i), pulse))
        }
        return array
    }


    /**
     * Convert color value to RGB string
     *
     * @param color The color to convert
     * @return The rgb value as string
     */
    fun getColorRgbString(color: Int): String {
        return if (color == Color.TRANSPARENT) {
            ""
        } else String.format(
            Locale.getDefault(),
            "%d  %d  %d",
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }

    /**
     * Convert rgb string color to color
     *
     * @param rgbValue The rgb string representation of the color
     * @return An Integer color value
     */
    fun getColorFromRgb(rgbValue: String): Int {
        var rgb = rgbValue.trim()
        rgb = rgb.replace(",", " ").trim()
        if (rgb.isEmpty()) {
            return 0
        }
        val rgbArray = rgb.split("\\s+".toPattern()).toTypedArray()
        return Color.rgb(rgbArray[0].toInt(), rgbArray[1].toInt(), rgbArray[2].toInt())
    }


    fun getAverageColor(pixels: IntArray, ignoreDarkPixels: Boolean): Int {
        var list = pixels.toCollection(ArrayList())
        for (i in 40 downTo 10 step 3) {
            list = removeOutliers(list, i)
        }

        if (ignoreDarkPixels) {
            list = removeDarkColors(list)
            // Reject if too few pixels remaining
            if (list.size < pixels.size / 25) {
                return Color.TRANSPARENT
            }
        } else {

            // Reject if too few pixels remaining
            if (list.size < pixels.size / 10) {
                return Color.TRANSPARENT
            }
        }
        return getMean(list)
    }

    private fun removeDarkColors(pixels: ArrayList<Int>): ArrayList<Int> {
        val newList = ArrayList<Int>()

        var darkLuminance = 1000.0
        for (element in pixels) {
            val value = ColorUtils.calculateLuminance(element)
            if (value < darkLuminance) {
                darkLuminance = value
            }
        }

        var lightLuminance = 0.0
        for (element in pixels) {
            val value = ColorUtils.calculateLuminance(element)
            if (value > lightLuminance) {
                lightLuminance = value
            }
        }

        return if (abs(lightLuminance - darkLuminance) > 0.06) {
            val luminanceStart = lightLuminance - (abs(lightLuminance - darkLuminance) * 0.5)
            val luminanceEnd = lightLuminance - (abs(lightLuminance - darkLuminance) * 0.1)
            for (element in pixels) {
                if (ColorUtils.calculateLuminance(element) in luminanceStart..luminanceEnd) {
                    newList.add(element)
                }
            }
            newList
        } else {
            pixels
        }
    }

    private fun removeOutliers(pixels: ArrayList<Int>, distance: Int): ArrayList<Int> {
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

    private fun getMean(pixels: ArrayList<Int>): Int {
        var r = 0
        var g = 0
        var b = 0

        for (element in pixels) {
            r += element.red
            g += element.green
            b += element.blue
        }

        val pixelsCount = max(pixels.size, 1)
        return Color.rgb(r / pixelsCount, g / pixelsCount, b / pixelsCount)
    }


}

fun getBitmapPixels(bitmap: Bitmap, rect: Rect): IntArray {
    val pixels = IntArray(bitmap.width * bitmap.height)

    val x = max(0, rect.left)
    val y = max(0, rect.top)
    var width = rect.width()
    if (x + rect.width() > bitmap.width) {
        width = bitmap.width - x
    }
    var height = rect.height()
    if (y + rect.height() > bitmap.height) {
        height = bitmap.height - y
    }
    bitmap.getPixels(
        pixels, 0, bitmap.width, x, y, width, height
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

fun getAverageBrightness(pixels: IntArray): Int {
    var r = 0

    if (pixels.isEmpty()) {
        return 0
    }

    for (element in pixels) {
        r += element.red
    }

    return r / pixels.size
}
