package io.ffem.lite.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.common.RESULT_ID
import io.ffem.lite.data.AppDatabase
import io.ffem.lite.model.ResultInfo

class ResultViewActivity : BaseActivity() {
    lateinit var model: TestInfoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result_view)

        model = ViewModelProvider(this).get(
            TestInfoViewModel::class.java
        )

        val testId = intent.getStringExtra(RESULT_ID)!!
        val db = AppDatabase.getDatabase(baseContext)
        val result = db.resultDao().getResult(testId)!!
        val testInfo = App.getTestInfo(result.uuid)
        testInfo!!.error = result.error
        testInfo.fileName = result.id
        testInfo.resultInfo = ResultInfo(result.value, result.luminosity)
        testInfo.resultInfoGrayscale = ResultInfo(result.valueGrayscale)
        testInfo.setMarginOfError(result.marginOfError)
        model.setTest(testInfo)
        model.form = result

        supportFragmentManager
            .beginTransaction()
            .add(R.id.fragment_container, ResultFragment(false))
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
