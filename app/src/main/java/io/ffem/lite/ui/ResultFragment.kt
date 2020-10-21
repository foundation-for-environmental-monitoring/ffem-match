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
import androidx.fragment.app.activityViewModels
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.app.App.Companion.getTestInfo
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.RiskLevel
import io.ffem.lite.model.TestInfo
import io.ffem.lite.model.toLocalString
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.util.PreferencesUtil
import io.ffem.lite.util.toLocalString
import kotlinx.android.synthetic.main.app_bar_layout.*
import kotlinx.android.synthetic.main.fragment_result.*
import java.io.File

class ResultFragment : Fragment() {
    private val model: TestInfoViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (activity is ResultViewActivity) {
            button_submit.visibility = GONE
        } else {
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
        }

        displayResult(model.test.get())

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        toolbar.setTitle(R.string.result)
    }

    private fun displayResult(testInfo: TestInfo?) {
        if (testInfo == null) {
            return
        }

        val path =
            requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() +
                    File.separator + "captures" + File.separator

        val fileName = testInfo.name!!.replace(" ", "")
        val resultImagePath =
            File(path + testInfo.fileName + File.separator + fileName + "_result.jpg")
        val extractImagePath =
            File(path + testInfo.fileName + File.separator + fileName + "_swatch.jpg")

        val analyzedImagePath = File(path + testInfo.fileName + File.separator + fileName + ".jpg")

        if (resultImagePath.exists()) {
            image_result.setImageURI(Uri.fromFile(resultImagePath))
        }

        if (analyzedImagePath.exists()) {
            image_full.setImageURI(Uri.fromFile(analyzedImagePath))
        }

        if (extractImagePath.exists()) {
            image_extract.setImageURI(Uri.fromFile(extractImagePath))
            image_extract.refreshDrawableState()
        }

        if (isDiagnosticMode()) {
            val gsExtractImagePath =
                File(path + testInfo.fileName + File.separator + fileName + "_swatch_gs.jpg")
            if (gsExtractImagePath.exists()) {
                image_extract_gs.setImageURI(Uri.fromFile(gsExtractImagePath))
                image_extract_gs.refreshDrawableState()
            } else {
                lyt_color_extracts_gs.visibility = GONE
            }
        } else {
            lyt_color_extracts_gs.visibility = GONE
        }

        val requestedTestId = PreferencesUtil.getString(context, App.TEST_ID_KEY, "")
        if (testInfo.error == ErrorType.NO_ERROR && testInfo.resultInfo.result >= 0) {
            text_name.text = testInfo.name!!.toLocalString()
            text_name2.text = ""
            text_result.text = testInfo.getResultString(requireContext())

            if (isDiagnosticMode()) {
                val grayscaleResult = testInfo.getResultGrayscaleString()
                text_grayscale_result.text = grayscaleResult
                if (grayscaleResult.isNotEmpty()) {
                    text_unit2.text = testInfo.unit
                }
            } else {
                grayscale_lyt.visibility = GONE
            }

            text_unit.text = testInfo.unit
            text_risk.text = testInfo.getRisk(requireContext())
            when {
                testInfo.getRiskType() == RiskLevel.HIGH -> {
                    text_risk.setTextColor(resources.getColor(R.color.high_risk, null))
                }
                testInfo.getRiskType() == RiskLevel.MEDIUM -> {
                    text_risk.setTextColor(resources.getColor(R.color.medium_risk, null))
                }
                testInfo.getRiskType() == RiskLevel.LOW -> {
                    text_risk.setTextColor(resources.getColor(R.color.low_risk, null))
                }
            }
            text_error_margin.text = String.format("%.2f", testInfo.getMarginOfError())
            lyt_error_message.visibility = GONE
            lyt_result.visibility = VISIBLE
            lyt_result_details.visibility = VISIBLE
            if (requestedTestId.isNullOrEmpty()) {
                button_submit.setText(R.string.close)
            } else {
                button_submit.setText(R.string.submit_result)
            }
        } else {
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
