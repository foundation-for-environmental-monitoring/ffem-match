@file:Suppress("DEPRECATION")

package io.ffem.lite.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.ffem.lite.R
import io.ffem.lite.common.BROADCAST_TEST_COMPLETED
import io.ffem.lite.common.Constants
import io.ffem.lite.databinding.FragmentRunAboveTestBinding
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.util.SoundUtil
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

class CuvetteBelowFragment : BaseRunTest(), RunTest {
    private var ignoreShake = false
    private var waitingForStillness = false

    private var _binding: FragmentRunAboveTestBinding? = null
    private val b get() = _binding!!

    private lateinit var broadcastManager: LocalBroadcastManager

    private var mainScope = MainScope()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRunAboveTestBinding.inflate(inflater, container, false)
        initializeTest()
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        b.previewButton.setOnClickListener {
            b.customResultButton.visibility = GONE
            b.dummyResultButton.visibility = GONE
            b.previewButton.visibility = GONE
            setupCamera()
            b.analyzeButton.visibility = VISIBLE
            if (testInfo!!.subTest().timeDelay > 10) {
                b.startTimerBtn.visibility = VISIBLE
                b.skipTimerButton.visibility = VISIBLE
                b.analyzeButton.visibility = GONE
            }
        }

        b.analyzeButton.setOnClickListener {
            b.buttonsLayout.visibility = GONE
            b.previewButton.visibility = GONE
            b.customResultButton.visibility = GONE
            b.dummyResultButton.visibility = GONE
            b.startTimerBtn.visibility = GONE
            b.analyzeButton.visibility = GONE
            b.skipTimerButton.visibility = GONE
            startTest(true)
        }

        b.skipTimerButton.setOnClickListener {
            b.buttonsLayout.visibility = GONE
            b.customResultButton.visibility = GONE
            b.dummyResultButton.visibility = GONE
            b.previewButton.visibility = GONE
            b.startTimerBtn.visibility = GONE
            b.analyzeButton.visibility = GONE
            b.skipTimerButton.visibility = GONE
            startTest(true)
        }

        b.startTimerBtn.setOnClickListener {
            b.buttonsLayout.visibility = GONE
            b.cameraView.visibility = VISIBLE
            b.startTimerBtn.visibility = GONE
            b.skipTimerButton.visibility = GONE
            stopPreview()
            turnFlashOff()
            if (camera != null) {
                camera!!.stopPreview()
                camera!!.setPreviewCallback(null)
                cameraPreview!!.holder.removeCallback(cameraPreview)
                camera!!.release()
                camera = null
            }
            if (cameraPreview != null) {
                b.cameraView.removeAllViews()
                cameraPreview!!.destroyDrawingCache()
                cameraPreview = null
            }
            cameraStarted = false

            mainScope.cancel()
            mainScope = MainScope()
            countdown[0] = 0
            if (model.test.get()!!.subTest().timeDelay > 10) {
                waitingForStillness = false
                timeDelay =
                    max(Constants.SHORT_DELAY, model.test.get()!!.subTest().timeDelay)
                setCountDown()
            } else {
                b.cameraLyt.visibility = INVISIBLE
                waitingForStillness = true
                dismissShakeAndStartTest()
            }
        }

        broadcastManager = LocalBroadcastManager.getInstance(requireContext())

        if (AppPreferences.returnDummyResults(requireContext()) && activity is TestActivity) {
            if (model.isCalibration) {
                b.customResultButton.visibility = GONE
            } else {
                b.customResultButton.visibility = VISIBLE
                b.customResultButton.setOnClickListener {
                    var inputValue: String

                    val builder = MaterialAlertDialogBuilder(requireContext())
                    val customAlertDialogView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.dialog_calibration_point, getView() as ViewGroup?, false)
                    val input = customAlertDialogView.findViewById<View>(R.id.input) as EditText
                    input.showSoftInputOnFocus = true
                    builder.setView(customAlertDialogView)
                        .setTitle("Dummy Result")
                        .setMessage("Enter a dummy value")
                        .setPositiveButton(android.R.string.ok) { dialog, _ ->
                            inputValue = input.text.toString()
                            for (result in testInfo!!.results) {
                                result.setResult(inputValue.toDouble())
                            }

                            val intent = Intent(BROADCAST_TEST_COMPLETED)
                            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(
                                intent
                            )

                            dialog.dismiss()
                            hideSystemUI()
                        }
                        .setNegativeButton(R.string.cancel) { dialog, _ ->
                            dialog.dismiss()
                            hideSystemUI()
                        }
                        .show()
                    input.showSoftInputOnFocus = true
                    input.post {
                        input.requestFocus()
                        val imm =
                            input.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(
                            input,
                            InputMethodManager.SHOW_IMPLICIT
                        )
                    }
                }
            }

            b.dummyResultButton.visibility = VISIBLE
            b.dummyResultButton.setOnClickListener {
                getDummyResult(model.test.get())
                val intent = Intent(BROADCAST_TEST_COMPLETED)
                LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(
                    intent
                )
            }
        }
    }

    private fun hideSystemUI() {
        activity?.window?.decorView?.systemUiVisibility =
            (SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or SYSTEM_UI_FLAG_FULLSCREEN)
    }

    override fun setupCamera() {
        super.setupCamera()
        b.cameraView.visibility = VISIBLE
        if (b.cameraView.childCount == 0) {
            b.cameraView.addView(cameraPreview)
        }
        turnFlashOn()
    }

    override fun initializeTest() {
        super.initializeTest()
        pictureCount = 0
        ignoreShake = false
        waitingForStillness = true
    }

    override fun onResume() {
        if (AppPreferences.returnDummyResults(requireContext()) && activity is TestActivity) {
            if (model.isCalibration) {
                b.customResultButton.visibility = GONE
            } else {
                b.customResultButton.visibility = VISIBLE
            }
            b.dummyResultButton.visibility = VISIBLE
        }
        b.buttonsLayout.visibility = VISIBLE
        b.analyzeButton.visibility = GONE
        b.skipTimerButton.visibility = GONE
        b.startTimerBtn.visibility = GONE
        b.timerLayout.visibility = GONE
        b.countdownTmr.visibility = GONE
        b.cameraView.visibility = VISIBLE
        b.previewButton.visibility = VISIBLE
        b.cameraLyt.visibility = VISIBLE
        waitingForStillness = true
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        b.timerLayout.visibility = GONE
        b.countdownTmr.visibility = GONE
        b.analyzeButton.visibility = GONE
        b.skipTimerButton.visibility = GONE
    }

    private fun dismissShakeAndStartTest() {
        b.cameraLyt.visibility = VISIBLE
        b.cameraView.visibility = VISIBLE
        b.circleView.visibility = VISIBLE
        startTest(true)
    }

    override fun startTest(timerSkipped: Boolean) {
        if (!cameraStarted) {
            setupCamera()
            camera!!.startPreview()
        }
        super.startTest(timerSkipped)
    }

    override fun releaseResources() {
        b.cameraView.removeAllViews()
        mainScope.cancel()
        super.releaseResources()
    }

    private val mCountdown = Runnable { setCountDown() }
    private fun setCountDown() {
        if (countdown[0] < timeDelay) {
            b.analyzeButton.visibility = GONE
            b.previewButton.visibility = GONE
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
            mainScope.launch {
                delay(1000)
                mCountdown.run()
            }
        } else {
            b.analyzeButton.visibility = GONE
            b.timerLayout.visibility = GONE
            b.countdownTmr.visibility = GONE
            b.cameraLyt.visibility = INVISIBLE
            waitingForStillness = true
            dismissShakeAndStartTest()
        }
    }
}