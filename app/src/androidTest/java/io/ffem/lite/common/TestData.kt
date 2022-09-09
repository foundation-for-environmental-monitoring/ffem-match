package io.ffem.lite.common

import io.ffem.lite.R
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.RiskLevel
import io.ffem.lite.model.RiskType


val fluoride =
    TestDetails(R.string.fluoride, R.string.water, "WC-FM-F", 0, R.string.water_tests_1)

val pH =
    TestDetails(
        R.string.ph,
        R.string.water,
        "WC-FM-pH",
        45,
        R.string.water_tests_2,
        riskType = RiskType.ALKALINITY
    )

val iron = TestDetails(
    R.string.iron,
    R.string.water,
    "WC-FM-Fe",
    300,
    R.string.water_tests_3,
    riskType = RiskType.SAFETY
)

val residualChlorine = TestDetails(
    R.string.residual_chlorine,
    R.string.water,
    "WC-FM-Cl",
    0,
    R.string.water_tests_1
)

val invalidTest =
    TestDetails(R.string.invalid_card_test, R.string.water, "WR-FM-Err", 0, R.string.water_tests_2)

val testDataList = mutableMapOf(
    0 to TestData(residualChlorine, 0.2, 0.25, risk = RiskLevel.RISK_1, calibratedResult = 1.0),
    1 to TestData(fluoride, 0.26, 0.25, risk = RiskLevel.RISK_1, calibratedResult = 0.26),
    2 to TestData(fluoride, 0.5, 0.25, risk = RiskLevel.RISK_1, calibratedResult = 1.0),
    3 to TestData(pH, 4.03, 0.5, risk = RiskLevel.RISK_1, calibratedResult = 1.0),
    4 to TestData(iron, 1.48, 0.15, risk = RiskLevel.RISK_1, calibratedResult = 1.0),
    5 to TestData(
        fluoride,
        -1.0,
        0.15,
        risk = RiskLevel.RISK_1,
        calibratedResult = 0.0,
        expectedResultError = ErrorType.NO_MATCH
    ),
    6 to TestData(
        fluoride,
        1.58,
        0.15,
        risk = RiskLevel.RISK_1,
        calibratedResult = 0.0,
        expectedResultError = ErrorType.NO_MATCH
    )
)

data class TestData(
    var testDetails: TestDetails,
    var expectedResult: Double = -1.0,
    var expectedMarginOfError: Double = -1.0,
    var expectedResultError: ErrorType = ErrorType.NO_ERROR,
    var expectedScanError: Int = -1,
    var risk: RiskLevel = RiskLevel.RISK_0,
    var maxResult: Double = -1.0,
    var calibratedResult: Double = -1.0,
    var calibratedRisk: RiskLevel = RiskLevel.RISK_0
)

data class TestDetails(
    var name: Int,
    var sampleType: Int,
    var id: String,
    var timeDelay: Int,
    var group: Int,
    var riskType: RiskType = RiskType.NORMAL
)