@file:Suppress("DEPRECATION")

package io.ffem.lite.ui

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.Camera
import android.hardware.Camera.PictureCallback
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import io.ffem.lite.R
import io.ffem.lite.common.Constants.DELAY_BETWEEN_SAMPLING
import io.ffem.lite.common.Constants.DELAY_INITIAL
import io.ffem.lite.common.Constants.SAMPLE_CROP_LENGTH_DEFAULT
import io.ffem.lite.common.Constants.SHORT_DELAY
import io.ffem.lite.common.Constants.SKIP_SAMPLING_COUNT
import io.ffem.lite.helper.SwatchHelper
import io.ffem.lite.helper.SwatchHelper.analyzeColor
import io.ffem.lite.model.Calibration
import io.ffem.lite.model.ColorInfo
import io.ffem.lite.model.ResultInfo
import io.ffem.lite.model.TestInfo
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.util.AlertUtil
import io.ffem.lite.util.ColorUtil
import io.ffem.lite.util.ImageUtil
import io.ffem.lite.util.SoundUtil.playShortResource
import java.util.*

open class BaseRunTest : Fragment(), RunTest {
    protected val countdown = intArrayOf(0)
    protected val results = ArrayList<ResultInfo>()
    private val delayHandler = Handler()
    private var isFlashOn: Boolean = false

    protected var cameraStarted = false

