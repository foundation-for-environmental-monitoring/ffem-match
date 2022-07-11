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
        val testResult = db.resultDao().getResult(testId)!!
        val testInfo = App.getTestInfo(testResult.uuid)
        val result = testInfo!!.subTest()
        result.error = testResult.error
        testInfo.fileName = testResult.id
        result.resultInfo = ResultInfo(testResult.value)
        result.resultInfo.luminosity = testResult.luminosity
        // todo: fix this
//        result.setMarginOfError(testResult.marginOfError)
        model.setTest(testInfo)
        model.form = testResult

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
