@file:Suppress("SpellCheckingInspection")

package io.ffem.lite.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

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

fun removeDarkColors(pixels: ArrayList<Int>): ArrayList<Int> {
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

    return if (abs(lightLuminance - darkLuminance) > 0.01) {
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