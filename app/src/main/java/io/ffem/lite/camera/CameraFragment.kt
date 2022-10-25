package io.ffem.lite.camera

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.button.MaterialButton
import io.ffem.lite.R
import io.ffem.lite.common.Constants
import io.ffem.lite.common.ERROR_EVENT_BROADCAST
import io.ffem.lite.common.ERROR_MESSAGE
import io.ffem.lite.databinding.FragmentCameraBinding
import io.ffem.lite.model.TestInfo
import io.ffem.lite.preference.AppPreferences.getSampleTestImageNumberInt
import io.ffem.lite.preference.AppPreferences.useCameraFlash
import io.ffem.lite.ui.TestInfoViewModel
import io.ffem.lite.util.SoundUtil
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
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
open class CameraFragment : Fragment() {
    private lateinit var camera: Camera
    private var _binding: FragmentCameraBinding? = null
    private val b get() = _binding!!
    protected val model: TestInfoViewModel by activityViewModels()

    private lateinit var metrics: DisplayMetrics
    private var currentLuminosity: Int = -1
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var container: ConstraintLayout
    private var broadcastManager: LocalBroadcastManager? = null

    private var lightSensor: Sensor? = null
    private lateinit var lightEventListener: SensorEventListener
    private lateinit var sensorManager: SensorManager
    private lateinit var executorService: ExecutorService
    private lateinit var cameraControl: CameraControl

    private var displayId: Int = -1
    private var preview: Preview? = null

    private lateinit var colorCardAnalyzer: ColorCardAnalyzerBase
    private val mainScope = MainScope()

    private var luminosityTextView: TextView? = null
    private var messageText: TextView? = null
    private var takePhotoButton: MaterialButton? = null
    private var cameraContainer: ConstraintLayout? = null
    private var cardOverlay: AppCompatImageView? = null

    private val countdown = intArrayOf(0)
    private var timeDelay = 0
    private var cancelled = false

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            val message = intent.getStringExtra(ERROR_MESSAGE)
            val messageOverlay = view?.findViewById<TextView>(R.id.message_overlay)

