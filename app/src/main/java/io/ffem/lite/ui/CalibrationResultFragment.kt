package io.ffem.lite.ui

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import io.ffem.lite.R
import io.ffem.lite.app.App.Companion.getTestInfo
import io.ffem.lite.common.TEST_ID_KEY
import io.ffem.lite.data.AppDatabase
import io.ffem.lite.data.Calibration
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.TestInfo
import io.ffem.lite.model.toLocalString
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.util.PreferencesUtil
import io.ffem.lite.util.toLocalString
import io.ffem.lite.util.toast
import kotlinx.android.synthetic.main.app_bar_layout.*
import kotlinx.android.synthetic.main.fragment_calibration_result.*
import java.io.File

class CalibrationResultFragment : Fragment() {
    private val model: TestInfoViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calibration_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isDiagnosticMode()) {
            toolbar.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.diagnostic
                )
            )
        } else {
            toolbar.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.colorPrimary
                )
            )
        }
        submit_btn.visibility = VISIBLE

        val testInfo = model.test.get()!!

        toolbar.visibility = VISIBLE
        toolbar.setTitle(R.string.confirm_calibration)

        submit_btn.setOnClickListener {
            if (model.test.get()!!.error == ErrorType.NO_ERROR) {
                val result = testInfo.resultInfo
                val calibratedValue = testInfo.calibratedResultInfo.calibratedValue
                for (s in result.swatches!!) {
                    if (s.value == calibratedValue.value) {
                        result.calibratedValue.color = s.color
                        break
                    }
                }

                val db = AppDatabase.getDatabase(requireContext())
                try {
                    db.resultDao().insertCalibration(
                        Calibration(
                            testInfo.uuid!!,
                            testInfo.calibratedResultInfo.calibratedValue.value,
                            Color.red(result.calibratedValue.color) - Color.red(result.sampleColor),
                            Color.green(result.calibratedValue.color) - Color.green(result.sampleColor),
                            Color.blue(result.calibratedValue.color) - Color.blue(result.sampleColor)
                        )
                    )
                    requireContext().toast(testInfo.name + " calibrated successfully")
                } finally {
                    db.close()
                }
            }
            requireActivity().finish()
        }
        displayResult(testInfo)
    }

    override fun onResume() {
        super.onResume()
        displayResult(model.test.get())
    }

    private fun displayResult(testInfo: TestInfo?) {

        val path =
            requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() +
                    File.separator + "captures" + File.separator

        val fileName = testInfo!!.name!!.replace(" ", "")
        val extractImagePath =
            File(path + testInfo.fileName + File.separator + fileName + "_swatch" + ".jpg")
        val analyzedImagePath = File(path + testInfo.fileName + File.separator + fileName + ".jpg")

        if (analyzedImagePath.exists()) {
            full_photo_img.setImageURI(Uri.fromFile(analyzedImagePath))
        }

        if (extractImagePath.exists()) {
            extract_img.setImageURI(Uri.fromFile(extractImagePath))
            extract_img.refreshDrawableState()
        }

        if (testInfo.error == ErrorType.NO_ERROR && testInfo.resultInfo.result >= 0) {
            val calibrationValue = testInfo.calibratedResultInfo.calibratedValue.value
            name_txt.text = testInfo.name!!.toLocalString()
            name2_txt.text = ""
            value_txt.text = calibrationValue.toString()
            error_message_lyt.visibility = GONE
            result_lyt.visibility = VISIBLE

            for (swatch in testInfo.resultInfo.swatches!!) {
                if (swatch.value == calibrationValue) {
                    btn_card_color.setBackgroundColor(swatch.color)
                    break
                }
            }

            btn_calibrated_color.setBackgroundColor(testInfo.resultInfo.sampleColor)
            submit_btn.setText(R.string.confirm)

        } else {
            val requestedTestId = PreferencesUtil.getString(context, TEST_ID_KEY, "")
            name_txt.text = ""
            if (testInfo.uuid != requestedTestId) {
                val requestedTest = getTestInfo(requestedTestId!!)
                if (requestedTest != null) {
                    name2_txt.text = requestedTest.name!!.toLocalString()
                } else {
                    name2_txt.text = testInfo.name!!.toLocalString()
                }
            } else {
                name2_txt.text = testInfo.name!!.toLocalString()
            }

            error_txt.text = testInfo.error.toLocalString(requireContext())
            error_message_lyt.visibility = VISIBLE
            result_lyt.visibility = GONE
            if (!extractImagePath.exists()) {
                color_extracts_lyt.visibility = GONE
            }
            if (!analyzedImagePath.exists()) {
                analyzed_photo_lyt.visibility = GONE
            }
            submit_btn.setText(R.string.close)
        }
    }
}
