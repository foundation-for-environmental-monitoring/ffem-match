package io.ffem.lite.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.gson.Gson
import io.ffem.lite.R
import io.ffem.lite.model.*
import io.ffem.lite.preference.MAX_COLOR_DISTANCE_RGB
import io.ffem.lite.preference.getColorDistanceTolerance
import timber.log.Timber
import java.io.IOException
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

const val INTERPOLATION_COUNT = 250.0
const val MAX_DISTANCE = 999

object ColorUtil {

    private var gson = Gson()
    private var swatches: ArrayList<Swatch> = ArrayList()

//    // Grey scale matrix
//    private fun createGreyMatrix(): ColorMatrix {
//        return ColorMatrix(
//            floatArrayOf(
//                0.2989f,
//                0.5870f,
//                0.1140f,
//                0f,
//                0f,
//                0.2989f,
//                0.5870f,
//                0.1140f,
//                0f,
//                0f,
//                0.2989f,
//                0.5870f,
//                0.1140f,
//                0f,
//                0f,
//                0f,
//                0f,
//                0f,
//                1f,
//                0f
//            )
//        )
//    }
//
//    // Threshold matrix
//    @Suppress("SameParameterValue")
//    private fun createThresholdMatrix(threshold: Int): ColorMatrix {
//        return ColorMatrix(
//            floatArrayOf(
//                85f,
//                85f,
//                85f,
//                0f,
//                -255f * threshold,
//                85f,
//                85f,
//                85f,
//                0f,
//                -255f * threshold,
//                85f,
//                85f,
//                85f,
//                0f,
//                -255f * threshold,
//                0f,
//                0f,
//                0f,
//                1f,
//                0f
//            )
//        )
//    }

//    fun extractGrid(image: Bitmap): Bitmap? {
//        val options = BitmapFactory.Options()
//        options.inScaled = false
//        val bitmapPaint = Paint()
//
//        //load source bitmap and prepare destination bitmap
//        val result = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
//        val c = Canvas(result)
//
//        //first convert bitmap to grey scale:
//        bitmapPaint.colorFilter = ColorMatrixColorFilter(createGreyMatrix())
//        c.drawBitmap(image, 0f, 0f, bitmapPaint)
//
//        //then convert the resulting bitmap to black and white using threshold matrix
//        bitmapPaint.colorFilter = ColorMatrixColorFilter(createThresholdMatrix(120))
//        c.drawBitmap(result, 0f, 0f, bitmapPaint)
//
//        var height = 0
//        for (i in image.height - 1 downTo 0) {
//            val pixel = result.getPixel(image.width / 4, i)
//            if (Color.red(pixel) == 0) {
//                height = i
//                break
//            }
//        }
//
//        var left = 0
//        for (i in image.width / 4 downTo 0) {
//            val pixel = result.getPixel(i, height)
//            if (Color.red(pixel) > 0) {
//                left = i + 1
//                break
//            }
//        }
//
//        var top = 0
//        for (i in height downTo 0) {
//            val pixel = result.getPixel(left, i)
//            if (Color.red(pixel) > 0) {
//                top = i
//                break
//            }
//        }
//
//        var rightBottom = 0
//        for (i in image.height - 1 downTo 0) {
//            val pixel = result.getPixel((image.width / 4) * 3, i)
//            if (Color.red(pixel) == 0) {
//                rightBottom = i
//                break
//            }
//        }
//
//        var right = 0
//        for (i in (image.width / 4) * 3 until image.width) {
//            val pixel = result.getPixel(i, rightBottom)
//            if (Color.red(pixel) > 0) {
//                right = i + 1
//                break
//            }
//        }
//
//        return Bitmap.createBitmap(image, left, top, right - left, height - top)
//    }

//    fun extractGrid(image: Bitmap): Bitmap? {
//        val originalMat = Mat()
//        Utils.bitmapToMat(image, originalMat)
//        val destMat = Mat()
//        Imgproc.cvtColor(originalMat, destMat, Imgproc.COLOR_BGR2GRAY)
//
//        val height = destMat.height()
//        val width = destMat.width()
//
//        Imgproc.medianBlur(destMat, destMat, 1)
//
//        val threshold = Mat()
//        Imgproc.adaptiveThreshold(
//            destMat, threshold, 255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C,
//            Imgproc.THRESH_BINARY, 11, 2.0
//        )
//        val contours: List<MatOfPoint> = ArrayList()
//        Imgproc.findContours(
//            threshold,
//            contours,
//            Mat(),
//            Imgproc.RETR_LIST,
//            Imgproc.CHAIN_APPROX_SIMPLE
//        )
//
//        var i = 0
//        val bottle_enclosure_area = 100000000
//        var inner_approx = null
//
//        // Find large squares
//        for (contour in contours) {
//
//            val contour2f = MatOfPoint2f()
//            contour.convertTo(contour2f, CvType.CV_32F)
//
//            if (Imgproc.arcLength(contour2f, true) < width / 2 ||
//                Imgproc.arcLength(contour2f, true) > 5000
//            ) {
//                continue
//            }
//            val approx = MatOfPoint2f()
//            Imgproc.approxPolyDP(
//                contour2f, approx,
//                0.08 * Imgproc.arcLength(contour2f, true), true
//            )
//
//            val rect = Imgproc.boundingRect(approx)
//            val x = rect.x
//            val y = rect.y
//            val w = rect.width
//            val h = rect.height
//
//            val ar = w / h
//
//            // Drop the contours that are not squares, not four-sided and crisscrossed
//            // This should roughly be the left and right outer contours.
//            if (w > originalMat.cols() / 4 && approx.toArray().size == 4 && ar >= 0.8 && ar <= 1.2
//                && Imgproc.pointPolygonTest(
//                    contour2f,
//                    Point(
//                        (x + w / 2).toDouble(),
//                        (y + h / 2).toDouble()
//                    ), false
//                ) > 0
//                && Imgproc.pointPolygonTest(
//                    contour2f,
//                    Point(
//                        (x + w / 3).toDouble(),
//                        (y + h * 2 / 3).toDouble()
//                    ), false
//                ) > 0
//            ) {
//
//                val pts = MatOfPoint2f()
//                pts.alloc(4)
//                for (idx in 0..3) {
//                    pts.toArray()[idx] = approx.toArray()[idx]
//                }
//                val warped = getWarpedImage(originalMat, pts)
//
//                val bitmap =
//                    Bitmap.createBitmap(warped!!.cols(), warped.rows(), Bitmap.Config.ARGB_8888)
////                Core.normalize(warped, mResult8u, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8U)
//                Utils.matToBitmap(warped, bitmap)
//
//                return bitmap
//            }
//        }
//        return null
//    }
//
//
//    private fun getWarpedImage2(image: Mat, pts: MatOfPoint2f): Mat {
//
//        val rect = reOrderPoints(pts)
//        val tl = rect.toArray()[0]
//        val br = rect.toArray()[1]
//        val tr = Point(br.x, tl.y)
//        val bl = Point(tl.x, br.y)
//
//        val wA = sqrt((br.x - bl.x).pow(2) + ((br.y - bl.y).pow(2)))
//        val wB = sqrt((tr.x - tl.x).pow(2) + ((tr.y - tl.y).pow(2)))
//        val maxWidth = max(wA, wB)
//
//        val hA = sqrt((tr.x - br.x).pow(2) + ((tr.y - br.y).pow(2)))
//        val hB = sqrt((tl.x - bl.x).pow(2) + ((tl.y - bl.y).pow(2)))
//        val maxHeight = max(hA, hB)
//
//        val dst = MatOfPoint2f(
//            Point(0.0, 0.0),
//            Point(maxWidth - 1, 0.0),
//            Point(maxWidth - 1, maxHeight - 1),
//            Point(0.0, maxHeight - 1)
//        )
//
////        val dst = MatOfPoint2f(
////            Point(0.0, 0.0),
////            Point((450 - 1).toDouble(), 0.0),
////            Point(0.0, (450 - 1).toDouble()),
////            Point((450 - 1).toDouble(), (450 - 1).toDouble())
////        )
//
//        val warpMat = Imgproc.getPerspectiveTransform(MatOfRect(rect), (dst))
////        val warped = Imgproc.warpPerspective(image, warpMat, (maxWidth.toDouble(), maxHeight.toDouble()))
//
//        val destImage = Mat()
//        Imgproc.warpPerspective(
//            image,
//            destImage,
//            warpMat,
//            image.size()
//        )
//
//        return destImage
//    }

//    private fun reOrderPoints(approx: MatOfPoint2f): MatOfPoint2f {
//        val sortedPoints = arrayOfNulls<Point?>(4)
//
//        //calculate the center of mass of our contour image using moments
//        val moment: Moments = Imgproc.moments(approx)
//        val x = (moment._m10 / moment._m00).toInt()
//        val y = (moment._m01 / moment._m00).toInt()
//
//        var data: DoubleArray
//        var count = 0
//        for (i in 0 until approx.rows()) {
//            data = approx.get(i, 0)
//            val datax = data[0]
//            val datay = data[1]
//            if (datax < x && datay < y) {
//                sortedPoints[0] = Point(datax, datay)
//                count++
//            } else if (datax > x && datay < y) {
//                sortedPoints[1] = Point(datax, datay)
//                count++
//            } else if (datax < x && datay > y) {
//                sortedPoints[2] = Point(datax, datay)
//                count++
//            } else if (datax > x && datay > y) {
//                sortedPoints[3] = Point(datax, datay)
//                count++
//            }
//        }
//        val src = MatOfPoint2f(
//            sortedPoints[0],
//            sortedPoints[1],
//            sortedPoints[2],
//            sortedPoints[3]
//        )
//
//        return src
//    }
//
//    //    https://stackoverflow.com/questions/47407438/how-to-compute-coordinates-of-rectange-and-then-do-perspective-transformation-us
//    private fun getWarpedImage(
//        image: Mat,
//        approx: MatOfPoint2f
//    ): Mat? {
//        //calculate the center of mass of our contour image using moments
//        val moment: Moments = Imgproc.moments(approx)
//        val x = (moment._m10 / moment._m00).toInt()
//        val y = (moment._m01 / moment._m00).toInt()
//
//        val sortedPoints = arrayOfNulls<Point?>(4)
//
//        var data: DoubleArray
//        var count = 0
//        for (i in 0 until approx.rows()) {
//            data = approx.get(i, 0)
//            val datax = data[0]
//            val datay = data[1]
//            if (datax < x && datay < y) {
//                sortedPoints[0] = Point(datax, datay)
//                count++
//            } else if (datax > x && datay < y) {
//                sortedPoints[1] = Point(datax, datay)
//                count++
//            } else if (datax < x && datay > y) {
//                sortedPoints[2] = Point(datax, datay)
//                count++
//            } else if (datax > x && datay > y) {
//                sortedPoints[3] = Point(datax, datay)
//                count++
//            }
//        }
//
//        val src = MatOfPoint2f(
//            sortedPoints[0],
//            sortedPoints[1],
//            sortedPoints[2],
//            sortedPoints[3]
//        )
//
//        val dst = MatOfPoint2f(
//            Point(0.0, 0.0),
//            Point((450 - 1).toDouble(), 0.0),
//            Point(0.0, (450 - 1).toDouble()),
//            Point((450 - 1).toDouble(), (450 - 1).toDouble())
//        )
//
//        val warpMat: Mat? = Imgproc.getPerspectiveTransform(src, dst)
//
//        val destImage = Mat()
//        Imgproc.warpPerspective(
//            image,
//            destImage,
//            warpMat,
//            image.size()
//        )
//        return destImage
//    }

    fun extractColors(context: Context, image: Bitmap): ResultDetail {

        val input = context.resources.openRawResource(R.raw.calibration2)
        try {

//            for (i in 0..image.width) {
//                val pixel = image.getPixel(i, image.height - 5)
//            }

            val content = FileUtil.readTextFile(input)
            val calibration = gson.fromJson(content, Calibration::class.java)

            for (i in calibration.values) {
                i.color = image.getPixel(i.x, i.y)
            }

            swatches.add(Swatch(0.0, getCalibrationColor(0f, calibration)))
            swatches.add(Swatch(0.5, getCalibrationColor(.5f, calibration)))
            swatches.add(Swatch(1.0, getCalibrationColor(1f, calibration)))
            swatches.add(Swatch(1.5, getCalibrationColor(1.5f, calibration)))
            swatches.add(Swatch(2.0, getCalibrationColor(2f, calibration)))
            swatches = generateGradient(swatches)

            val colorInfo = ColorInfo(image.getPixel(190, 620))
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
