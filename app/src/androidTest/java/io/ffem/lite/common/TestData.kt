package io.ffem.lite.common

import io.ffem.lite.R
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.RiskLevel
import io.ffem.lite.model.RiskType

val fluoride = TestDetails("Fluoride", "bcc31cb61159", R.string.water_tests_1, 0)
val fluorideHighRange = TestDetails(
    "Fluoride - High Range", "b6cb9742737a",
    R.string.water_tests_1, 1
)
val iron = TestDetails(
    "Iron", "352410b1893d", R.string.water_tests_2,
    2, riskType = RiskType.SAFETY
)
val nitrate = TestDetails("Nitrate", "1b6b24c0fa93", R.string.water_tests_2, 3)
val pH = TestDetails("pH", "7aa7fc084354", R.string.water_tests_2, 0,riskType = RiskType.ALKALINITY)
val residualChlorine = TestDetails(
    "Residual Chlorine", "ad0d47bcc96b", R.string.water_tests_2,
    1, riskType = RiskType.QUANTITY
)
val invalidTest = TestDetails("Residual Chlorine", "ad0d47bcc96b", R.string.water_tests_2, 2)

val testDataList = mutableMapOf(
    0 to TestData(residualChlorine, 0.5, 0.25, risk = RiskLevel.LOW),
    1 to TestData(residualChlorine, 0.0, 0.3, risk = RiskLevel.MEDIUM),
    2 to TestData(residualChlorine, expectedScanError = R.string.invalid_barcode),
    3 to TestData(fluoride, expectedScanError = R.string.invalid_barcode),
    4 to TestData(pH, expectedResultError = ErrorType.NO_MATCH),
    5 to TestData(residualChlorine, expectedScanError = R.string.color_card_not_found),
    6 to TestData(residualChlorine, expectedResultError = ErrorType.NO_MATCH),
    7 to TestData(residualChlorine, 0.5, 0.25, risk = RiskLevel.LOW),
    8 to TestData(residualChlorine, 1.5, 0.25, risk = RiskLevel.HIGH),
    9 to TestData(residualChlorine, expectedScanError = R.string.align_color_card),
    10 to TestData(residualChlorine, expectedResultError = ErrorType.CALIBRATION_ERROR),
    11 to TestData(residualChlorine, expectedScanError = R.string.correct_camera_tilt),
    12 to TestData(residualChlorine, expectedScanError = R.string.color_card_not_found),
    13 to TestData(residualChlorine, expectedScanError = R.string.align_color_card),
    14 to TestData(pH, 6.4, 0.5, risk = RiskLevel.LOW),
    15 to TestData(residualChlorine, 0.43, 0.25, risk = RiskLevel.LOW),
    16 to TestData(residualChlorine, expectedResultError = ErrorType.CALIBRATION_ERROR),
    17 to TestData(residualChlorine, expectedScanError = R.string.align_color_card),
    18 to TestData(residualChlorine, expectedScanError = R.string.color_card_not_found),
    19 to TestData(residualChlorine, 3.0, 0.28, risk = RiskLevel.HIGH),
    20 to TestData(residualChlorine, expectedScanError = R.string.align_color_card),
    21 to TestData(residualChlorine, expectedScanError = R.string.align_color_card),
    22 to TestData(pH, expectedScanError = R.string.color_card_not_found),
    23 to TestData(fluorideHighRange, expectedResultError = ErrorType.NO_MATCH),
    24 to TestData(fluoride, 1.0, 0.25, risk = RiskLevel.MEDIUM),
    25 to TestData(nitrate, 0.0, 0.80, risk = RiskLevel.LOW),
    26 to TestData(iron, 0.0, 0.15, risk = RiskLevel.LOW),
    27 to TestData(fluoride, 0.5, 0.25, risk = RiskLevel.LOW),
    28 to TestData(pH, 7.0, 0.5, risk = RiskLevel.MEDIUM),
    500 to TestData(pH, expectedScanError = R.string.sample_image_not_found)
)

data class TestData(
    var testDetails: TestDetails,
    var expectedResult: Double = -1.0,
    var expectedMarginOfError: Double = -1.0,
    var expectedResultError: ErrorType = ErrorType.NO_ERROR,
    var expectedScanError: Int = -1,
    var risk: RiskLevel = RiskLevel.LOW
)

data class TestDetails(
    var name: String,
    var id: String,
    var group: Int,
    var buttonIndex: Int,
    var riskType: RiskType = RiskType.NORMAL
)