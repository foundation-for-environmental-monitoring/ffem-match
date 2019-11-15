package io.ffem.lite.camera

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import io.ffem.lite.app.App
import io.ffem.lite.util.MIN_LINE_BRIGHTNESS
import java.util.*
import kotlin.math.max

class BarcodeAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {

    private lateinit var bitmap: Bitmap
    private lateinit var leftBarcodeBitmap: Bitmap
    private var processing = false

    private var done: Boolean = false
    private lateinit var localBroadcastManager: LocalBroadcastManager

    private var cropLeft = 0
    private var cropRight = 0
    private var cropTop = 0
    private var cropBottom = 0

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

//        val drawable = ContextCompat.getDrawable(context, R.drawable.compact_sample)
//        bitmap = (drawable as BitmapDrawable).bitmap

        leftBarcodeBitmap = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height / 2
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
                        for (barcode in result) {
                            if (!barcode.rawValue.isNullOrEmpty()) {

                                val height = barcode.boundingBox!!.height()

                                if (height > bitmap.width * .050
                                ) {
                                    try {
                                        cropTop = barcode.boundingBox!!.left
                                        cropBottom = barcode.boundingBox!!.right
                                        cropLeft = barcode.boundingBox!!.bottom + 10

                                        for (i in barcode.boundingBox!!.bottom + 10
                                                until leftBarcodeBitmap.height) {
                                            val pixel = leftBarcodeBitmap.getPixel(
                                                leftBarcodeBitmap.width / 2, i
                                            )
                                            if (pixel.red < MIN_LINE_BRIGHTNESS &&
                                                pixel.green < MIN_LINE_BRIGHTNESS &&
                                                pixel.blue < MIN_LINE_BRIGHTNESS
                                            ) {
                                                cropLeft = i
                                                break
                                            }
                                        }

                                        leftBarcodeBitmap.recycle()

                                        val rightBarcodeBitmap =
                                            Bitmap.createBitmap(
                                                bitmap,
                                                0,
                                                bitmap.height / 2,
                                                bitmap.width,
                                                bitmap.height / 2
                                            )


                                        detector.detectInImage(
                                            FirebaseVisionImage.fromBitmap(rightBarcodeBitmap)
                                        )
                                            .addOnFailureListener(fun(_: Exception) {
                                                processing = false
                                            })
                                            .addOnSuccessListener(
                                                fun(result: List<FirebaseVisionBarcode>) {
                                                    for (barcode2 in result) {
                                                        cropRight = barcode2.boundingBox!!.top - 10
                                                        for (i in barcode2.boundingBox!!.top - 10
                                                                downTo 0) {
                                                            val pixel = rightBarcodeBitmap.getPixel(
                                                                rightBarcodeBitmap.width / 2, i
                                                            )
                                                            if (pixel.red < MIN_LINE_BRIGHTNESS &&
                                                                pixel.green < MIN_LINE_BRIGHTNESS &&
                                                                pixel.blue < MIN_LINE_BRIGHTNESS
                                                            ) {
                                                                cropRight = i
                                                                break
                                                            }
                                                        }

                                                        rightBarcodeBitmap.recycle()
                                                        val testId = UUID.randomUUID().toString()
                                                        analyzeBarcode(
                                                            context,
                                                            testId,
                                                            bitmap,
                                                            result
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
        context: Context, id: String,
        bitmap: Bitmap, result: List<FirebaseVisionBarcode>
    ) {
        if (result.isEmpty()) {
            processing = false
            return
        }
        for (barcode2 in result) {
            if (!barcode2.rawValue.isNullOrEmpty()) {
                if (barcode2.boundingBox!!.height() > bitmap.width * .050
                ) {

                    done = true

                    val bitmapRotated = Utilities.rotateImage(bitmap, 270)

                    val croppedBitmap = Bitmap.createBitmap(
                        bitmapRotated, cropLeft, cropTop,
                        max(1, cropRight + ((bitmapRotated.width / 2) - cropLeft)),
                        max(1, cropBottom - cropTop)
                    )

                    bitmap.recycle()

                    Utilities.savePicture(
                        context.applicationContext, id,
                        App.TEST_PARAMETER_NAME, Utilities.bitmapToBytes(croppedBitmap)
                    )

                    croppedBitmap.recycle()

                    val testId = UUID.randomUUID().toString()
                    val filePath =
                        Utilities.savePicture(
                            context.applicationContext, testId,
                            App.TEST_PARAMETER_NAME, Utilities.bitmapToBytes(bitmapRotated)
                        )

                    bitmapRotated.recycle()

                    val intent = Intent(CameraFragment.CAPTURED_EVENT)
                    intent.putExtra(App.FILE_PATH_KEY, filePath)
                    intent.putExtra(App.TEST_ID_KEY, testId)
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
    }
}
