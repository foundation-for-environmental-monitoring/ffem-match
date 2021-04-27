package io.ffem.lite.ui

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import io.ffem.lite.R
import io.ffem.lite.preference.isDiagnosticMode

abstract class BaseFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateTheme()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        changeActionBarStyleBasedOnCurrentMode()
    }

    private fun updateTheme() {
        requireActivity().setTheme(R.style.AppTheme_Main)
    }

    /**
     * Switches action bar style between user mode or diagnostic mode
     */
    private fun changeActionBarStyleBasedOnCurrentMode() {
        val toolbar = view?.findViewById<Toolbar>(R.id.toolbar)
        val appActivity = (requireActivity() as AppCompatActivity)
        if (isDiagnosticMode()) {
            toolbar?.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.diagnostic)
            )
        } else {
            val typedValue = TypedValue()
            appActivity.theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
            val color = typedValue.data
            toolbar?.setBackgroundColor(color)
        }
    }
}


