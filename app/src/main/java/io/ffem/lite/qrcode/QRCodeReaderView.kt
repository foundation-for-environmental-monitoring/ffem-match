/*
 * Copyright 2014 David Lázaro Esparcia.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("DEPRECATION")

package io.ffem.lite.qrcode

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.Camera
import android.os.AsyncTask
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.google.zxing.*
import com.google.zxing.client.android.camera.CameraManager
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.detector.Detector
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min

/**
 * QRCodeReaderView Class which uses ZXing lib to easily integrate a QR decoder view.
 *
 * @author David Lázaro
 */
@Suppress("DEPRECATION")
class QRCodeReaderView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    SurfaceView(context, attrs), SurfaceHolder.Callback, Camera.PreviewCallback {

    private var mOnQRCodeReadListener: OnQRCodeReadListener? = null
    private var detector: Detector? = null
    private var mPreviewWidth: Int = 0
    private var mPreviewHeight: Int = 0
    private var mCameraManager: CameraManager? = null
    private var mQrDecodingEnabled = true
    private var decodeFrameTask: DecodeFrameTask? = null
    private var decodeHints: Map<DecodeHintType, Any>? = mapOf(DecodeHintType.TRY_HARDER to 1)

    init {

        if (checkCameraHardware()) {
            mCameraManager = CameraManager(getContext())
            mCameraManager!!.setPreviewCallback(this)
            holder.addCallback(this)
            setBackCamera()
        } else {
            throw RuntimeException("Error: Camera not found")
        }
    }

    /**
     * Set the callback to return decoding result
     *
     * @param onQRCodeReadListener the listener
     */
    fun setOnQRCodeReadListener(onQRCodeReadListener: OnQRCodeReadListener) {
        mOnQRCodeReadListener = onQRCodeReadListener
    }

    /**
     * Starts camera preview and decoding
     */
    fun startCamera() {
        mCameraManager!!.startPreview()
    }

    /**
     * Stop camera preview and decoding
     */
    fun stopCamera() {
        mCameraManager!!.stopPreview()
    }

    /**
     * Set Camera autofocus interval value
     * default value is 5000 ms.
     *
     * @param autofocusIntervalInMs autofocus interval value
     */
    fun setAutofocusInterval(autofocusIntervalInMs: Long) {
        if (mCameraManager != null) {
            mCameraManager!!.setAutofocusInterval(autofocusIntervalInMs)
        }
    }

    /**
     * Allows user to specify the camera ID, rather than determine
     * it automatically based on available cameras and their orientation.
     *
     * @param cameraId camera ID of the camera to use. A negative value means "no preference".
     */
    private fun setPreviewCameraId(cameraId: Int) {
        mCameraManager!!.previewCameraId = cameraId
    }

