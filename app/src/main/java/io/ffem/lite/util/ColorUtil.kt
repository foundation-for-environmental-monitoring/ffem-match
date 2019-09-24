package io.ffem.lite.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
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
import io.ffem.lite.camera.MARGIN
import io.ffem.lite.camera.Utilities
import io.ffem.lite.model.*
import io.ffem.lite.preference.MAX_COLOR_DISTANCE_RGB
import io.ffem.lite.preference.getColorDistanceTolerance
import timber.log.Timber
import java.io.IOException
import java.util.*
import kotlin.math.*

const val INTERPOLATION_COUNT = 250.0
const val MAX_DISTANCE = 999

object ColorUtil {

    private var gson = Gson()
    private var swatches: ArrayList<Swatch> = ArrayList()

    private lateinit var leftBarcodeBitmap: Bitmap
    private lateinit var rightBarcodeBitmap: Bitmap
    private var processing = false

    private var done: Boolean = false
    private lateinit var localBroadcastManager: LocalBroadcastManager

    private var cropLeft = 0
    private var cropRight = 0
    private var cropTop = 0
    private var cropBottom = 0

    fun extractImage(context: Context, id: String, bitmap: Bitmap) {

        val detector: FirebaseVisionBarcodeDetector by lazy {
            localBroadcastManager = LocalBroadcastManager.getInstance(context)
            val options = FirebaseVisionBarcodeDetectorOptions.Builder()
                .setBarcodeFormats(
                    FirebaseVisionBarcode.FORMAT_CODE_128
                )
                .build()
            FirebaseVision.getInstance().getVisionBarcodeDetector(options)
        }

        val leftBarcodeBitmap = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, (bitmap.height * 0.15).toInt()
        )

