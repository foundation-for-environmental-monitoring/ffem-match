package io.ffem.lite.camera

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import io.ffem.lite.app.App.Companion.FILE_PATH_KEY
import io.ffem.lite.app.App.Companion.TEST_ID_KEY
import io.ffem.lite.app.App.Companion.TEST_PARAMETER_NAME
import io.ffem.lite.camera.CameraFragment.Companion.CAPTURED_EVENT
import io.ffem.lite.util.hasBlackPixelsOnBottomEdge
import io.ffem.lite.util.hasBlackPixelsOnTopEdge
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

const val MAX_SIDE_MARGIN = 50
const val MAX_LONG_SIDE_MARGIN = 40
const val MAX_ANGLE = 15
const val BARCODE_SIZE_PERCENT = 0.15

class BarcodeLargeAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {

    private lateinit var bitmap: Bitmap
    private lateinit var leftBarcodeBitmap: Bitmap
    private lateinit var rightBarcodeBitmap: Bitmap
    private var processing = false

    private var done: Boolean = false
    private lateinit var localBroadcastManager: LocalBroadcastManager

    private var cropLeft = 0
    private var cropRight = 0

    private val detector: FirebaseVisionBarcodeDetector by lazy {
        localBroadcastManager = LocalBroadcastManager.getInstance(context)
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
        if (done) {
            return
        }

        if (processing) {
            return
        }

        processing = true

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

        bitmap = mediaImage.bitmap

        leftBarcodeBitmap = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, (bitmap.height * BARCODE_SIZE_PERCENT).toInt()
        )

        taskLeftBarcode =
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
                            return
                        }
                        for (leftBarcode in result) {
                            if (!leftBarcode.rawValue.isNullOrEmpty()) {

                                cropLeft = leftBarcode.boundingBox!!.left
                                cropRight = leftBarcode.boundingBox!!.right
                                val width = leftBarcode.boundingBox!!.width()
                                val height = leftBarcode.boundingBox!!.height()

                                if (height > bitmap.height * .065
                                    && width > bitmap.width * .38
                                ) {
                                    try {

                                        // Ensure barcode is not too far from the edge
                                        if (leftBarcode.boundingBox!!.top > MAX_SIDE_MARGIN) {
                                            processing = false
                                            return
                                        }

                                        if (hasBlackPixelsOnTopEdge(
                                                leftBarcodeBitmap, cropLeft,
                                                leftBarcode.boundingBox!!.width()
                                            )
                                        ) {
                                            processing = false
                                            leftBarcodeBitmap.recycle()
                                            return
                                        }

                                        leftBarcodeBitmap.recycle()

                                        rightBarcodeBitmap = Bitmap.createBitmap(
                                            bitmap,
                                            0,
                                            (bitmap.height * 0.85).toInt(),
                                            bitmap.width,
                                            (bitmap.height * BARCODE_SIZE_PERCENT).toInt()
                                        )

                                        detector.detectInImage(
                                            FirebaseVisionImage.fromBitmap(rightBarcodeBitmap)
                                        )
                                            .addOnFailureListener(fun(_: Exception) {
                                                processing = false
                                            })
                                            .addOnSuccessListener(
                                                fun(result: List<FirebaseVisionBarcode>) {
                                                    analyzeBarcode(result)
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

    private fun analyzeBarcode(result: List<FirebaseVisionBarcode>) {
        if (result.isEmpty()) {
            processing = false
            return
        }
        for (rightBarcode in result) {

            // Ensure barcode is not too far from the edge
            if (rightBarcodeBitmap.height - rightBarcode.boundingBox!!.bottom > MAX_SIDE_MARGIN) {
                processing = false
                return
            }

            if (!rightBarcode.rawValue.isNullOrEmpty()) {
                if (rightBarcode.boundingBox!!.height() > bitmap.height * .065
                    && rightBarcode.boundingBox!!.width() > bitmap.width * .38
                ) {
                    // Check if image angle is ok
                    if (abs(cropLeft - rightBarcode.boundingBox!!.left) > MAX_ANGLE ||
                        abs(cropRight - rightBarcode.boundingBox!!.right) > MAX_ANGLE
                    ) {
                        processing = false
                        return
                    }

                    if (hasBlackPixelsOnBottomEdge(
                            rightBarcodeBitmap, cropLeft,
                            rightBarcode.boundingBox!!.width()
                        )
                    ) {
                        processing = false
                        rightBarcodeBitmap.recycle()
                        return
                    }

                    rightBarcodeBitmap.recycle()

                    done = true

                    val left = max(0, rightBarcode.boundingBox!!.left - MAX_LONG_SIDE_MARGIN)
                    val right =
                        min(bitmap.width, rightBarcode.boundingBox!!.right + MAX_LONG_SIDE_MARGIN)

                    val croppedBitmap =
                        Bitmap.createBitmap(bitmap, left, 0, right - left, bitmap.height)

                    val bitmapRotated = Utilities.rotateImage(croppedBitmap, 270)

                    croppedBitmap.recycle()

                    val testId = UUID.randomUUID().toString()
                    val filePath =
                        Utilities.savePicture(
                            context.applicationContext, testId,
                            TEST_PARAMETER_NAME, Utilities.bitmapToBytes(bitmapRotated)
                        )

                    bitmapRotated.recycle()

                    val intent = Intent(CAPTURED_EVENT)
                    intent.putExtra(FILE_PATH_KEY, filePath)
                    intent.putExtra(TEST_ID_KEY, testId)
                    localBroadcastManager.sendBroadcast(
                        intent
                    )
                } else {
                    processing = false
                }
            } else {
                processing = false
            }
        }
        processing = false
    }
}
