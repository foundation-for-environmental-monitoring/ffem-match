package io.ffem.lite.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.ffem.lite.common.BROADCAST_TEST_COMPLETED
import io.ffem.lite.common.Constants
import io.ffem.lite.helper.SwatchHelper
import io.ffem.lite.model.ResultInfo
import io.ffem.lite.model.TestInfo
import io.ffem.lite.preference.AppPreferences
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import timber.log.Timber
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias ColorListener = (results: ArrayList<ResultInfo>, color: Int) -> Unit

open class BaseRunTestFragment : Fragment() {
    protected val countdown = intArrayOf(0)
    protected lateinit var colorAnalyzer: ColorAnalyzer
    protected lateinit var viewFinder: PreviewView
    protected var testInfo: TestInfo? = null
    protected val model: TestInfoViewModel by activityViewModels()

    protected var cameraStarted = false

    private var mainScope = MainScope()

    protected var timeDelay = 0
    private var mHandler: Handler? = null
    private var resultListener: OnResultListener? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var broadcastManager: LocalBroadcastManager
    private var testScope = MainScope()

    private val testCompletedBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            stopCamera()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        broadcastManager = LocalBroadcastManager.getInstance(requireContext())
        broadcastManager.registerReceiver(
            testCompletedBroadcastReceiver,
            IntentFilter(BROADCAST_TEST_COMPLETED)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        initializeTest()
    }

    protected open fun initializeTest() {
        testInfo = model.test.get()
        countdown[0] = 0
        mHandler = Handler()
    }

    protected open fun stopCamera() {
        cameraProvider?.unbindAll()
    }

    protected fun stopPreview() {
        stopCamera()
        cameraStarted = false
    }

    protected fun startCamera(start: Boolean) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            colorAnalyzer = ColorAnalyzer(
                model.test.get()!!,
                { results: ArrayList<ResultInfo>, _: Int ->

                    releaseResources()

                    val testInfo = model.test.get()!!
                    for (i in 0 until Constants.SKIP_SAMPLING_COUNT) {
                        if (results.size > 1) {
                            results.removeAt(0)
                        }
                    }
                    val result = SwatchHelper.getAverageResult(results, requireContext())
                    model.sampleColor =
                        SwatchHelper.getAverageColor(results, requireContext())
                    model.calibrationColor = model.sampleColor
                    model.resultInfoList = results

//                    val subTest = testInfo.subTest()
//                    model.currentResult = SwatchHelper.analyzeColor(
//                        testInfo, model.sampleColor, subTest.colors, requireContext()
//                    )

                    testInfo.subTest().setResult(result)

                    if (resultListener != null) {
                        resultListener!!.onResult()
                    }

                }, requireContext()
            )
            colorAnalyzer.reset()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, colorAnalyzer)
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider?.unbindAll()

                // Bind use cases to camera
                val camera = cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )

                val cameraControl = camera?.cameraControl
                cameraControl?.setLinearZoom(0f)
                val useTorch = AppPreferences.useCameraFlash(false)
                if (useTorch) {
                    if (camera?.cameraInfo!!.hasFlashUnit()) {
                        cameraControl?.enableTorch(true)
                    } else {
                        Toast.makeText(
                            context,
                            "Camera flash not available", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (exc: Exception) {
                Timber.e(exc)
            }

            if (start) {
                colorAnalyzer.takePhoto()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        resultListener = if (context is OnResultListener) {
            context
        } else {
            throw IllegalArgumentException(
                context.toString()
                        + " must implement OnResultListener"
            )
        }
    }

    protected open fun releaseResources() {
        testScope.cancel()
        mainScope.cancel()
    }

    override fun onPause() {
        super.onPause()
        testScope.cancel()
        mainScope.cancel()
        cameraProvider?.unbindAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }

    interface OnResultListener {
        fun onResult()
    }

    protected fun getDummyResult(testInfo: TestInfo?) {
        if (model.isCalibration) {
            model.calibrationColor = randomColor()
        } else {
            var maxDilution: Int = testInfo!!.getMaxDilution()
            if (maxDilution == -1) {
                maxDilution = 15
            }
            var maxValue = 100.0
            for (result in testInfo.results) {
                val random = Random()
                if (result.colors.size > 0) {
                    maxValue = result.colors[result.colors.size - 1].value
                }
                val randomResult = random.nextDouble() * maxValue
                result.setResult(randomResult)

                for (color in result.colors) {
                    color.color = randomColor()
                }
            }
        }
    }

    private fun randomColor(): Int {
        val random = Random()
        val red = random.nextInt(256)
        val green = random.nextInt(256)
        val blue = random.nextInt(256)
        return Color.rgb(red, green, blue)
    }
}