        detector.detectInImage(FirebaseVisionImage.fromBitmap(leftBarcodeBitmap))
            .addOnFailureListener(
                fun(_: Exception) {
                    processing = false
                }
            )
            .addOnSuccessListener(
                fun(result: List<FirebaseVisionBarcode>) {
                    if (result.isEmpty()) {
                        processing = false
                    }
                    for (barcode in result) {
                        if (!barcode.rawValue.isNullOrEmpty()) {

                            val left = barcode.boundingBox!!.left
                            val right = barcode.boundingBox!!.right
                            val width = barcode.boundingBox!!.width()
                            val height = barcode.boundingBox!!.height()

//                                Timber.e("Width %s", width)
//                                Timber.e("Height %s", height)
//                                Timber.e("Image Width %s", bitmap.width)
//                                Timber.e("Image Height %s", bitmap.height)
//                                Timber.e("-----------------------")
//                                Timber.e("")

                            if (height > bitmap.height * .065
                                && width > bitmap.width * .38
                            ) {
                                try {
                                    for (i in left + MARGIN until right - MARGIN) {
                                        val pixel = leftBarcodeBitmap.getPixel(i, 5)
                                        if (pixel.red < 50 && pixel.green < 50 && pixel.blue < 50) {
                                            processing = false
                                            return
                                        }
                                    }

                                    cropLeft = barcode.boundingBox!!.left
                                    cropRight = barcode.boundingBox!!.right
                                    cropTop = barcode.boundingBox!!.bottom + 10

                                    leftBarcodeBitmap.recycle()

                                    rightBarcodeBitmap = Bitmap.createBitmap(
                                        bitmap,
                                        0,
                                        (bitmap.height * 0.85).toInt(),
                                        bitmap.width,
                                        (bitmap.height * 0.15).toInt()
                                    )

                                    detector.detectInImage(
                                        FirebaseVisionImage.fromBitmap(rightBarcodeBitmap)
                                    )
                                        .addOnFailureListener(fun(_: Exception) {
                                            processing = false
                                        })
                                        .addOnSuccessListener(
                                            fun(result: List<FirebaseVisionBarcode>) {
                                                analyzeBarcode(context, id, bitmap, result)
                                            }
                                        )
                                } catch (ignored: Exception) {
                                    processing = false
                                }
                            } else {
                                processing = false
                            }
                        } else {
                            processing = false
                        }
                    }
                }
            )
    }

    private fun analyzeBarcode(
        context: Context,
        id: String,
        bitmap: Bitmap,
        result: List<FirebaseVisionBarcode>
    ) {
        if (result.isEmpty()) {
            processing = false
        }
        for (barcode2 in result) {
            if (!barcode2.rawValue.isNullOrEmpty()) {
                if (barcode2.boundingBox!!.height() > bitmap.height * .065
                    && barcode2.boundingBox!!.width() > bitmap.width * .38
                ) {
                    for (i in barcode2.boundingBox!!.left + MARGIN until
                            barcode2.boundingBox!!.right - MARGIN) {
                        val pixel =
                            rightBarcodeBitmap.getPixel(
                                i,
                                rightBarcodeBitmap.height - 5
                            )
                        if (pixel.red < 50 && pixel.green < 50 && pixel.blue < 50) {
                            processing = false
                            return
                        }
                    }

                    done = true

                    val left2 = max(0, barcode2.boundingBox!!.left - MARGIN)
                    val right2 = min(bitmap.width, barcode2.boundingBox!!.right + MARGIN)

                    cropBottom =
                        bitmap.height - cropTop - rightBarcodeBitmap.height + barcode2.boundingBox!!.top - 10

                    val croppedBitmap = Bitmap.createBitmap(
                        bitmap, left2, cropTop,
                        right2 - left2,
                        cropBottom
                    )

                    var marginTop = 0
                    for (i in 0..croppedBitmap.height) {
                        val pixel = croppedBitmap.getPixel(
                            croppedBitmap.width / 2, i
                        )
                        if (Color.red(pixel) < 100) {
                            marginTop = i
                            cropTop += i
                            break
                        }
                    }

                    for (i in croppedBitmap.height - 1 downTo 0) {
                        val pixel = croppedBitmap.getPixel(
                            croppedBitmap.width / 2, i
                        )
                        if (Color.red(pixel) < 100) {
                            cropBottom -= croppedBitmap.height - i
                            break
                        }
                    }

                    croppedBitmap.recycle()

                    val croppedBitmap2 = Bitmap.createBitmap(
                        bitmap, cropLeft, cropTop,
                        cropRight - cropLeft,
                        cropBottom - marginTop
                    )

                    val resultDetail = extractColors(context, croppedBitmap2)

//                    val croppedBitmap1 =
//                        Bitmap.createBitmap(bitmap, left2, 0, right2 - left2, bitmap.height)

                    val bitmapRotated = Utilities.rotateImage(croppedBitmap2, 270)

//                    croppedBitmap1.recycle()
                    croppedBitmap2.recycle()

                    val filePath = Utilities.savePicture(
                        context.applicationContext, id,
                        "", Utilities.bitmapToBytes(bitmapRotated)
                    )

                    bitmapRotated.recycle()

                    val intent = Intent(App.LOCAL_RESULT_EVENT)
//                    intent.putExtra(App.FILE_PATH_KEY, filePath)
                    intent.putExtra(App.TEST_ID_KEY, id)
                    intent.putExtra(
                        App.TEST_RESULT,
                        (round(resultDetail.result * 100) / 100.0).toString()
                    )
                    localBroadcastManager.sendBroadcast(
                        intent
                    )
                } else {
                    processing = false
                }

//                rightBarcodeBitmap.recycle()
            } else {
                processing = false
            }
        }
        processing = false
    }

    private fun extractColors(context: Context, image: Bitmap): ResultDetail {

        val input = context.resources.openRawResource(R.raw.calibration2)
        try {

//            for (i in 0..image.width) {
//                val pixel = image.getPixel(i, image.height - 5)
//            }

            val content = FileUtil.readTextFile(input)
            val calibration = gson.fromJson(content, Calibration::class.java)

            for (i in calibration.values) {
                val x = (i.x * image.width) / calibration.width
                val y = (i.y * image.height) / calibration.height
                i.color = image.getPixel(x, y)
            }

            swatches.add(Swatch(0.0, getCalibrationColor(0f, calibration)))
            swatches.add(Swatch(0.5, getCalibrationColor(.5f, calibration)))
            swatches.add(Swatch(1.0, getCalibrationColor(1f, calibration)))
            swatches.add(Swatch(1.5, getCalibrationColor(1.5f, calibration)))
            swatches.add(Swatch(2.0, getCalibrationColor(2f, calibration)))
            swatches = generateGradient(swatches)


            val x1 = (190 * image.width) / calibration.width
            val y1 = (620 * image.height) / calibration.height

            val colorInfo = ColorInfo(image.getPixel(x1, y1))
            return analyzeColor(5, colorInfo, swatches)

        } catch (e: IOException) {
            Timber.e(e, "error reading tests")
        }
        return ResultDetail((-1).toDouble(), 0)
    }

    private fun getCalibrationColor(pointValue: Float, calibration: Calibration): Int {
        var red = 0
        var green = 0
        var blue = 0

        var count = 0

        for (i in calibration.values) {
            if (i.value == pointValue) {
                count += 1
                red += Color.red(i.color)
                green += Color.green(i.color)
                blue += Color.blue(i.color)
            }
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

        var distance: Double
        distance = getMaxDistance(getColorDistanceTolerance().toDouble())

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
