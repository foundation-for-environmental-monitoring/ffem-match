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
import com.google.gson.Gson
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.model.TestConfig
import io.ffem.lite.util.FileUtil
import io.ffem.lite.util.hasBlackPixelsInArea
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

        if (hasBlackPixelsInArea(bitmap, 100, 0, bitmap.width, 5)) {
            processing = false
            return
        }

        if (hasBlackPixelsInArea(
                bitmap, 100, bitmap.height - 5, bitmap.width, bitmap.height
            )
        ) {
            processing = false
            return
        }

        fullBitmap.recycle()

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

    private fun analyzeBarcode(
        context: Context, bitmap: Bitmap, result: List<FirebaseVisionBarcode>
    ) {
        if (result.isEmpty()) {
            processing = false
            return
        }
        for (barcode2 in result) {
            if (!barcode2.rawValue.isNullOrEmpty()) {
                if (barcode2.boundingBox!!.width() > bitmap.width * .44
//                    && barcode2.boundingBox!!.width() < bitmap.width * .48
                ) {

                    val input = context.resources.openRawResource(R.raw.calibration)
                    val content = FileUtil.readTextFile(input)
                    val testConfig = Gson().fromJson(content, TestConfig::class.java)

                    var testName = ""
                    for (test in testConfig.tests) {
                        if (test.uuid == result[0].displayValue!!) {
                            testName = test.name!!
                            break
                        }
                    }

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

                    val intent = Intent(CameraFragment.CAPTURED_EVENT)
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
