package io.ffem.lite.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import io.ffem.lite.R
import kotlinx.android.synthetic.main.app_bar_layout.*

class ResultActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        setSupportActionBar(toolbar)

        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, ResultFragment()).commit()

        setTitle(R.string.result)
    }

    fun onSubmitResult(@Suppress("UNUSED_PARAMETER") view: View) {
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
