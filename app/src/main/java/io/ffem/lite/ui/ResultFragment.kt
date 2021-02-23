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
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import io.ffem.lite.R
import io.ffem.lite.app.App.Companion.getTestInfo
import io.ffem.lite.common.TEST_ID_KEY
import io.ffem.lite.databinding.FragmentResultBinding
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.RiskLevel
import io.ffem.lite.model.TestInfo
import io.ffem.lite.model.toLocalString
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.util.PreferencesUtil
import io.ffem.lite.util.toLocalString
import java.io.File

class ResultFragment(externalRequest: Boolean) : Fragment() {
    private var _binding: FragmentResultBinding? = null
    private val b get() = _binding!!
    private val model: TestInfoViewModel by activityViewModels()
    private val isExternalRequest = externalRequest

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (activity is ResultViewActivity) {
            b.nextButton.visibility = GONE
        } else {
            b.infoLayout.visibility = GONE
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

            if (model.test.get()!!.error != ErrorType.NO_ERROR || isExternalRequest) {
                b.nextButton.setText(R.string.close)
            }

            b.nextButton.setOnClickListener {
                if (model.test.get()!!.error != ErrorType.NO_ERROR || isExternalRequest) {
                    (requireActivity() as TestActivity).submitResult()
                } else {
                    (requireActivity() as TestActivity).pageNext()
                }
            }
        }

        displayResult(model.test.get())

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        view?.findViewById<Toolbar>(R.id.toolbar)?.setTitle(R.string.result)
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

        if (isDiagnosticMode()) {
            val gsExtractImagePath =
                File(path + testInfo.fileName + File.separator + fileName + "_swatch_gs.jpg")
            if (gsExtractImagePath.exists()) {
                b.extractGsImg.setImageURI(Uri.fromFile(gsExtractImagePath))
                b.extractGsImg.refreshDrawableState()
            } else {
                b.lytColorExtractsGs.visibility = GONE
            }
        } else {
            b.lytColorExtractsGs.visibility = GONE
        }

        val requestedTestId = PreferencesUtil.getString(context, TEST_ID_KEY, "")
        if (testInfo.error == ErrorType.NO_ERROR && testInfo.resultInfo.result >= 0) {
            b.nameTxt.text = testInfo.name!!.toLocalString()
            b.name2Txt.text = ""
            b.resultTxt.text = testInfo.getResultString(requireContext())

            if (isDiagnosticMode()) {
                val grayscaleResult = testInfo.getResultGrayscaleString()
                b.grayscaleResultTxt.text = grayscaleResult
                if (grayscaleResult.isNotEmpty()) {
                    b.unit2Txt.text = testInfo.unit
                }
            } else {
                b.grayscaleLyt.visibility = GONE
            }

            b.unitTxt.text = testInfo.unit
            b.valueTxt.text = testInfo.getRisk(requireContext())
            when {
                testInfo.getRiskType() == RiskLevel.HIGH -> {
                    b.valueTxt.setTextColor(resources.getColor(R.color.high_risk, null))
                }
                testInfo.getRiskType() == RiskLevel.MEDIUM -> {
                    b.valueTxt.setTextColor(resources.getColor(R.color.medium_risk, null))
                }
                testInfo.getRiskType() == RiskLevel.LOW -> {
                    b.valueTxt.setTextColor(resources.getColor(R.color.low_risk, null))
                }
            }
            b.errorMarginTxt.text = String.format("%.2f", testInfo.getMarginOfError())
            if (testInfo.resultInfo.luminosity > -1) {
                b.luminosityTxt.text = testInfo.resultInfo.luminosity.toString()
            } else {
                b.luminosityLyt.visibility = GONE
            }
            b.errorMessageLyt.visibility = GONE
            b.resultLyt.visibility = VISIBLE
            b.resultDetailsLyt.visibility = VISIBLE

            if (model.form.source.isEmpty()) {
                b.infoLayout.visibility = GONE
            } else {
                b.titleText.text = model.form.source
                b.sourceTypeText.text = model.form.sourceType
                b.commentText.text = model.form.comment
            }
        } else {
            b.nameTxt.text = ""
            if (testInfo.uuid != requestedTestId) {
                val requestedTest = getTestInfo(requestedTestId!!)
                if (requestedTest != null) {
                    b.name2Txt.text = requestedTest.name!!.toLocalString()
                } else {
                    b.name2Txt.text = testInfo.name!!.toLocalString()
                }
            } else {
                b.name2Txt.text = testInfo.name!!.toLocalString()
            }

            b.errorTxt.text = testInfo.error.toLocalString(requireContext())
            b.errorMessageLyt.visibility = VISIBLE
            b.resultLyt.visibility = GONE
            b.resultDetailsLyt.visibility = GONE
            if (!extractImagePath.exists()) {
                b.colorExtractsLyt.visibility = GONE
            }
            if (!analyzedImagePath.exists()) {
                b.analyzedPhotoLyt.visibility = GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
