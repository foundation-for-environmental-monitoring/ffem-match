package io.ffem.lite.ui

import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.widget.Toolbar
import androidx.core.animation.doOnEnd
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.FirebaseDatabase
import io.ffem.lite.R
import io.ffem.lite.common.Constants
import io.ffem.lite.data.DataHelper
import io.ffem.lite.databinding.FragmentSensorBinding
import io.ffem.lite.model.FactoryConfig
import io.ffem.lite.model.PulseWidth
import io.ffem.lite.model.Result
import io.ffem.lite.model.TestInfo
import io.ffem.lite.util.ColorUtil
import io.ffem.lite.util.WidgetUtil.setStatusColor
import io.ffem.lite.util.toLocalString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import kotlin.concurrent.timerTask

open class SensorFragmentBase : Fragment() {
    private var _binding: FragmentSensorBinding? = null
    val b get() = _binding!!
    private val model: TestInfoViewModel by activityViewModels()

    private var submitResultListener: OnSubmitResultListener? = null
    private lateinit var timer: Timer

    private var externalSensorId = ""
    var currentPulseWidth = ""
    private var emptyPulseWidth = ""

    private var emptyCalibration: FactoryConfig? = null
    private var masterCalibration: FactoryConfig? = null
    private var standardCalibration: PulseWidth? = PulseWidth()

    private var emptyCalibrated = false
    private var analysisStarted = false
    private var analysisCompleted = false
    private var startStandardCalibration = false
    private var standardCalibrationCompleted = false

    private var minValue: Double = 0.0
    private var maxValue: Double = 0.0

