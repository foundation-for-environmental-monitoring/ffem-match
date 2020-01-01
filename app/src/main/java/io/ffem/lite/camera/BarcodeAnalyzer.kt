package io.ffem.lite.camera

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.provider.MediaStore
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.app.App.Companion.getTestName
import io.ffem.lite.util.ColorUtil.fixBoundary
import io.ffem.lite.util.ColorUtil.isBarcodeValid
import io.ffem.lite.util.ColorUtil.isTilted
import java.util.*
import kotlin.math.max


const val MAX_ANGLE = 16

class BarcodeAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {

    private lateinit var bitmap: Bitmap
    private var processing = false
//    private var processBlackAndWhite = false

    private var done: Boolean = false
    private var capturePhoto: Boolean = false
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

    override fun analyze(image: ImageProxy, rotationDegrees: Int) {
        if (done || processing) {
            return
        }
        processing = true

//        processBlackAndWhite = !processBlackAndWhite

        //YUV_420 is normally the input type here
        var rotation = rotationDegrees % 360
        if (rotation < 0) {
            rotation += 360
        }

        mediaImage = FirebaseVisionImage.fromMediaImage(
            image.image!!, when (rotation) {
                0 -> FirebaseVisionImageMetadata.ROTATION_0
                90 -> FirebaseVisionImageMetadata.ROTATION_90
                180 -> FirebaseVisionImageMetadata.ROTATION_180
                270 -> FirebaseVisionImageMetadata.ROTATION_270
                else -> {
                    FirebaseVisionImageMetadata.ROTATION_0
                }
            }
        )

        localBroadcastManager = LocalBroadcastManager.getInstance(context)

        bitmap = mediaImage.bitmap

        if (capturePhoto) {
            done = true

            bitmap = Bitmap.createBitmap(
                bitmap, (bitmap.width * 0.20).toInt(), 0,
                bitmap.width - (bitmap.width * 0.40).toInt(),
                bitmap.height
            )

            savePhoto(bitmap, "Unknown", true)
            return
        }

//        val expectedValue = (PreferencesUtil
//            .getString(context, R.string.expectedValueKey, "").toFloat().toInt())
//
//        val drawable = ContextCompat.getDrawable(
//            context, context.resources.getIdentifier(
//                "test$expectedValue",
//                "drawable", context.packageName
//            )
//        )
//
//        bitmap = (drawable as BitmapDrawable).bitmap

        bitmap = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width,
            bitmap.height / 2
        )

        bitmap = Utilities.rotateImage(bitmap, 90)

        var badLighting = false

        val leftBarcodeBitmap = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height / 2
        )

//        if (processBlackAndWhite) {
//            leftBarcodeBitmap = ImageUtil.toBlackAndWhite(leftBarcodeBitmap, 110)
//        }

        taskLeftBarcode =
            detector.detectInImage(FirebaseVisionImage.fromBitmap(leftBarcodeBitmap))
                .addOnFailureListener(
                    fun(_: Exception) {
                        sendMessage(context.getString(R.string.color_card_not_found) + ".")
                        processing = false
                        return
                    }
                )
                .addOnSuccessListener(
                    fun(result: List<FirebaseVisionBarcode>) {
                        if (result.isEmpty()) {
                            sendMessage(context.getString(R.string.color_card_not_found) + "..")
                            processing = false
                            return
                        }
                        for (leftBarcode in result) {
                            if (!leftBarcode.rawValue.isNullOrEmpty()) {

//                                val testName = App.getTestName(result[0].displayValue!!)

                                val leftBoundingBox = fixBoundary(leftBarcode, leftBarcodeBitmap)

                                if (leftBoundingBox.top < 300) {
                                    try {

                                        if (!isBarcodeValid(
                                                leftBarcodeBitmap,
                                                leftBoundingBox,
                                                true
                                            )
                                        ) {
                                            badLighting = true
                                        }

                                        val rightBarcodeBitmap = Bitmap.createBitmap(
                                            bitmap, 0, bitmap.height / 2,
                                            bitmap.width, bitmap.height / 2
                                        )

//                                        if (processBlackAndWhite) {
//                                            rightBarcodeBitmap = ImageUtil.toBlackAndWhite(
//                                                rightBarcodeBitmap, 110
//                                            )
//                                        }

                                        detector.detectInImage(
                                            FirebaseVisionImage.fromBitmap(rightBarcodeBitmap)
                                        )
                                            .addOnFailureListener(fun(_: Exception) {
                                                sendMessage(context.getString(R.string.color_card_not_found) + "...")
                                                processing = false
                                                return
                                            })
                                            .addOnSuccessListener(
                                                fun(result: List<FirebaseVisionBarcode>) {
                                                    if (result.isNullOrEmpty()) {
                                                        sendMessage(context.getString(R.string.color_card_not_found) + "....")
                                                        processing = false
                                                        return
                                                    }

                                                    for (rightBarcode in result) {

                                                        val rightBoundingBox =
                                                            fixBoundary(
                                                                rightBarcode,
                                                                rightBarcodeBitmap
                                                            )

                                                        if (isTilted(
                                                                leftBoundingBox, rightBoundingBox
                                                            )
                                                        ) {
//                                                            bitmap.recycle()
                                                            sendMessage(context.getString(R.string.correct_camera_tilt))
                                                            processing = false
                                                            return
                                                        }

                                                        if (badLighting || !isBarcodeValid(
                                                                rightBarcodeBitmap,
                                                                rightBoundingBox, false
                                                            )
                                                        ) {
//                                                            bitmap.recycle()
                                                            sendMessage(context.getString(R.string.try_moving_well_lit))
                                                            processing = false
                                                            return
                                                        }

                                                        analyzeBarcode(
                                                            bitmap,
                                                            rightBarcode,
                                                            rightBoundingBox,
                                                            leftBoundingBox
                                                        )
                                                    }
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
        bitmap: Bitmap, rightBarcode: FirebaseVisionBarcode,
        rightBoundingBox: Rect, leftBoundingBox: Rect
    ) {
        if ((bitmap.height / 2) - rightBoundingBox.bottom < 300) {
            if (!rightBarcode.rawValue.isNullOrEmpty()) {
                val testName = getTestName(rightBarcode.displayValue!!)
                if (testName.isEmpty()) {
                    processing = false
                    return
                }

                done = true

                val finalBitmap = Bitmap.createBitmap(
                    bitmap, max(leftBoundingBox.left - 20, 0),
                    max(leftBoundingBox.top - 40, 0),
                    leftBoundingBox.right - leftBoundingBox.left + 40,
                    rightBoundingBox.bottom - leftBoundingBox.top + (bitmap.height / 2) + 80
                )

                bitmap.recycle()

                savePhoto(finalBitmap, testName, false)

            } else {
                processing = false
            }
        } else {
            processing = false
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
}
