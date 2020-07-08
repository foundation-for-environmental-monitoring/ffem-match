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
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.app.App.Companion.getTestInfo
import io.ffem.lite.app.AppDatabase
import io.ffem.lite.model.Calibration
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.TestInfo
import io.ffem.lite.model.toLocalString
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.util.PreferencesUtil
import io.ffem.lite.util.toLocalString
import kotlinx.android.synthetic.main.app_bar_layout.*
import kotlinx.android.synthetic.main.fragment_calibration_result.*
import java.io.File

class CalibrationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calibration_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val testInfo = CalibrationFragmentArgs.fromBundle(requireArguments()).testInfo
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
        button_submit.visibility = VISIBLE

        displayResult(testInfo)

        toolbar.visibility = VISIBLE
        toolbar.setTitle(R.string.confirm_calibration)
        requireActivity().title = getString(R.string.confirm)

        val calibrationValue =
            CalibrationFragmentArgs.fromBundle(requireArguments()).calibrationValue.value
        button_submit.setOnClickListener {
            val r = testInfo.resultInfo
            for (s in r.swatches!!) {
                if (s.value == calibrationValue) {
                    r.calibrationColor = s.color
                    break
                }
            }

            val db = AppDatabase.getDatabase(requireContext())
            db.resultDao().insertCalibration(
                Calibration(
                    testInfo.uuid!!,
                    calibrationValue,
                    Color.red(r.calibrationColor) - Color.red(r.color),
                    Color.green(r.calibrationColor) - Color.green(r.color),
                    Color.blue(r.calibrationColor) - Color.blue(r.color)
                )
            )

            requireActivity().finish()
        }
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
            image_full.setImageURI(Uri.fromFile(analyzedImagePath))
        }

        if (extractImagePath.exists()) {
            image_extract.setImageURI(Uri.fromFile(extractImagePath))
            image_extract.refreshDrawableState()
        }

        if (testInfo.error == ErrorType.NO_ERROR && testInfo.resultInfo.result >= 0) {
            val calibrationValue =
                CalibrationFragmentArgs.fromBundle(requireArguments()).calibrationValue
            text_name.text = testInfo.name!!.toLocalString()
            text_name2.text = ""
            text_risk.text = calibrationValue.value.toString()
            lyt_error_message.visibility = GONE
            lyt_result.visibility = VISIBLE

            btn_card_color.setBackgroundColor(calibrationValue.color)
            btn_calibrated_color.setBackgroundColor(testInfo.resultInfo.color)

            button_submit.setText(R.string.confirm)

        } else {
            val requestedTestId = PreferencesUtil.getString(context, App.TEST_ID_KEY, "")
            text_name.text = ""
            if (testInfo.uuid != requestedTestId) {
                val requestedTest = getTestInfo(requestedTestId!!)
                if (requestedTest != null) {
                    text_name2.text = requestedTest.name!!.toLocalString()
                } else {
                    text_name2.text = testInfo.name!!.toLocalString()
                }
            } else {
                text_name2.text = testInfo.name!!.toLocalString()
            }

            text_error.text = testInfo.error.toLocalString(requireContext())
            lyt_error_message.visibility = VISIBLE
            lyt_result.visibility = GONE
            if (!extractImagePath.exists()) {
                lyt_color_extracts.visibility = GONE
            }
            if (!analyzedImagePath.exists()) {
                lyt_analyzed_photo.visibility = GONE
            }
            button_submit.setText(R.string.close)
        }
    }
}