    private fun getToolbar(): Toolbar? {
        return requireView().findViewById(R.id.toolbar)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSensorBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        if (activity is TestActivity) {
//            view.findViewById<AppBarLayout>(R.id.appBarLayout).visibility = GONE
//        }
        super.onViewCreated(view, savedInstanceState)

        val testInfo = model.test.get()!!
        if (getToolbar() != null) {
            getToolbar()!!.title = testInfo.name?.toLocalString()
        }

        b.emptyCalibrateButton.setOnClickListener {
            analysisCompleted = false
            standardCalibrationCompleted = false
            emptyCalibrated = false
            b.emptyCalibrateButton.visibility = GONE
            b.sampleLayout.visibility = GONE
            b.calibrateProgress.visibility = VISIBLE
            startSensor()
        }

        b.skipButton.setOnClickListener {
            if (b.skipButton.isEnabled) {
                val expandAnimator = ValueAnimator
                    .ofInt(b.standardCalibrateLayout.height, 0)
                    .setDuration(200)

                expandAnimator.addUpdateListener {
                    val value = it.animatedValue as Int
                    b.standardCalibrateLayout.layoutParams.height = value
                    b.standardCalibrateLayout.requestLayout()
                }

                expandAnimator.doOnEnd {
                    b.standardCalibrateLayout.visibility = GONE
                }

                expandAnimator.start()

                b.analyzeTitle.text = "3. Analyze "
                enableAnalysis()
            }
        }

        b.standardCalibrateButton.setOnClickListener {
            var inputValue: String

            val builder = MaterialAlertDialogBuilder(requireContext())
            val customAlertDialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_calibration_point, getView() as ViewGroup?, false)
            val input = customAlertDialogView.findViewById<View>(R.id.input) as EditText
            input.showSoftInputOnFocus = true
            builder.setView(customAlertDialogView)
                .setTitle("Calibration point")
                .setMessage("Enter standard value ($minValue - $maxValue)")
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    b.skipButton.visibility = GONE
                    inputValue = input.text.toString()
                    standardCalibration!!.value = inputValue.toDouble()
                    if (standardCalibration!!.value < minValue) {
                        standardCalibration!!.value = minValue
                    } else if (standardCalibration!!.value > maxValue) {
                        standardCalibration!!.value = maxValue
                    }

                    b.standardValueText.text = standardCalibration!!.value.toString()
                    b.standardValueLayout.visibility = VISIBLE
                    dialog.dismiss()
                    analysisCompleted = false
                    emptyCalibrated = true
                    b.standardCalibrateButton.visibility = GONE
                    b.sampleLayout.visibility = GONE
                    b.standardCalibrateProgress.visibility = VISIBLE
                    startSensor()
                    startStandardCalibration = true
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

        b.analyzeButton.setOnClickListener {
            analysisCompleted = false
            analysisStarted = true
            b.analyzeButton.visibility = GONE
            b.analyzeProgress.visibility = VISIBLE
            startSensor()
        }
    }

    private fun hideSystemUI() {
        activity?.window?.decorView?.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    private fun setExternalSensorId(value: String) {
        if (value.isEmpty()) {
            this.externalSensorId = "FM-ARDTCS"
        }
    }

    fun startTimer() {
        if (::timer.isInitialized) {
            timer.cancel()
        }
        timer = Timer()
        timer.schedule(timerTask {
            when {
                currentPulseWidth.isNotEmpty() && externalSensorId.isNotEmpty() -> {
                    timer.cancel()
                    if (emptyCalibrated) {

                        if (!standardCalibrationCompleted) {
                            val aPulseWidthArray = currentPulseWidth.split(",")
                            val aPulse = ArrayList<Int>()
                            aPulse.add(aPulseWidthArray[0].toInt())
                            aPulse.add(aPulseWidthArray[1].toInt())
                            aPulse.add(aPulseWidthArray[2].toInt())
                            aPulse.add(aPulseWidthArray[3].toInt())
                            standardCalibration!!.a = aPulse

                            MainScope().launch(Dispatchers.Main) {
                                try {
                                    b.standardCalibrateText.text =
                                        currentPulseWidth.replace(",", ", ")
                                    enableAnalysis()
                                } catch (_: Throwable) {
                                }
                            }

                        } else {
                            MainScope().launch(Dispatchers.Main) {
                                try {
                                    analyze()
                                } catch (_: Throwable) {
                                }
                            }
                        }
                    } else {
                        emptyCalibrated = true
                        emptyPulseWidth = currentPulseWidth
                        currentPulseWidth = ""
                        enableStandardCalibration()
                    }
                }
                externalSensorId.isEmpty() -> {
                    send("d")
                }
                emptyPulseWidth.isNotEmpty() -> {
                    send("s")
                }
                externalSensorId.isNotEmpty() && currentPulseWidth.isEmpty() -> {
                    send("a")
                }
            }
        }, 3000, 2000)
    }

    private fun getSensorId(id: String): Boolean {

        if (id.isEmpty()) {
            return false
        }

        MainScope().launch {
            emptyCalibration = DataHelper.getCalibrationFromTheCloud(
                id, "empty"
            )
            masterCalibration = DataHelper.getCalibrationFromTheCloud(
                id, model.test.get()!!.uuid
            )
        }
        return false
    }

    protected fun handleMessage(bytes: ByteArray) {
        timer.cancel()
        val message = String(bytes).replace('\u0000', ' ').trim()
        display(message)
        when {
            message.startsWith("d=[") && message.endsWith("]") -> {
                externalSensorId = message.substring(3, message.length - 1)
                setExternalSensorId(externalSensorId)
                if (externalSensorId.contains("[") || externalSensorId.contains("]")) {
                    externalSensorId = ""
                } else {
                    getSensorId(externalSensorId)
                }
            }
            (message.startsWith("a=[") || message.startsWith("s=[")) && message.endsWith("]") -> {
                currentPulseWidth = message.substring(3, message.length - 1)
                if (currentPulseWidth.contains("[") || currentPulseWidth.contains("]")) {
                    currentPulseWidth = ""
                }
            }
            message.startsWith("d=") -> {
                externalSensorId = message.substring(3, message.length - 1)
                setExternalSensorId(externalSensorId)
                if (externalSensorId.contains("[") || externalSensorId.contains("]")) {
                    externalSensorId = ""
                } else {
                    getSensorId(externalSensorId)
                }
            }
        }
        if (!emptyCalibrated) {
            startTimer()
        } else if (!standardCalibrationCompleted) {
            if (startStandardCalibration) {
                startTimer()
            }
        } else if (analysisStarted && !analysisCompleted) {
            startTimer()
        }
    }

    open fun send(s: String) {

    }

    open fun startSensor() {
    }

    fun analyze() {
        try {

            if (emptyCalibration != null && emptyCalibration!!.calibration.isNotEmpty() &&
                masterCalibration != null && masterCalibration!!.uuid == model.test.get()!!.uuid
            ) {

                val (result, uncalibratedResult, distance) = analyzeData(
                    emptyCalibration, masterCalibration, standardCalibration,
                    currentPulseWidth, emptyPulseWidth, model.test.get()!!
                )

//                model.currentResult = ResultInfo(result, distance = distance)

                if (standardCalibrationCompleted) {
                    analysisCompleted = true
                }

                if (analysisCompleted) {
                    b.resultLayout.visibility = VISIBLE
                    b.uncalibratedResultLayout.visibility = VISIBLE
                    b.distanceLayout.visibility = VISIBLE
                    if (standardCalibration != null && standardCalibration!!.a.isNotEmpty()) {
                        if (result == -1.0) {
                            b.resultText.text = getString(R.string.no_result)
                        } else {
                            b.resultText.text = Constants.DECIMAL_FORMAT.format(result)
                        }
                    } else {
                        b.resultLayout.visibility = GONE
                    }
                    if (uncalibratedResult == -1.0) {
                        b.uncalibratedResultText.text = getString(R.string.no_result)
                    } else {
                        b.uncalibratedResultText.text =
                            Constants.DECIMAL_FORMAT.format(uncalibratedResult)
                    }
                    b.distanceText.text = Constants.DECIMAL_FORMAT.format(distance)
                    b.currentSampleText.text = currentPulseWidth.replace(",", ", ")
                    b.analyzeProgress.visibility = GONE
                    b.sampleLayout.visibility = VISIBLE
                    b.analyzeLayout.setStatusColor(completed = true, required = true)
                }

                Handler(Looper.getMainLooper()).post {
                    submitResultListener?.onSubmitResult(model.test.get()!!.results)
                }

            } else {
                display("Not calibrated")
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle("Not calibrated")
                    .setMessage("Unable to download calibrations. Check internet connection")
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok) { _, _ -> requireActivity().finish() }
                    .create()
                    .show()
            }
        } catch (e: Exception) {
            display(e.toString())
        }
    }

    protected fun enableEmptyCalibration() {
        b.connectLayout.setStatusColor(completed = true, required = true)
        b.calibrateLayout.setStatusColor(completed = false, required = true)
        b.emptyCalibrateButton.isEnabled = true
        b.calibrateLayout.alpha = 1f
    }

    private fun enableAnalysis() {
        startStandardCalibration = false
        standardCalibrationCompleted = true
        b.skipButton.visibility = GONE
        b.standardCalibrateButton.visibility = GONE
        b.standardCalibrateProgress.visibility = GONE
        if (standardCalibration == null || standardCalibration!!.a.isEmpty()) {
            b.standardValuesLayout.visibility = GONE
            b.standardValueLayout.visibility = GONE
        } else {
            b.standardValuesLayout.visibility = VISIBLE
        }
        b.standardCalibrateLayout.setStatusColor(
            completed = true,
            required = true
        )
        b.analyzeLayout.alpha = 1f
        b.analyzeButton.isEnabled = true
        b.analyzeLayout.setStatusColor(completed = false, required = true)
        b.standardCalibrateButton.isEnabled = false
    }

    private fun enableStandardCalibration() {
        MainScope().launch(Dispatchers.Main) {
            timer.cancel()
            try {
                minValue = masterCalibration!!.calibration[0].value
                maxValue = masterCalibration!!.calibration[masterCalibration!!
                    .calibration.size - 1].value

                if (emptyCalibration == null || emptyCalibration!!.calibration.isEmpty()) {
                    throw Exception()
                } else {
                    b.downloadedEmptyText.text =
                        emptyCalibration!!.calibration[0].a.toString().replace("[", "")
                            .replace("]", "").replace(",", ", ")
                    b.currentEmptyText.text = emptyPulseWidth.replace(",", ", ")
                    b.emptyValuesLayout.visibility = VISIBLE
                    b.emptyMasterLayout.visibility = VISIBLE
                    b.calibrateProgress.visibility = GONE
                    b.sampleLayout.visibility = GONE
                    b.emptyCalibrateButton.visibility = GONE
                    b.calibrateLayout.setStatusColor(completed = true, required = true)
                    b.standardCalibrateButton.isEnabled = true
                    b.skipButton.alpha = 1f
                    b.skipButton.isEnabled = true
                    b.standardCalibrateLayout.isEnabled = true
                    b.standardCalibrateLayout.setStatusColor(completed = false, required = true)
                    b.standardCalibrateLayout.alpha = 1f
                }
            } catch (e: Exception) {
                display("Not calibrated")
                MainScope().launch(Dispatchers.Main) {
                    MaterialAlertDialogBuilder(requireActivity())
                        .setTitle("Not calibrated")
                        .setMessage("Unable to download calibrations. Check internet connection")
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok) { _, _ -> requireActivity().finish() }
                        .create()
                        .show()
                }
            }
        }
    }

