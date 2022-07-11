package io.ffem.lite.ui

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.ffem.lite.R
import io.ffem.lite.databinding.EditCustomDilutionBinding

class EditCustomDilution : DialogFragment() {
    private var listener: OnCustomDilutionListener? = null
    private lateinit var dialogView: View
    private var _binding: EditCustomDilutionBinding? = null
    private val b get() = _binding!!
    private var editDilutionFactor: EditText? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext()).apply {
            dialogView =
                onCreateView(LayoutInflater.from(requireContext()), null, savedInstanceState)
            onViewCreated(dialogView, savedInstanceState)
            setView(dialogView)
                .setPositiveButton(
                    R.string.ok
                ) { _: DialogInterface?, _: Int ->
                    if (formEntryValid()) {
                        if (listener != null) {
                            listener!!.onCustomDilution(
                                editDilutionFactor?.text.toString().toInt()
                            )
                        }
                        closeKeyboard(editDilutionFactor)
                        dismiss()
                    }
                }
                .setNegativeButton(
                    R.string.cancel
                ) { _: DialogInterface?, _: Int ->
                    dismiss()
                }
        }.create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = EditCustomDilutionBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editDilutionFactor = dialogView.findViewById(R.id.dilutionFactor_edit)
    }

    private fun formEntryValid(): Boolean {
        if (editDilutionFactor?.text.toString().trim { it <= ' ' }.isEmpty()) {
            editDilutionFactor?.error = getString(R.string.required)
            return false
        }
        return true
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (context is OnCustomDilutionListener) {
            context
        } else {
            throw IllegalArgumentException(
                context.toString()
                        + " must implement OnCustomDilutionListener"
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onResume() {
        super.onResume()
        editDilutionFactor?.post {
            editDilutionFactor?.requestFocus()
            val imm =
                editDilutionFactor?.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(
                editDilutionFactor,
                InputMethodManager.SHOW_IMPLICIT
            )
        }
    }

    /**
     * Hides the keyboard.
     *
     * @param input the EditText for which the keyboard is open
     */
    private fun closeKeyboard(input: EditText?) {
        val imm = requireContext().getSystemService(
            Context.INPUT_METHOD_SERVICE
        ) as InputMethodManager
        imm.hideSoftInputFromWindow(input!!.windowToken, 0)
    }

    interface OnCustomDilutionListener {
        fun onCustomDilution(dilution: Int)
    }
}