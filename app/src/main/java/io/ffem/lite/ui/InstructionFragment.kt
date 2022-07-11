package io.ffem.lite.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import io.ffem.lite.databinding.FragmentInstructionBinding
import io.ffem.lite.model.ButtonType
import io.ffem.lite.model.Instruction


private const val ARG_INSTRUCTION = "testInfo"
private const val ARG_SHOW_OK = "show_ok"

class InstructionFragment : BaseFragment() {
    private var _binding: FragmentInstructionBinding? = null
    private val b get() = _binding!!
    private val model: TestInfoViewModel by activityViewModels()
    private var instruction: Instruction? = null
    private var showOk: ButtonType? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            instruction = it.getParcelable(ARG_INSTRUCTION)
            showOk = requireArguments().getSerializable(ARG_SHOW_OK) as ButtonType?
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInstructionBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model.setContent(b.instructionLyt, instruction)

        b.buttonRetest.visibility = View.GONE
        b.textDilutionInfo.visibility = View.GONE
        b.acceptButton.visibility = View.GONE
        b.startTimerBtn.visibility = View.GONE
        b.buttonSkipTimer.visibility = View.GONE

        if (showOk === ButtonType.START_TIMER) {
            b.startTimerBtn.visibility = View.VISIBLE
            b.buttonSkipTimer.visibility = View.VISIBLE
        }

        if (showOk === ButtonType.ACCEPT) {
            b.acceptButton.visibility = View.VISIBLE
            b.buttonRetest.visibility = View.GONE
        }

        if (showOk === ButtonType.RETEST) {
            b.acceptButton.visibility = View.VISIBLE
            b.buttonRetest.visibility = View.VISIBLE
            b.textDilutionInfo.visibility = View.VISIBLE
        }

        b.acceptButton.setOnClickListener {
            (activity as TestActivity).onAcceptClick()
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment Instruction.
         */
        @JvmStatic
        fun newInstance(instruction: Instruction?, button: ButtonType) =
            InstructionFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_INSTRUCTION, instruction)
                    putSerializable(ARG_SHOW_OK, button)
                }
            }
    }
}