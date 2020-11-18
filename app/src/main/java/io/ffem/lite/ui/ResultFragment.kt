package io.ffem.lite.ui

import android.graphics.BitmapFactory
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
            submit_btn.visibility = GONE
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
            submit_btn.visibility = VISIBLE
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
            result_img.setImageURI(Uri.fromFile(resultImagePath))
        }

        if (analyzedImagePath.exists()) {
            full_photo_img.setImageURI(Uri.fromFile(analyzedImagePath))
        }

        if (extractImagePath.exists()) {
            extract_img.setImageURI(Uri.fromFile(extractImagePath))
            val bitmap = BitmapFactory.decodeFile(extractImagePath.path)
            if (bitmap.height > bitmap.width) {
                extract_img.rotation = -90f
            }
            bitmap.recycle()
            extract_img.refreshDrawableState()
        }

        if (isDiagnosticMode()) {
            val gsExtractImagePath =
                File(path + testInfo.fileName + File.separator + fileName + "_swatch_gs.jpg")
            if (gsExtractImagePath.exists()) {
                extract_gs_img.setImageURI(Uri.fromFile(gsExtractImagePath))
                extract_gs_img.refreshDrawableState()
            } else {
                lyt_color_extracts_gs.visibility = GONE
            }
        } else {
            lyt_color_extracts_gs.visibility = GONE
        }

        val requestedTestId = PreferencesUtil.getString(context, App.TEST_ID_KEY, "")
        if (testInfo.error == ErrorType.NO_ERROR && testInfo.resultInfo.result >= 0) {
            name_txt.text = testInfo.name!!.toLocalString()
            name2_txt.text = ""
            result_txt.text = testInfo.getResultString(requireContext())

            if (isDiagnosticMode()) {
                val grayscaleResult = testInfo.getResultGrayscaleString()
                grayscale_result_txt.text = grayscaleResult
                if (grayscaleResult.isNotEmpty()) {
                    unit2_txt.text = testInfo.unit
                }
            } else {
                grayscale_lyt.visibility = GONE
            }

            unit_txt.text = testInfo.unit
            value_txt.text = testInfo.getRisk(requireContext())
            when {
                testInfo.getRiskType() == RiskLevel.HIGH -> {
                    value_txt.setTextColor(resources.getColor(R.color.high_risk, null))
                }
                testInfo.getRiskType() == RiskLevel.MEDIUM -> {
                    value_txt.setTextColor(resources.getColor(R.color.medium_risk, null))
                }
                testInfo.getRiskType() == RiskLevel.LOW -> {
                    value_txt.setTextColor(resources.getColor(R.color.low_risk, null))
                }
            }
            error_margin_txt.text = String.format("%.2f", testInfo.getMarginOfError())
            if (testInfo.resultInfo.luminosity > -1) {
                luminosity_txt.text = testInfo.resultInfo.luminosity.toString()
            } else {
                luminosity_lyt.visibility = GONE
            }
            error_message_lyt.visibility = GONE
            result_lyt.visibility = VISIBLE
            result_details_lyt.visibility = VISIBLE
            if (requestedTestId.isNullOrEmpty()) {
                submit_btn.setText(R.string.close)
            } else {
                submit_btn.setText(R.string.submit_result)
            }
        } else {
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
            result_details_lyt.visibility = GONE
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
