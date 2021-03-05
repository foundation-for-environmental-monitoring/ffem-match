package io.ffem.lite.ui

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.preference.isDiagnosticMode

abstract class BaseActivity : AppCompatActivity() {

    private var toolbar: Toolbar? = null
    private var textToolbarTitle: TextView? = null
    private var mTitle: String? = null
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG && !isTestLab()) {
            firebaseAnalytics = Firebase.analytics
            FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true)
        }
        updateTheme()
        changeActionBarStyleBasedOnCurrentMode()
    }

    private fun updateTheme() {

        if (this !is TestActivity) {
            setTheme(R.style.AppTheme_Main)
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true)
        val windowBackground = typedValue.data
        window.setBackgroundDrawable(ColorDrawable(windowBackground))
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toolbar = findViewById(R.id.toolbar)
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
        textToolbarTitle = findViewById(R.id.toolbar_title_txt)
        if (textToolbarTitle != null && title != null) {
            mTitle = title.toString()
            textToolbarTitle!!.text = title
        }
    }

    override fun setTitle(titleId: Int) {
        textToolbarTitle = findViewById(R.id.toolbar_title_txt)
        if (textToolbarTitle != null && titleId != 0) {
            mTitle = getString(titleId)
            textToolbarTitle!!.setText(titleId)
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

    private fun isTestLab(): Boolean {
        try {
            val testLabSetting = Settings.System.getString(contentResolver, "firebase.test.lab")
            if ("true" == testLabSetting) {
                return true
            }
        } catch (e: Exception) {
        }
        return false
    }
}


