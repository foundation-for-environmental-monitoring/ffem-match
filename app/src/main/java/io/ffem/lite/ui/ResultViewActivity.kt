package io.ffem.lite.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.model.TestInfo

class ResultViewActivity : BaseActivity() {
    lateinit var model: TestInfoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_view)

        model = ViewModelProvider(this).get(
            TestInfoViewModel::class.java
        )

        val testInfo = intent.getParcelableExtra<TestInfo>(App.TEST_INFO_KEY)!!
        model.setTest(testInfo)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
