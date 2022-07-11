package io.ffem.lite.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import io.ffem.lite.R
import io.ffem.lite.databinding.FragmentSelectDilutionBinding
import java.util.*

class SelectDilutionFragment : BaseFragment() {
    private var _binding: FragmentSelectDilutionBinding? = null
    private val b get() = _binding!!
    private val model: TestInfoViewModel by activityViewModels()
    private var listener: OnDilutionSelectedListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectDilutionBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        for (dilution in model.test.get()!!.dilutions) {
            when (dilution) {
                1 -> {
                    b.noDilutionBtn.setOnClickListener {
                        listener!!.onDilutionSelected(dilution)
                    }
                    b.noDilutionBtn.visibility = View.VISIBLE
                }
                2 -> {
                    b.dilution1Btn.text = String.format(
                        Locale.getDefault(),
                        getString(R.string.times_dilution), dilution
                    )
                    b.dilution1Btn.setOnClickListener {
                        listener!!.onDilutionSelected(dilution)
                    }
                    b.dilution1Btn.visibility = View.VISIBLE
                }
                5 -> {
                    b.dilution2Btn.text = String.format(
                        Locale.getDefault(),
                        getString(R.string.times_dilution), dilution
                    )
                    b.dilution2Btn.setOnClickListener {
                        listener!!.onDilutionSelected(dilution)
                    }
                    b.dilution2Btn.visibility = View.VISIBLE
                }
                -1 -> {
                    b.customDilutionBtn.setOnClickListener {
                        showCustomDilutionDialog()
                    }
                    b.customDilutionBtn.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showCustomDilutionDialog() {
        if (activity != null) {
            val ft = requireActivity().supportFragmentManager.beginTransaction()
            val editCustomDilution = EditCustomDilution()
            editCustomDilution.show(ft, editCustomDilution.javaClass.name)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (context is OnDilutionSelectedListener) {
            context
        } else {
            throw IllegalArgumentException(
                context.toString()
                        + " must implement OnDilutionSelectedListener"
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnDilutionSelectedListener {
        fun onDilutionSelected(dilution: Int)
    }
}