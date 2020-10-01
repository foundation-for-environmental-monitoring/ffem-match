package io.ffem.lite.camera

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.media.Image
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.Barcode.FORMAT_CODE_128
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.app.App.Companion.DEFAULT_TEST_UUID
import io.ffem.lite.app.App.Companion.TEST_INFO_KEY
import io.ffem.lite.app.App.Companion.getTestInfo
import io.ffem.lite.app.App.Companion.getTestName
import io.ffem.lite.model.ImageEdgeType
import io.ffem.lite.model.TestInfo
import io.ffem.lite.preference.getSampleTestImageNumberInt
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.util.ColorUtil.fixBoundary
import io.ffem.lite.util.ColorUtil.isBarcodeValid
import io.ffem.lite.util.ColorUtil.isTilted
import io.ffem.lite.util.getBitmapPixels
import io.ffem.lite.util.isNotBright
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.math.max
import kotlin.math.min


const val MAX_ANGLE = 14

class BarcodeAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {

    companion object {
        private var capturePhoto: Boolean = false
        private var processing = false
        private var done: Boolean = false
        var autoFocusCounter = 0
    }

    private lateinit var bitmap: Bitmap
    private lateinit var localBroadcastManager: LocalBroadcastManager

    private val detector: BarcodeScanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                FORMAT_CODE_128
            ).build()
        BarcodeScanning.getClient(options)
    }

    override fun analyze(image: ImageProxy) {
        if (done || processing) {
            return
        }
        processing = true

        @ExperimentalGetImage
        val imageProxy = image.image

        localBroadcastManager = LocalBroadcastManager.getInstance(context)

        if (BuildConfig.DEBUG && (isDiagnosticMode() || BuildConfig.INSTRUMENTED_TEST_RUNNING.get())) {
            val imageNumber = getSampleTestImageNumberInt()
            if (imageNumber > -1) {
                try {
                    val drawable = ContextCompat.getDrawable(
                        context, context.resources.getIdentifier(
                            "test_${java.lang.String.format(Locale.ROOT, "%03d", imageNumber)}",
                            "drawable", context.packageName
                        )
                    )
                    bitmap = (drawable as BitmapDrawable).bitmap
                } catch (ex: Exception) {
                    sendMessage(context.getString(R.string.sample_image_not_found))
                    endProcessing(image, true)
                    return
                }
            } else {
                @ExperimentalGetImage
                bitmap = imageProxy!!.toBitmap()
            }
        } else {
            @ExperimentalGetImage
            bitmap = imageProxy!!.toBitmap()
        }

        bitmap = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width / 2,
            bitmap.height
        )

        if (capturePhoto) {
            done = true
            val testInfo = getTestInfo(DEFAULT_TEST_UUID)!!
            savePhoto(bitmap, testInfo)
            endProcessing(image, true)
            return
        }

        var badLighting = false

        var rect = Rect(100, 0, bitmap.width - 100, 10)
        var pixels = getBitmapPixels(bitmap, rect)
        if (isNotBright(pixels)) {
            endProcessing(image, true)
            return
        }

        rect = Rect(100, bitmap.height - 10, bitmap.width - 100, bitmap.height)
        pixels = getBitmapPixels(bitmap, rect)
        if (isNotBright(pixels)) {
            endProcessing(image, true)
            return
        }

        val leftBarcodeBitmap = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height / 2
        )

        detector.process(InputImage.fromBitmap(leftBarcodeBitmap, 0))
            .addOnFailureListener(
                fun(_: Exception) {
                    sendMessage(context.getString(R.string.color_card_not_found))
                    endProcessing(image, true)
                    return
                }
            )
            .addOnSuccessListener(
                fun(result: List<Barcode>) {
                    if (result.isEmpty()) {
                        sendMessage(context.getString(R.string.color_card_not_found))
                        endProcessing(image, true)
                        return
                    }
                    for (leftBarcode in result) {
                        if (!leftBarcode.rawValue.isNullOrEmpty()) {
                            var testName = getTestName(result[0].displayValue!!)
                            if (testName.isEmpty()) {
                                sendMessage(context.getString(R.string.invalid_barcode))
                                endProcessing(image, true)
                                return
                            }

                            if (autoFocusCounter < 10) {
                                autoFocusCounter++
                                endProcessing(image, false)
                                return
                            }

                            try {
                                val leftBoundingBox =
                                    fixBoundary(
                                        leftBarcode,
                                        leftBarcodeBitmap,
                                        ImageEdgeType.WhiteTop
                                    )

                                if (leftBoundingBox.top in 11..80) {
                                    if (!isBarcodeValid(
                                            leftBarcodeBitmap,
                                            leftBoundingBox,
                                            ImageEdgeType.WhiteTop
                                        )
                                    ) {
                                        badLighting = true
                                        leftBarcodeBitmap.recycle()
                                        endProcessing(image, false)
                                        return
                                    }

                                    leftBarcodeBitmap.recycle()

                                    val rightBarcodeBitmap = Bitmap.createBitmap(
                                        bitmap, 0, bitmap.height / 2,
                                        bitmap.width, bitmap.height / 2
                                    )

                                    detector.process(
                                        InputImage.fromBitmap(rightBarcodeBitmap, 0)
                                    )
                                        .addOnFailureListener(fun(_: Exception) {
                                            endProcessing(image, true)
                                            return
                                        })
                                        .addOnSuccessListener(
                                            fun(result: List<Barcode>) {
                                                if (result.isNullOrEmpty()) {
                                                    endProcessing(image, true)
                                                    return
                                                }

                                                for (rightBarcode in result) {

                                                    val rightBoundingBox =
                                                        fixBoundary(
                                                            rightBarcode,
                                                            rightBarcodeBitmap,
                                                            ImageEdgeType.WhiteDown
                                                        )

                                                    if (rightBarcodeBitmap.height - rightBoundingBox.bottom !in 11..80) {
                                                        rightBarcodeBitmap.recycle()
                                                        endProcessing(image, false)
                                                        return
                                                    }

                                                    if (isTilted(
                                                            leftBoundingBox, rightBoundingBox
                                                        )
                                                    ) {
                                                        sendMessage(context.getString(R.string.correct_camera_tilt))
                                                        endProcessing(image, false)
                                                        return
                                                    }

                                                    testName =
                                                        getTestName(result[0].displayValue!!)
                                                    if (testName.isEmpty()) {
                                                        sendMessage(context.getString(R.string.invalid_barcode))
                                                        endProcessing(image, false)
                                                        return
                                                    }

                                                    if (badLighting || !isBarcodeValid(
                                                            rightBarcodeBitmap,
                                                            rightBoundingBox,
                                                            ImageEdgeType.WhiteDown
                                                        )
                                                    ) {
                                                        sendMessage(context.getString(R.string.try_moving_well_lit))
                                                        rightBarcodeBitmap.recycle()
                                                        endProcessing(image, false)
                                                        return
                                                    }

                                                    rightBarcodeBitmap.recycle()

                                                    analyzeBarcode(
                                                        image,
                                                        bitmap,
                                                        rightBarcode,
                                                        rightBoundingBox,
                                                        leftBoundingBox
                                                    )
                                                }
                                            }
                                        )
                                } else {
                                    sendMessage(context.getString(R.string.color_card_not_found))
                                    endProcessing(image, true)
                                }
                            } catch (ignored: Exception) {
                                endProcessing(image, true)
                            }
                        } else {
                            endProcessing(image, true)
                        }
                    }
                }
            )
    }

    private fun Image.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer // Y
        val uBuffer = planes[1].buffer // U
        val vBuffer = planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun endProcessing(image: ImageProxy, reset: Boolean) {
        if (::bitmap.isInitialized) {
            bitmap.recycle()
        }
        processing = false
        if (reset) {
            autoFocusCounter = 0
        }
        image.close()
    }

    private fun analyzeBarcode(
        image: ImageProxy,
        bitmap: Bitmap, rightBarcode: Barcode,
        rightBoundingBox: Rect, leftBoundingBox: Rect
    ) {
        if (!rightBarcode.rawValue.isNullOrEmpty()) {
            val testInfo = getTestInfo(rightBarcode.displayValue!!)
            if (testInfo == null) {
                sendMessage(context.getString(R.string.invalid_barcode))
                endProcessing(image, false)
                return
            }

            done = true

            val cropLeft = max(leftBoundingBox.left - 20, 0)
            val cropWidth = min(
                leftBoundingBox.right - cropLeft + 40,
                bitmap.width - cropLeft
            )
            val cropTop = max(leftBoundingBox.top - 40, 0)
            val cropHeight = min(
                rightBoundingBox.bottom - leftBoundingBox.top + (bitmap.height / 2) + 80,
                bitmap.height - cropTop
            )

            val finalBitmap = Bitmap.createBitmap(
                bitmap, cropLeft, cropTop, cropWidth, cropHeight
            )

            savePhoto(finalBitmap, testInfo)

            finalBitmap.recycle()

            endProcessing(image, true)

        } else {
            endProcessing(image, true)
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

        val intent = Intent(App.CAPTURED_EVENT)
        intent.putExtra(TEST_INFO_KEY, testInfo)
        localBroadcastManager.sendBroadcast(
            intent
        )
    }

    private fun sendMessage(s: String) {
        val intent = Intent(App.ERROR_EVENT)
        intent.putExtra(App.ERROR_MESSAGE, s)
        localBroadcastManager.sendBroadcast(
            intent
        )
    }

    fun takePhoto() {
        capturePhoto = true
    }

    fun reset() {
        done = false
        processing = false
        capturePhoto = false
        autoFocusCounter = 0
    }
}
