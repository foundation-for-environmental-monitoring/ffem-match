package io.ffem.lite.common

import io.ffem.lite.R
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.RiskType

val fluoride = TestDetails("Fluoride", "bcc31cb61159", R.string.water_tests_1, 0)
val fluorideHighRange =
    TestDetails("Fluoride - High Range", "b6cb9742737a", R.string.water_tests_1, 1)
val pH = TestDetails("pH", "7aa7fc084354", R.string.water_tests_2, 0)
val residualChlorine = TestDetails("Residual Chlorine", "ad0d47bcc96b", R.string.water_tests_2, 1)
val invalidTest = TestDetails("Residual Chlorine", "ad0d47bcc96b", R.string.water_tests_2, 2)

val testDataList = mutableMapOf(
    0 to TestData(residualChlorine, 0.5, 0.25, risk = RiskType.LOW),
    1 to TestData(residualChlorine, 0.0, 0.3, risk = RiskType.MEDIUM),
    2 to TestData(residualChlorine, expectedScanError = R.string.invalid_barcode),
    3 to TestData(fluoride, expectedScanError = R.string.invalid_barcode),
    4 to TestData(pH, expectedResultError = ErrorType.NO_MATCH),
    5 to TestData(residualChlorine, expectedScanError = R.string.place_color_card),
    6 to TestData(residualChlorine, expectedResultError = ErrorType.NO_MATCH),
    7 to TestData(residualChlorine, 0.5, 0.25, risk = RiskType.LOW),
    8 to TestData(residualChlorine, 1.5, 0.25, risk = RiskType.HIGH),
    9 to TestData(residualChlorine, expectedScanError = R.string.place_color_card),
    10 to TestData(residualChlorine, expectedResultError = ErrorType.CALIBRATION_ERROR),
    11 to TestData(residualChlorine, expectedScanError = R.string.correct_camera_tilt),
    12 to TestData(residualChlorine, expectedScanError = R.string.place_color_card),
    13 to TestData(residualChlorine, expectedScanError = R.string.place_color_card),
    14 to TestData(pH, 6.4, 0.5, risk = RiskType.MEDIUM),
    15 to TestData(residualChlorine, 0.43, 0.25, risk = RiskType.LOW),
    16 to TestData(residualChlorine, expectedResultError = ErrorType.CALIBRATION_ERROR),
    17 to TestData(residualChlorine, expectedScanError = R.string.place_color_card),
    18 to TestData(residualChlorine, expectedScanError = R.string.place_color_card),
    19 to TestData(residualChlorine, 3.0, 0.28, risk = RiskType.HIGH),
    20 to TestData(residualChlorine, expectedScanError = R.string.place_color_card),
    21 to TestData(residualChlorine, expectedScanError = R.string.place_color_card),
    22 to TestData(pH, expectedScanError = R.string.place_color_card),
    23 to TestData(fluorideHighRange, expectedResultError = ErrorType.NO_MATCH),
    500 to TestData(pH, expectedScanError = R.string.sample_image_not_found)
)

data class TestData(
    var testDetails: TestDetails,
    var expectedResult: Double = -1.0,
    var expectedMarginOfError: Double = -1.0,
    var expectedResultError: ErrorType = ErrorType.NO_ERROR,
    var expectedScanError: Int = -1,
    var risk: RiskType = RiskType.LOW
)

data class TestDetails(
    var name: String,
    var id: String,
    var group: Int,
    var buttonIndex: Int
)