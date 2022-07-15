package io.ffem.lite.ui

import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import io.ffem.lite.R
import io.ffem.lite.common.Constants.DECIMAL_FORMAT
import io.ffem.lite.databinding.FragmentResultBinding
import io.ffem.lite.helper.SwatchHelper
import io.ffem.lite.model.*
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.util.toLocalString
import java.io.File

class ResultFragment(var externalRequest: Boolean) : BaseFragment() {
    private var _binding: FragmentResultBinding? = null
    private val b get() = _binding!!
    private val model: TestInfoViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        b.nextButton.setOnClickListener {
            (activity as TestActivity).onAcceptClick()
        }

        if (activity is ResultViewActivity) {
            b.nextButton.visibility = GONE
        } else {
//            b.infoLayout.visibility = GONE
            val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
            if (toolbar != null) {
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
            }

            val subTest = model.test.get()!!.subTest()
            if (subTest.error != ErrorType.NO_ERROR || !externalRequest
                || model.test.get()!!.subtype == TestType.TITRATION
            ) {
                b.nextButton.setText(R.string.close)
            }

            b.nextButton.setOnClickListener {
                if (subTest.error != ErrorType.NO_ERROR || externalRequest) {
                    (requireActivity() as TestActivity).onAcceptClick()
                } else {
                    (requireActivity() as TestActivity).pageNext()
                }
            }
        }
    }

    override fun onResume() {
        b.colorVue.setBackgroundColor(model.calibrationColor)
        super.onResume()
        requireActivity().title = "Result"

        val testInfo = model.test.get()

        if (activity != null) {
            requireActivity().setTitle(R.string.result)
        }
        if (testInfo != null) {
            if (!model.isCalibration && testInfo.subtype == TestType.CUVETTE) {
                if (SwatchHelper.isCalibrationComplete(testInfo) &&
                    SwatchHelper.isSwatchListValid(testInfo, false)
                ) {
                    b.warningText.visibility = GONE
                } else {
                    b.warningText.visibility = VISIBLE
                }
            }

            if (testInfo.subtype == TestType.CUVETTE || testInfo.subtype == TestType.CARD) {
                b.dilutionTxt.visibility = VISIBLE
                setInfo(testInfo)
            }

            var subTest = testInfo.subTest()
            var displayMainResult = true
            var displayPosition = 1
            for (r in testInfo.results) {
                if (r.display == 0 && testInfo.results.size > 1) {
                    r.display = ++displayPosition
                }
                when (r.display) {
                    1 -> {
                        subTest = r
                        displayMainResult = true
                    }
                    2 -> {
                        if (r.resultInfo.result > -1 && r.getResultString()
                                .isNotEmpty()
                        ) {
                            b.detailsLyt.visibility = VISIBLE
                            val name = r.name
                            b.name1Txt.text = name
                            b.result1Txt.text = r.getResultString()
                            b.unit1Txt.text = r.unit
                            b.result1Lyt.visibility = VISIBLE
                            displayMainResult = false
                        }
                    }
                    3 -> {
                        if (r.resultInfo.result > -1 && r.getResultString()
                                .isNotEmpty()
                        ) {
                            val name = r.name
                            b.name2Txt.text = name
                            b.result2Txt.text = r.getResultString()
                            b.unit2Txt.text = r.unit
                            b.result2Lyt.visibility = VISIBLE
                            displayMainResult = false
                        }
                    }
                }
            }

            // If multiple results to be displayed then hide the single result view
            if (!displayMainResult) {
                b.resultLyt.visibility = GONE
                b.detailsTxt.text = testInfo.name?.toLocalString()
            }

            if (subTest.calibratedResult.result > -1) {
                b.actualResultText.text = subTest.getActualResult(requireContext())
            } else {
                b.actualResultLayout.visibility = GONE
            }

            try {
                b.sampleRgbText.text = String.format(
                    "%s   %s   %s",
                    Color.red(subTest.resultInfo.sampleColor),
                    Color.green(subTest.resultInfo.sampleColor),
                    Color.blue(subTest.resultInfo.sampleColor)
                )
            } catch (ignored: Exception) {
            }

            b.uncalibratedResultText.text = subTest.getUncalibratedResult(requireContext())
            b.uncalibratedResultLayout.visibility = VISIBLE
            b.resultTxt.text = subTest.getResultString()
            b.nameTxt.text = testInfo.name!!.toLocalString()
            b.dilutionTxt.text = resources.getQuantityString(
                R.plurals.dilutions,
                testInfo.dilution, testInfo.dilution
            )
            b.unitTxt.text = subTest.unit
            when {
                testInfo.dilution >= testInfo.getMaxDilution() -> {
                    b.retestMessage.visibility = GONE
                }
                subTest.resultInfo.highLevelsFound -> {
                    b.retestMessage.visibility = VISIBLE
                }
                else -> {
                    b.retestMessage.visibility = GONE
                }
            }
        }
    }

    private fun setInfo(testInfo: TestInfo) {
        val subTest = testInfo.subTest()

        if (isDiagnosticMode()) {
            b.errorMarginText.text = String.format("%.2f", subTest.getMarginOfError())
            b.diagnosticsLayout.visibility = VISIBLE
        } else {
            b.diagnosticsLayout.visibility = GONE
        }

        b.resultTxt.text = DECIMAL_FORMAT.format(subTest.resultInfo.result)
        b.resultTxt.visibility = VISIBLE
        b.riskText.text = subTest.getRiskString(requireContext())

        val path =
            requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() +
                    File.separator + "captures" + File.separator

        val fileName = testInfo.name!!.replace(" ", "")
        val resultImagePath =
            File(path + testInfo.fileName + File.separator + fileName + "_result.png")
        val extractImagePath =
            File(path + testInfo.fileName + File.separator + fileName + "_swatch.jpg")

        val analyzedImagePath = File(path + testInfo.fileName + File.separator + fileName + ".jpg")

        if (resultImagePath.exists()) {
            b.resultImg.setImageURI(Uri.fromFile(resultImagePath))
        }

        if (analyzedImagePath.exists()) {
            b.fullPhotoImg.setImageURI(Uri.fromFile(analyzedImagePath))
        }

        if (extractImagePath.exists()) {
            b.extractImg.setImageURI(Uri.fromFile(extractImagePath))
            val bitmap = BitmapFactory.decodeFile(extractImagePath.path)
            if (bitmap.height > bitmap.width) {
                b.extractImg.rotation = -90f
            }
            bitmap.recycle()
            b.extractImg.refreshDrawableState()
        }

        if (model.isCalibration) {
            b.riskText.visibility = GONE
            b.safetyMsgLayout.visibility = GONE
            b.retestMessage.visibility = GONE
            b.resultTxt.visibility = GONE
            b.unitTxt.visibility = GONE
//            b.colorBar.visibility = GONE
            b.valueTxt.visibility = VISIBLE
            b.colorVue.visibility = VISIBLE
            b.valueTxt.text = model.calibrationPoint.toString()
        } else {
            b.nameTxt.text = testInfo.name?.toLocalString()
            b.dilutionTxt.text = resources.getQuantityString(
                R.plurals.dilutions,
                testInfo.dilution, testInfo.dilution
            )
            b.unitTxt.text = subTest.unit
            when {
                testInfo.dilution >= testInfo.getMaxDilution() -> {
                    b.dilutionTxt.visibility = GONE
                }
                subTest.resultInfo.highLevelsFound -> {
                    b.dilutionTxt.visibility = VISIBLE
                }
                else -> {
                    b.dilutionTxt.visibility = GONE
                }
            }

//            val resultInfo = ResultInfo(
//                result.resultValue, 0, 0,
//                0f, 0.0, result.colors, dilution = result.dilution
//            )
//            val maxValue =
//                MathUtil.applyFormula(result.colors[result.colors.size - 1].value, result.formula)
//            val resultImage = createResultImage(resultInfo, maxValue, result.formula, 500)

//            FileUtil.savePicture(
//                requireContext(), result.uuid,
//                testInfo.uuid, ImageUtil.bitmapToBytes(resultImage),
//                "_result"
//            )

//            b.colorBar.setImageBitmap(resultImage)
//            b.colorBar.visibility = VISIBLE

            if (model.test.get()!!.sampleType == TestSampleType.WATER) {
                when (subTest.getSafetyLevel()) {
                    SafetyLevel.ACCEPTABLE -> {
                        b.safetyHeading.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.safe_green
                            )
                        )
                        b.safetyHeading.text = getString(R.string.low_risk_safe)
                        if (subTest.riskType == RiskType.ALKALINITY) {
                            b.safetyMessage.text = getString(R.string.ph_acceptable_msg)
                        } else {
                            b.safetyMessage.text = String.format(
                                getString(R.string.safety_acceptable_msg),
                                testInfo.name!!.toLocalString()
                            )
                        }
                    }
                    SafetyLevel.PERMISSIBLE -> {
                        b.safetyHeading.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.draft_orange
                            )
                        )
                        b.safetyHeading.text = getString(R.string.low_risk)
                        b.safetyMessage.text =
                            String.format(
                                getString(R.string.safety_permissible_msg),
                                testInfo.name!!.toLocalString()
                            )
                    }
                    else -> {
                        b.safetyHeading.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.risk_red
                            )
                        )
                        b.safetyHeading.text = getString(R.string.high_risk_unsafe)
                        if (subTest.riskType == RiskType.ALKALINITY) {
                            b.safetyMessage.text = getString(R.string.ph_unsafe_msg)
                        } else {
                            b.safetyMessage.text =
                                String.format(
                                    getString(R.string.safety_unsafe_msg),
                                    testInfo.name!!.toLocalString()
                                )
                        }
                    }
                }
                b.safetyMsgLayout.visibility = VISIBLE
            }

            if (testInfo.subtype == TestType.CARD) {
                b.colorExtractsLayout.visibility = VISIBLE
                b.analyzedPhotoLayout.visibility = VISIBLE
                if (subTest.error == ErrorType.NO_ERROR &&
                    (subTest.resultInfo.result >= 0 || subTest.calibratedResult.result >= 0)
                ) {
//                b.sampleTypeText.text = testInfo.sampleType.toLocalString()
                    b.nameTxt.text = testInfo.name!!.toLocalString()
                    b.name3Txt.text = ""
                    b.name3Txt.visibility = GONE
                    b.resultTxt.text = subTest.getResultString()

                    if (isDiagnosticMode()) {
                        b.errorMarginText.text = String.format("%.2f", subTest.getMarginOfError())
                    } else {
                        b.marginLayout.visibility = GONE
                    }

                    b.unitTxt.text = subTest.unit
//                    b.valueTxt.text = subTest.getRiskString()
//                when {
//                    subTest.getRiskType() == RiskLevel.RISK_3 -> {
//                        b.valueTxt.setTextColor(resources.getColor(R.color.high_risk, null))
//                    }
//                    subTest.getRiskType() == RiskLevel.RISK_2 -> {
//                        b.valueTxt.setTextColor(resources.getColor(R.color.medium_risk, null))
//                    }
//                    subTest.getRiskType() == RiskLevel.RISK_1 -> {
//                        b.valueTxt.setTextColor(resources.getColor(R.color.low_risk, null))
//                    }
//                }
                    if (subTest.resultInfo.luminosity > -1) {
                        b.luminosityTxt.text = subTest.resultInfo.luminosity.toString()
                    } else {
                        b.luminosityLyt.visibility = GONE
                    }
                    b.errorMessageLyt.visibility = GONE
                    b.resultLyt.visibility = VISIBLE
                    b.detailsLyt.visibility = VISIBLE

                    b.resultLayout.visibility = GONE
                    b.riskText.visibility = VISIBLE
                    b.result1Lyt.visibility = VISIBLE
                    b.name1Txt.text = getString(R.string.result)
                    b.result1Txt.text = subTest.getResultString()
                    b.unit1Txt.text = subTest.unit
                    if (testInfo.subTest().resultInfo.luminosity > -1) {
                        b.luminosityTxt.text = testInfo.subTest().resultInfo.luminosity.toString()
                    } else {
                        b.luminosityLyt.visibility = GONE
                    }

                    // todo fix this
//                    if (model.form.source.isEmpty()) {
//                        b.infoLayout.visibility = GONE
//                    } else {
//                        b.titleText.text = model.form.source
//                        b.sourceTypeText.text = model.form.sourceType
//                        b.commentText.text = model.form.comment
//                    }

                } else {
                    b.errorMessageLyt.visibility = VISIBLE
                    b.nameTxt.text = ""
                    b.name3Txt.text = testInfo.name!!.toLocalString()
                    b.errorTxt.text = subTest.error.toLocalString()
                    b.resultLyt.visibility = GONE
                    b.detailsLyt.visibility = GONE
                    b.safetyMsgLayout.visibility = GONE
                    if (!extractImagePath.exists()) {
                        b.colorExtractsLayout.visibility = GONE
                    }
                    if (!analyzedImagePath.exists()) {
                        b.analyzedPhotoLayout.visibility = GONE
                    }
                }
            }
        }
    }
}
