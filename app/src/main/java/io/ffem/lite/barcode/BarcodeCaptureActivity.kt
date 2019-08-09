/*
 * Copyright (C) The Android Open Source Project
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

package io.ffem.lite.barcode

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.android.material.snackbar.Snackbar
import io.ffem.lite.R
import io.ffem.lite.app.AppDatabase
import io.ffem.lite.model.TestResult
import io.ffem.lite.util.PreferencesUtil
import okhttp3.*
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Activity for the multi-tracker app.  This app detects barcodes and displays the value with the
 * rear facing camera. During detection overlay graphics are drawn to indicate the position,
 * size, and ID of each barcode.
 */
class BarcodeCaptureActivity : AppCompatActivity(), BarcodeGraphicTracker.BarcodeUpdateListener {
    private lateinit var mCameraSource: CameraSource
    private var mPreview: CameraSourcePreview? = null
    private lateinit var mGraphicOverlay: GraphicOverlay<BarcodeGraphic>
    // helper objects for detecting taps and pinches.
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var gestureDetector: GestureDetector? = null

    /**
     * Initializes the UI and creates the detector pipeline.
     */
    public override fun onCreate(icicle: Bundle?) {
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        super.onCreate(icicle)
        setContentView(R.layout.barcode_capture)

        mPreview = findViewById(R.id.preview)
        mGraphicOverlay = findViewById(R.id.graphicOverlay)

        // read parameters from the intent used to launch the activity.
        val autoFocus = intent.getBooleanExtra(AutoFocus, true)
        val useFlash = intent.getBooleanExtra(UseFlash, false)

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(autoFocus, useFlash)
        } else {
            requestCameraPermission()
        }

        gestureDetector = GestureDetector(this, CaptureGestureListener())
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())

