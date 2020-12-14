package io.ffem.lite.camera

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
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
import io.ffem.lite.app.App.Companion.getTestInfo
import io.ffem.lite.app.App.Companion.getTestName
import io.ffem.lite.common.*
import io.ffem.lite.model.ImageEdgeType
import io.ffem.lite.model.TestInfo
import io.ffem.lite.preference.getSampleTestImageNumberInt
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.preference.manualCaptureOnly
import io.ffem.lite.util.BarcodeColorUtil.fixBoundary
import io.ffem.lite.util.BarcodeColorUtil.isBarcodeTilted
import io.ffem.lite.util.BarcodeColorUtil.isBarcodeValid
import io.ffem.lite.util.BarcodeColorUtil.isTilted
import io.ffem.lite.util.ImageUtil.resizeBitmap
import io.ffem.lite.util.ImageUtil.toBitmap
import io.ffem.lite.util.getBitmapPixels
import io.ffem.lite.util.isNotBright
import java.util.*


const val MAX_ANGLE = 14

@Deprecated("Use QR color card instead")
class BarcodeAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {

    companion object {
        private var capturePhoto: Boolean = false
        private var processing = false
        private var done: Boolean = false
        var autoFocusCounter = 0
        var autoFocusCounter2 = 0
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

    override fun analyze(imageProxy: ImageProxy) {
        if (done || processing) {
            return
        }
        processing = true

        localBroadcastManager = LocalBroadcastManager.getInstance(context)

        try {
            if (capturePhoto) {
                done = true
                bitmap = getBitmap(imageProxy)
                val testInfo = getTestInfo(DEFAULT_TEST_UUID)!!
                bitmap = resizeBitmap(bitmap)
                savePhoto(bitmap, testInfo)
                bitmap.recycle()
                imageProxy.close()
                return
            } else {
                if (manualCaptureOnly()) {
                    processing = false
                    imageProxy.close()
                    return
                }
                bitmap = getBitmap(imageProxy)
            }
        } catch (e: Exception) {
            return
        }

        if (manualCaptureOnly()) {
            endProcessing(imageProxy, true)
            return
        }

        var badLighting = false

        var rect = Rect(100, 0, bitmap.width - 100, 10)
        var pixels = getBitmapPixels(bitmap, rect)
        if (isNotBright(pixels)) {
            endProcessing(imageProxy, true)
            sendMessage(context.getString(R.string.color_card_not_found))
            return
        }

        rect = Rect(100, bitmap.height - 10, bitmap.width - 100, bitmap.height)
        pixels = getBitmapPixels(bitmap, rect)
        if (isNotBright(pixels)) {
            endProcessing(imageProxy, true)
            sendMessage(context.getString(R.string.color_card_not_found))
            return
        }

        val barcodeHeight = ((bitmap.height / 2) - (.20 * bitmap.height / 2)).toInt()

        val rightBarcodeBitmap = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, barcodeHeight
        )

        detector.process(InputImage.fromBitmap(rightBarcodeBitmap, 0))
            .addOnFailureListener(
                fun(_: Exception) {
                    rightBarcodeBitmap.recycle()
                    endProcessing(imageProxy, true)
                    sendMessage(context.getString(R.string.color_card_not_found))
                    return
                }
            )
            .addOnSuccessListener(
                fun(result: List<Barcode>) {
                    if (result.isEmpty()) {
                        rightBarcodeBitmap.recycle()
                        endProcessing(imageProxy, true)
                        sendMessage(context.getString(R.string.color_card_not_found))
                        return
                    }
                    if (result.isNotEmpty()) {
                        val rightBarcode = result[0]
                        if (!rightBarcode.rawValue.isNullOrEmpty()) {
                            var testName = getTestName(result[0].displayValue!!)
                            if (testName.isEmpty()) {
                                rightBarcodeBitmap.recycle()
                                endProcessing(imageProxy, true)
                                sendMessage(context.getString(R.string.invalid_barcode))
                                return
                            }

                            if (autoFocusCounter < 6) {
                                autoFocusCounter++
                                rightBarcodeBitmap.recycle()
                                endProcessing(imageProxy, false)
                                return
                            }

                            try {
                                if (isBarcodeTilted(rightBarcode.cornerPoints)
                                ) {
                                    rightBarcodeBitmap.recycle()
                                    endProcessing(imageProxy, true)
                                    sendMessage(context.getString(R.string.correct_camera_tilt))
                                    return
                                }

                                val rightBoundingBox =
                                    fixBoundary(
                                        rightBarcode,
                                        rightBarcodeBitmap,
                                        ImageEdgeType.WhiteTop
                                    )

                                if (rightBoundingBox.top in 7..80) {
                                    if (!isBarcodeValid(
                                            rightBarcodeBitmap,
                                            rightBoundingBox,
                                            ImageEdgeType.WhiteTop
                                        )
                                    ) {
                                        badLighting = true
                                        rightBarcodeBitmap.recycle()
                                        endProcessing(imageProxy, true)
                                        return
                                    }

                                    rightBarcodeBitmap.recycle()

                                    val leftBarcodeBitmap = Bitmap.createBitmap(
                                        bitmap, 0, bitmap.height - barcodeHeight,
                                        bitmap.width, barcodeHeight
                                    )

                                    detector.process(
                                        InputImage.fromBitmap(leftBarcodeBitmap, 0)
                                    )
                                        .addOnFailureListener(fun(_: Exception) {
                                            endProcessing(imageProxy, true)
                                            return
                                        })
                                        .addOnSuccessListener(
                                            fun(result: List<Barcode>) {
                                                if (result.isNullOrEmpty()) {
                                                    leftBarcodeBitmap.recycle()
                                                    endProcessing(imageProxy, false)
                                                    return
                                                }

                                                if (result.isNotEmpty()) {
                                                    val leftBarcode = result[0]
                                                    val leftBoundingBox =
                                                        fixBoundary(
                                                            leftBarcode,
                                                            leftBarcodeBitmap,
                                                            ImageEdgeType.WhiteDown
                                                        )

                                                    if (leftBarcodeBitmap.height - leftBoundingBox.bottom !in 11..122) {
                                                        leftBarcodeBitmap.recycle()
                                                        endProcessing(imageProxy, false)
                                                        return
                                                    }

                                                    if (isTilted(rightBoundingBox, leftBoundingBox)
                                                    ) {
                                                        leftBarcodeBitmap.recycle()
                                                        endProcessing(imageProxy, false)
                                                        sendMessage(context.getString(R.string.correct_camera_tilt))
                                                        return
                                                    }

                                                    testName = getTestName(result[0].displayValue!!)
                                                    if (testName.isEmpty()) {
                                                        leftBarcodeBitmap.recycle()
                                                        endProcessing(imageProxy, false)
                                                        sendMessage(context.getString(R.string.invalid_barcode))
                                                        return
                                                    }

                                                    if (badLighting || !isBarcodeValid(
                                                            leftBarcodeBitmap,
                                                            leftBoundingBox,
                                                            ImageEdgeType.WhiteDown
                                                        )
                                                    ) {
                                                        leftBarcodeBitmap.recycle()
                                                        endProcessing(imageProxy, false)
                                                        sendMessage(context.getString(R.string.try_moving_well_lit))
                                                        return
                                                    }

                                                    leftBarcodeBitmap.recycle()

                                                    if (autoFocusCounter2 < 4) {
                                                        autoFocusCounter2++
                                                        endProcessing(imageProxy, false)
                                                        return
                                                    }

                                                    analyzeBarcode(
                                                        imageProxy,
                                                        bitmap,
                                                        rightBarcode
                                                    )
                                                }
                                            }
                                        )
                                } else {
                                    endProcessing(imageProxy, true)
                                    sendMessage(context.getString(R.string.color_card_not_found))
                                }
                            } catch (ignored: Exception) {
                                endProcessing(imageProxy, true)
                            }
                        } else {
                            endProcessing(imageProxy, true)
                        }
                    }
                }
            )
    }

