package io.ffem.lite.camera

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.common.CARD_CAPTURED_EVENT_BROADCAST
import io.ffem.lite.common.OVERLAY_UPDATE_BROADCAST
import io.ffem.lite.common.TEST_INFO_KEY
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.TestInfo
import io.ffem.lite.util.ImageColorUtil
import io.ffem.lite.util.ImageUtil.toBitmap
import io.ffem.lite.zxing.BinaryBitmap
import io.ffem.lite.zxing.LuminanceSource
import io.ffem.lite.zxing.RGBLuminanceSource
import io.ffem.lite.zxing.common.HybridBinarizer
import io.ffem.lite.zxing.datamatrix.decoder.DataMatrixReader
import io.ffem.lite.zxing.datamatrix.decoder.SpecificAreaReader
import io.ffem.lite.zxing.qrcode.QRCodeReader
import io.ffem.lite.zxing.qrcode.detector.FinderPatternInfo
import kotlin.math.max
import kotlin.math.roundToInt

const val MAX_TILT_ALLOWED = 12

class ColorCardAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {

    companion object {
        private var capturePhoto: Boolean = false
        private var processing = false
        private var done: Boolean = false
        var autoFocusCounter = 0
        var autoFocusCounter2 = 0
        var pattern: FinderPatternInfo? = null
    }

    private lateinit var localBroadcastManager: LocalBroadcastManager

    fun getPattern(): FinderPatternInfo? {
        return pattern
    }

    override fun analyze(imageProxy: ImageProxy) {
        if (done || processing) {
            return
        }
        processing = true

        localBroadcastManager = LocalBroadcastManager.getInstance(context)

        try {

            pattern = getPatternFromBitmap(imageProxy.toBitmap())
            if (pattern != null) {

                sendOverlayUpdate()

                pattern?.apply {

                    // Check if camera is too far
                    if (topLeft.x < imageProxy.width * 0.04 ||
                        bottomRight.y > imageProxy.height * 0.95 ||
                        topRight.y < imageProxy.height * 0.035
                    ) {
                        sendMessage(context.getString(R.string.too_close))
                        endProcessing(imageProxy, true)
                        return
                    }

                    // Check if camera is too far
                    if (topLeft.x > imageProxy.width * 0.1 ||
                        bottomRight.y < imageProxy.height * 0.88 ||
                        topRight.y > imageProxy.height * 0.2

                    ) {
                        sendMessage(context.getString(R.string.closer))
                        endProcessing(imageProxy, true)
                        return
                    }

                    // Check if image is tilted
                    if (topLeft.x - bottomLeft.x > MAX_TILT_ALLOWED ||
                        topLeft.y - topRight.y > MAX_TILT_ALLOWED ||
                        topRight.x - bottomRight.x > MAX_TILT_ALLOWED ||
                        bottomLeft.y - bottomRight.y > MAX_TILT_ALLOWED
                    ) {
                        sendMessage(context.getString(R.string.correct_camera_tilt))
                        endProcessing(imageProxy, true)
                        return
                    }

//                    if (autoFocusCounter < 10) {
//                        autoFocusCounter++
//                        endProcessing(imageProxy, false)
//                        return
//                    }

                    val testInfo = App.getTestInfo(testId)

                    val bitmap = getBitmap(imageProxy)
                    savePhoto(bitmap, testInfo!!)

                    val croppedBitmap = perspectiveTransform(bitmap, pattern!!)

                    ImageColorUtil.getResult(context, testInfo, ErrorType.NO_ERROR, croppedBitmap)
                    croppedBitmap.recycle()
                    bitmap.recycle()

                    if (testInfo.resultInfo.result > -1) {
                        done = true
                        val intent = Intent(CARD_CAPTURED_EVENT_BROADCAST)
                        intent.putExtra(TEST_INFO_KEY, testInfo)
                        localBroadcastManager.sendBroadcast(
                            intent
                        )
                    } else {
                        endProcessing(imageProxy, true)
                        sendMessage(context.getString(R.string.color_card_not_found))
                        sendOverlayUpdate()
                    }
                }
            } else {
                sendMessage(context.getString(R.string.color_card_not_found))
                sendOverlayUpdate()
                endProcessing(imageProxy, true)
            }
        } catch (e: Exception) {
            endProcessing(imageProxy, true)
            return
        }
        endProcessing(imageProxy, true)
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

        val top = shiftY + (bitmap.height * 0.24).toInt()
        val height = bitmap.height * 0.53

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

    private fun getBitmap(image: ImageProxy): Bitmap {
        val bitmap = image.toBitmap()
        return Bitmap.createBitmap(
            bitmap, 0, 0,
            (bitmap.width * 0.45).toInt(),
            bitmap.height
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
            val dataResult = SpecificAreaReader(DataMatrixReader()).decode(bitmap)
            if (!dataResult.text.isNullOrEmpty()) {
                result = QRCodeReader().getPatterns(bitmap, null)
                result.testId = dataResult.text
                result.width = bitmap.width
                result.height = bitmap.height
            }
        } catch (e: Exception) {
            return null
        }
        return result
    }

    private fun endProcessing(imageProxy: ImageProxy, reset: Boolean) {
        processing = false
        if (reset) {
            autoFocusCounter = 0
            autoFocusCounter2 = 0
        } else {
            sendMessage("")
        }
        imageProxy.close()
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
        val intent = Intent(App.ERROR_EVENT)
        intent.putExtra(App.ERROR_MESSAGE, s)
        intent.putExtra(
            App.SCAN_PROGRESS,
            autoFocusCounter + autoFocusCounter2
        )
        localBroadcastManager.sendBroadcast(
            intent
        )
    }

    private fun sendOverlayUpdate() {
        val intent = Intent(OVERLAY_UPDATE_BROADCAST)
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
