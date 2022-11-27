package io.ffem.lite.model

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import io.ffem.lite.data.AppDatabase
import io.ffem.lite.data.TestResult

class MainViewModel(application: Application) : ViewModel() {

    val testResults: LiveData<List<TestResult>>
    private val repository: ResultRepository

    init {
        val resultDb = AppDatabase.getDatabase(application)
        val resultDao = resultDb.resultDao()
        repository = ResultRepository(resultDao)

        testResults = repository.testResults
    }
}