    private fun analyzeData(
        emptyCalibration: FactoryConfig?,
        masterCalibration: FactoryConfig?,
        standardCalibration: PulseWidth?,
        currentPulseWidth: String,
        emptyPulseWidth: String,
        testInfo: TestInfo
    ): Triple<Double, Double, Double> {

        var result = -1.0
        var uncalibratedResult = -1.0
        var distance = Double.MAX_VALUE

        try {
            val currentPulseArray = currentPulseWidth.split(",")
            val emptyPulses = emptyPulseWidth.split(",")
            val aPulse = ArrayList<Int>()

            val colorToFind = PulseWidth(0.0, aPulse)

            if (standardCalibration == null) {
                aPulse.add(
                    currentPulseArray[0].toInt() - (emptyPulses[0].toInt() - emptyCalibration!!.calibration[0].a[0])
                )
                aPulse.add(
                    currentPulseArray[1].toInt() - (emptyPulses[1].toInt() - emptyCalibration.calibration[0].a[1])
                )
                aPulse.add(
                    currentPulseArray[2].toInt() - (emptyPulses[2].toInt() - emptyCalibration.calibration[0].a[2])
                )
                aPulse.add(
                    currentPulseArray[3].toInt() - (emptyPulses[3].toInt() - emptyCalibration.calibration[0].a[3])
                )
            } else {
                aPulse.add(currentPulseArray[0].toInt())
                aPulse.add(currentPulseArray[1].toInt())
                aPulse.add(currentPulseArray[2].toInt())
                aPulse.add(currentPulseArray[3].toInt())
            }

            val calibrations = ArrayList<PulseWidth>()
            for (i in 0 until masterCalibration!!.calibration.size - 1) {
                calibrations.addAll(
                    ColorUtil.interpolate(
                        masterCalibration.calibration[i],
                        masterCalibration.calibration[i + 1]
                    )
                )
            }

            for (calibration in calibrations) {
                val tempDistance = ColorUtil.getDistance(calibration, colorToFind)
                if (tempDistance < distance) {
                    distance = tempDistance
                    if (tempDistance < 1000) {
                        uncalibratedResult = calibration.value
                    }
                }
            }

            standardCalibration.let {
                if (it != null && it.a.isNotEmpty()) {
                    try {
                        val pulse = ArrayList<Int>()
                        for ((index, calibration) in calibrations.withIndex()) {
                            if (calibration.value >= it.value) {
                                val cal = calibrations[index - 1]
                                pulse.add(cal.a[0] - it.a[0])
                                pulse.add(cal.a[1] - it.a[1])
                                pulse.add(cal.a[2] - it.a[2])
                                pulse.add(cal.a[3] - it.a[3])
                                break
                            }
                        }
                        aPulse[0] += pulse[0]
                        aPulse[1] += pulse[1]
                        aPulse[2] += pulse[2]
                        aPulse[3] += pulse[3]
                    } catch (_: Exception) {
                    }

                    for (calibration in calibrations) {
                        val tempDistance = ColorUtil.getDistance(calibration, colorToFind)
                        if (tempDistance < distance) {
                            distance = tempDistance
                            if (tempDistance < 1000) {
                                result = calibration.value
                            }
                        }
                    }
                } else {
                    result = uncalibratedResult
                }
            }
            testInfo.subTest().setResult(uncalibratedResult)
            uncalibratedResult = testInfo.subTest().resultInfo.result

            testInfo.subTest().setResult(result)

        } catch (_: Exception) {
        }

        return Triple(testInfo.subTest().resultInfo.result, uncalibratedResult, distance)
    }

