package io.ffem.lite.camera

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.button.MaterialButton
import io.ffem.lite.R
import io.ffem.lite.common.*
import io.ffem.lite.data.AppDatabase
import io.ffem.lite.data.TestResult
import io.ffem.lite.databinding.FragmentCameraBinding
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.TestInfo
import io.ffem.lite.preference.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executor
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
class CameraFragment : Fragment() {
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private lateinit var metrics: DisplayMetrics
    private var currentLuminosity: Int = -1
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var container: ConstraintLayout
    private lateinit var broadcastManager: LocalBroadcastManager

    private var lightSensor: Sensor? = null
    private lateinit var lightEventListener: SensorEventListener
    private lateinit var sensorManager: SensorManager
    private lateinit var mainExecutor: Executor
    private lateinit var executorService: ExecutorService
    private lateinit var cameraControl: CameraControl

    private var displayId: Int = -1
    private var preview: Preview? = null

    private lateinit var barcodeAnalyzer: BarcodeAnalyzer
    private lateinit var colorCardAnalyzer: ColorCardAnalyzer
    private val mainScope = MainScope()

    private var luminosityTextView: TextView? = null
    private var messageText: TextView? = null
    private var takePhotoButton: MaterialButton? = null
    private var cameraContainer: ConstraintLayout? = null
    private var cardOverlay: AppCompatImageView? = null
    private var progressBar: ProgressBar? = null

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            val message = intent.getStringExtra(ERROR_MESSAGE)
            val scanProgress = intent.getIntExtra(SCAN_PROGRESS, 0)
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
            val progressBar = view?.findViewById<ProgressBar>(R.id.progress_bar)
            progressBar?.progress = scanProgress
        }
    }

    private val capturedPhotoBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val testInfo = intent.getParcelableExtra<TestInfo>(TEST_INFO_KEY) ?: return
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
                        testInfo.maxValue,
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
        broadcastManager.registerReceiver(broadcastReceiver, IntentFilter(ERROR_EVENT_BROADCAST))

        if (useColorCardVersion1()) {
            broadcastManager.registerReceiver(
                capturedPhotoBroadcastReceiver,
                IntentFilter(CAPTURED_EVENT_BROADCAST)
            )
        }

        lifecycleScope.launch {
            delay(300)
            startCamera()
        }

        if (takePhotoButton != null) {
            if (manualCaptureOnly()) {
                takePhotoButton?.visibility = VISIBLE
            } else {
                takePhotoButton?.visibility = GONE
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view as ConstraintLayout
    }

    private fun startCamera() {
        binding.cameraPreview.post {
            displayId = binding.cameraPreview.display.displayId
            updateCameraUi()
            bindCameraUseCases()

            if (getSampleTestImageNumberInt() > -1) {
                messageText?.visibility = VISIBLE
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
        metrics = DisplayMetrics().also { binding.cameraPreview.display?.getRealMetrics(it) }
//        Timber.d("Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
//        Timber.d("Preview aspect ratio: $screenAspectRatio")

        val rotation = binding.cameraPreview.display?.rotation

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
                    it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                }

            try {
                val analysis = if (metrics.widthPixels <= 480) {
                    ImageAnalysis.Builder()
                        .setTargetRotation(rotation)
                        .build()
                } else {
                    ImageAnalysis.Builder()
                        .setTargetAspectRatio(screenAspectRatio)
                        .setTargetRotation(rotation)
                        .build()
                }

                if (useColorCardVersion1()) {
                    barcodeAnalyzer = BarcodeAnalyzer(requireContext())
                    barcodeAnalyzer.reset()
                    analysis.setAnalyzer(executorService, barcodeAnalyzer)
                } else {
                    colorCardAnalyzer = ColorCardAnalyzer(requireContext())
                    colorCardAnalyzer.previewHeight = binding.cameraPreview.measuredHeight
                    colorCardAnalyzer.viewFinderHeight = cardOverlay!!.measuredHeight
                    colorCardAnalyzer.previewWidth = metrics.widthPixels
                    colorCardAnalyzer.reset()
                    progressBar!!.visibility = GONE
                    analysis.setAnalyzer(executorService, colorCardAnalyzer)
                }

                // Must unbind use cases before rebinding them.
                cameraProvider.unbindAll()

                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, analysis
                )
                cameraControl = camera.cameraControl
                cameraControl.setLinearZoom(0f)
                cameraControl.enableTorch(useFlashMode())

                binding.cameraPreview.afterMeasured {
                    val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                        binding.cameraPreview.width.toFloat(),
                        binding.cameraPreview.height.toFloat()
                    )
                    val centerWidth = binding.cameraPreview.width.toFloat() / 2
                    val centerHeight = binding.cameraPreview.height.toFloat() / 5
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
        cameraContainer.let {
            container.removeView(it)
        }

        val view = View.inflate(requireContext(), R.layout.preview_overlay, container)

        messageText = view.findViewById(R.id.message_txt)
        progressBar = view.findViewById(R.id.progress_bar)
        luminosityTextView = view.findViewById(R.id.luminosity_txt)
        takePhotoButton = view.findViewById(R.id.take_photo_btn)
        cameraContainer = view.findViewById(R.id.camera_ui_container)
        cardOverlay = view.findViewById(R.id.card_overlay)

        if (manualCaptureOnly()) {
            takePhotoButton!!.visibility = VISIBLE
            takePhotoButton!!.setOnClickListener {
                barcodeAnalyzer.takePhoto()
            }
        } else {
            takePhotoButton!!.visibility = GONE
        }

        if (useColorCardVersion1()) {
            cardOverlay!!.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.card_overlay
                )
            )
            cardOverlay!!.animate()
                .setStartDelay(100)
                .alpha(0.0f)
                .setDuration(8000)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (cardOverlay != null) {
                            cardOverlay!!.visibility = View.INVISIBLE
                        }
                    }
                })
        } else {
            cardOverlay!!.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.card_overlay_2
                )
            )
        }
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
