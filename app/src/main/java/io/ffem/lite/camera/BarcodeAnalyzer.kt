package io.ffem.lite.camera

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.provider.MediaStore
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.app.App.Companion.getTestName
import io.ffem.lite.model.ImageEdgeType
import io.ffem.lite.util.ColorUtil.fixBoundary
import io.ffem.lite.util.ColorUtil.isBarcodeValid
import io.ffem.lite.util.ColorUtil.isTilted
import io.ffem.lite.util.PreferencesUtil
import java.util.*
import kotlin.math.max
import kotlin.math.min

const val MAX_ANGLE = 12

class BarcodeAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {

    companion object {
        private var capturePhoto: Boolean = false
        private var processing = false
        private var done: Boolean = false
    }

    private lateinit var bitmap: Bitmap
    private lateinit var localBroadcastManager: LocalBroadcastManager

    private val detector: FirebaseVisionBarcodeDetector by lazy {
        val options = FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(
                FirebaseVisionBarcode.FORMAT_CODE_128
            )
            .build()
        FirebaseVision.getInstance().getVisionBarcodeDetector(options)
    }

    private var taskLeftBarcode: Task<out Any>? = null
    private lateinit var mediaImage: FirebaseVisionImage

    override fun analyze(image: ImageProxy) {
        if (done || processing) {
            return
        }
        processing = true

        mediaImage = FirebaseVisionImage.fromMediaImage(
            image.image!!, FirebaseVisionImageMetadata.ROTATION_180
        )

        localBroadcastManager = LocalBroadcastManager.getInstance(context)

        try {
            @Suppress("ConstantConditionIf")
            bitmap = if (BuildConfig.TEST_RUNNING) {
                val expectedValue = (PreferencesUtil
                    .getString(context, R.string.expectedValueKey, "").toFloat().toInt())

                val drawable = ContextCompat.getDrawable(
                    context, context.resources.getIdentifier(
                        "test$expectedValue",
                        "drawable", context.packageName
                    )
                )

                (drawable as BitmapDrawable).bitmap
            } else {
                mediaImage.bitmap
            }
        } catch (ex: Exception) {
            sendMessage(context.getString(R.string.place_color_card))
            endProcessing(image)
            return
        }

        bitmap = Bitmap.createBitmap(
            bitmap, bitmap.width / 2, 0,
            bitmap.width / 2,
            bitmap.height
        )

        if (capturePhoto) {
            done = true
//            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height)
            savePhoto(bitmap, "Unknown", true)
            endProcessing(image)
            return
        }

        var badLighting = false

        val leftBarcodeBitmap = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height / 2
        )

