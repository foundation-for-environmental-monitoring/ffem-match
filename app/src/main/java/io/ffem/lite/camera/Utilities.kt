package io.ffem.lite.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Environment.DIRECTORY_PICTURES
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.File.separator
import java.io.FileOutputStream
import java.io.IOException

object Utilities {

    fun savePng(
        context: Context,
        id: String,
        name: String,
        bmp: Bitmap,
        nameExt: String
    ) {
        try {
            val path = context.getExternalFilesDir(DIRECTORY_PICTURES).toString() +
                    separator + "captures" + separator

            val basePath = File(path)
            if (!basePath.exists())
                Timber.d(if (basePath.mkdirs()) "Success" else "Failed")

            val fixedName = name.replace(" ", "")
            val fileName = fixedName + nameExt

            val filePath = File("$path$id")

            if (!filePath.exists())
                Timber.d(if (filePath.mkdirs()) "Success" else "Failed")

            val byteStream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, byteStream)
            val bytes = byteStream.toByteArray()

            val stream = FileOutputStream(filePath.path + separator + "$fileName.png")
            stream.write(bytes)
            stream.flush()
            stream.fd.sync()
            stream.close()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Saves a specified picture on external disk.
     */
    fun savePicture(
        context: Context,
        id: String,
        name: String,
        bytes: ByteArray,
        nameExt: String
    ) {
        try {
            val path = context.getExternalFilesDir(DIRECTORY_PICTURES).toString() +
                    separator + "captures" + separator

            val basePath = File(path)
            if (!basePath.exists())
                Timber.d(if (basePath.mkdirs()) "Success" else "Failed")

            val fixedName = name.replace(" ", "")
            val fileName = fixedName + nameExt

            val filePath = File("$path$id")

            if (!filePath.exists())
                Timber.d(if (filePath.mkdirs()) "Success" else "Failed")

            val stream = FileOutputStream(filePath.path + separator + "$fileName.jpg")
            stream.write(bytes)
            stream.flush()
            stream.fd.sync()
            stream.close()

            if (name != "Unknown") {
                val unknown = File("$path$id" + separator + "Unknown.jpg")
                if (unknown.exists()) {
                    val newName = File("$path$id$separator$fixedName.jpg")
                    unknown.renameTo(newName)
                }
            }

            // Create a no media file in the folder to prevent images showing up in Gallery app
            val noMediaFile = File(path, ".nomedia")
            if (!noMediaFile.exists()) {
                try {
                    noMediaFile.createNewFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
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
     * Converts bitmap to byte array
     */
    fun bitmapToBytes(bitmap: Bitmap): ByteArray {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos)
        return bos.toByteArray()
    }
}
