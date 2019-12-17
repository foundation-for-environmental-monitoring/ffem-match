package io.ffem.lite.camera

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
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
import io.ffem.lite.util.ImageUtil
import io.ffem.lite.util.getBitmapPixels
import io.ffem.lite.util.hasBlackPixelsInArea
import io.ffem.lite.util.isDark
import timber.log.Timber
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

const val MAX_ANGLE = 12

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
        Timber.e("Initializing detector")
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

        val fullBitmap = mediaImage.bitmap

//        val expectedValue = (PreferencesUtil
//            .getString(context, R.string.expectedValueKey, "").toFloat().toInt())
//
//        val drawable = ContextCompat.getDrawable(
//            context, context.resources.getIdentifier(
//                "test$expectedValue",
//                "drawable", context.packageName
//            )
//        )

//        val fullBitmap = (drawable as BitmapDrawable).bitmap

        bitmap = Bitmap.createBitmap(
            fullBitmap, 0, (fullBitmap.height * 0.18).toInt(),
            fullBitmap.width,
            fullBitmap.height - (fullBitmap.height * 0.36).toInt()
        )
        fullBitmap.recycle()

        if (hasBlackPixelsInArea(bitmap, 200, 0, bitmap.width - 100, 5)) {
            processing = false
            bitmap.recycle()
            return
        }

        if (hasBlackPixelsInArea(bitmap, 200, bitmap.height - 5, bitmap.width - 100, bitmap.height)
        ) {
            processing = false
            bitmap.recycle()
            return
        }

        leftBarcodeBitmap = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height / 2
        )

        localBroadcastManager = LocalBroadcastManager.getInstance(context)

        val bwBitmap = ImageUtil.toBlackAndWhite(leftBarcodeBitmap, 90)
        val rect = Rect(200, 0, bitmap.width - 100, 5)
        val pixels = getBitmapPixels(bwBitmap, rect)
        if (isDark(pixels)) {
            processing = false
            sendMessage(context.getString(R.string.try_moving_well_lit))
            bwBitmap.recycle()
            bitmap.recycle()
            return
        }

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

                                if (barcode.boundingBox!!.width() > bitmap.width * .44
//                                    && barcode.boundingBox!!.width() < bitmap.width * .48
                                ) {
                                    try {
                                        cropTop = (bitmap.width - barcode.boundingBox!!.right) - 5
                                        cropBottom = (bitmap.width - barcode.boundingBox!!.left) + 5
                                        cropLeft = barcode.boundingBox!!.bottom + 5

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

                                                        // Check if image angle is ok
                                                        if (abs(
                                                                barcode.boundingBox!!.left
                                                                        - barcode2.boundingBox!!.left
                                                            ) > MAX_ANGLE ||
                                                            abs(
                                                                barcode.boundingBox!!.right
                                                                        - barcode2.boundingBox!!.right
                                                            ) > MAX_ANGLE
                                                        ) {
                                                            processing = false
                                                            sendMessage(context.getString(R.string.correct_camera_tilt))
                                                            return
                                                        }

                                                        cropRight = barcode2.boundingBox!!.top - 10

                                                        rightBarcodeBitmap.recycle()
                                                        analyzeBarcode(
                                                            context,
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

    private fun sendMessage(s: String) {
        val intent = Intent(App.ERROR_EVENT)
        intent.putExtra(App.ERROR_MESSAGE, s)
        localBroadcastManager.sendBroadcast(
            intent
        )
    }

    private fun analyzeBarcode(
        context: Context, bitmap: Bitmap, result: List<FirebaseVisionBarcode>
    ) {
        if (result.isEmpty()) {
            processing = false
            return
        }
        for (barcode2 in result) {
            if (!barcode2.rawValue.isNullOrEmpty()) {
                if (barcode2.boundingBox!!.width() > bitmap.width * .44) {

                    val testName = getTestName(result[0].displayValue!!)
                    if (testName.isEmpty()) {
                        processing = false
                        return
                    }

                    done = true

                    var bitmapRotated = Utilities.rotateImage(bitmap, 270)

                    cropTop = max(0, cropTop - 15)
                    val height = min(
                        max(1, cropBottom - cropTop + 15),
                        bitmapRotated.height - cropTop
                    )

                    bitmapRotated = Bitmap.createBitmap(
                        bitmapRotated, 0, cropTop,
                        bitmapRotated.width, height
                    )

                    bitmap.recycle()

                    val testId = UUID.randomUUID().toString()
                    val filePath =
                        Utilities.savePicture(
                            context.applicationContext, testId,
                            testName, Utilities.bitmapToBytes(bitmapRotated)
                        )

                    bitmapRotated.recycle()

                    val intent = Intent(App.CAPTURED_EVENT)
                    intent.putExtra(App.FILE_PATH_KEY, filePath)
                    intent.putExtra(App.TEST_ID_KEY, testId)
                    intent.putExtra(App.TEST_NAME_KEY, testName)
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
