package io.ffem.lite.model

class PageIndex {
    var instruction = -1
    var listPage = 0
    var calibrateOptionPage = -1
    var dilutionPage = -1
    var calibrationPage = -1
    var startTimerPage = -1
    var testPage = -1
    var confirmationPage = -1
    var resultPage = -1
    var submitPage = -1
    var totalPageCount = -1
    var instructionFirstIndex = -1

    fun clear() {
        instruction = -1
        listPage = 0
        calibrateOptionPage = -1
        resultPage = -1
        submitPage = -1
        startTimerPage = -1
        dilutionPage = -1
        testPage = -1
        resultPage = -1
        totalPageCount = -1
    }
}