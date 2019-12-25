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
import io.ffem.lite.util.ColorUtil.fixBoundary
import io.ffem.lite.util.ColorUtil.isBarcodeValid
import io.ffem.lite.util.ColorUtil.isTilted
import io.ffem.lite.util.ImageUtil
import timber.log.Timber
import java.util.*
import kotlin.math.max
import kotlin.math.min

const val MAX_ANGLE = 14

class BarcodeAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {

    private lateinit var bitmap: Bitmap
    private var processing = false

    private var done: Boolean = false
    private lateinit var localBroadcastManager: LocalBroadcastManager

    private var cropLeft = 0
    private var cropRight = 0
    private var cropTop = 0

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
//
//        bitmap = (drawable as BitmapDrawable).bitmap

        bitmap = Bitmap.createBitmap(
            fullBitmap, 0, (fullBitmap.height * 0.18).toInt(),
            fullBitmap.width,
            fullBitmap.height - (fullBitmap.height * 0.36).toInt()
        )

        localBroadcastManager = LocalBroadcastManager.getInstance(context)

        var badLighting = false

        val leftBarcodeBitmapColor = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height / 2
        )

        val leftBarcodeBitmap = ImageUtil.toBlackAndWhite(leftBarcodeBitmapColor, 100)
        leftBarcodeBitmapColor.recycle()

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

                                if (leftBoundingBox.width() > bitmap.width * .44) {
                                    try {
                                        cropTop =
                                            (bitmap.width - leftBoundingBox.right) - 10
                                        cropLeft = leftBoundingBox.bottom + 1

                                        if (!isBarcodeValid(
                                                leftBarcodeBitmap,
                                                leftBoundingBox,
                                                true
                                            )
                                        ) {
                                            badLighting = true
                                        }

                                        val rightBarcodeBitmapColor = Bitmap.createBitmap(
                                            bitmap, 0, bitmap.height / 2,
                                            bitmap.width, bitmap.height / 2
                                        )

                                        val rightBarcodeBitmap =
                                            ImageUtil.toBlackAndWhite(rightBarcodeBitmapColor, 100)
                                        rightBarcodeBitmapColor.recycle()

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

                                                        cropRight = rightBoundingBox.top - 10

                                                        analyzeBarcode(
                                                            context,
                                                            bitmap,
                                                            rightBarcode, rightBoundingBox
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
        context: Context, bitmap: Bitmap, rightBarcode: FirebaseVisionBarcode,
        rightBoundingBox: Rect
    ) {
        if (rightBoundingBox.width() > bitmap.width * .44) {
            if (!rightBarcode.rawValue.isNullOrEmpty()) {
                val testName = getTestName(rightBarcode.displayValue!!)
                if (testName.isEmpty()) {
                    processing = false
                    return
                }

                done = true

                var bitmapRotated = Utilities.rotateImage(bitmap, 270)

                cropTop = max(0, cropTop - 10)

                bitmapRotated = Bitmap.createBitmap(
                    bitmapRotated, 0, cropTop,
                    bitmapRotated.width,
                    min(rightBoundingBox.width() + 40, bitmapRotated.height - cropTop)
                )

                bitmap.recycle()

                val testId = UUID.randomUUID().toString()
                val filePath = Utilities.savePicture(
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

    private fun sendMessage(s: String) {
        val intent = Intent(App.ERROR_EVENT)
        intent.putExtra(App.ERROR_MESSAGE, s)
        localBroadcastManager.sendBroadcast(
            intent
        )
    }
}
