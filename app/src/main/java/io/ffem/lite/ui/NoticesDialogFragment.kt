package io.ffem.lite.ui

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import io.ffem.lite.R

class NoticesDialogFragment : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notices_dialog, container, false)

        val linearLayout = view.findViewById<LinearLayout>(R.id.about_container)
        makeEverythingClickable(linearLayout)

        view.findViewById<ImageButton>(R.id.home_button).setOnClickListener { dismiss() }

        return view
    }

    private fun makeEverythingClickable(vg: ViewGroup) {
        for (i in 0 until vg.childCount) {
            if (vg.getChildAt(i) is ViewGroup) {
                makeEverythingClickable(vg.getChildAt(i) as ViewGroup)
            } else if (vg.getChildAt(i) is TextView) {
                val tv = vg.getChildAt(i) as TextView
                if (tv.isClickable) {
                    tv.setLinkTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.colorAccent
                        )
                    )
                    tv.movementMethod = LinkMovementMethod.getInstance()
                }
            }
        }
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

    companion object {

        fun newInstance(): NoticesDialogFragment {
            return NoticesDialogFragment()
        }
    }
}