    /**
     * Camera preview from device back camera
     */
    fun setBackCamera() {
        setPreviewCameraId(Camera.CameraInfo.CAMERA_FACING_BACK)
    }

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        if (decodeFrameTask != null) {
            decodeFrameTask!!.cancel(true)
            decodeFrameTask = null
        }
    }

    /****************************************************
     * SurfaceHolder.Callback,Camera.PreviewCallback
     */

    override fun surfaceCreated(holder: SurfaceHolder) {
        Timber.d("surfaceCreated")

        try {
            // Indicate camera, our View dimensions
            mCameraManager!!.openDriver(holder, this.width, this.height)
        } catch (e: IOException) {
            Timber.w(e, "Can not openDriver")
            mCameraManager!!.closeDriver()
        } catch (e: RuntimeException) {
            Timber.w(e, "Can not openDriver")
            mCameraManager!!.closeDriver()
        }

        try {
            detector = Detector()
            mCameraManager!!.startPreview()
        } catch (e: Exception) {
            Timber.e(e)
            mCameraManager!!.closeDriver()
        }

    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Timber.d("surfaceChanged")

        if (holder.surface == null) {
            Timber.e("Error: preview surface does not exist")
            return
        }

        if (mCameraManager!!.previewSize == null) {
            Timber.e("Error: preview size does not exist")
            return
        }

        mPreviewWidth = mCameraManager!!.previewSize.x
        mPreviewHeight = mCameraManager!!.previewSize.y

        mCameraManager!!.stopPreview()

        // Fix the camera sensor rotation
        mCameraManager!!.setPreviewCallback(this)

        mCameraManager!!.startPreview()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Timber.d("surfaceDestroyed")

        mCameraManager!!.setPreviewCallback(null)
        mCameraManager!!.stopPreview()
        mCameraManager!!.closeDriver()
    }

    // Called when camera take a frame
    override fun onPreviewFrame(data: ByteArray, camera: Camera) {
        if (!mQrDecodingEnabled || decodeFrameTask != null && (decodeFrameTask!!.status == AsyncTask.Status.RUNNING || decodeFrameTask!!.status == AsyncTask.Status.PENDING)) {
            return
        }

        decodeFrameTask = DecodeFrameTask(this, decodeHints)
        decodeFrameTask!!.execute(data, camera)
    }

    /**
     * Check if this device has a camera
     */
    private fun checkCameraHardware(): Boolean {
        return when {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA) -> // this device has a camera
                true
            context.packageManager
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT) -> // this device has a front camera
                true
            else -> // this device has any camera
                context.packageManager.hasSystemFeature(
                    PackageManager.FEATURE_CAMERA_ANY
                )
        }
    }

    interface OnQRCodeReadListener {

        fun onQRCodeRead(text: String, points: Array<PointF?>)
    }

    private class DecodeFrameTask internal constructor(view: QRCodeReaderView, hints: Map<DecodeHintType, Any>?) :
        AsyncTask<Any, Void, Result>() {

        private val viewRef: WeakReference<QRCodeReaderView> = WeakReference(view)
        private val hintsRef: WeakReference<Map<DecodeHintType, Any>?> = WeakReference(hints)
        private val qrToViewPointTransformer = QRToViewPointTransformer()

        override fun doInBackground(vararg params: Any): Result? {
            val view = viewRef.get() ?: return null

            val source = view.mCameraManager!!.buildLuminanceSource(
                params[0] as ByteArray, view.mPreviewWidth,
                view.mPreviewHeight
            )

            val hybBin = HybridBinarizer(source)
            val bitmap = BinaryBitmap(hybBin)

            try {

//                val points = null
//                val resBitmap = BitmapFactory.decodeResource(view.resources, R.drawable.color_card)
//                val bitmap = bitmapToBinaryBitmap(resBitmap)

                val detectorResult = view.detector!!.detect(bitmap.blackMatrix, hintsRef.get())
                val points = detectorResult.points

                val parameters = (params[1] as Camera).parameters
                val width = parameters.previewSize.width
                val height = parameters.previewSize.height

                val yuv = YuvImage(params[0] as ByteArray, parameters.previewFormat, width, height, null)

                val out = ByteArrayOutputStream()
                yuv.compressToJpeg(Rect(0, 0, width, height), 100, out)

                val bytes = out.toByteArray()

                var top = min(points[0].y, points[1].y).toInt()
                top = min(top.toFloat(), points[2].y).toInt()

                var left = min(points[0].x, points[1].x).toInt()
                left = min(left.toFloat(), points[2].x).toInt()

                var right = max(points[0].x, points[1].x).toInt()
                right = max(right.toFloat(), points[2].x).toInt()

                var bottom = max(points[0].y, points[1].y).toInt()
                bottom = max(bottom.toFloat(), points[2].y).toInt()

                bottom += 10

                val newBitmap = cropImage(bytes, top, left, right, bottom)
//                val newBitmap = cropImage(getImageByteArray(resBitmap), top, left, right, bottom)

                val barCodeBitmap = cropImage(getImageByteArray(newBitmap), 0, 26, 480, 60)

//                val barCodeBitmap = BitmapFactory.decodeResource(view.resources, R.drawable.barcode)

                val barCodeText = readBarcode(barCodeBitmap)

                return Result(barCodeText, ByteArray(0), points, BarcodeFormat.QR_CODE)

            } catch (e: NotFoundException) {
                Timber.d("No QR Code found")
            } catch (e: FormatException) {
                Timber.d(e, "FormatException")
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return null
        }

        override fun onPostExecute(result: Result?) {
            super.onPostExecute(result)

            val view = viewRef.get()

            // Notify we found a QRCode
            if (view != null && result != null && view.mOnQRCodeReadListener != null) {
                // Transform resultPoints to View coordinates
//                val transformedPoints = transformToViewCoordinates(view, result.resultPoints)
//                view.mOnQRCodeReadListener!!.onQRCodeRead(result.text, transformedPoints)
            }
        }

//        /**
//         * Transform result to surfaceView coordinates
//         *
//         *
//         * This method is needed because coordinates are given in landscape camera coordinates when
//         * device is in portrait mode and different coordinates otherwise.
//         *
//         * @return a new PointF array with transformed points
//         */
//        private fun transformToViewCoordinates(
//            view: QRCodeReaderView,
//            resultPoints: Array<ResultPoint>
//        ): Array<PointF?> {
//            val viewSize = Point(view.width, view.height)
//            val cameraPreviewSize = view.mCameraManager!!.previewSize
//            val isMirrorCamera = view.mCameraManager!!.previewCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT
//
//            return qrToViewPointTransformer.transform(
//                resultPoints, isMirrorCamera,
//                viewSize, cameraPreviewSize
//            )
//        }
    }

    companion object {

        @Throws(IOException::class)
        fun cropImage(data: ByteArray, top: Int, left: Int, right: Int, bottom: Int): Bitmap {
            val regionDecoder = BitmapRegionDecoder
                .newInstance(data, 0, data.size, false)
            val rect = Rect(left, top, right, bottom)
            return regionDecoder.decodeRegion(rect, BitmapFactory.Options())
        }

        fun getImageByteArray(bmp: Bitmap): ByteArray {
            val baos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
            return baos.toByteArray()
        }

        fun readBarcode(bitmap: Bitmap): String? {
            var contents: String? = null

            val intArray = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
            val reader = MultiFormatReader()

            try {
                val result = reader.decode(BinaryBitmap(HybridBinarizer(source)))
                contents = result.toString()
            } catch (e: NotFoundException) {
                e.printStackTrace()
            } catch (e: ChecksumException) {
                e.printStackTrace()
            } catch (e: FormatException) {
                e.printStackTrace()
            }

            return contents
        }


//        fun bitmapToBinaryBitmap(bitmap: Bitmap): BinaryBitmap {
//            val intArray = IntArray(bitmap.width * bitmap.height)
//            bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
//            val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
//            return BinaryBitmap(HybridBinarizer(source))
//        }
    }
}