            if (messageOverlay != null && !message.isNullOrEmpty()) {
                messageOverlay.text = message
                messageOverlay.visibility = VISIBLE
            } else {
                mainScope.cancel(null)
                mainScope.launch {
                    delay(2000)
                    if (messageOverlay != null) {
                        messageOverlay.visibility = GONE
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        executorService = Executors.newFixedThreadPool(1)

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
                        luminosityTextView?.text = lux
                        luminosityTextView?.visibility = VISIBLE
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
        cancelled = false
        if (lightSensor != null) {
            sensorManager.registerListener(
                lightEventListener,
                lightSensor,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }
        broadcastManager!!.registerReceiver(broadcastReceiver, IntentFilter(ERROR_EVENT_BROADCAST))
//        if (AppPreferences.returnDummyResults(requireContext()) && activity is TestActivity) {
//            if (model.isCalibration) {
//                b.customResultButton.visibility = GONE
//            } else {
//                b.customResultButton.visibility = VISIBLE
//            }
//            b.dummyResultButton.visibility = VISIBLE
//        }

        b.cameraLyt.visibility = INVISIBLE
        b.analyzeButton.visibility = GONE
        b.skipTimerButton.visibility = GONE
        b.analyzeButton.visibility = GONE
        b.startTimerBtn.visibility = GONE
        b.timerLayout.visibility = GONE
        b.buttonsLayout.visibility = VISIBLE
//        b.previewButton.visibility = VISIBLE

        if (model.test.get() != null && model.test.get()!!.subTest().timeDelay > 10) {
            b.startTimerBtn.visibility = VISIBLE
            b.skipTimerButton.visibility = VISIBLE
            b.timerLayout.visibility = VISIBLE
            timeDelay = max(Constants.SHORT_DELAY, model.test.get()!!.subTest().timeDelay)
            b.countdownTmr.alpha = 0.2f
            b.countdownTmr.setProgress(timeDelay, 60)
            b.analyzeButton.visibility = GONE
        } else {
            lifecycleScope.launch {
//                if (isVisible) {
                delay(300)
                startCamera()
//                }
            }
        }
        super.onResume()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        b.cameraPreview.layoutParams.height = requireActivity().window.decorView.height

//        b.previewButton.setOnClickListener {
//            b.cameraLyt.visibility = VISIBLE
//            b.timerLayout.visibility = GONE
//            b.previewButton.visibility = GONE
//            b.analyzeButton.visibility = VISIBLE
//            if (model.test.get()!!.subTest().timeDelay > 10) {
//                b.startTimerBtn.visibility = VISIBLE
//                b.skipTimerButton.visibility = VISIBLE
//                b.analyzeButton.visibility = GONE
//            }
//
//            testScope = MainScope()
//            testScope.launch {
//                delay(100)
//                try {
//                    startCamera()
//                } catch (e: Exception) {
//                }
//                delay(1000)
//            }
//
//        }

        b.analyzeButton.setOnClickListener {
            startTest()
            b.analyzeButton.visibility = GONE
        }

        b.skipTimerButton.setOnClickListener {
            b.timerLayout.visibility = GONE
            b.buttonsLayout.visibility = GONE
//            b.previewButton.visibility = GONE
            b.startTimerBtn.visibility = GONE
            b.analyzeButton.visibility = GONE
            b.skipTimerButton.visibility = GONE
            startTest()
        }

        b.startTimerBtn.setOnClickListener {
            b.buttonsLayout.visibility = GONE
            b.cameraLyt.visibility = VISIBLE
            b.startTimerBtn.visibility = GONE
            b.skipTimerButton.visibility = GONE
            b.countdownTmr.alpha = 1f
//            stopPreview()
//            cameraStarted = false

            countdown[0] = 0
            if (model.test.get()!!.subTest().timeDelay > 10) {
                timeDelay = max(Constants.SHORT_DELAY, model.test.get()!!.subTest().timeDelay)
                setCountDown()
            } else {
                b.cameraLyt.visibility = INVISIBLE
            }
        }
        return b.root
    }

    private fun startTest() {
        startCamera()
    }

    private val mCountdown = Runnable { setCountDown() }
    private fun setCountDown() {
        if (countdown[0] < timeDelay) {
            b.analyzeButton.visibility = GONE
//            b.previewButton.visibility = GONE
            b.timerLayout.visibility = VISIBLE
            b.countdownTmr.visibility = VISIBLE
            b.cameraLyt.visibility = INVISIBLE
            countdown[0]++
            if (timeDelay > 10) {
                if (timeDelay - countdown[0] < 31) {
                    SoundUtil.playShortResource(requireContext(), R.raw.beep)
                } else if ((timeDelay - countdown[0]) % 15 == 0) {
                    SoundUtil.playShortResource(requireContext(), R.raw.beep)
                }
            }

            b.countdownTmr.setProgress(timeDelay - countdown[0], 60)
            lifecycleScope.launch {
                delay(1000)
                if (!cancelled) {
                    mCountdown.run()
                }
            }
        } else {
            b.analyzeButton.visibility = GONE
            b.timerLayout.visibility = GONE
            b.cameraLyt.visibility = INVISIBLE
            startTest()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view as ConstraintLayout
    }

    private fun startCamera() {
        b.cameraLyt.visibility = VISIBLE
        b.cameraPreview.visibility = VISIBLE
        b.cameraPreview.post {
            displayId = b.cameraPreview.display.displayId
            updateCameraUi()
            bindCameraUseCases()

            if (getSampleTestImageNumberInt() > -1) {
                messageText?.visibility = VISIBLE
            }
        }
    }

    override fun onPause() {
        cancelled = true

        val useTorch = useCameraFlash(true)
        if (useTorch) {
            if (::camera.isInitialized) {
                try {
                    cameraControl.enableTorch(false)
                } catch (_: Exception) {
                }
            }
        }

        if (::cameraProvider.isInitialized) {
            cameraProvider.unbindAll()
        }
        // Remove previous UI if any
        try {
            cameraContainer.let {
                container.removeView(it)
            }
        } catch (_: Exception) {
        }
        super.onPause()
        mainScope.cancel(null)
        broadcastManager!!.unregisterReceiver(broadcastReceiver)
        if (lightSensor != null) {
            sensorManager.unregisterListener(lightEventListener)
        }
        b.cameraLyt.visibility = INVISIBLE
        b.timerLayout.visibility = GONE
        b.analyzeButton.visibility = GONE
        b.skipTimerButton.visibility = GONE
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
        metrics = DisplayMetrics().also { b.cameraPreview.display?.getRealMetrics(it) }
//        Timber.d("Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
//        Timber.d("Preview aspect ratio: $screenAspectRatio")

        val rotation = b.cameraPreview.display?.rotation

        // Bind the cameraProvider to the LifeCycleOwner
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({

            // CameraProvider
            cameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation!!)
                .build()
                .also {
                    it.setSurfaceProvider(b.cameraPreview.surfaceProvider)
                }

            try {
                val analysis = if (metrics.widthPixels <= 550) {
                    ImageAnalysis.Builder()
                        .setTargetRotation(rotation)
                        .build()
                } else {
                    ImageAnalysis.Builder()
                        .setTargetAspectRatio(screenAspectRatio)
                        .setTargetRotation(rotation)
                        .build()
                }

                colorCardAnalyzer =
                    CircleColorCardAnalyzer(model.test.get() ?: TestInfo(), requireContext())

                colorCardAnalyzer.reset()
                analysis.setAnalyzer(executorService, colorCardAnalyzer)

                // Must unbind use cases before rebinding them.
                cameraProvider.unbindAll()

                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, analysis
                )
                cameraControl = camera.cameraControl
                cameraControl.setLinearZoom(0f)
                if (useCameraFlash(true)) {
                    try {
                        cameraControl.enableTorch(true)
                    } catch (_: Exception) {
                        Toast.makeText(
                            context,
                            "Camera flash not available", Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                b.cameraPreview.afterMeasured {
                    val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                        b.cameraPreview.width.toFloat(),
                        b.cameraPreview.height.toFloat()
                    )
                    val centerWidth = b.cameraPreview.width.toFloat() / 2
                    val centerHeight = b.cameraPreview.height.toFloat() / 5
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
        }, ContextCompat.getMainExecutor(requireContext()))
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
        cameraContainer.let {
            container.removeView(it)
        }

        val view = inflate(requireContext(), R.layout.preview_overlay, container)

        messageText = view.findViewById(R.id.message_txt)
        luminosityTextView = view.findViewById(R.id.luminosity_txt)
        takePhotoButton = view.findViewById(R.id.take_photo_btn)
        cameraContainer = view.findViewById(R.id.camera_ui_container)
        cardOverlay = view.findViewById(R.id.card_overlay)

        cardOverlay!!.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.preview_circle_overlay
            )
        )
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}
