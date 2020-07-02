package io.ffem.lite.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import io.ffem.lite.R
import io.ffem.lite.model.CalibrationValue
import io.ffem.lite.model.TestInfo
import io.ffem.lite.preference.isDiagnosticMode
import kotlinx.android.synthetic.main.app_bar_layout.*
import kotlinx.android.synthetic.main.fragment_calibration_list.*

class CalibrationItemFragment : Fragment() {
    private lateinit var testInfo: TestInfo
    private var mListener: OnCalibrationSelectedListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        testInfo = ResultFragmentArgs.fromBundle(requireArguments()).testInfo
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calibration_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (context != null) {
            calibrationList.addItemDecoration(DividerItemDecoration(context, 1))
        }

        loadDetails()

        text_title.text = testInfo.name

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
        requireActivity().title = getString(R.string.select_calibration_point)
    }

    /**
     * Display the calibration details such as date, expiry, batch number etc...
     */
    private fun loadDetails() {
        setAdapter(testInfo)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is OnCalibrationSelectedListener) {
            context
        } else {
            throw IllegalArgumentException(
                context.toString()
                        + " must implement OnCalibrationSelectedListener"
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    private fun setAdapter(testInfo: TestInfo) {
        calibrationList.adapter = CalibrationViewAdapter(testInfo, mListener)
    }

    interface OnCalibrationSelectedListener {
        fun onCalibrationSelected(calibrationValue: CalibrationValue?, testInfo: TestInfo?)
    }
}