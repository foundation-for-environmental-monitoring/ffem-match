package io.ffem.lite.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import io.ffem.lite.R
import io.ffem.lite.preference.isDiagnosticMode

abstract class BaseFragment : Fragment() {

    override fun onResume() {
        super.onResume()
        changeActionBarStyleBasedOnCurrentMode()
        val appActivity = (requireActivity() as AppCompatActivity)
        if (appActivity.supportActionBar != null) {
            appActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    /**
     * Switches action bar style between user mode or diagnostic mode
     */
    private fun changeActionBarStyleBasedOnCurrentMode() {
        val toolbar = view?.findViewById<Toolbar>(R.id.toolbar)
        val appActivity = (requireActivity() as AppCompatActivity)
        if (toolbar != null) {
            try {
                appActivity.setSupportActionBar(toolbar)
            } catch (ignored: Exception) {
                // do nothing
            }

            if (isDiagnosticMode()) {
                appActivity.supportActionBar?.setBackgroundDrawable(
                    ColorDrawable(
                        ContextCompat.getColor(requireContext(), R.color.diagnostic)
                    )
                )
                toolbar.setTitleTextColor(Color.WHITE)
            } else {
                val typedValue = TypedValue()
                appActivity.theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
                val color = typedValue.data

                appActivity.supportActionBar?.setBackgroundDrawable(ColorDrawable(color))
                toolbar.setTitleTextColor(Color.WHITE)
            }
        }
    }
}
