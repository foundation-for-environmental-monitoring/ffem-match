package io.ffem.lite.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import io.ffem.lite.R
import io.ffem.lite.databinding.FragmentCalibrationListBinding
import io.ffem.lite.model.CalibrationValue
import io.ffem.lite.model.TestInfo
import io.ffem.lite.preference.isDiagnosticMode

class CalibrationItemFragment : Fragment() {
    private var _binding: FragmentCalibrationListBinding? = null
    private val binding get() = _binding!!
    private val model: TestInfoViewModel by activityViewModels()
    private var mListener: OnCalibrationSelectedListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalibrationListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (context != null) {
            binding.calibrationList.addItemDecoration(DividerItemDecoration(context, 1))
        }

        loadDetails()

        binding.textTitle.text = model.test.get()!!.name

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
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = getString(R.string.select_calibration_point)
    }

    /**
     * Display the calibration details such as date, expiry, batch number etc...
     */
    private fun loadDetails() {
        setAdapter(model.test.get()!!)
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
        binding.calibrationList.adapter = CalibrationViewAdapter(testInfo, mListener)
    }

    interface OnCalibrationSelectedListener {
        fun onCalibrationSelected(calibrationValue: CalibrationValue)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}