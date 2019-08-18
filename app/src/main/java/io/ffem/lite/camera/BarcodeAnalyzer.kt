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
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import io.ffem.lite.camera.CameraFragment.Companion.CAPTURED_EVENT
import timber.log.Timber

class BarcodeAnalyzer(private val context: Context) : ImageAnalysis.Analyzer,
    OnSuccessListener<List<FirebaseVisionBarcode>> {

    private lateinit var leftBarcodeBitmap: Bitmap
    private lateinit var rightBarcodeBitmap: Bitmap

    private var done: Boolean = false
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private var right: Int = 0
    private var left: Int = 0
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
    private var taskRightBarcode: Task<out Any>? = null
    private lateinit var mediaImage: FirebaseVisionImage

    override fun analyze(image: ImageProxy, rotationDegrees: Int) {
        if (done) {
            return
        }

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
                    Timber.e("unexpected rotation: $rotationDegrees")
                    FirebaseVisionImageMetadata.ROTATION_0
                }
            }
        )

        leftBarcodeBitmap = Bitmap.createBitmap(
            mediaImage.bitmap, 0, 0,
            mediaImage.bitmap.width, (mediaImage.bitmap.height * 0.15).toInt()
        )

        taskLeftBarcode = detector.detectInImage(FirebaseVisionImage.fromBitmap(leftBarcodeBitmap))
            .addOnSuccessListener(
                fun(result: List<FirebaseVisionBarcode>) {
                    for (barcode in result) {
                        if (!barcode.rawValue.isNullOrEmpty()) {

                            left = barcode.boundingBox!!.left - 20
                            right = barcode.boundingBox!!.right + 20

                            for (i in left until right - left) {
                                val pixel = leftBarcodeBitmap.getPixel(i, 5)
                                if (pixel.red < 20 && pixel.green < 20 && pixel.blue < 20) {
                                    return
                                }
                            }

                            rightBarcodeBitmap = Bitmap.createBitmap(
                                mediaImage.bitmap, 0, (mediaImage.bitmap.height * 0.85).toInt(),
                                mediaImage.bitmap.width, (mediaImage.bitmap.height * 0.15).toInt()
                            )

                            taskRightBarcode = detector.detectInImage(
                                FirebaseVisionImage.fromBitmap(rightBarcodeBitmap)
                            )
                                .also {
                                    it.addOnSuccessListener(this)
                                }
                        }
                    }
                }
            )
    }

    override fun onSuccess(result: List<FirebaseVisionBarcode>) {

        if (done) {
            return
        }

        for (barcode in result) {
            if (!barcode.rawValue.isNullOrEmpty()) {
                left = barcode.boundingBox!!.left - 20
                right = barcode.boundingBox!!.right + 20

                for (i in left until right - left) {
                    val pixel = rightBarcodeBitmap.getPixel(i, rightBarcodeBitmap.height - 5)
                    if (pixel.red < 20 && pixel.green < 20 && pixel.blue < 20) {
                        return
                    }
                }

                leftBarcodeBitmap.recycle()
                rightBarcodeBitmap.recycle()

                done = true

                val croppedBitmap = Bitmap.createBitmap(
                    mediaImage.bitmap, left, 0,
                    right - left, mediaImage.bitmap.height
                )

                val bitmapRotated = Utilities.rotateImage(croppedBitmap, 270)

                croppedBitmap.recycle()

                val filePath = Utilities.savePicture("Fluoride", Utilities.bitmapToBytes(bitmapRotated))

                bitmapRotated.recycle()

                val intent = Intent(CAPTURED_EVENT)
                intent.putExtra("FilePath", filePath)
                localBroadcastManager.sendBroadcast(intent)
            }
        }
    }
}
