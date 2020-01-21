/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ffem.lite.camera

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.preference.useFlashMode
import kotlinx.android.synthetic.main.preview_overlay.*
import timber.log.Timber
import java.util.concurrent.Executor
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Main fragment for this app. Implements all camera operations including:
 * - Viewfinder
 * - Photo taking
 * - Image analysis
 */
class CameraFragment : Fragment() {

    private lateinit var container: ConstraintLayout
    private lateinit var broadcastManager: LocalBroadcastManager
    private lateinit var mainExecutor: Executor

    private lateinit var cameraControl: CameraControl
    //    private lateinit var cameraInfo: CameraInfo
    private lateinit var previewView: PreviewView

    private var displayId: Int = -1
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null

    private lateinit var barcodeAnalyzer: BarcodeAnalyzer

    private var analysis: ImageAnalysis? = null

    private lateinit var messageHandler: Handler
    private lateinit var runnable: Runnable

    private val broadcastReceiver2 = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            messageHandler.removeCallbacksAndMessages(null)

            val message = intent.getStringExtra(App.ERROR_MESSAGE)
            if (bottom_overlay != null && bottom_overlay.text != message) {
                bottom_overlay.setTextColor(Color.YELLOW)
                bottom_overlay.text = message
            }

            messageHandler.postDelayed(runnable, 3000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        messageHandler = Handler()

        runnable = Runnable {
            if (bottom_overlay != null && bottom_overlay.text != getString(R.string.place_color_card)) {
                bottom_overlay.setTextColor(Color.WHITE)
                bottom_overlay.text = getString(R.string.place_color_card)
            }
        }

        mainExecutor = ContextCompat.getMainExecutor(requireContext())

        broadcastManager = LocalBroadcastManager.getInstance(requireContext())
    }

    /**
     * Make sure that all permissions are still present, since user
     * could have removed them while the app was in paused state.
     */
    override fun onResume() {
        super.onResume()
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigate(
                    CameraFragmentDirections.actionCameraToPermissions()
                )
        }

        broadcastManager.registerReceiver(broadcastReceiver2, IntentFilter(App.ERROR_EVENT))
    }

    override fun onPause() {
        super.onPause()
        messageHandler.removeCallbacksAndMessages(runnable)
        broadcastManager.unregisterReceiver(broadcastReceiver2)

        if (!activity!!.isFinishing) {
            activity?.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Unregister the broadcast receivers and listeners
        messageHandler.removeCallbacksAndMessages(runnable)
        broadcastManager.unregisterReceiver(broadcastReceiver2)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_camera, container, false)

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        container = view as ConstraintLayout

        previewView = container.findViewById(R.id.previewView)

        // Wait for the views to be properly laid out
        previewView.post {

            // Keep track of the display in which this view is attached
            displayId = previewView.display.displayId

            // Build UI controls
            updateCameraUi()

            // Bind use cases
            bindCameraUseCases()

        }
    }

    /**
     * Inflate camera controls and update the UI manually upon config changes to avoid removing
     * and re-adding the view finder from the view hierarchy; this provides a seamless rotation
     * transition on devices that support it.
     *
     * NOTE: The flag is supported starting in Android 8 but there still is a small flash on the
     * screen for devices that run Android 9 or below.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateCameraUi()
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { previewView.display.getRealMetrics(it) }
        Timber.d("Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Timber.d("Preview aspect ratio: $screenAspectRatio")

        val rotation = previewView.display.rotation

        // Bind the cameraProvider to the LifeCycleOwner
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {

            // CameraProvider
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
                .setTargetName("Preview")
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build()

            // Default PreviewSurfaceProvider
            preview?.previewSurfaceProvider = previewView.previewSurfaceProvider

            barcodeAnalyzer = BarcodeAnalyzer(context!!)
            barcodeAnalyzer.reset()

//            imageAnalyzer = ImageAnalysis(analyzerConfig).apply {
//                val googlePlayServicesAvailable = GoogleApiAvailability.getInstance()
//                    .isGooglePlayServicesAvailable(context)
//                if (googlePlayServicesAvailable == ConnectionResult.SUCCESS) {
//                    setAnalyzer(Executors.newSingleThreadExecutor(), barcodeAnalyzer!!)
//                } else {
//                    activity?.finish()
//                }
//            }
//
            // Use a worker thread for image analysis to prevent preview glitches
//            val analyzerThread = HandlerThread("BarcodeReader").apply { start() }
//            setCallbackHandler(Handler(analyzerThread.looper))

            // ImageAnalysis
            analysis = ImageAnalysis.Builder()
                .setTargetName("Analysis")
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build()

            analysis?.setAnalyzer(mainExecutor, BarcodeAnalyzer(context!!))

            // Must unbind use cases before rebinding them.
            cameraProvider.unbindAll()

            try {
                // A variable number of use-cases can be passed here.
                val camera = cameraProvider.bindToLifecycle(
                    this as LifecycleOwner, cameraSelector, preview, analysis
                )
                cameraControl = camera.cameraControl
//                cameraInfo = camera.cameraInfo

                cameraControl.enableTorch(useFlashMode())

//                logCameraInfo()
            } catch (e: Exception) {
                Timber.e(e)
            }

        }, mainExecutor)
    }

//    private fun logCameraInfo() {
//        Timber.i(
//            "flash unit present: ${cameraInfo.hasFlashUnit()}, sensor rotation: ${cameraInfo.sensorRotationDegrees}°"
//        )
//        Timber.i(
//            "zoom: ratio min: ${cameraInfo.minZoomRatio.value}f, ratio max: ${cameraInfo.maxZoomRatio.value}f, ratio current: ${cameraInfo.zoomRatio.value}f"
//        )
//        Timber.i(
//            "zoom: ${cameraInfo.linearZoom.value}f linear"
//        )
//    }

    /**
     *  [androidx.camera.core.ImageAnalysisConfig] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    /**
     * Method used to re-draw the camera UI controls, called every time configuration changes.
     */
    private fun updateCameraUi() {

        // Remove previous UI if any
        container.findViewById<ConstraintLayout>(R.id.camera_ui_container)?.let {
            container.removeView(it)
        }

        View.inflate(requireContext(), R.layout.preview_overlay, container)

        if (isDiagnosticMode()) {
            capture_button.visibility = View.VISIBLE
        } else {
            capture_button.visibility = View.GONE
        }

        capture_button.setOnClickListener {
            barcodeAnalyzer.takePhoto()
        }

        card_overlay.animate()
            .setStartDelay(100)
            .alpha(0.0f)
            .setDuration(5000)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (card_overlay != null) {
                        card_overlay.visibility = View.INVISIBLE
                    }
                }
            })
    }

    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}
