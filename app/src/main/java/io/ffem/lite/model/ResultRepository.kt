package io.ffem.lite.model

import androidx.lifecycle.LiveData
import io.ffem.lite.data.CardCalibrationDao
import io.ffem.lite.data.TestResult

class ResultRepository(cardCalibrationDao: CardCalibrationDao) {
    val testResults: LiveData<List<TestResult>> = cardCalibrationDao.getResults()
}