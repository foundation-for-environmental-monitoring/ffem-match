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
import io.ffem.lite.common.*
import io.ffem.lite.common.Constants.CARD_DEFAULT_HEIGHT
import io.ffem.lite.common.Constants.CARD_DEFAULT_WIDTH
import io.ffem.lite.data.DataHelper
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.TestInfo
import io.ffem.lite.preference.*
import io.ffem.lite.scanner.zxing.BinaryBitmap
import io.ffem.lite.scanner.zxing.LuminanceSource
import io.ffem.lite.scanner.zxing.RGBLuminanceSource
import io.ffem.lite.scanner.zxing.Result
import io.ffem.lite.scanner.zxing.common.HybridBinarizer
import io.ffem.lite.scanner.zxing.datamatrix.DataMatrixReader
import io.ffem.lite.scanner.zxing.qrcode.QRCodeReader
import io.ffem.lite.scanner.zxing.qrcode.detector.FinderPatternInfo
import io.ffem.lite.util.ImageColorUtil
import io.ffem.lite.util.ImageUtil.toBitmap
import io.ffem.lite.util.getAverageBrightness
import io.ffem.lite.util.getBitmapPixels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

abstract class ColorCardAnalyzerBase(var testInfo: TestInfo, private val context: Context) :
    ImageAnalysis.Analyzer {
    private var currentTimestamp: Long = 0
    private lateinit var bitmap: Bitmap
    private var croppedBitmap: Bitmap? = null
    private var originalWidth: Int = 0
    private lateinit var localBroadcastManager: LocalBroadcastManager

    protected var swatchWidthPercentage: Double = -1.0
    private var swatchHeightPercentage: Double = -1.0
    protected var colorCardType = 0

    companion object {
        var ignoreWarnings: Boolean = false
        var processing = false
        var done: Boolean = false
        var autoFocusCounter = 0
        var autoFocusCounter2 = 0
        var pattern: FinderPatternInfo? = null
        val dataMatrixReader = DataMatrixReader()
    }

    override fun analyze(imageProxy: ImageProxy) {
        if (done || processing) {
            imageProxy.close()
            return
        }
        processing = true
        currentTimestamp = System.currentTimeMillis()
        if (!::localBroadcastManager.isInitialized) {
            localBroadcastManager = LocalBroadcastManager.getInstance(context)
        }
        val imageNumber = getSampleTestImageNumberInt()
        try {
            bitmap =
                if (BuildConfig.DEBUG && (isDiagnosticMode() || BuildConfig.INSTRUMENTED_TEST_RUNNING.get())) {
                    if (imageNumber > -1) {
                        try {
                            val fileName = "fm_circle_${
                                java.lang.String.format(
                                    Locale.ROOT,
                                    "%03d",
                                    imageNumber
                                )
                            }"

                            val drawable = ContextCompat.getDrawable(
                                context, context.resources.getIdentifier(
                                    fileName,
                                    "drawable", context.packageName
                                )
                            )
                            getBitmap(
                                (drawable as BitmapDrawable).bitmap
                            )
                        } catch (ex: Exception) {
                            sendMessage(context.getString(R.string.sample_image_not_found))
                            endProcessing(imageProxy)
                            throw Exception()
                        }
                    } else {
                        getBitmap(imageProxy.toBitmap())
                    }
                } else {
                    getBitmap(imageProxy.toBitmap())
                }

            processBitmap(imageProxy)

        } catch (e: Exception) {
            endProcessing(imageProxy)
            return
        }
        endProcessing(imageProxy)
    }

    private fun getBitmap(bitmap: Bitmap): Bitmap {
        originalWidth = bitmap.width

        val width = bitmap.height * CARD_DEFAULT_HEIGHT / CARD_DEFAULT_WIDTH

        return Bitmap.createBitmap(
            bitmap, 0, 0,
            width, bitmap.height
        )
    }

    // https://stackoverflow.com/questions/14861553/zxing-convert-bitmap-to-binarybitmap
    private fun getPatternFromBitmap(
        bitmap: Bitmap,
        getCode: Boolean = false
    ): FinderPatternInfo? {
        val intArray = IntArray(bitmap.width * bitmap.height)
        //copy pixel data from the Bitmap into the 'intArray' array
        bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source: LuminanceSource = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        return getPatternFromBinaryBitmap(binaryBitmap, getCode)
    }

    private fun getPatternFromBinaryBitmap(
        binaryBitmap: BinaryBitmap,
        getCode: Boolean = false
    ): FinderPatternInfo? {
        var pattern: FinderPatternInfo? = null
        try {
            pattern = QRCodeReader().getPatterns(binaryBitmap, null)
            if (pattern != null) {
                pattern.width = max(
                    (pattern.topRight.x - pattern.topLeft.x).toInt(),
                    (pattern.bottomRight.x - pattern.bottomLeft.x).toInt()
                )
                pattern.height = max(
                    (pattern.bottomLeft.y - pattern.topLeft.y).toInt(),
                    (pattern.bottomRight.y - pattern.topRight.y).toInt()
                )

                if (getCode) {
//                    val datamatrix = Bitmap.createBitmap(
//                        bitmap,
//                        pattern.topLeft.x.toInt() + (pattern.width / 5),
//                        0,
//                        (pattern.width / 1.7).toInt(),
//                        (pattern.height / 2.8).toInt()
//                    )

                    val data: Result = dataMatrixReader.decode(
                        binaryBitmap.crop(
                            pattern.topLeft.x.toInt() + (pattern.width / 5),
                            0,
                            (pattern.width / 1.7).toInt(),
                            (pattern.height / 2.8).toInt()
                        ), null
                    )
                    pattern.testId = DataHelper.checkHyphens(data.text.uppercase())
                }
            }
        } catch (e: Exception) {
            return pattern
        }
        return pattern
    }

    private fun processBitmap(imageProxy: ImageProxy) {
        pattern = getPatternFromBitmap(bitmap)
        if (pattern != null) {

            if (!ignoreWarnings) {
                pattern?.apply {
                    // Check if camera is too far
                    if (width < originalWidth * 0.27) {
                        sendMessage(context.getString(R.string.closer))
                        return
                    }
                }
            }

            bitmap = pattern?.run {
                val x = max(0, min(topLeft.x, bottomLeft.x).toInt() - (width * .11).toInt())
                val y = max(0, min(topLeft.y, topRight.y).toInt() - (height * .11).toInt())
                val w = min(bitmap.width - x, width + (width * .23).toInt())
                val h = min(bitmap.height - y, height + (height * .23).toInt())
                Bitmap.createBitmap(bitmap, x, y, w, h)
            }!!

            pattern = getPatternFromBitmap(bitmap, true)
            pattern?.apply {
                if (pattern != null) {

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
                        return
                    }
                    if (averageBrightness1 > getMaximumBrightness()) {
                        sendMessage(context.getString(R.string.too_bright))
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
                        return
                    }
                    if (averageBrightness2 > getMaximumBrightness()) {
                        sendMessage(context.getString(R.string.too_bright))
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
                        return
                    }
                    if (averageBrightness3 > getMaximumBrightness()) {
                        sendMessage(context.getString(R.string.too_bright))
                        return
                    }

                    // Check for shadows on card
                    val shadowTolerance = getShadowTolerance()
                    if (abs(averageBrightness1 - averageBrightness2) > shadowTolerance ||
                        abs(averageBrightness2 - averageBrightness3) > shadowTolerance ||
                        abs(averageBrightness1 - averageBrightness3) > shadowTolerance
                    ) {
                        sendMessage(context.getString(R.string.too_many_shadows))
                        return
                    }

                    val allowedTilt =
                        max(13.0, Constants.MAX_TILT_PERCENTAGE_ALLOWED * imageProxy.height)
                    // Check if image is tilted
                    if (abs(topLeft.x - bottomLeft.x) > allowedTilt ||
                        abs(topLeft.y - topRight.y) > allowedTilt ||
                        abs(topRight.x - bottomRight.x) > allowedTilt ||
                        abs(bottomLeft.y - bottomRight.y) > allowedTilt
                    ) {
                        val forward = (bottomRight.y - topRight.y) - (bottomLeft.y - topLeft.y)
                        val backward = (bottomLeft.y - topLeft.y) - (bottomRight.y - topRight.y)
                        val leftBackward =
                            (topRight.x - topLeft.x) - (bottomRight.x - bottomLeft.x)
                        val leftForward =
                            (bottomRight.x - bottomLeft.x) - (topRight.x - topLeft.x)

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

                        return
                    }

                    if (!ignoreWarnings) {
                        // Check if camera is too close
                        if (topLeft.x < bitmap.width * 0.0789 ||
                            bottomLeft.x < bitmap.width * 0.0789 ||
                            bottomRight.y > bitmap.height * 0.96 ||
                            topRight.y < bitmap.height * 0.050
                        ) {
                            sendMessage(context.getString(R.string.too_close))
                            return
                        }
                    }

                    if (testInfo.uuid.isEmpty()) {
                        testInfo = DataHelper.getTestInfo(testId, context) ?: TestInfo()
                    }
                    // if requested test id does not match the card test id
                    if ((testId.isEmpty() ||
                                testInfo.uuid.uppercase(Locale.ROOT) != testId.uppercase(Locale.ROOT))
                    ) {
                        sendMessage(context.getString(R.string.wrong_card))
                        return
                    }
                    if (testInfo.uuid.substring(1, 2).uppercase() != "C"
                    ) {
                        sendMessage(context.getString(R.string.wrong_card))
                        return
                    }

                    if (isDiagnosticMode()) {
                        Utilities.savePicture(
                            context.applicationContext,
                            testInfo.fileName,
                            testInfo.name!!,
                            Utilities.bitmapToBytes(imageProxy.toBitmap()), "_photo"
                        )
                    }

                    savePhoto(bitmap, testInfo)

                    croppedBitmap = perspectiveTransform(
                        bitmap, pattern!!, swatchWidthPercentage, swatchHeightPercentage
                    )
                    bitmap.recycle()

                    if (croppedBitmap != null && testInfo.subTest().resultInfo.result > -2) {
                        ImageColorUtil.getResult(
                            context,
                            testInfo,
                            ErrorType.NO_ERROR,
                            croppedBitmap,
                            colorCardType
                        )
                        croppedBitmap?.recycle()

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
                        sendMessage(context.getString(R.string.scanning))
                        return
                    }
                } else {
                    sendMessage(context.getString(R.string.scanning))
                    return
                }
            }
        } else {
            sendMessage(context.getString(R.string.scanning))
            return
        }
    }

    private fun endProcessing(imageProxy: ImageProxy) {
        processing = false
        croppedBitmap?.recycle()
        if (::bitmap.isInitialized) {
            bitmap.recycle()
        }
        CoroutineScope(Dispatchers.IO).launch {
            delay(1000 - (System.currentTimeMillis() - currentTimestamp))
            imageProxy.close()
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

    //https://stackoverflow.com/questions/13161628/cropping-a-perspective-transformation-of-image-on-android
    private fun perspectiveTransform(
        bitmap: Bitmap, pattern: FinderPatternInfo,
        widthPercentage: Double, heightPercentage: Double
    ): Bitmap? {
        val matrix = Matrix()
        val width = max(
            pattern.topRight.x - pattern.topLeft.x,
            pattern.bottomRight.x - pattern.bottomLeft.x
        )
//        val height = (width * 58) / 40
        val height = max(
            pattern.bottomLeft.y - pattern.topLeft.y,
            pattern.bottomRight.y - pattern.topRight.y
        )
        val dst = floatArrayOf(
            0f, 0f,
            width, 0f,
            width,
            height, 0f,
            height
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

        val mappedTR = floatArrayOf(bitmap.width.toFloat(), 0f)
        matrix.mapPoints(mappedTR)

        val mappedLL = floatArrayOf(0f, bitmap.height.toFloat())
        matrix.mapPoints(mappedLL)

        val correctedBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val p = getPatternFromBitmap(correctedBitmap)

        return if (p != null) {
            val finalBitmap = Bitmap.createBitmap(
                correctedBitmap,
                p.topLeft.x.toInt(),
                p.topLeft.y.toInt(),
                (p.topRight.x - p.topLeft.x).toInt(),
                (p.bottomRight.y - p.topRight.y).toInt(),
                null,
                true
            )
            correctedBitmap.recycle()

            val shiftX = (finalBitmap.width * widthPercentage).toInt()
            val shiftY = if (heightPercentage > -1) {
                (finalBitmap.height * heightPercentage).toInt()
            } else {
                shiftX
            }
            val centerX = finalBitmap.width / 2
            val centerY = finalBitmap.height / 2
            Bitmap.createBitmap(
                finalBitmap,
                centerX - shiftX,
                centerY - shiftY,
                shiftX * 2,
                shiftY * 2,
                null,
                true
            )
        } else {
            null
        }
    }

    fun reset() {
        done = false
        processing = false
        ignoreWarnings = false
        autoFocusCounter = 0
        autoFocusCounter2 = 0
    }
}