    @JvmField
    protected var pictureCount = 0
    protected var timeDelay = 0
    private var mHandler: Handler? = null
    private var alertDialogToBeDestroyed: AlertDialog? = null
    protected var testInfo: TestInfo? = null
    protected val model: TestInfoViewModel by activityViewModels()
    private var mCalibration: Calibration? = null
    private var dilution = 1
    protected var camera: Camera? = null
    private var resultListener: OnResultListener? = null
    protected var cameraPreview: CameraPreview? = null
    private val mRunnableCode = Runnable {
        if (pictureCount < AppPreferences.getSamplingTimes(requireContext())) {
            pictureCount++
            playShortResource(requireContext(), R.raw.beep)
            takePicture()
        } else {
            releaseResources()
        }
    }
    private val mPicture = PictureCallback { data, _ ->
        val bitmap = ImageUtil.getBitmap(data)
        getAnalyzedResult(bitmap)
        // test has time delay so take the pictures quickly with short delay
        if (testInfo!!.subTest().timeDelay > 0) {
            mHandler!!.postDelayed(mRunnableCode, (SHORT_DELAY * 1000).toLong())
        } else {
            mHandler!!.postDelayed(mRunnableCode, DELAY_BETWEEN_SAMPLING * 1000.toLong())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // disable the key guard when device wakes up and shake alert is displayed
        if (mCalibration != null && activity != null) {
            requireActivity().window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        super.onViewCreated(view, savedInstanceState)
    }

    protected open fun initializeTest() {
        pictureCount = 0
        testInfo = model.test.get()
        countdown[0] = 0
        results.clear()
        mHandler = Handler()
    }

    protected open fun setupCamera() {
        if (cameraPreview == null) {
            cameraPreview = CameraPreview(requireContext())
        }
        camera = cameraPreview!!.camera
        camera!!.startPreview()
        cameraPreview!!.setupCamera(camera!!)
    }

    protected fun stopPreview() {
        if (camera != null) {
            camera!!.stopPreview()
        }
        cameraStarted = false
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

    override fun onDetach() {
        super.onDetach()
        resultListener = null
    }

    private fun takePicture() {
        if (camera == null || !cameraStarted) {
            return
        }
        camera!!.startPreview()
        turnFlashOn()
        camera!!.takePicture(null, null, mPicture)
    }

    /**
     * Get the test result by analyzing the bitmap.
     *
     * @param bmp the bitmap of the photo taken during analysis
     */
    private fun getAnalyzedResult(bmp: Bitmap) {
        val bitmap = ImageUtil.rotateImage(requireActivity(), bmp)
        val croppedBitmap = ImageUtil.getCroppedBitmap(
            bitmap,
            SAMPLE_CROP_LENGTH_DEFAULT
        )
        //Extract the color from the photo which will be used for comparison
        //        if (mTestInfo!!.subTest().grayScale) {
//            croppedBitmap = ImageUtil.getGrayscale(croppedBitmap)
//        }
        val photoColor: ColorInfo =
            ColorUtil.getColorFromBitmap(croppedBitmap, SAMPLE_CROP_LENGTH_DEFAULT)
        if (mCalibration != null) {
            mCalibration!!.color = photoColor.color
            mCalibration!!.date = Date().time
        }

        val subTest = testInfo!!.subTest()
        val resultInfo = analyzeColor(
            testInfo!!,
            photoColor.color,
            subTest.colors,
            requireContext()
        )
//        resultInfo.sampleBitmap = bitmap
//        resultInfo.croppedBitmap = croppedBitmap
//        resultInfo.dilution = dilution

        results.add(resultInfo)
        if (resultListener != null && pictureCount >= AppPreferences.getSamplingTimes(requireContext())) {
            for (i in 0 until SKIP_SAMPLING_COUNT) {
                if (results.size > 1) {
                    results.removeAt(0)
                }
            }

            val result = SwatchHelper.getAverageResult(results, requireContext())
            testInfo!!.subTest().setResult(result)
            model.sampleColor = SwatchHelper.getAverageColor(results, requireContext())
            model.calibrationColor = model.sampleColor
            model.resultInfoList = results

//            model.currentResult = analyzeColor(
//                testInfo!!,
//                model.sampleColor, subTest.colors, requireContext()
//            )

            resultListener!!.onResult(mCalibration)
        }
    }

    override fun setCalibration(item: Calibration?) {
        mCalibration = item
    }

    override fun setDilution(dilution: Int) {
        this.dilution = dilution
    }

    private fun stopRepeatingTask() {
        if (mHandler != null) {
            mHandler!!.removeCallbacks(mRunnableCode)
        }
    }

    protected open fun startTest(timerSkipped: Boolean) {
        if (!cameraStarted) {
            playShortResource(requireActivity(), R.raw.beep)
            var initialDelay = 0
            //If the test has a time delay config then use that otherwise use standard delay
            if (testInfo!!.subTest().timeDelay < 5) {
                initialDelay = DELAY_INITIAL + DELAY_BETWEEN_SAMPLING
            } else if (timerSkipped) {
                initialDelay = DELAY_INITIAL
            }
//            binding!!.layoutWait.visibility = View.VISIBLE
            delayHandler.postDelayed(mRunnableCode, initialDelay * 1000.toLong())
            cameraStarted = true
        }
    }

    /**
     * Turn flash off.
     */
    protected fun turnFlashOff() {
        if (camera == null) {
            return
        }
        val parameters = camera!!.parameters
        val flashMode = Camera.Parameters.FLASH_MODE_OFF
        parameters.flashMode = flashMode
        try {
            camera!!.parameters = parameters
            isFlashOn = false
        } catch (_: Exception) {
        }
    }

    /**
     * Turn flash on.
     */
    protected fun turnFlashOn() {
        if (!AppPreferences.useCameraFlash(false)
            || camera == null || isFlashOn
        ) {
            return
        }
        val parameters = camera!!.parameters
        val flashMode = Camera.Parameters.FLASH_MODE_TORCH
        parameters.flashMode = flashMode
        try {
            camera!!.parameters = parameters
            isFlashOn = true
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Camera flash not available", Toast.LENGTH_SHORT
            ).show()
        }
    }

    protected open fun releaseResources() {
        isFlashOn = false
        cameraStarted = false
        pictureCount = 0
        timeDelay = 0
        if (camera != null) {
            camera!!.stopPreview()
            camera!!.setPreviewCallback(null)
            cameraPreview!!.holder.removeCallback(cameraPreview)
            camera!!.release()
            camera = null
        }
        if (cameraPreview != null) {
            cameraPreview!!.destroyDrawingCache()
            cameraPreview = null
        }
        delayHandler.removeCallbacksAndMessages(null)
        if (alertDialogToBeDestroyed != null) {
            alertDialogToBeDestroyed!!.dismiss()
        }
        stopRepeatingTask()
    }

    /**
     * Show an error message dialog.
     *
     * @param message the message to be displayed
     * @param bitmap  any bitmap image to displayed along with error message
     */
    fun showError(
        message: String,
        bitmap: Bitmap?,
        activity: Activity
    ) {
        stopScreenPinning()
        releaseResources()
        playShortResource(requireActivity(), R.raw.error)
        alertDialogToBeDestroyed = AlertUtil.showError(
            activity,
            R.string.error, message, bitmap, R.string.ok,
            { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
                activity.setResult(Activity.RESULT_CANCELED)
                stopScreenPinning()
                activity.finish()
            }, null, null
        )
    }

    private fun stopScreenPinning() {
        try {
            requireActivity().stopLockTask()
        } catch (ignored: Exception) {
        }
    }

    override fun onPause() {
        releaseResources()
        super.onPause()
    }

    interface OnResultListener {
        fun onResult(calibration: Calibration?)
    }

    protected fun getDummyResult(testInfo: TestInfo?) {
        if (model.isCalibration) {
            model.calibrationColor = randomColor()
        } else {
//            var maxDilution: Int = testInfo!!.getMaxDilution()
//            if (maxDilution == -1) {
//                maxDilution = 15
//            }
            var maxValue = 100.0
            for (result in testInfo!!.results) {
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

    companion object {
//        private fun timeConversion(sec: Int): String {
//            var seconds = sec
//            var minutes = seconds / SECONDS_IN_A_MINUTE
//            seconds -= minutes * SECONDS_IN_A_MINUTE
//            val hours = minutes / MINUTES_IN_AN_HOUR
//            minutes -= hours * MINUTES_IN_AN_HOUR
//            return String.format(Locale.US, "%02d", hours) + ":" +
//                    String.format(Locale.US, "%02d", minutes) + ":" +
//                    String.format(Locale.US, "%02d", seconds)
//        }
    }
}