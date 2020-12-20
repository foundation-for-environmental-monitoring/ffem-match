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

class ResultFragment : Fragment() {
    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!
    private val model: TestInfoViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (activity is ResultViewActivity) {
            binding.submitBtn.visibility = GONE
        } else {
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
            binding.submitBtn.visibility = VISIBLE
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
            binding.resultImg.setImageURI(Uri.fromFile(resultImagePath))
        }

        if (analyzedImagePath.exists()) {
            binding.fullPhotoImg.setImageURI(Uri.fromFile(analyzedImagePath))
        }

        if (extractImagePath.exists()) {
            binding.extractImg.setImageURI(Uri.fromFile(extractImagePath))
            val bitmap = BitmapFactory.decodeFile(extractImagePath.path)
            if (bitmap.height > bitmap.width) {
                binding.extractImg.rotation = -90f
            }
            bitmap.recycle()
            binding.extractImg.refreshDrawableState()
        }

        if (isDiagnosticMode()) {
            val gsExtractImagePath =
                File(path + testInfo.fileName + File.separator + fileName + "_swatch_gs.jpg")
            if (gsExtractImagePath.exists()) {
                binding.extractGsImg.setImageURI(Uri.fromFile(gsExtractImagePath))
                binding.extractGsImg.refreshDrawableState()
            } else {
                binding.lytColorExtractsGs.visibility = GONE
            }
        } else {
            binding.lytColorExtractsGs.visibility = GONE
        }

        val requestedTestId = PreferencesUtil.getString(context, TEST_ID_KEY, "")
        if (testInfo.error == ErrorType.NO_ERROR && testInfo.resultInfo.result >= 0) {
            binding.nameTxt.text = testInfo.name!!.toLocalString()
            binding.name2Txt.text = ""
            binding.resultTxt.text = testInfo.getResultString(requireContext())

            if (isDiagnosticMode()) {
                val grayscaleResult = testInfo.getResultGrayscaleString()
                binding.grayscaleResultTxt.text = grayscaleResult
                if (grayscaleResult.isNotEmpty()) {
                    binding.unit2Txt.text = testInfo.unit
                }
            } else {
                binding.grayscaleLyt.visibility = GONE
            }

            binding.unitTxt.text = testInfo.unit
            binding.valueTxt.text = testInfo.getRisk(requireContext())
            when {
                testInfo.getRiskType() == RiskLevel.HIGH -> {
                    binding.valueTxt.setTextColor(resources.getColor(R.color.high_risk, null))
                }
                testInfo.getRiskType() == RiskLevel.MEDIUM -> {
                    binding.valueTxt.setTextColor(resources.getColor(R.color.medium_risk, null))
                }
                testInfo.getRiskType() == RiskLevel.LOW -> {
                    binding.valueTxt.setTextColor(resources.getColor(R.color.low_risk, null))
                }
            }
            binding.errorMarginTxt.text = String.format("%.2f", testInfo.getMarginOfError())
            if (testInfo.resultInfo.luminosity > -1) {
                binding.luminosityTxt.text = testInfo.resultInfo.luminosity.toString()
            } else {
                binding.luminosityLyt.visibility = GONE
            }
            binding.errorMessageLyt.visibility = GONE
            binding.resultLyt.visibility = VISIBLE
            binding.resultDetailsLyt.visibility = VISIBLE
            if (requestedTestId.isNullOrEmpty()) {
                binding.submitBtn.setText(R.string.close)
            } else {
                binding.submitBtn.setText(R.string.submit_result)
            }
        } else {
            binding.nameTxt.text = ""
            if (testInfo.uuid != requestedTestId) {
                val requestedTest = getTestInfo(requestedTestId!!)
                if (requestedTest != null) {
                    binding.name2Txt.text = requestedTest.name!!.toLocalString()
                } else {
                    binding.name2Txt.text = testInfo.name!!.toLocalString()
                }
            } else {
                binding.name2Txt.text = testInfo.name!!.toLocalString()
            }

            binding.errorTxt.text = testInfo.error.toLocalString(requireContext())
            binding.errorMessageLyt.visibility = VISIBLE
            binding.resultLyt.visibility = GONE
            binding.resultDetailsLyt.visibility = GONE
            if (!extractImagePath.exists()) {
                binding.colorExtractsLyt.visibility = GONE
            }
            if (!analyzedImagePath.exists()) {
                binding.analyzedPhotoLyt.visibility = GONE
            }
            binding.submitBtn.setText(R.string.close)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
