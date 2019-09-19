package io.ffem.lite.camera

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
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
import io.ffem.lite.app.App.Companion.FILE_PATH_KEY
import io.ffem.lite.app.App.Companion.TEST_ID_KEY
import io.ffem.lite.app.App.Companion.TEST_PARAMETER_NAME
import io.ffem.lite.app.App.Companion.TEST_RESULT
import io.ffem.lite.camera.CameraFragment.Companion.CAPTURED_EVENT
import io.ffem.lite.util.ColorUtil
import java.util.*
import kotlin.math.max
import kotlin.math.min

const val MARGIN = 40

class BarcodeAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {

    private lateinit var bitmap: Bitmap
    private lateinit var leftBarcodeBitmap: Bitmap
    private lateinit var rightBarcodeBitmap: Bitmap
    private var processing = false

    private var done: Boolean = false
    private lateinit var localBroadcastManager: LocalBroadcastManager

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

    private var cropLeft = 0
    private var cropRight = 0
    private var cropTop = 0
    private var cropBottom = 0

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

//        val drawable = ContextCompat.getDrawable(context, R.drawable.test2)
//        bitmap = (drawable as BitmapDrawable).bitmap

        leftBarcodeBitmap = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, (bitmap.height * 0.15).toInt()
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
                        }
                        for (barcode in result) {
                            if (!barcode.rawValue.isNullOrEmpty()) {

                                val left = barcode.boundingBox!!.left
                                val right = barcode.boundingBox!!.right
                                val width = barcode.boundingBox!!.width()
                                val height = barcode.boundingBox!!.height()

//                                Timber.e("Width %s", width)
//                                Timber.e("Height %s", height)
//                                Timber.e("Image Width %s", bitmap.width)
//                                Timber.e("Image Height %s", bitmap.height)
//                                Timber.e("-----------------------")
//                                Timber.e("")

                                if (height > bitmap.height * .065
                                    && width > bitmap.width * .38
                                ) {
                                    try {
                                        for (i in left + MARGIN until right - MARGIN) {
                                            val pixel = leftBarcodeBitmap.getPixel(i, 5)
                                            if (pixel.red < 50 && pixel.green < 50 && pixel.blue < 50) {
                                                processing = false
                                                return
                                            }
                                        }

                                        cropLeft = barcode.boundingBox!!.left
                                        cropRight = barcode.boundingBox!!.right
                                        cropTop = barcode.boundingBox!!.bottom + 10

                                        leftBarcodeBitmap.recycle()

                                        rightBarcodeBitmap = Bitmap.createBitmap(
                                            bitmap,
                                            0,
                                            (bitmap.height * 0.85).toInt(),
                                            bitmap.width,
                                            (bitmap.height * 0.15).toInt()
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
        }
        for (barcode2 in result) {
            if (!barcode2.rawValue.isNullOrEmpty()) {
                if (barcode2.boundingBox!!.height() > bitmap.height * .065
                    && barcode2.boundingBox!!.width() > bitmap.width * .38
                ) {
                    for (i in barcode2.boundingBox!!.left + MARGIN until
                            barcode2.boundingBox!!.right - MARGIN) {
                        val pixel =
                            rightBarcodeBitmap.getPixel(
                                i,
                                rightBarcodeBitmap.height - 5
                            )
                        if (pixel.red < 50 && pixel.green < 50 && pixel.blue < 50) {
                            processing = false
                            return
                        }
                    }

                    done = true

                    val left2 = max(0, barcode2.boundingBox!!.left - MARGIN)
                    val right2 = min(bitmap.width, barcode2.boundingBox!!.right + MARGIN)

                    cropBottom =
                        bitmap.height - cropTop - rightBarcodeBitmap.height + barcode2.boundingBox!!.top - 10

                    val croppedBitmap = Bitmap.createBitmap(
                        bitmap, left2, cropTop,
                        right2 - left2,
                        cropBottom
                    )

                    var marginTop = 0
                    for (i in 0..croppedBitmap.height) {
                        val pixel = croppedBitmap.getPixel(
                            croppedBitmap.width / 2, i
                        )
                        if (Color.red(pixel) < 100) {
                            marginTop = i
                            cropTop += i
                            break
                        }
                    }

                    for (i in croppedBitmap.height - 1 downTo 0) {
                        val pixel = croppedBitmap.getPixel(
                            croppedBitmap.width / 2, i
                        )
                        if (Color.red(pixel) < 100) {
                            cropBottom -= croppedBitmap.height - i
                            break
                        }
                    }

                    croppedBitmap.recycle()

                    val croppedBitmap2 = Bitmap.createBitmap(
                        bitmap, cropLeft, cropTop,
                        cropRight - cropLeft,
                        cropBottom - marginTop
                    )

                    val testId = UUID.randomUUID().toString()
                    val resultDetail = ColorUtil.extractColors(context, croppedBitmap2)

//                    val croppedBitmap1 =
//                        Bitmap.createBitmap(bitmap, left2, 0, right2 - left2, bitmap.height)

                    val bitmapRotated = Utilities.rotateImage(croppedBitmap2, 270)

//                    croppedBitmap1.recycle()
                    croppedBitmap2.recycle()

                    val filePath = Utilities.savePicture(
                        context.applicationContext, testId,
                        TEST_PARAMETER_NAME, Utilities.bitmapToBytes(bitmapRotated)
                    )

                    bitmapRotated.recycle()

                    val intent = Intent(CAPTURED_EVENT)
                    intent.putExtra(FILE_PATH_KEY, filePath)
                    intent.putExtra(TEST_ID_KEY, testId)
                    intent.putExtra(TEST_RESULT, resultDetail.result.toString())
                    localBroadcastManager.sendBroadcast(
                        intent
                    )
                } else {
                    processing = false
                }

                rightBarcodeBitmap.recycle()
            } else {
                processing = false
            }
        }
        processing = false
    }
}
