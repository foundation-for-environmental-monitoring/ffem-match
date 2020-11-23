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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.app.App.Companion.SCAN_PROGRESS
import io.ffem.lite.common.CAPTURED_EVENT_BROADCAST
import io.ffem.lite.common.Constants.ANALYZER_IMAGE_MAX_WIDTH
import io.ffem.lite.common.Constants.IMAGE_CROP_PERCENTAGE
import io.ffem.lite.common.OVERLAY_UPDATE_BROADCAST
import io.ffem.lite.data.AppDatabase
import io.ffem.lite.data.TestResult
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.TestInfo
import io.ffem.lite.preference.*
import kotlinx.android.synthetic.main.fragment_camera.*
import kotlinx.android.synthetic.main.preview_overlay.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
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

    private lateinit var metrics: DisplayMetrics
    private var currentLuminosity: Int = -1
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var container: ConstraintLayout
    private lateinit var broadcastManager: LocalBroadcastManager

    private var lightSensor: Sensor? = null
    private lateinit var lightEventListener: SensorEventListener
    private lateinit var sensorManager: SensorManager
    private lateinit var mainExecutor: Executor
    private lateinit var cameraControl: CameraControl

    private var displayId: Int = -1
    private var preview: Preview? = null

    private lateinit var barcodeAnalyzer: BarcodeAnalyzer
    private lateinit var colorCardAnalyzer: ColorCardAnalyzer
    private val mainScope = MainScope()

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            val message = intent.getStringExtra(App.ERROR_MESSAGE)
            val scanProgress = intent.getIntExtra(SCAN_PROGRESS, 0)

            if (bottom_overlay != null && !message.isNullOrEmpty()) {
                bottom_overlay.setTextColor(Color.YELLOW)
                bottom_overlay.text = message
            } else {
                mainScope.cancel(null)
                mainScope.launch {
                    delay(2000)
                    if (bottom_overlay != null && bottom_overlay.text != getString(R.string.align_color_card)) {
                        bottom_overlay.setTextColor(Color.WHITE)
                        bottom_overlay.text = getString(R.string.align_color_card)
                    }
                }
            }
            progress_bar.progress = scanProgress
        }
    }

    private val overlayUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (scanner_ovr != null) {
                scanner_ovr.refreshOverlay(
                    colorCardAnalyzer.getPattern(),
                    container.measuredWidth,
                    max(
                        ANALYZER_IMAGE_MAX_WIDTH * IMAGE_CROP_PERCENTAGE,
                        requireActivity().window.decorView.height * IMAGE_CROP_PERCENTAGE
                    ).toInt(),
                    (requireActivity().window.decorView.height - container.measuredHeight) / 2
                )
            }
        }
    }

    private val capturedPhotoBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val testInfo = intent.getParcelableExtra<TestInfo>(App.TEST_INFO_KEY) ?: return
            if (!AppPreferences.isCalibration()) {
                val db: AppDatabase = AppDatabase.getDatabase(requireContext())
                db.resultDao().insert(
                    TestResult(
                        testInfo.fileName,
                        testInfo.uuid!!,
                        0,
                        testInfo.name!!,
                        Date().time,
                        -1.0,
                        -1.0,
                        0.0,
                        currentLuminosity,
                        ErrorType.NO_ERROR,
                        getSampleTestImageNumber()
                    )
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainExecutor = ContextCompat.getMainExecutor(requireContext())

        broadcastManager = LocalBroadcastManager.getInstance(requireContext())

        sensorManager = requireActivity().getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        if (lightSensor != null) {
            lightEventListener = object : SensorEventListener {
                override fun onSensorChanged(sensorEvent: SensorEvent) {
                    val value = sensorEvent.values[0].toInt()
                    if (currentLuminosity != value) {
                        currentLuminosity = value
                        val lux = getString(R.string.brightness) + ": $value"
                        luminosity_txt?.text = lux
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor, i: Int) {}
            }
        }
    }

    /**
     * Make sure that all permissions are still present, since user
     * could have removed them while the app was in paused state.
     */
    override fun onResume() {
        super.onResume()
        if (lightSensor != null) {
            sensorManager.registerListener(
                lightEventListener,
                lightSensor,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }
        broadcastManager.registerReceiver(broadcastReceiver, IntentFilter(App.ERROR_EVENT))
        broadcastManager.registerReceiver(
            capturedPhotoBroadcastReceiver,
            IntentFilter(CAPTURED_EVENT_BROADCAST)
        )

        broadcastManager.registerReceiver(
            overlayUpdateReceiver,
            IntentFilter(OVERLAY_UPDATE_BROADCAST)
        )

        lifecycleScope.launch {
            delay(300)
            startCamera()
        }

        if (take_photo_btn != null) {
            if (manualCaptureOnly()) {
                take_photo_btn.visibility = VISIBLE
            } else {
                take_photo_btn.visibility = GONE
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_camera, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view as ConstraintLayout
    }

    private fun startCamera() {
        camera_preview.post {
            displayId = view?.findViewById<PreviewView>(R.id.camera_preview)!!.display.displayId
            updateCameraUi()
            bindCameraUseCases()

            if (getSampleTestImageNumberInt() > -1) {
                message_txt.visibility = VISIBLE
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mainScope.cancel(null)
        cameraProvider.unbindAll()
        broadcastManager.unregisterReceiver(broadcastReceiver)
        broadcastManager.unregisterReceiver(capturedPhotoBroadcastReceiver)
        if (lightSensor != null) {
            sensorManager.unregisterListener(lightEventListener)
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
        metrics = DisplayMetrics().also { camera_preview.display.getRealMetrics(it) }
//        Timber.d("Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
//        Timber.d("Preview aspect ratio: $screenAspectRatio")

        val rotation = camera_preview.display.rotation

        // Bind the cameraProvider to the LifeCycleOwner
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({

            // CameraProvider
            cameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
                .setTargetName("Preview")
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build()
                .also {
                    it.setSurfaceProvider(camera_preview.surfaceProvider)
                }

            try {
                val analysis = ImageAnalysis.Builder()
                    .setTargetName("Analysis")
                    .setTargetAspectRatio(screenAspectRatio)
                    .setTargetRotation(rotation)
                    .setImageQueueDepth(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                if (useColorCardVersion2()) {
                    colorCardAnalyzer = ColorCardAnalyzer(requireContext())
                    colorCardAnalyzer.reset()
                    analysis.setAnalyzer(mainExecutor, colorCardAnalyzer)
                } else {
                    barcodeAnalyzer = BarcodeAnalyzer(requireContext())
                    barcodeAnalyzer.reset()
                    analysis.setAnalyzer(mainExecutor, barcodeAnalyzer)
                }

                // Must unbind use cases before rebinding them.
                cameraProvider.unbindAll()

                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, analysis
                )
                cameraControl = camera.cameraControl
                cameraControl.enableTorch(useFlashMode())

                camera_preview.afterMeasured {
                    val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                        camera_preview.width.toFloat(), camera_preview.height.toFloat()
                    )
                    val centerWidth = camera_preview.width.toFloat() / 2
                    val centerHeight = camera_preview.height.toFloat() / 5
                    //create a point on the center of the view
                    val autoFocusPoint = factory.createPoint(centerWidth, centerHeight)
                    cameraControl.startFocusAndMetering(
                        FocusMeteringAction.Builder(
                            autoFocusPoint,
                            FocusMeteringAction.FLAG_AF
                        ).apply {
                            //auto-focus every 1 seconds
                            setAutoCancelDuration(1, TimeUnit.SECONDS)
                        }.build()
                    )
                }

            } catch (e: Exception) {
                Timber.e(e)
            }

        }, mainExecutor)
    }

    /**
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
        camera_ui_container.let {
            container.removeView(it)
        }

        View.inflate(requireContext(), R.layout.preview_overlay, container)

        if (manualCaptureOnly()) {
            take_photo_btn.visibility = VISIBLE
            take_photo_btn.setOnClickListener {
                barcodeAnalyzer.takePhoto()
            }
        } else {
            take_photo_btn.visibility = GONE
        }

        if (useColorCardVersion2()) {
            card_overlay.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.card_2_overlay
                )
            )
        } else {
            card_overlay.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.card_overlay
                )
            )
        }

        card_overlay.animate()
            .setStartDelay(100)
            .alpha(0.0f)
            .setDuration(8000)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (card_overlay != null) {
                        card_overlay.visibility = View.INVISIBLE
                    }
                }
            })
    }

    private inline fun View.afterMeasured(crossinline block: () -> Unit) {
        viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (measuredWidth > 0 && measuredHeight > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    block()
                }
            }
        })
    }

    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}
