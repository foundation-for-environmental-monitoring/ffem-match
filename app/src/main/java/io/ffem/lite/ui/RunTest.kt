package io.ffem.lite.ui

import io.ffem.lite.model.Calibration


interface RunTest {
    fun setCalibration(item: Calibration?)
    fun setDilution(dilution: Int)
}