    protected open fun resetInterface() {
        analysisCompleted = false
        startStandardCalibration = false
        standardCalibrationCompleted = false
        emptyCalibrated = false
        externalSensorId = ""
        currentPulseWidth = ""
        emptyPulseWidth = ""
        emptyCalibration = null
        masterCalibration = null
        standardCalibration = null
        standardCalibration = PulseWidth()
        MainScope().launch(Dispatchers.Main) {
            b.connectLayout.setStatusColor(completed = false, required = true)
            b.calibrateLayout.setStatusColor(completed = false, required = false)
            b.calibrateLayout.alpha = .4f
            b.analyzeLayout.setStatusColor(completed = false, required = false)
            b.analyzeLayout.alpha = .4f
            b.standardCalibrateLayout.setStatusColor(completed = false, required = false)
            b.standardCalibrateLayout.alpha = .4f
            b.emptyValuesLayout.visibility = GONE
            b.emptyMasterLayout.visibility = GONE
            b.emptyCalibrateButton.visibility = VISIBLE
            b.emptyCalibrateButton.isEnabled = false
            b.calibrateProgress.visibility = GONE
            b.sampleLayout.visibility = GONE
            b.standardCalibrateButton.visibility = VISIBLE
            b.standardCalibrateButton.isEnabled = false
            b.standardValuesLayout.visibility = GONE
            b.standardValueText.text = ""
            b.standardValueLayout.visibility = GONE
            b.standardCalibrateProgress.visibility = GONE
            b.analyzeLayout.alpha = 0.3f
            b.analyzeButton.isEnabled = false
            b.analyzeButton.visibility = VISIBLE
            b.analyzeButton.isEnabled = false
            b.analyzeProgress.visibility = GONE
            b.resultLayout.visibility = GONE
            display("Connection closed")
        }
        if (::timer.isInitialized) {
            timer.cancel()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        submitResultListener = if (context is OnSubmitResultListener) {
            context
        } else {
            throw IllegalArgumentException(
                context.toString()
                        + " must implement OnSubmitResultListener"
            )
        }
    }

    fun display(message: String) {
        MainScope().launch(Dispatchers.Main) {
            try {
                Timber.e("device: $message")
//                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            } catch (_: Throwable) {
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        submitResultListener = null
    }

    override fun onPause() {
        super.onPause()
        if (::timer.isInitialized) {
            timer.cancel()
        }
    }

    interface OnSubmitResultListener {
        fun onSubmitResult(results: ArrayList<Result>)
    }

    companion object {
        init {
            try {
                FirebaseDatabase.getInstance().setPersistenceEnabled(true)
            } catch (_: Exception) {
            }
        }
    }
}