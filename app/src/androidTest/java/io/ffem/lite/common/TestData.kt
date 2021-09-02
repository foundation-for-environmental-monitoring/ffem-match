package io.ffem.lite.common

import io.ffem.lite.R
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.RiskLevel
import io.ffem.lite.model.RiskType

val fluoride = TestDetails(R.string.fluoride, R.string.water, "WR-FM-F", R.string.water_tests_1)
val iron = TestDetails(
    R.string.iron, R.string.water, "WR-FM-Fe", R.string.water_tests_2, riskType = RiskType.SAFETY
)
val pH = TestDetails(
    R.string.ph, R.string.water, "WR-FM-pH", R.string.water_tests_2, riskType = RiskType.ALKALINITY
)
val phosphate = TestDetails(
    R.string.phosphate,
    R.string.water,
    "WR-FM-PO4",
    R.string.water_tests_2,
    riskType = RiskType.SAFETY
)
val residualChlorine = TestDetails(
    R.string.residual_chlorine,
    R.string.water,
    "WR-FM-Cl",
    R.string.water_tests_2,
    riskType = RiskType.QUANTITY
)
val phosphorousSoil = TestDetails(
    R.string.phosphorous, R.string.soil, "SR-FM-P", R.string.soil_tests_1
)
val nitrogenSoil = TestDetails(
    R.string.nitrogen, R.string.soil, "SR-FM-N", R.string.soil_tests_1
)
val potassiumSoil = TestDetails(
    R.string.potassium, R.string.soil, "SR-FM-K", R.string.soil_tests_1
)

val invalidTest =
    TestDetails(R.string.invalid_card_test, R.string.water, "WR-FM-Err", R.string.water_tests_2)

val testDataList = mutableMapOf(
    0 to TestData(residualChlorine, 2.0, 0.25, risk = RiskLevel.HIGH, calibratedResult = 1.0),
    1 to TestData(pH, expectedResultError = ErrorType.NO_MATCH),
    2 to TestData(iron, 0.5, 0.25, risk = RiskLevel.HIGH),
    3 to TestData(phosphate, 1.8, 0.47, risk = RiskLevel.HIGH),
    4 to TestData(iron, 0.5, 0.25, risk = RiskLevel.HIGH),
    5 to TestData(residualChlorine, 2.5, 0.25, risk = RiskLevel.HIGH, maxResult = 3.0),
    6 to TestData(phosphorousSoil, 12.5, 0.322, risk = RiskLevel.LOW),
    7 to TestData(nitrogenSoil, 300.0, 0.21, risk = RiskLevel.HIGH),
    8 to TestData(potassiumSoil, 100.0, 0.16, risk = RiskLevel.MEDIUM),
    9 to TestData(residualChlorine, 2.0, 0.25, risk = RiskLevel.HIGH),
    10 to TestData(residualChlorine, 2.0, 0.25, risk = RiskLevel.HIGH),
    11 to TestData(residualChlorine, 2.0, 0.25, risk = RiskLevel.HIGH),
    12 to TestData(residualChlorine, expectedScanError = R.string.not_bright),
    13 to TestData(residualChlorine, expectedScanError = R.string.too_close),
    14 to TestData(residualChlorine, expectedScanError = R.string.closer),
    15 to TestData(residualChlorine, expectedScanError = R.string.too_close)
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