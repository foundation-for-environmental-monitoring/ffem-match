package io.ffem.lite.ui

import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.fragment.app.DialogFragment
import io.ffem.lite.databinding.FragmentNoticesDialogBinding


class NoticesDialogFragment : DialogFragment() {
    private var _binding: FragmentNoticesDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoticesDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        makeEverythingClickable(binding.aboutContainer)

        binding.homeButton.setOnClickListener { dismiss() }
        super.onViewCreated(view, savedInstanceState)
    }

    private fun makeEverythingClickable(vg: ViewGroup) {
        for (i in 0 until vg.childCount) {
            if (vg.getChildAt(i) is ViewGroup) {
                makeEverythingClickable(vg.getChildAt(i) as ViewGroup)
            } else if (vg.getChildAt(i) is TextView) {
                val tv = vg.getChildAt(i) as TextView
                if (tv.isClickable) {
                    tv.setLinkTextColor(
                        requireContext().getColorFromAttr(android.R.attr.textColorSecondary)
                    )
                    tv.movementMethod = LinkMovementMethod.getInstance()
                }
            }
        }
    }

    @ColorInt
    fun Context.getColorFromAttr(
        @AttrRes attrColor: Int
    ): Int {
        val typedArray = theme.obtainStyledAttributes(intArrayOf(attrColor))
        val textColor = typedArray.getColor(0, 0)
        typedArray.recycle()
        return textColor
    }

    override fun onResume() {
        if (dialog!!.window != null) {
            val params = dialog!!.window!!.attributes
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.MATCH_PARENT
            dialog!!.window!!.attributes = params
        }

        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        fun newInstance(): NoticesDialogFragment {
            return NoticesDialogFragment()
        }
    }
}
