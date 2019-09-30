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
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Rational
import android.util.Size
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import io.ffem.lite.R
import io.ffem.lite.preference.useFlashMode
import io.ffem.lite.ui.BarcodeActivity.Companion.getOutputDirectory
import io.ffem.lite.util.AutoFitPreviewBuilder
import kotlinx.android.synthetic.main.preview_overlay.*
import timber.log.Timber
import java.io.File

class CameraFragment : Fragment() {

    private lateinit var container: ConstraintLayout
    private lateinit var viewFinder: TextureView
    private lateinit var outputDirectory: File
    private lateinit var broadcastManager: LocalBroadcastManager

    private var displayId = -1
    private var lensFacing = CameraX.LensFacing.BACK
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null

    /** Internal reference of the [DisplayManager] */
    private lateinit var displayManager: DisplayManager


    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            preview?.removePreviewOutputListener()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mark this as a retain fragment, so the lifecycle does not get restarted on config change
        retainInstance = true
    }

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since user could have removed them
        //  while the app was on paused state
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                CameraFragmentDirections.actionCameraToPermissions()
            )
        }
        broadcastManager.registerReceiver(broadcastReceiver, IntentFilter(CAPTURED_EVENT))
    }

    override fun onPause() {
        super.onPause()
        imageAnalyzer?.removeAnalyzer()
        preview?.removePreviewOutputListener()
        CameraX.unbindAll()
        broadcastManager.unregisterReceiver(broadcastReceiver)

        if (!activity!!.isFinishing) {
            activity?.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        broadcastManager.unregisterReceiver(broadcastReceiver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_camera, container, false)

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view as ConstraintLayout
        viewFinder = container.findViewById(R.id.view_finder)

        broadcastManager = LocalBroadcastManager.getInstance(view.context)

        // Every time the orientation of device changes, recompute layout
        displayManager = viewFinder.context
            .getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

        // Determine the output directory
        outputDirectory = getOutputDirectory(requireContext())

        viewFinder.post {

            displayId = viewFinder.display.displayId

            updateCameraUi()
            bindCameraUseCases()
        }
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)
        Timber.d("Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        // Set up the view finder use case to display camera preview
        val viewFinderConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)
            // We request aspect ratio but no resolution to let CameraX optimize our use cases
            setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        // Use the auto-fit preview builder to automatically handle size and orientation changes
        preview = AutoFitPreviewBuilder.build(viewFinderConfig, viewFinder)

        // Setup image analysis pipeline that computes average pixel luminance in real time
        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            setLensFacing(lensFacing)

            val actManager = context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)

            if (memInfo.totalMem / 1048576 < 900) {
                setTargetResolution(Size(800, 500))
            } else {
                setTargetResolution(Size(1800, 1000))
            }

            // Use a worker thread for image analysis to prevent preview glitches
            val analyzerThread = HandlerThread("BarcodeReader").apply { start() }
            setCallbackHandler(Handler(analyzerThread.looper))

            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)

            // call this again if rotation changes
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        imageAnalyzer = ImageAnalysis(analyzerConfig).apply {
            val googlePlayServicesAvailable = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context)
            if (googlePlayServicesAvailable == ConnectionResult.SUCCESS) {
                analyzer = BarcodeAnalyzer(context!!)
            } else {
                activity?.finish()
            }
        }

        // Apply declared configs to CameraX using the same lifecycle owner
        CameraX.bindToLifecycle(
            viewLifecycleOwner, preview, imageAnalyzer
        )

        preview!!.enableTorch(useFlashMode())
    }

    private fun updateCameraUi() {
        container.findViewById<ConstraintLayout>(R.id.camera_ui_container)?.let {
            container.removeView(it)
        }

        View.inflate(requireContext(), R.layout.preview_overlay, container)

        card_overlay.animate()
            .setStartDelay(1000)
            .alpha(0.0f)
            .setDuration(4000)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (card_overlay != null) {
                        card_overlay.visibility = GONE
                    }
                }
            })
    }

    companion object {
        const val CAPTURED_EVENT = "captured_event"
    }
}
