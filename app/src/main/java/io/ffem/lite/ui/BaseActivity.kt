package io.ffem.lite.ui

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.ffem.lite.R
import io.ffem.lite.preference.isDiagnosticMode
import kotlinx.android.synthetic.main.app_bar_layout.*

abstract class BaseActivity : AppCompatActivity() {

    private var mTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateTheme()
        changeActionBarStyleBasedOnCurrentMode()
    }

    private fun updateTheme() {

        setTheme(R.style.AppTheme_Main)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true)
        val windowBackground = typedValue.data
        window.setBackgroundDrawable(ColorDrawable(windowBackground))

    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (toolbar != null) {
            try {
                setSupportActionBar(toolbar)
            } catch (ignored: Exception) {
                // do nothing
            }

        }
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = ""
        }
    }

    override fun onResume() {
        super.onResume()
        changeActionBarStyleBasedOnCurrentMode()
        if (supportActionBar != null) {
            supportActionBar!!.title = ""
        }

        title = mTitle
    }

    override fun setTitle(title: CharSequence?) {
        if (textToolbarTitle != null && title != null) {
            mTitle = title.toString()
            textToolbarTitle.text = title
        }
    }

    override fun setTitle(titleId: Int) {
        if (textToolbarTitle != null && titleId != 0) {
            mTitle = getString(titleId)
            textToolbarTitle.setText(titleId)
        }
    }

    /**
     * Switches action bar style between user mode or diagnostic mode
     */
    protected fun changeActionBarStyleBasedOnCurrentMode() {
        if (isDiagnosticMode()) {
            if (supportActionBar != null) {
                supportActionBar!!.setBackgroundDrawable(
                    ColorDrawable(
                        ContextCompat.getColor(this, R.color.diagnostic)
                    )
                )
            }
            window.statusBarColor = ContextCompat.getColor(this, R.color.diagnostic_status)
        } else {

            val typedValue = TypedValue()
            theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
            var color = typedValue.data

            if (supportActionBar != null) {
                supportActionBar!!.setBackgroundDrawable(ColorDrawable(color))
            }

            theme.resolveAttribute(R.attr.colorPrimaryDark, typedValue, true)
            color = typedValue.data

            window.statusBarColor = color
        }
    }
}


