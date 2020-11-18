package io.ffem.lite.camera

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
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
import io.ffem.lite.zxing.PlanarYUVLuminanceSource
import io.ffem.lite.zxing.RGBLuminanceSource
import io.ffem.lite.zxing.common.HybridBinarizer
import io.ffem.lite.zxing.datamatrix.decoder.DataMatrixReader
import io.ffem.lite.zxing.datamatrix.decoder.SpecificAreaReader
import io.ffem.lite.zxing.qrcode.QRCodeReader
import io.ffem.lite.zxing.qrcode.detector.FinderPatternInfo
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.roundToInt

const val MAX_TILT_ALLOWED = 10

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

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }

    override fun analyze(imageProxy: ImageProxy) {
        if (done || processing) {
            return
        }
        processing = true

        localBroadcastManager = LocalBroadcastManager.getInstance(context)

        try {

            val data = imageProxy.planes[0].buffer.toByteArray()
            val source: PlanarYUVLuminanceSource? =
                buildLuminanceSource(data, imageProxy.width, imageProxy.height)

            val binaryBitmap = BinaryBitmap(
                HybridBinarizer(
                    source
                )
            )

            pattern = getPatternFromBitmap(imageProxy.toBitmap())
//            pattern = getPattern(binaryBitmap)
            if (pattern != null) {

                sendOverlayUpdate()

                pattern?.apply {

                    // Check if camera is too far
                    if (topLeft.x < binaryBitmap.width * 0.04 ||
                        bottomRight.y > binaryBitmap.height * 0.91 ||
                        topRight.y < binaryBitmap.height * 0.12

                    ) {
                        sendMessage(context.getString(R.string.too_close))
                        endProcessing(imageProxy, true)
                        return
                    }

                    // Check if camera is too far
                    if (topLeft.x > binaryBitmap.width * 0.08 ||
                        bottomRight.y < binaryBitmap.height * 0.75 ||
                        topRight.y > binaryBitmap.height * 0.2

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

        val top = shiftY + (bitmap.height * 0.21).toInt()
        val height = bitmap.height * 0.58

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

    private fun buildLuminanceSource(
        data: ByteArray?,
        width: Int,
        height: Int
    ): PlanarYUVLuminanceSource? {
        val rect = Rect(0, 0, width, height)
        // Go ahead and assume it's YUV rather than die.
        return PlanarYUVLuminanceSource(
            data, width, height, rect.left, rect.top,
            rect.width(), rect.height(), false
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

    private fun getPatternFromBitmap(bMap: Bitmap): FinderPatternInfo? {
        val intArray = IntArray(bMap.width * bMap.height)
        //copy pixel data from the Bitmap into the 'intArray' array
        bMap.getPixels(intArray, 0, bMap.width, 0, 0, bMap.width, bMap.height)
        val source: LuminanceSource = RGBLuminanceSource(bMap.width, bMap.height, intArray)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        return getPattern(bitmap)
    }

    private fun getPattern(
        bitmap: BinaryBitmap
    ): FinderPatternInfo? {
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
