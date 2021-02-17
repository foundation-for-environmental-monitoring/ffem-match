package io.ffem.lite.camera

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.common.*
import io.ffem.lite.common.Constants.CALIBRATION_COLOR_AREA_WIDTH_PERCENTAGE
import io.ffem.lite.common.Constants.MAX_TILT_PERCENTAGE_ALLOWED
import io.ffem.lite.common.Constants.QR_TO_COLOR_AREA_DISTANCE_PERCENTAGE
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.TestInfo
import io.ffem.lite.preference.*
import io.ffem.lite.util.ImageColorUtil
import io.ffem.lite.util.ImageUtil.toBitmap
import io.ffem.lite.util.PreferencesUtil
import io.ffem.lite.util.getAverageBrightness
import io.ffem.lite.util.getBitmapPixels
import io.ffem.lite.zxing.BinaryBitmap
import io.ffem.lite.zxing.LuminanceSource
import io.ffem.lite.zxing.RGBLuminanceSource
import io.ffem.lite.zxing.Result
import io.ffem.lite.zxing.common.HybridBinarizer
import io.ffem.lite.zxing.datamatrix.decoder.DataMatrixReader
import io.ffem.lite.zxing.qrcode.QRCodeReader
import io.ffem.lite.zxing.qrcode.detector.FinderPatternInfo
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ColorCardAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {
    private lateinit var bitmap: Bitmap
    private lateinit var croppedBitmap: Bitmap

    companion object {
        private var capturePhoto: Boolean = false
        private var processing = false
        private var done: Boolean = false
        var autoFocusCounter = 0
        var autoFocusCounter2 = 0
        var pattern: FinderPatternInfo? = null
        val dataMatrixReader = DataMatrixReader()
    }

    var previewWidth: Int = 0
    var previewHeight: Int = 0
    var viewFinderHeight: Int = 0
    private lateinit var localBroadcastManager: LocalBroadcastManager

    override fun analyze(imageProxy: ImageProxy) {
        if (done || processing) {
            return
        }
        processing = true

        localBroadcastManager = LocalBroadcastManager.getInstance(context)

        try {
            bitmap =
                if (BuildConfig.DEBUG && (isDiagnosticMode() || BuildConfig.INSTRUMENTED_TEST_RUNNING.get())) {
                    val imageNumber = getSampleTestImageNumberInt()
                    if (imageNumber > -1) {
                        try {
                            val drawable = ContextCompat.getDrawable(
                                context, context.resources.getIdentifier(
                                    "test_${
                                        java.lang.String.format(
                                            Locale.ROOT,
                                            "%03d",
                                            imageNumber
                                        )
                                    }",
                                    "drawable", context.packageName
                                )
                            )
                            getBitmap(
                                (drawable as BitmapDrawable).bitmap,
                                previewHeight,
                                previewWidth
                            )
                        } catch (ex: Exception) {
                            sendMessage(context.getString(R.string.sample_image_not_found))
                            endProcessing(imageProxy)
                            throw Exception()
                        }
                    } else {
                        getBitmap(imageProxy.toBitmap(), previewHeight, previewWidth)
                    }
                } else {
                    getBitmap(imageProxy.toBitmap(), previewHeight, previewWidth)
                }

            pattern = getPatternFromBitmap(bitmap)
            if (pattern != null) {

                pattern?.apply {

                    // Check if camera is too close
                    if (topLeft.x < bitmap.width * 0.018 ||
                        bottomRight.y > bitmap.height * 0.96 ||
                        topRight.y < bitmap.height * 0.048
                    ) {
                        sendMessage(context.getString(R.string.too_close))
                        endProcessing(imageProxy)
                        return
                    }

                    val allowedTilt = max(13.0, MAX_TILT_PERCENTAGE_ALLOWED * imageProxy.height)
                    // Check if image is tilted
                    if (abs(topLeft.x - bottomLeft.x) > allowedTilt ||
                        abs(topLeft.y - topRight.y) > allowedTilt ||
                        abs(topRight.x - bottomRight.x) > allowedTilt ||
                        abs(bottomLeft.y - bottomRight.y) > allowedTilt
                    ) {

                        val forward = (bottomRight.y - topRight.y) - (bottomLeft.y - topLeft.y)
                        val backward = (bottomLeft.y - topLeft.y) - (bottomRight.y - topRight.y)
                        val leftBackward = (topRight.x - topLeft.x) - (bottomRight.x - bottomLeft.x)
                        val leftForward = (bottomRight.x - bottomLeft.x) - (topRight.x - topLeft.x)

                        when (max(max(max(forward, backward), leftBackward), leftForward)) {
                            forward -> {
                                sendMessage(context.getString(R.string.tilt_forwards))
                            }
                            backward -> {
                                sendMessage(context.getString(R.string.tilt_backwards))
                            }
                            leftBackward -> {
                                sendMessage(context.getString(R.string.tilt_left_backwards))
                            }
                            leftForward -> {
                                sendMessage(context.getString(R.string.tilt_left_forwards))
                            }
                        }

                        endProcessing(imageProxy)
                        return
                    }

                    // Check if camera is too far
                    if (bottomRight.y - topRight.y < bitmap.height * 0.8 ||
                        topRight.x - topLeft.x < bitmap.width * 0.7
                    ) {
                        sendMessage(context.getString(R.string.closer))
                        endProcessing(imageProxy)
                        return
                    }

                    // Check brightness levels of white areas on the card
                    val whiteAreaStart = bitmap.height * 0.07
                    val whiteAreaEnd = bitmap.height * 0.17
                    val whiteAreaWidth = (bitmap.width * 0.2).toInt()
                    var rect = Rect(
                        topLeft.x.toInt(),
                        (topLeft.y + whiteAreaStart).toInt(),
                        topLeft.x.toInt() + whiteAreaWidth,
                        (topLeft.y + whiteAreaEnd).toInt()
                    )
                    var pixels = getBitmapPixels(bitmap, rect)
                    val averageBrightness1 = getAverageBrightness(pixels)
                    if (averageBrightness1 < getMinimumBrightness()) {
                        sendMessage(context.getString(R.string.not_bright))
                        endProcessing(imageProxy)
                        return
                    }
                    if (averageBrightness1 > getMaximumBrightness()) {
                        sendMessage(context.getString(R.string.too_bright))
                        endProcessing(imageProxy)
                        return
                    }

                    rect = Rect(
                        topRight.x.toInt() - whiteAreaWidth,
                        (topRight.y + whiteAreaStart).toInt(),
                        topRight.x.toInt(),
                        (topRight.y + whiteAreaEnd).toInt(),
                    )

                    pixels = getBitmapPixels(bitmap, rect)
                    val averageBrightness2 = getAverageBrightness(pixels)
                    if (averageBrightness2 < getMinimumBrightness()) {
                        sendMessage(context.getString(R.string.not_bright))
                        endProcessing(imageProxy)
                        return
                    }
                    if (averageBrightness2 > getMaximumBrightness()) {
                        sendMessage(context.getString(R.string.too_bright))
                        endProcessing(imageProxy)
                        return
                    }

                    rect = Rect(
                        bottomLeft.x.toInt(),
                        (bottomLeft.y - whiteAreaEnd).toInt(),
                        topRight.x.toInt(),
                        (bottomLeft.y - whiteAreaStart).toInt(),
                    )
                    pixels = getBitmapPixels(bitmap, rect)
                    val averageBrightness3 = getAverageBrightness(pixels)
                    if (averageBrightness3 < getMinimumBrightness()) {
                        sendMessage(context.getString(R.string.not_bright))
                        endProcessing(imageProxy)
                        return
                    }
                    if (averageBrightness3 > getMaximumBrightness()) {
                        sendMessage(context.getString(R.string.too_bright))
                        endProcessing(imageProxy)
                        return
                    }

                    // Check for shadows on card
                    val shadowTolerance = getShadowTolerance()
                    if (abs(averageBrightness1 - averageBrightness2) > shadowTolerance ||
                        abs(averageBrightness2 - averageBrightness3) > shadowTolerance ||
                        abs(averageBrightness1 - averageBrightness3) > shadowTolerance
                    ) {
                        sendMessage(context.getString(R.string.too_many_shadows))
                        endProcessing(imageProxy)
                        return
                    }

//                    if (autoFocusCounter < 10) {
//                        autoFocusCounter++
//                        endProcessing(imageProxy, false)
//                        return
//                    }

                    val testInfo = App.getTestInfo(testId)
                    if (testInfo == null) {
                        if (testId.isNotEmpty()) {
                            // if test id is not recognized
                            sendMessage(context.getString(R.string.invalid_barcode))
                            endProcessing(imageProxy)
                            return
                        }
                    } else {
                        // if requested test id does not match the card test id
                        val requestedTestId = PreferencesUtil.getString(context, TEST_ID_KEY, "")
                        if ((requestedTestId!!.isNotEmpty() && testInfo.uuid != requestedTestId)) {
                            sendMessage(context.getString(R.string.wrong_card))
                            return
                        }
                        savePhoto(bitmap, testInfo)
                    }

                    croppedBitmap = perspectiveTransform(bitmap, pattern!!)
                    bitmap.recycle()

                    if (testInfo != null && testInfo.resultInfo.result > -2) {
                        ImageColorUtil.getResult(
                            context,
                            testInfo,
                            ErrorType.NO_ERROR,
                            croppedBitmap
                        )
                        croppedBitmap.recycle()

                        sendMessage(context.getString(R.string.analyzing_photo))
                        done = true
                        val intent = Intent(CARD_CAPTURED_EVENT_BROADCAST)
                        intent.putExtra(TEST_INFO_KEY, testInfo)
                        localBroadcastManager.sendBroadcast(
                            intent
                        )

                        val resultIntent = Intent(RESULT_EVENT_BROADCAST)
                        resultIntent.putExtra(TEST_INFO_KEY, testInfo)
                        localBroadcastManager.sendBroadcast(
                            resultIntent
                        )
                    } else {
                        sendMessage(context.getString(R.string.color_card_not_found))
                        endProcessing(imageProxy)
                        return
                    }
                }
            } else {
                sendMessage(context.getString(R.string.color_card_not_found))
                endProcessing(imageProxy)
                return
            }
        } catch (e: Exception) {
            endProcessing(imageProxy)
            return
        }
        endProcessing(imageProxy)
    }

    private fun getBitmap(bitmap: Bitmap, previewHeight: Int, previewWidth: Int): Bitmap {
        val actualHeight = (bitmap.height * previewHeight) / previewWidth
        val margin = if (actualHeight < bitmap.width) {
            bitmap.width - actualHeight
        } else {
            0
        }
        val height = min(
            bitmap.height,
            ((bitmap.width * bitmap.height) / actualHeight)
        )
        val width = min(
            viewFinderHeight,
            ((actualHeight * viewFinderHeight) / previewHeight)
        )

        return Bitmap.createBitmap(
            bitmap, margin / 2, (bitmap.height - height) / 2,
            width, height
        )
    }

    //https://stackoverflow.com/questions/13161628/cropping-a-perspective-transformation-of-image-on-android
    private fun perspectiveTransform(bitmap: Bitmap, pattern: FinderPatternInfo): Bitmap {
        val matrix = Matrix()
        val dst = floatArrayOf(
            0f, 0f,
            bitmap.width.toFloat(), 0f,
            bitmap.width.toFloat(),
            bitmap.height.toFloat(), 0f,
            bitmap.height.toFloat()
        )
        val src = floatArrayOf(
            pattern.topLeft.x,
            pattern.topLeft.y,
            pattern.topRight.x,
            pattern.topRight.y,
            pattern.bottomRight.x,
            pattern.bottomRight.y,
            pattern.bottomLeft.x,
            pattern.bottomLeft.y
        )
        matrix.setPolyToPoly(src, 0, dst, 0, src.size shr 1)
        val mappedTL = floatArrayOf(0f, 0f)
        matrix.mapPoints(mappedTL)
        val mapTLx = mappedTL[0].roundToInt()
        val mapTLy = mappedTL[1].roundToInt()

        val mappedTR = floatArrayOf(bitmap.width.toFloat(), 0f)
        matrix.mapPoints(mappedTR)
//        val mapTRx = Math.round(mappedTR[0])
        val mapTRy = mappedTR[1].roundToInt()

        val mappedLL = floatArrayOf(0f, bitmap.height.toFloat())
        matrix.mapPoints(mappedLL)
        val mapLLx = mappedLL[0].roundToInt()
//        val mapLLy = mappedLL[1].roundToInt()

        val shiftX = max(-mapTLx, -mapLLx)
        val shiftY = max(-mapTRy, -mapTLy)

        val resultBitmap: Bitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        val top = shiftY + (bitmap.height * QR_TO_COLOR_AREA_DISTANCE_PERCENTAGE).toInt()
        val height = bitmap.height * CALIBRATION_COLOR_AREA_WIDTH_PERCENTAGE

        return Bitmap.createBitmap(
            resultBitmap,
            shiftX,
            top,
            bitmap.width,
            height.toInt(),
            null,
            true
        )
    }

    // https://stackoverflow.com/questions/14861553/zxing-convert-bitmap-to-binarybitmap
    private fun getPatternFromBitmap(bMap: Bitmap): FinderPatternInfo? {
        val intArray = IntArray(bMap.width * bMap.height)
        //copy pixel data from the Bitmap into the 'intArray' array
        bMap.getPixels(intArray, 0, bMap.width, 0, 0, bMap.width, bMap.height)
        val source: LuminanceSource = RGBLuminanceSource(bMap.width, bMap.height, intArray)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        return getPatternFromBinaryBitmap(binaryBitmap)
    }

    private fun getPatternFromBinaryBitmap(bitmap: BinaryBitmap): FinderPatternInfo? {
        var result: FinderPatternInfo? = null
        try {
            result = QRCodeReader().getPatterns(bitmap, null)
            if (result != null) {
                result.width = bitmap.width
                result.height = bitmap.height
                val dataResult: Result = dataMatrixReader.decode(
                    bitmap.crop(
                        result.topLeft.x.toInt() + (bitmap.width / 5),
                        0,
                        (bitmap.width / 2.4).toInt(),
                        (bitmap.height / 3.7).toInt()
                    ), null
                )
                result.testId = dataResult.text
            }
        } catch (e: Exception) {
            return result
        }
        return result
    }

    private fun endProcessing(imageProxy: ImageProxy) {
        processing = false
        imageProxy.close()
        if (::croppedBitmap.isInitialized) {
            croppedBitmap.recycle()
        }
        if (::bitmap.isInitialized) {
            bitmap.recycle()
        }
    }

    private fun savePhoto(bitmap: Bitmap, testInfo: TestInfo) {

        val bitmapRotated = Utilities.rotateImage(bitmap, 90)

        Utilities.savePicture(
            context.applicationContext,
            testInfo.fileName,
            testInfo.name!!,
            Utilities.bitmapToBytes(bitmapRotated), ""
        )

        bitmapRotated.recycle()
    }

    private fun sendMessage(s: String) {
        val intent = Intent(ERROR_EVENT_BROADCAST)
        intent.putExtra(ERROR_MESSAGE, s)
        intent.putExtra(
            SCAN_PROGRESS,
            autoFocusCounter + autoFocusCounter2
        )
        localBroadcastManager.sendBroadcast(
            intent
        )
    }

    fun reset() {
        done = false
        processing = false
        capturePhoto = false
        autoFocusCounter = 0
        autoFocusCounter2 = 0
    }
}
