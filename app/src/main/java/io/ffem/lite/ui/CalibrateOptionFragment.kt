package io.ffem.lite.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import io.ffem.lite.R
import io.ffem.lite.data.AppDatabase
import io.ffem.lite.databinding.FragmentCalibrateOptionBinding
import io.ffem.lite.preference.isDiagnosticMode

class CalibrateOptionFragment : BaseFragment() {
    private var _binding: FragmentCalibrateOptionBinding? = null
    private val b get() = _binding!!
    private var listener: OnCalibrationOptionListener? = null
    private val model: TestInfoViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalibrateOptionBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var calibrationAvailable = false
        val subTest = model.test.get()!!.subTest()
        for (v in subTest.values) {
            if (v.calibrate) {
                calibrationAvailable = true
                break
            }
        }

        if (calibrationAvailable) {
            b.calibrateBtn.visibility = VISIBLE
            b.calibrateBtn.setOnClickListener {
                listener!!.onCalibrationOption(true)
            }
        }

        b.startBtn.setOnClickListener {
            listener!!.onCalibrationOption(false)
        }

        val db = AppDatabase.getDatabase(requireContext())
        try {
            val calibration = db.resultDao().getCalibration(model.test.get()!!.uuid)
            if (null != calibration) {
                b.calibrateBtn.setText(R.string.recalibrate)
                if (isDiagnosticMode()) {
                    b.clearCalibrationButton.visibility = View.VISIBLE
                    b.clearCalibrationButton.setOnClickListener {
                        val db1 = AppDatabase.getDatabase(requireContext())
                        try {
                            db1.resultDao().deleteCalibration(model.test.get()!!.uuid)
                            b.clearCalibrationButton.visibility = View.GONE
                            b.calibrateBtn.setText(R.string.calibrate)
                        } finally {
                            db1.close()
                        }
                    }
                }
            } else {
                b.clearCalibrationButton.visibility = View.GONE
            }
        } finally {
            db.close()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (context is OnCalibrationOptionListener) {
            context
        } else {
            throw IllegalArgumentException(
                context.toString()
                        + " must implement OnCalibrationOptionListener"
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnCalibrationOptionListener {
        fun onCalibrationOption(calibrate: Boolean)
    }
}