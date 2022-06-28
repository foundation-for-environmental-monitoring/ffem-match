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
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import io.ffem.lite.R
import io.ffem.lite.app.App.Companion.getTestInfo
import io.ffem.lite.common.TEST_ID_KEY
import io.ffem.lite.data.AppDatabase
import io.ffem.lite.data.Calibration
import io.ffem.lite.databinding.FragmentCalibrationResultBinding
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.TestInfo
import io.ffem.lite.model.toLocalString
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.util.PreferencesUtil
import io.ffem.lite.util.toLocalString
import io.ffem.lite.util.toast
import java.io.File

class CalibrationResultFragment : Fragment() {
    private var _binding: FragmentCalibrationResultBinding? = null
    private val binding get() = _binding!!
    private val model: TestInfoViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalibrationResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
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
        binding.submitBtn.visibility = VISIBLE

        val testInfo = model.test.get()!!

        toolbar.visibility = VISIBLE
        toolbar.setTitle(R.string.confirm_calibration)

        binding.submitBtn.setOnClickListener {
            val subTest = testInfo.subTest()
            if (subTest.error == ErrorType.NO_ERROR) {
                val result = subTest.resultInfo
                val calibratedValue = subTest.calibratedResult.calibratedValue
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
                            subTest.calibratedResult.calibratedValue.value,
                            result.calibratedValue.color,
                            Color.red(result.calibratedValue.color) - Color.red(result.sampleColor),
                            Color.green(result.calibratedValue.color) - Color.green(result.sampleColor),
                            Color.blue(result.calibratedValue.color) - Color.blue(result.sampleColor)
                        )
                    )
                    requireContext().toast(
                        getString(
                            R.string.calibrated_successfully,
                            testInfo.name!!.toLocalString()
                        ), Toast.LENGTH_LONG
                    )
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

        val subTest = testInfo.subTest()
        if (subTest.error < ErrorType.CALIBRATION_ERROR) {
            val calibrationValue = subTest.calibratedResult.calibratedValue.value
            binding.nameTxt.text = testInfo.name!!.toLocalString()
            binding.name2Txt.text = ""
            binding.valueTxt.text = calibrationValue.toString()
            binding.errorMessageLyt.visibility = GONE
            binding.resultLyt.visibility = VISIBLE

            for (swatch in subTest.resultInfo.swatches!!) {
                if (swatch.value == calibrationValue) {
                    binding.btnCardColor.setBackgroundColor(swatch.color)
                    break
                }
            }

            binding.btnCalibratedColor.setBackgroundColor(subTest.resultInfo.sampleColor)
            binding.submitBtn.setText(R.string.confirm)

        } else {
            val requestedTestId = PreferencesUtil.getString(requireContext(), TEST_ID_KEY, "")
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

            binding.errorTxt.text = subTest.error.toLocalString(requireContext())
            binding.errorMessageLyt.visibility = VISIBLE
            binding.resultLyt.visibility = GONE
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
