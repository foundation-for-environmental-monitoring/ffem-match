package io.ffem.lite.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
import io.ffem.lite.R
import io.ffem.lite.databinding.FragmentTitrationBinding
import io.ffem.lite.model.Result
import io.ffem.lite.util.MathUtil
import io.ffem.lite.util.toLocalString
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class TitrationFragment : BaseFragment() {
    private var _binding: FragmentTitrationBinding? = null
    private val b get() = _binding!!
    private val model: TestInfoViewModel by activityViewModels()

    private var submitResultListener: OnSubmitResultListener? = null

    private fun getToolbar(): Toolbar? {
        return requireView().findViewById(R.id.toolbar)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTitrationBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MainScope().launch {
            delay(200)
            showSoftKeyboard(b.editTitration1)
        }

        b.editTitration1.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                b.editTitration1.error = null
                b.editTitration2.error = null
            }

            override fun afterTextChanged(editable: Editable) {}
        })
        b.editTitration2.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                b.editTitration1.error = null
                b.editTitration2.error = null
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        val testInfo = model.test.get()!!
        if (getToolbar() != null) {
            getToolbar()!!.title = testInfo.name?.toLocalString()
        }

        if (testInfo.results.size > 1) {
            b.textInput1.text = testInfo.subTest().name!!.toLocalString()
            b.textInput2.text = testInfo.results[1].name!!.toLocalString()
            b.editTitration2.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (submitResultListener != null) {
                        val n1String = b.editTitration1.text.toString()
                        val n2String = b.editTitration2.text.toString()
                        if (n1String.isEmpty()) {
                            b.editTitration1.error = getString(R.string.value_is_required)
                            b.editTitration1.requestFocus()
                        } else {
                            val n1 = n1String.toFloat()
                            if (n2String.isEmpty()) {
                                b.editTitration2.error = getString(R.string.value_is_required)
                                b.editTitration2.requestFocus()
                            } else {
                                val n2 = n2String.toFloat()
                                if (n1 > n2) {
                                    b.editTitration1.error = getString(
                                        R.string.titration_entry_error,
                                        b.textInput1.text.toString(),
                                        b.textInput2.text.toString()
                                    )
                                    b.editTitration1.requestFocus()
                                } else {
                                    closeKeyboard(b.editTitration2)
                                    closeKeyboard(b.editTitration1)
                                    for (i in testInfo.results.indices) {
                                        val formula = testInfo.results[i].formula
                                        if (formula!!.isNotEmpty()) {
                                            testInfo.results[i].setFinalResult(
                                                MathUtil.eval(
                                                    String.format(
                                                        Locale.US,
                                                        formula,
                                                        n1,
                                                        n2
                                                    )
                                                )
                                            )
                                        }
                                    }
                                    submitResultListener!!.onSubmitResult(testInfo.results)
                                }
                            }
                        }
                    }
                    return@setOnEditorActionListener true
                }
                false
            }
        } else {
            b.inputTitleTxt.text = getString(R.string.enter_titration_result)
            b.textInput1.visibility = View.GONE
            b.textInput2.visibility = View.GONE
            b.editTitration2.visibility = View.GONE
            b.editTitration1.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (submitResultListener != null) {
                        val n1String = b.editTitration1.text.toString()
                        if (n1String.isEmpty()) {
                            b.editTitration1.error = getString(R.string.value_is_required)
                            b.editTitration1.requestFocus()
                        } else {
                            closeKeyboard(b.editTitration2)
                            closeKeyboard(b.editTitration1)
                            testInfo.subTest().setResult(n1String.toDouble())
                            submitResultListener!!.onSubmitResult(testInfo.results)
                        }
                    }
                    return@setOnEditorActionListener true
                }
                false
            }
        }
//        }
    }

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

    private fun showSoftKeyboard(view: View?) {
        if (activity != null && requireView().requestFocus()) {
            val imm =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    //    private void hideSoftKeyboard(View view) {
    //        if (getActivity() != null) {
    //            InputMethodManager imm = (InputMethodManager)
    //                    getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    //            if (imm != null) {
    //                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
    //            }
    //        }
    //    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        submitResultListener = if (context is OnSubmitResultListener) {
            context
        } else {
            throw IllegalArgumentException(
                context.toString()
                        + " must implement OnSubmitResultListener"
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        submitResultListener = null
    }

    interface OnSubmitResultListener {
        fun onSubmitResult(results: ArrayList<Result>)
    }
}