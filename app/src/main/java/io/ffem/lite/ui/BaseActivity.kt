package io.ffem.lite.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.analytics.FirebaseAnalytics
import io.ffem.lite.R
import io.ffem.lite.preference.isDiagnosticMode

abstract class BaseActivity : AppCompatActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        updateTheme()
        changeActionBarStyleBasedOnCurrentMode()
    }

//    private fun updateTheme() {
//
//        if (this is SettingsActivity) {
//            setTheme(R.style.Theme_Main_Settings)
//        } else if (this !is TestActivity) {
//            setTheme(R.style.Theme_Main)
//        }
//
//        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
//
//        val typedValue = TypedValue()
//        theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true)
//        val windowBackground = typedValue.data
//        window.setBackgroundDrawable(ColorDrawable(windowBackground))
//    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        initToolbar()
        changeActionBarStyleBasedOnCurrentMode()
    }

    override fun onResume() {
        super.onResume()
        try {
            changeActionBarStyleBasedOnCurrentMode()
        } catch (_: Exception) {
        }
    }

    /**
     * Switches action bar style between user mode or diagnostic mode
     */
    fun changeActionBarStyleBasedOnCurrentMode() {
        if (isDiagnosticMode()) {
            if (supportActionBar != null) {
                supportActionBar!!.setBackgroundDrawable(
                    ColorDrawable(
                        ContextCompat.getColor(this, R.color.diagnostic)
                    )
                )
                val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
                toolbar.setTitleTextColor(Color.WHITE)
            }
            window.statusBarColor = ContextCompat.getColor(this, R.color.diagnostic_status)
        } else {

            val typedValue = TypedValue()
            theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
            var color = typedValue.data

            if (supportActionBar != null) {
                supportActionBar!!.setBackgroundDrawable(ColorDrawable(color))

                val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
                toolbar.setTitleTextColor(Color.WHITE)
            }

            theme.resolveAttribute(R.attr.colorPrimaryVariant, typedValue, true)
            color = typedValue.data

            window.statusBarColor = color
        }
    }

    private fun initToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
    }
}


