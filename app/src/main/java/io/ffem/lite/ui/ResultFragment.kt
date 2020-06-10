package io.ffem.lite.ui

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
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.RiskType
import io.ffem.lite.model.TestInfo
import io.ffem.lite.model.toLocalString
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.util.PreferencesUtil
import io.ffem.lite.util.toLocalString
import kotlinx.android.synthetic.main.app_bar_layout.*
import kotlinx.android.synthetic.main.fragment_result.*
import java.io.File

class ResultFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().setTitle(R.string.result)
        var testInfo = requireArguments().getParcelable<TestInfo>(App.TEST_INFO_KEY)
        if (testInfo == null) {
            testInfo = ResultFragmentArgs.fromBundle(requireArguments()).testInfo
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
        } else {
            toolbar.visibility = GONE
            button_submit.visibility = GONE
        }
        displayResult(testInfo)

        super.onViewCreated(view, savedInstanceState)
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

        if (testInfo.error == ErrorType.NO_ERROR && testInfo.resultDetail.result >= 0) {
            text_name.text = testInfo.name!!.toLocalString()
            text_name2.text = ""
            text_result.text = testInfo.getResultString(requireContext())
            text_unit.text = testInfo.unit
            text_risk.text = testInfo.getRisk(requireContext())
            when {
                testInfo.getRiskType() == RiskType.HIGH -> {
                    text_risk.setTextColor(resources.getColor(R.color.high_risk, null))
                }
                testInfo.getRiskType() == RiskType.MEDIUM -> {
                    text_risk.setTextColor(resources.getColor(R.color.medium_risk, null))
                }
                testInfo.getRiskType() == RiskType.LOW -> {
                    text_risk.setTextColor(resources.getColor(R.color.low_risk, null))
                }
            }
            text_error_margin.text = String.format("%.2f", testInfo.getMarginOfError())
            lyt_error_message.visibility = GONE
            lyt_result.visibility = VISIBLE
            lyt_result_details.visibility = VISIBLE
            button_submit.setText(R.string.submit_result)

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
            lyt_result_details.visibility = GONE
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
