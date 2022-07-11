package io.ffem.lite.ui

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.InputMethodManager
import android.widget.DatePicker
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.ffem.lite.R
import io.ffem.lite.data.CalibrationDatabase
import io.ffem.lite.databinding.DialogCalibrationExpiryBinding
import io.ffem.lite.model.CalibrationDetail
import io.ffem.lite.model.TestInfo
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.util.ThemeUtils
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class CalibrationExpiryDialog : DialogFragment() {
    private var _binding: DialogCalibrationExpiryBinding? = null
    private val b get() = _binding!!

    private val calendar = Calendar.getInstance()
    private var testInfo: TestInfo? = null
    private var isEditing = false

    private var savedListener: OnCalibrationExpirySavedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            testInfo = requireArguments().getParcelable(ARG_TEST_INFO)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCalibrationExpiryBinding.inflate(LayoutInflater.from(context))

        val db: CalibrationDatabase = CalibrationDatabase.getDatabase(requireContext())
        try {
            val calibrationDetail = db.calibrationDao().getCalibrationDetail(testInfo!!.uuid)
            if (calibrationDetail != null && calibrationDetail.expiry > Date().time) {
                if (calibrationDetail.expiry >= 0) {
                    calendar.timeInMillis = calibrationDetail.expiry
                    b.editExpiryDate.setText(
                        SimpleDateFormat("dd-MMM-yyyy", Locale.US)
                            .format(Date(calibrationDetail.expiry))
                    )
                }
            }
        } finally {
            db.close()
        }

        setupDatePicker()
        if (!isEditing && isDiagnosticMode()) {
            showKeyboard()
        }
        val dialog = MaterialAlertDialogBuilder(
            requireContext()
        )
            .setTitle(R.string.reagent_expiry)
            .setPositiveButton(
                R.string.save
            ) { _: DialogInterface?, _: Int ->
                dismiss()
            }
            .setNegativeButton(
                R.string.cancel
            ) { _: DialogInterface?, _: Int ->
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

    private fun setupDatePicker() {
        val onDateSetListener =
            OnDateSetListener { _: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                calendar[Calendar.YEAR] = year
                calendar[Calendar.MONTH] = monthOfYear
                calendar[Calendar.DAY_OF_MONTH] = dayOfMonth
                val date = SimpleDateFormat("dd MMM yyyy", Locale.US).format(calendar.time)
                b.editExpiryDate.setText(date)
            }
        val datePickerDialog = DatePickerDialog(
            requireContext(), onDateSetListener,
            calendar[Calendar.YEAR],
            calendar[Calendar.MONTH],
            calendar[Calendar.DAY_OF_MONTH]
        )
        val date = Calendar.getInstance()
        date.add(Calendar.DATE, 1)
        date[Calendar.HOUR_OF_DAY] = date.getMinimum(Calendar.HOUR_OF_DAY)
        date[Calendar.MINUTE] = date.getMinimum(Calendar.MINUTE)
        date[Calendar.SECOND] = date.getMinimum(Calendar.SECOND)
        date[Calendar.MILLISECOND] = date.getMinimum(Calendar.MILLISECOND)
        datePickerDialog.datePicker.minDate = date.timeInMillis
        if (testInfo!!.monthsValid != 0) {
            date.add(Calendar.MONTH, testInfo!!.monthsValid)
            date[Calendar.HOUR_OF_DAY] = date.getMaximum(Calendar.HOUR_OF_DAY)
            date[Calendar.MINUTE] = date.getMaximum(Calendar.MINUTE)
            date[Calendar.SECOND] = date.getMaximum(Calendar.SECOND)
            date[Calendar.MILLISECOND] = date.getMaximum(Calendar.MILLISECOND)
            datePickerDialog.datePicker.maxDate = date.timeInMillis
        }
        b.editExpiryDate.onFocusChangeListener = OnFocusChangeListener { _: View?, k: Boolean ->
            if (k) {
                showDatePicker(datePickerDialog)
            }
        }
        b.editExpiryDate.setOnClickListener {
            showDatePicker(datePickerDialog)
        }
    }

    private fun showDatePicker(datePickerDialog: DatePickerDialog) {
        datePickerDialog.show()
        datePickerDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(
            ThemeUtils(context).colorOnSurface
        )
        datePickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(
            ThemeUtils(context).colorOnSurface
        )
    }

    override fun onStart() {
        super.onStart()
        val d = dialog as AlertDialog?
        if (d != null) {
            val positiveButton = d.getButton(Dialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View) {
                    if (formEntryValid()) {
                        saveDetails(testInfo!!.uuid)
                        closeKeyboard(b.editExpiryDate)
                        dismiss()
                    }
                }

                fun saveDetails(uuid: String?) {
                    val db: CalibrationDatabase = CalibrationDatabase.getDatabase(requireContext())
                    try {
                        var insert = false
                        val dao = db.calibrationDao()
                        var calibrationDetail = dao.getCalibrationDetail(uuid)
                        if (calibrationDetail == null) {
                            insert = true
                            calibrationDetail = CalibrationDetail()
                            calibrationDetail.testId = uuid!!
                            calibrationDetail.isCurrent = true
                        }
                        calibrationDetail.date = Calendar.getInstance().timeInMillis
                        calibrationDetail.expiry = calendar.timeInMillis
                        if (insert) {
                            dao.insert(calibrationDetail)
                        } else {
                            dao.update(calibrationDetail)
                        }
                        savedListener?.onCalibrationExpirySavedListener()
                    } finally {
                        db.close()
                    }
                }

                private fun formEntryValid(): Boolean {
                    if (!isEditing && isDiagnosticMode()) {
                        return false
                    }
                    if (b.editExpiryDate.text.toString().trim { it <= ' ' }.isEmpty()) {
                        b.editExpiryDate.error = getString(R.string.required)
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment is OnCalibrationExpirySavedListener) {
            savedListener = parentFragment as OnCalibrationExpirySavedListener
        } else if (context is OnCalibrationExpirySavedListener) {
            savedListener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        savedListener = null
    }

    interface OnCalibrationExpirySavedListener {
        fun onCalibrationExpirySavedListener()
    }

    override fun onPause() {
        super.onPause()

        activity?.window?.decorView?.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TEST_INFO = "testInfo"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment SaveCalibrationDialogFragment.
         */
        fun newInstance(testInfo: TestInfo?, isEdit: Boolean): CalibrationExpiryDialog {
            val fragment = CalibrationExpiryDialog()
            fragment.isEditing = isEdit
            val args = Bundle()
            args.putParcelable(ARG_TEST_INFO, testInfo)
            fragment.arguments = args
            return fragment
        }
    }
}