package io.ffem.lite.common

import io.ffem.lite.R
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.RiskLevel
import io.ffem.lite.model.RiskType

val fluoride = TestDetails(R.string.fluoride, "bcc31cb61159", R.string.water_tests_1)
val iron = TestDetails(
    R.string.iron, "352410b1893d", R.string.water_tests_2, riskType = RiskType.SAFETY
)
val pH = TestDetails(
    R.string.ph, "7aa7fc084354", R.string.water_tests_2, riskType = RiskType.ALKALINITY
)
val residualChlorine = TestDetails(
    R.string.residual_chlorine, "ad0d47bcc96b", R.string.water_tests_2, riskType = RiskType.QUANTITY
)
val phosphate = TestDetails(
    R.string.phosphate, "0242ac120002", R.string.water_tests_2, riskType = RiskType.SAFETY
)
val invalidTest = TestDetails(R.string.invalid_card_test, "ad0d47bcc96b", R.string.water_tests_2)

val testDataList = mutableMapOf(
    0 to TestData(residualChlorine, 2.0, 0.25, risk = RiskLevel.HIGH, calibratedResult = 1.0),
    1 to TestData(pH, expectedResultError = ErrorType.NO_MATCH),
    2 to TestData(iron, 0.5, 0.25, risk = RiskLevel.HIGH),
    3 to TestData(phosphate, 1.8, 0.47, risk = RiskLevel.HIGH),
    4 to TestData(iron, 0.5, 0.25, risk = RiskLevel.HIGH),
    5 to TestData(residualChlorine, 2.5, 0.25, risk = RiskLevel.HIGH, maxResult = 2.5)
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
    var id: String,
    var group: Int,
    var riskType: RiskType = RiskType.NORMAL
)