package io.ffem.lite.barcode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Environment.getExternalStorageDirectory
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import okhttp3.*
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.File.separator
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object Utilities {

    // API url where images will be posted for further processing.
    private const val API_URL = "http://ec2-52-66-17-109.ap-south-1.compute.amazonaws.com:5000"

    /**
     * return the timestamp on yyMMdd_hhmmss format
     */
    private val timestamp: String
        get() {
            val sdf = SimpleDateFormat("yyMMdd_hhmmss", Locale.US)
            return sdf.format(Date())
        }

    /**
     * Saves a specified picture on external disk.
     */
    fun savePicture(barcodeValue: String, bytes: ByteArray): String {
        try {
            val mainPath =
                getExternalStorageDirectory().toString() + separator + "MaskIt" + separator + "images" + separator
            val basePath = File(mainPath)
            if (!basePath.exists())
                Timber.d(if (basePath.mkdirs()) "Success" else "Failed")

            val filePath = mainPath + "photo_" + timestamp + "_" + barcodeValue + ".jpg"
            val captureFile = File(filePath)
            if (!captureFile.exists())
                Timber.d(if (captureFile.createNewFile()) "Success" else "Failed")
            val stream = FileOutputStream(captureFile)
            stream.write(bytes)
            stream.flush()
            stream.close()
            return filePath
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return ""
    }

    /**
     * Uploads a file to server using multipart post.
     */
    @Throws(Exception::class)
    fun uploadToServer(filePath: String) {
        val file = File(filePath)
        val contentType = file.toURL().openConnection().contentType

        Timber.d("file: %s", file.path)
        Timber.d("contentType: %s", contentType)

        val fileBody = RequestBody.create(MediaType.parse(contentType), file)
        val filename = file.name

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("user_id", "1")
            .addFormDataPart("group_id", "1")
            .addFormDataPart("image", filename, fileBody)
            .build()

        val request = Request.Builder()
            .url(API_URL)
            .post(requestBody)
            .build()

        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Timber.d(e, "Upload Failed!")
            }

            override fun onResponse(call: Call, response: Response) {
                Timber.d("Upload completed!")
                response.body()!!.close()
            }
        })
    }

    /**
     * Rotate an image by the specified degree.
     *
     * @param bitmap: input image bitmap
     * @param degree: degree to rotate
     */
    fun rotateImage(bitmap: Bitmap, degree: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Converts bitmap to
     */
    fun bitmapToBytes(bitmap: Bitmap): ByteArray {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
        return bos.toByteArray()
    }

    /**
     * Tries to crop an image for area within to barcodes.
     * This was needed because, the barcode detector library as at times detection only one barcode (right or left).
     * We needed to crop the image to extract the area within both barcodes.
     * NOTE: This method works on best effort basis and does not always work!
     *
     * @param bitmap - input image bitmap
     * @param rect   - rect of the identifies bitmap
     */
    private fun checkAndCrop(bitmap: Bitmap, rect: Rect): Bitmap {
        val x1 = rect.left
        val y1 = rect.top

        val x2 = rect.right
        val y2 = rect.bottom

        val detectedBarcodeType: String
        val outBitmap: Bitmap
        if (x1 < 1920 / 2 && x2 < 1920 / 2) { // Left barcode is detected
            outBitmap = cropImage(bitmap, Rect(x1, y1, 1980, y1))
            detectedBarcodeType = "LEFT"
        } else if (x1 > 1920 / 2 && x2 > 1920 / 2) { // Right barcode is detected
            outBitmap = bitmap
            detectedBarcodeType = "RIGHT"
        } else if (x1 < 1920 / 2 && x2 > 1920 / 2) { // both are detected
            outBitmap = cropImage(bitmap, rect)
            detectedBarcodeType = "BOTH"
        } else { // could not detect!
            outBitmap = bitmap
            detectedBarcodeType = "NONE"
        }

        Timber.d("Detected Barcode type: %s", detectedBarcodeType)
        return outBitmap
    }

    /**
     * Crops an image
     */
    private fun cropImage(bitmap: Bitmap, rect: Rect): Bitmap {
        Timber.d("Input - size: %s x %s", bitmap.width, bitmap.height)

        val top = if (rect.top < 50) 0 else rect.top - 50
        val bottom = if (rect.bottom > 1000) 1080 else rect.bottom + 80

        return try {
            val outBitmap = Bitmap.createBitmap(bitmap, rect.left, top, rect.right - rect.left, bottom - rect.top)
            Timber.d("Output - size: %s x %s", outBitmap.width, outBitmap.height)
            outBitmap
        } catch (e: Exception) {
            bitmap
        }
    }

    /**
     * Checks is it's a valid aspect ratio - as per:
     * width/height OR height/width == 5
     * NOTE: This is a hacky way to check!
     */
    fun isValidAspectRatio(barcode: Barcode): Boolean {
        val w = barcode.boundingBox.width()
        val h = barcode.boundingBox.height()
        if (w == 0 || h == 0) return false

        if (w > h && w / h < 5 || h > w && h / w < 5) {
            Timber.d("width:$w, height:$h")
            return true
        } else {
            Timber.d("width:$w, height:$h")
        }
        return false
    }

    /**
     * Detects barcode(s) from a supplied image.
     *
     * @param bitmap - input image bitmap.
     */
    fun detectBarcode(bitmap: Bitmap, applicationContext: Context): Bitmap {
        val frame = Frame.Builder().setBitmap(bitmap).build()
        val barcodeDetector = BarcodeDetector.Builder(applicationContext)
            .build()
        val sparseArray = barcodeDetector.detect(frame)


        if (sparseArray.size() > 0) {
            for (i in 0 until sparseArray.size()) {
                Timber.d("Value: %s----%s", sparseArray.valueAt(i).rawValue,
                    sparseArray.valueAt(i).displayValue)
            }
            return Utilities.checkAndCrop(bitmap, sparseArray.valueAt(0).boundingBox)

        } else {
            Timber.e("SparseArray null or empty")
        }

        return bitmap
    }
}