//        Snackbar.make(
//            mGraphicOverlay!!, "Tap to capture. Pinch/Stretch to zoom",
//            Snackbar.LENGTH_LONG
//        ).show()
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private fun requestCameraPermission() {
        Timber.w("Camera permission is not granted. Requesting permission")

        val permissions = arrayOf(Manifest.permission.CAMERA)

        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            )
        ) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM)
            return
        }

        val thisActivity = this

        val listener = { _: View ->
            ActivityCompat.requestPermissions(
                thisActivity, permissions,
                RC_HANDLE_CAMERA_PERM
            )
        }

        findViewById<View>(R.id.topLayout).setOnClickListener(listener)
        Snackbar.make(
            mGraphicOverlay, R.string.camera_permission_required,
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(R.string.ok, listener)
            .show()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val b = scaleGestureDetector!!.onTouchEvent(e)

        val c = gestureDetector!!.onTouchEvent(e)

        return b || c || super.onTouchEvent(e)
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     *
     *
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private fun createCameraSource(autoFocus: Boolean, useFlash: Boolean) {
        val context = applicationContext

        // A barcode detector is created to track barcodes.  An associated multi-processor instance
        // is set to receive the barcode detection results, track the barcodes, and maintain
        // graphics for each barcode on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each barcode.
        val barcodeDetector = BarcodeDetector.Builder(context).build()
        val barcodeFactory = BarcodeTrackerFactory(mGraphicOverlay, this)
        barcodeDetector.setProcessor(
            MultiProcessor.Builder(barcodeFactory).build()
        )

        if (!barcodeDetector.isOperational) {
            // Note: The first time that an app using the barcode or face API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any barcodes
            // and/or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Timber.w("Detector dependencies are not yet available.")

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            val lowStorageFilter = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            val hasLowStorage = registerReceiver(null, lowStorageFilter) != null

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show()
                Timber.w(getString(R.string.low_storage_error))
            }
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        var builder: CameraSource.Builder = CameraSource.Builder(applicationContext, barcodeDetector)
            .setFacing(CameraSource.CAMERA_FACING_BACK)
            .setRequestedPreviewSize(1280, 960)
            .setRequestedFps(15.0f)

        // make sure that auto focus is an available option
        builder = builder.setFocusMode(
            if (autoFocus) Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE else null
        )

        mCameraSource = builder
            .setFlashMode(if (useFlash) Camera.Parameters.FLASH_MODE_TORCH else null)
            .build()
    }

    /**
     * Restarts the camera.
     */
    override fun onResume() {
        super.onResume()
        startCameraSource()
    }

    /**
     * Stops the camera.
     */
    override fun onPause() {
        super.onPause()
        if (mPreview != null) {
            mPreview!!.stop()
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    override fun onDestroy() {
        super.onDestroy()
        if (mPreview != null) {
            mPreview!!.release()
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on [.requestPermissions].
     *
     *
     * **Note:** It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     *
     *
     * @param requestCode  The request code passed in [.requestPermissions].
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     * which is either [PackageManager.PERMISSION_GRANTED]
     * or [PackageManager.PERMISSION_DENIED]. Never null.
     * @see .requestPermissions
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Timber.d("Got unexpected permission result: %s", requestCode)
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Timber.d("Camera permission granted - initialize the camera source")
            // we have permission, so create the camera source
            val autoFocus = intent.getBooleanExtra(AutoFocus, false)
            val useFlash = intent.getBooleanExtra(UseFlash, false)
            createCameraSource(autoFocus, useFlash)
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Multi tracker sample")
            .setMessage(R.string.no_camera_permission)
            .setPositiveButton(R.string.ok) { _, _ -> finish() }
            .show()
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    @Throws(SecurityException::class)
    private fun startCameraSource() {
        // check that the device has play services available.
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
            applicationContext
        )
        if (code != ConnectionResult.SUCCESS) {
            val dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS)
            dlg.show()
        }

        if (this::mCameraSource.isInitialized) {
            try {
                mPreview!!.start(mCameraSource, mGraphicOverlay)
            } catch (e: IOException) {
                Timber.e(e, "Unable to start camera source.")
                mCameraSource.release()
            }
        }
    }

    /**
     * onTap returns the tapped barcode result to the calling Activity.
     *
     * @param rawX - the raw position of the tap
     * @param rawY - the raw position of the tap.
     * @return true if the activity is ending.
     */
    private fun onTap(rawX: Float, rawY: Float): Boolean {
        // Find tap point in preview frame coordinates.
        val location = IntArray(2)
        mGraphicOverlay.getLocationOnScreen(location)
        val x = (rawX - location[0]) / mGraphicOverlay.widthScaleFactor
        val y = (rawY - location[1]) / mGraphicOverlay.heightScaleFactor

        // Find the barcode whose center is closest to the tapped point.
        var best: Barcode? = null
        var bestDistance = java.lang.Float.MAX_VALUE

        for (graphic in mGraphicOverlay.graphics) {
            val barcode = graphic.barcode
            if (barcode!!.boundingBox.contains(x.toInt(), y.toInt())) {
                // Exact hit, no need to keep looking.
                best = barcode
                break
            }
            val dx = x - barcode.boundingBox.centerX()
            val dy = y - barcode.boundingBox.centerY()
            val distance = dx * dx + dy * dy  // actually squared distance
            if (distance < bestDistance) {
                best = barcode
                bestDistance = distance
            }
        }

        sendDummyImage("Fluoride")

//        if (best != null) {
//            captureImage(best.displayValue, best.boundingBox)
//        }

        return false
    }

    private fun sendDummyImage(name: String) {
        Timber.d("isExternalStorageWritable :%s", isPermissionsGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE))

        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.card_barcode_2)

        sendToServer(name, bitmap)

        setResult(Activity.RESULT_OK, Intent())
        finish()
    }

    /**
     * capture an image of the current view in camera
     *
     * @param barcodeValue the barcode value
     * @param rect         the rect
     */
    private fun captureImage(barcodeValue: String, rect: Rect) {
        Timber.d("taking picture")
        // take picture
        mCameraSource.takePicture(null, { bytes ->
            Timber.d("isExternalStorageWritable :%s", isPermissionsGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            Timber.d("isExternalStorageReadable :%s", isPermissionsGranted(Manifest.permission.READ_EXTERNAL_STORAGE))
            Timber.d("isCameraPermission :%s", isPermissionsGranted(Manifest.permission.CAMERA))
            var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            Timber.d("width: %s, height: %s", bitmap.width, bitmap.height)

            bitmap = Utilities.rotateImage(bitmap, 270)

            bitmap = Utilities.detectBarcode(bitmap, applicationContext)

            sendToServer(barcodeValue, bitmap)

            setResult(Activity.RESULT_OK, Intent())
            finish()
        })
    }

    private fun sendToServer(barcodeValue: String, bitmap: Bitmap) {

        val filePath = Utilities.savePicture(barcodeValue, Utilities.bitmapToBytes(bitmap))
        try {
            // Add barcode value as exif metadata in the image.
            val imageDescription = "{\"test_type\" : $barcodeValue}"
            val exif = ExifInterface(filePath)
            exif.setAttribute("ImageDescription", imageDescription)
            exif.saveAttributes()
        } catch (e: IOException) {
            // handle the error
        }

        try {
            //            Utilities.uploadToServer(filePath);

            val file = File(filePath)
            val contentType = file.toURL().openConnection().contentType

            Timber.d("file: %s", file.path)
            Timber.d("contentType: %s", contentType)

            val fileBody = RequestBody.create(MediaType.parse(contentType), file)
            val filename = file.name

            val testId = UUID.randomUUID().toString()

            PreferencesUtil.setString(this, "testRunId", testId)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("user_id", "1")
                .addFormDataPart("testId", testId)
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

                    val intent = Intent()
                    setResult(Activity.RESULT_OK, intent)
                    response.body()!!.close()
                    val db = AppDatabase.getDatabase(baseContext)

                    val date = Date()
                    db.resultDao().insert(TestResult(testId, barcodeValue, date.time, "", "Analysing"))

                    finish()

                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    /**
     * Checks if required permission is granted. If not, request permission from user.
     *
     * @param permission - permission to check
     * @return granted value
     */
    private fun isPermissionsGranted(permission: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                Timber.v("Permission is granted")
                true
            } else {

                Timber.v("Permission is revoked")
                ActivityCompat.requestPermissions(this, arrayOf(permission), 1)
                false
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Timber.v("Permission $permission is granted")
            true
        }
    }

    override fun onBarcodeDetected(barcode: Barcode) {
        //do something with barcode data returned
        Timber.d("Captured barcode: %s, bounds :  %s", barcode.displayValue, barcode.boundingBox)

        if (Utilities.isValidAspectRatio(barcode)) {
            captureImage(barcode.displayValue, barcode.boundingBox)
        }
    }

    private inner class CaptureGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return onTap(e.rawX, e.rawY) || super.onSingleTapConfirmed(e)
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.OnScaleGestureListener {

        /**
         * Responds to scaling events for a gesture in progress.
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to
         * retrieve extended info about event state.
         * @return Whether or not the detector should consider this event
         * as handled. If an event was not handled, the detector
         * will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example,
         * only wants to update scaling factors if the change is
         * greater than 0.01.
         */
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            return false
        }

        /**
         * Responds to the beginning of a scaling gesture. Reported by
         * new pointers going down.
         *
         * @param detector The detector reporting the event - use this to
         * retrieve extended info about event state.
         * @return Whether or not the detector should continue recognizing
         * this gesture. For example, if a gesture is beginning
         * with a focal point outside of a region where it makes
         * sense, onScaleBegin() may return false to ignore the
         * rest of the gesture.
         */
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            return true
        }

        /**
         * Responds to the end of a scale gesture. Reported by existing
         * pointers going up.
         *
         *
         * Once a scale has ended, [ScaleGestureDetector.getFocusX]
         * and [ScaleGestureDetector.getFocusY] will return focal point
         * of the pointers remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to
         * retrieve extended info about event state.
         */
        override fun onScaleEnd(detector: ScaleGestureDetector) {
            mCameraSource.doZoom(detector.scaleFactor)
        }
    }

    companion object {
        // constants used to pass extra data in the intent
        const val AutoFocus = "AutoFocus"
        const val UseFlash = "UseFlash"
        //        val BarcodeObject = "Barcode"
        //    private static final String TAG = "Barcode-reader";
        // intent request code to handle updating play services if needed.
        private const val RC_HANDLE_GMS = 9001
        // permission request codes need to be < 256
        private const val RC_HANDLE_CAMERA_PERM = 2
        private const val API_URL = "http://ec2-52-66-17-109.ap-south-1.compute.amazonaws.com:5000"
    }

}
