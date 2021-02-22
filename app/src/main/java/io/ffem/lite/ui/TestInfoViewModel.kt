package io.ffem.lite.ui

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import io.ffem.lite.data.AppDatabase
import io.ffem.lite.data.TestResult
import io.ffem.lite.model.TestInfo

class TestInfoViewModel(application: Application) : AndroidViewModel(application) {
    lateinit var db: AppDatabase
    lateinit var form: TestResult

    @JvmField
    val test = ObservableField<TestInfo>()

    fun setTest(testInfo: TestInfo?) {
        test.set(testInfo)
        Companion.testInfo = testInfo
    }

    companion object {
        private var testInfo: TestInfo? = null
    }
}