    private fun getBitmap(image: ImageProxy): Bitmap {
        val bitmap =
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
                        (drawable as BitmapDrawable).bitmap
                    } catch (ex: Exception) {
                        sendMessage(context.getString(R.string.sample_image_not_found))
                        endProcessing(image, true)
                        throw Exception()
                    }
                } else {
                    image.toBitmap()
                }
            } else {
                image.toBitmap()
            }

        return Bitmap.createBitmap(
            bitmap, 0, 0,
            (bitmap.width * 0.45).toInt(),
            bitmap.height
        )
    }

    private fun endProcessing(imageProxy: ImageProxy, reset: Boolean) {
        if (::bitmap.isInitialized) {
            bitmap.recycle()
        }
        processing = false
        if (reset) {
            autoFocusCounter = 0
            autoFocusCounter2 = 0
        } else {
            sendMessage("")
        }
        imageProxy.close()
    }

    private fun analyzeBarcode(
        image: ImageProxy,
        bitmap: Bitmap, LeftBarcode: Barcode
    ) {
        if (!LeftBarcode.rawValue.isNullOrEmpty()) {
            val testInfo = getTestInfo(LeftBarcode.displayValue!!)
            if (testInfo == null) {
                sendMessage(context.getString(R.string.invalid_barcode))
                endProcessing(image, false)
                return
            }

            done = true

//            val cropLeft = max(leftBoundingBox.left - 20, 0)
//            val cropWidth = min(
//                leftBoundingBox.right - cropLeft + 40,
//                bitmap.width - cropLeft
//            )
//            val cropTop = max(leftBoundingBox.top - 40, 0)
//            val cropHeight = min(
//                rightBoundingBox.bottom - leftBoundingBox.top + (bitmap.height / 2) + 80,
//                bitmap.height - cropTop
//            )

//            val finalBitmap = Bitmap.createBitmap(
//                bitmap, cropLeft, cropTop, cropWidth, cropHeight
//            )

            savePhoto(bitmap, testInfo)

            bitmap.recycle()

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

        val intent = Intent(CAPTURED_EVENT_BROADCAST)
        intent.putExtra(TEST_INFO_KEY, testInfo)
        localBroadcastManager.sendBroadcast(
            intent
        )
    }

    private fun sendMessage(s: String) {
        val intent = Intent(ERROR_EVENT_BROADCAST)
        intent.putExtra(ERROR_MESSAGE, s)
        intent.putExtra(SCAN_PROGRESS, autoFocusCounter + autoFocusCounter2)
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
        autoFocusCounter2 = 0
    }
}
