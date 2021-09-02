package io.ffem.lite.common

import io.ffem.lite.R
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.RiskLevel
import io.ffem.lite.model.RiskType

val fluoride = TestDetails(R.string.fluoride, R.string.water, "b61159", R.string.water_tests_1)
val iron = TestDetails(
    R.string.iron, R.string.water, "b1893d", R.string.water_tests_2, riskType = RiskType.SAFETY
)
val pH = TestDetails(
    R.string.ph, R.string.water, "084354", R.string.water_tests_2, riskType = RiskType.ALKALINITY
)
val residualChlorine = TestDetails(
    R.string.residual_chlorine,
    R.string.water,
    "bcc96b",
    R.string.water_tests_2,
    riskType = RiskType.QUANTITY
)
val phosphate = TestDetails(
    R.string.phosphate, R.string.water, "120002", R.string.water_tests_2, riskType = RiskType.SAFETY
)
val phosphorousSoil = TestDetails(
    R.string.phosphorous, R.string.soil, "4813a6", R.string.soil_tests_1
)
val nitrogenSoil = TestDetails(
    R.string.nitrogen, R.string.soil, "5040ba", R.string.soil_tests_1
)
val potassiumSoil = TestDetails(
    R.string.potassium, R.string.soil, "cd6be9", R.string.soil_tests_1
)

val invalidTest =
    TestDetails(R.string.invalid_card_test, R.string.water, "bcc96b", R.string.water_tests_2)

val testDataList = mutableMapOf(
    0 to TestData(residualChlorine, 2.0, 0.25, risk = RiskLevel.HIGH, calibratedResult = 1.0),
    1 to TestData(pH, expectedResultError = ErrorType.NO_MATCH),
    2 to TestData(iron, 0.5, 0.25, risk = RiskLevel.HIGH),
    3 to TestData(phosphate, 1.8, 0.47, risk = RiskLevel.HIGH),
    4 to TestData(iron, 0.5, 0.25, risk = RiskLevel.HIGH),
    5 to TestData(residualChlorine, 2.5, 0.25, risk = RiskLevel.HIGH, maxResult = 2.5),
    6 to TestData(phosphorousSoil, 12.5, 0.322, risk = RiskLevel.LOW),
    7 to TestData(nitrogenSoil, 300.0, 0.21, risk = RiskLevel.HIGH),
    8 to TestData(potassiumSoil, 100.0, 0.16, risk = RiskLevel.MEDIUM),
)

data class TestData(
    var testDetails: TestDetails,
    var expectedResult: Double = -1.0,
    var expectedMarginOfError: Double = -1.0,
    var expectedResultError: ErrorType = ErrorType.NO_ERROR,
    var expectedScanError: Int = -1,
    var risk: RiskLevel = RiskLevel.LOW,
    var maxResult: Double = -1.0,
    var calibratedResult: Double = -1.0
)

data class TestDetails(
    var name: Int,
    var sampleType: Int,
    var id: String,
    var group: Int,
    var riskType: RiskType = RiskType.NORMAL
)