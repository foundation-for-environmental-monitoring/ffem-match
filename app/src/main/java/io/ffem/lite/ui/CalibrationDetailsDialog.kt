package io.ffem.lite.ui

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.ffem.lite.R
import io.ffem.lite.common.BROADCAST_CALIBRATION_CHANGED
import io.ffem.lite.data.CalibrationDatabase
import io.ffem.lite.databinding.DialogCalibrationDetailsBinding
import io.ffem.lite.model.Calibration
import io.ffem.lite.model.CalibrationDetail
import io.ffem.lite.model.TestInfo
import io.ffem.lite.preference.isDiagnosticMode
import timber.log.Timber

class CalibrationDetailsDialog : DialogFragment() {
    private var _binding: DialogCalibrationDetailsBinding? = null
    private val b get() = _binding!!

    private var testInfo: TestInfo? = null
    private var isEditing = false

    private var savedListener: OnCalibrationDetailsSavedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            testInfo = requireArguments().getParcelable(ARG_TEST_INFO)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCalibrationDetailsBinding.inflate(LayoutInflater.from(context))

        if (!isEditing && isDiagnosticMode()) {
            b.editName.requestFocus()
            showKeyboard()
        } else {
            b.editName.visibility = View.GONE
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.save_calibration)
            .setPositiveButton(
                R.string.save
            ) { _: DialogInterface?, _: Int ->
                closeKeyboard(b.editName)
                dismiss()
            }
            .setNegativeButton(
                R.string.cancel
            ) { _: DialogInterface?, _: Int ->
                closeKeyboard(b.editName)
                dismiss()
            }
        dialog.setView(b.root)
        return dialog.create()
    }

    private fun showKeyboard() {
        val imm = requireContext().getSystemService(
            Context.INPUT_METHOD_SERVICE
        ) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    override fun onStart() {
        super.onStart()
        val d = dialog as AlertDialog?
        if (d != null) {
            val positiveButton = d.getButton(Dialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View) {
                    if (formEntryValid()) {
                        saveDetails(
                            testInfo!!.uuid,
                            b.editName.text.toString(),
                            b.descEdit.text.toString()
                        )
                        closeKeyboard(b.editName)
                        dismiss()
                    }
                }

                fun saveDetails(uuid: String?, name: String, desc: String) {
                    val db: CalibrationDatabase = CalibrationDatabase.getDatabase(requireContext())
                    try {
                        val dao = db.calibrationDao()
                        val calibrationInfo = dao.getCalibrations(uuid)
                        if (calibrationInfo != null) {
                            var saveName = name
                            var count = 0
                            while (dao.getCalibrationByName(saveName) != null) {
                                if (count > 0) {
                                    saveName = "$name ($count)"
                                }
                                count += 1
                            }

                            val calibrationDetail = CalibrationDetail()
                            calibrationDetail.testId = uuid!!
                            calibrationDetail.name = saveName
                            calibrationDetail.isCurrent = false
                            calibrationDetail.desc = desc
                            calibrationDetail.expiry = calibrationInfo.details.expiry
                            calibrationDetail.date = calibrationInfo.details.date

                            var colors = ""
                            for (calibration in calibrationInfo.calibrations) {
                                if (colors.isNotEmpty()) {
                                    colors += ","
                                }
                                colors += calibration.value.toString() + ":" + calibration.color
                            }
                            calibrationDetail.colors = colors

                            val newCalibrations = ArrayList<Calibration>()
                            val id = dao.insert(calibrationDetail)

                            for (calibration in calibrationInfo.calibrations) {
                                val newCalibration = calibration.copy()
                                newCalibration.calibrationId = id
                                newCalibrations.add(newCalibration)
                            }

                            dao.insertAll(newCalibrations)

                            val currentCalibrationDetail = dao.getCalibrationDetail(uuid)
                            if (currentCalibrationDetail != null) {
                                currentCalibrationDetail.name = saveName
                                currentCalibrationDetail.desc = desc
                                dao.update(currentCalibrationDetail)
                            }

                            savedListener?.onCalibrationDetailsSaved()
                            LocalBroadcastManager.getInstance(requireContext())
                                .sendBroadcast(Intent(BROADCAST_CALIBRATION_CHANGED))
                        }
                    } finally {
                        db.close()
                    }
                }

                private fun formEntryValid(): Boolean {
                    if (!isEditing && isDiagnosticMode()
                        && b.editName.text.toString().trim { it <= ' ' }.isEmpty()
                    ) {
                        b.editName.error = getString(R.string.saveInvalidFileName)
                        return false
                    }
                    return true
                }
            })
        }
    }

    /**
     * Hides the keyboard.
     *
     * @param input the EditText for which the keyboard is open
     */
    private fun closeKeyboard(input: EditText?) {
        try {
            val imm = requireContext().getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager
            imm.hideSoftInputFromWindow(input!!.windowToken, 0)
            if (activity != null) {
                val view = requireActivity().currentFocus
                if (view != null) {
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        closeKeyboard(b.editName)
        hideSystemUI()
    }

    override fun onPause() {
        super.onPause()
        closeKeyboard(b.editName)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment is OnCalibrationDetailsSavedListener) {
            savedListener = parentFragment as OnCalibrationDetailsSavedListener
        } else if (context is OnCalibrationDetailsSavedListener) {
            savedListener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        savedListener = null
    }

    interface OnCalibrationDetailsSavedListener {
        fun onCalibrationDetailsSaved()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun hideSystemUI() {
        activity?.window?.decorView?.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    companion object {
        private const val ARG_TEST_INFO = "testInfo"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment SaveCalibrationDialogFragment.
         */
        fun newInstance(testInfo: TestInfo?, isEdit: Boolean): CalibrationDetailsDialog {
            val fragment = CalibrationDetailsDialog()
            fragment.isEditing = isEdit
            val args = Bundle()
            args.putParcelable(ARG_TEST_INFO, testInfo)
            fragment.arguments = args
            return fragment
        }
    }
}