        taskLeftBarcode =
            detector.detectInImage(FirebaseVisionImage.fromBitmap(leftBarcodeBitmap))
                .addOnFailureListener(
                    fun(_: Exception) {
                        sendMessage(context.getString(R.string.color_card_not_found))
                        endProcessing(image)
                        return
                    }
                )
                .addOnSuccessListener(
                    fun(result: List<FirebaseVisionBarcode>) {
                        if (result.isEmpty()) {
                            sendMessage(context.getString(R.string.color_card_not_found))
                            endProcessing(image)
                            return
                        }
                        for (leftBarcode in result) {
                            if (!leftBarcode.rawValue.isNullOrEmpty()) {
                                var testName = getTestName(result[0].displayValue!!)
                                if (testName.isEmpty()) {
                                    sendMessage(context.getString(R.string.invalid_barcode))
                                    endProcessing(image)
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
                                        }

                                        val rightBarcodeBitmap = Bitmap.createBitmap(
                                            bitmap, 0, bitmap.height / 2,
                                            bitmap.width, bitmap.height / 2
                                        )

                                        detector.detectInImage(
                                            FirebaseVisionImage.fromBitmap(rightBarcodeBitmap)
                                        )
                                            .addOnFailureListener(fun(_: Exception) {
//                                                sendMessage(context.getString(R.string.color_card_not_found) + "...")
                                                endProcessing(image)
                                                return
                                            })
                                            .addOnSuccessListener(
                                                fun(result: List<FirebaseVisionBarcode>) {
                                                    if (result.isNullOrEmpty()) {
//                                                        sendMessage(context.getString(R.string.color_card_not_found) + "....")
                                                        endProcessing(image)
                                                        return
                                                    }

                                                    for (rightBarcode in result) {

                                                        val rightBoundingBox =
                                                            fixBoundary(
                                                                rightBarcode,
                                                                rightBarcodeBitmap,
                                                                ImageEdgeType.WhiteDown
                                                            )

                                                        if (isTilted(
                                                                leftBoundingBox, rightBoundingBox
                                                            )
                                                        ) {
                                                            sendMessage(context.getString(R.string.correct_camera_tilt))
                                                            endProcessing(image)
                                                            return
                                                        }

                                                        testName =
                                                            getTestName(result[0].displayValue!!)
                                                        if (testName.isEmpty()) {
                                                            sendMessage(context.getString(R.string.invalid_barcode))
                                                            endProcessing(image)
                                                            return
                                                        }

                                                        if (badLighting || !isBarcodeValid(
                                                                rightBarcodeBitmap,
                                                                rightBoundingBox,
                                                                ImageEdgeType.WhiteDown
                                                            )
                                                        ) {
                                                            sendMessage(context.getString(R.string.try_moving_well_lit))
                                                            endProcessing(image)
                                                            return
                                                        }

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
                                        endProcessing(image)
                                    }
                                } catch (ignored: Exception) {
                                    endProcessing(image)
                                }
                            } else {
                                endProcessing(image)
                            }
                        }
                    }
                )
    }

    private fun endProcessing(image: ImageProxy) {
        if (::bitmap.isInitialized) {
            bitmap.recycle()
        }
        processing = false
        image.close()
    }

    private fun analyzeBarcode(
        image: ImageProxy,
        bitmap: Bitmap, rightBarcode: FirebaseVisionBarcode,
        rightBoundingBox: Rect, leftBoundingBox: Rect
    ) {
        if ((bitmap.height / 2) - rightBoundingBox.bottom in 11..80) {
            if (!rightBarcode.rawValue.isNullOrEmpty()) {
                val testName = getTestName(rightBarcode.displayValue!!)
                if (testName.isEmpty()) {
                    sendMessage(context.getString(R.string.invalid_barcode))
                    endProcessing(image)
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

                savePhoto(finalBitmap, testName, false)

                endProcessing(image)

            } else {
                endProcessing(image)
            }
        } else {
            endProcessing(image)
        }
    }

    private fun savePhoto(bitmap: Bitmap, testName: String, saveCopy: Boolean) {

        val bitmapRotated = Utilities.rotateImage(bitmap, 270)

        val testId = UUID.randomUUID().toString()
        val filePath = Utilities.savePicture(
            context.applicationContext, testId,
            testName, Utilities.bitmapToBytes(bitmapRotated)
        )

        if (saveCopy) {
            saveBitmap(context, bitmap)
        }

        bitmapRotated.recycle()

        val intent = Intent(App.CAPTURED_EVENT)
        intent.putExtra(App.FILE_PATH_KEY, filePath)
        intent.putExtra(App.TEST_ID_KEY, testId)
        intent.putExtra(App.TEST_NAME_KEY, testName)
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

    private fun saveBitmap(
        context: Context, bitmap: Bitmap
    ) {
        try {
            val filename = "lite_" + System.currentTimeMillis()
            MediaStore.Images.Media.insertImage(
                context.contentResolver, bitmap, filename, "ffem Lite"
            )
        } catch (ignored: Exception) {
        }
    }

    fun reset() {
        done = false
        processing = false
        capturePhoto = false
    }
}
