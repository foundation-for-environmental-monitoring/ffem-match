package io.ffem.lite

import android.app.Activity
import android.content.Context
import android.os.Build
import io.ffem.lite.app.App.Companion.getTestInfo
import io.ffem.lite.model.RiskLevel
import io.ffem.lite.model.RiskLevel.*
import io.ffem.lite.model.RiskType
import io.ffem.lite.model.TestInfo
import io.ffem.lite.ui.ResultListActivity
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

const val FLUORIDE_ID = "f0f3c1dd-89af-49f1-83e7-bcc31cb61159"
const val FLUORIDE_HIGH_RANGE_ID = "93cec3c4-b3e4-4924-b722-b6cb9742737a"
const val PH_ID = "ff96e965-13a3-4507-9edf-7aa7fc084354"
const val RESIDUAL_CHLORINE_ID = "f1d64b11-64c4-4a34-806e-ad0d47bcc96b"
const val NITRATE_ID = "d69ca7a2-e357-4820-a99b-1b6b24c0fa93"
const val IRON_ID = "7fd5d20d-73e3-4c95-86ef-352410b1893d"
const val PHOSPHATE_ID = "b396de78-0de6-11eb-adc1-0242ac120002"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class RiskIndicatorTest {

    @Test
    fun fluorideRiskIndicator() {

        val testInfo = getTestInfo(FLUORIDE_ID)!!

        Assert.assertEquals(10, testInfo.values.size)

        Assert.assertEquals(3, testInfo.risks.size)

        assertRisk(testInfo, 0.0, LOW)

        assertRisk(testInfo, 3.0, HIGH)

        assertRisk(testInfo, 2.0, HIGH)

        assertRisk(testInfo, 2.01, HIGH)

        assertRisk(testInfo, 1.3, MEDIUM)

        assertRisk(testInfo, 0.9, LOW)

        assertRisk(testInfo, 2.99, HIGH)

        assertRisk(testInfo, 0.99, LOW)

        assertRisk(testInfo, 1.00, MEDIUM)

        assertRisk(testInfo, 0.5, LOW)

        assertRisk(testInfo, 1.1, MEDIUM)

        assertRisk(testInfo, 4.1, HIGH)

        assertRisk(testInfo, 1.5, HIGH)

        assertRisk(testInfo, 1.99, HIGH)
    }


    @Test
    fun fluorideHighRangeRiskIndicator() {

        val testInfo = getTestInfo(FLUORIDE_HIGH_RANGE_ID)!!

        Assert.assertEquals(12, testInfo.values.size)

        Assert.assertEquals(3, testInfo.risks.size)

        assertRisk(testInfo, 0.0, LOW)

        assertRisk(testInfo, 3.0, HIGH)

        assertRisk(testInfo, 2.0, HIGH)

        assertRisk(testInfo, 2.01, HIGH)

        assertRisk(testInfo, 1.3, MEDIUM)

        assertRisk(testInfo, 0.9, LOW)

        assertRisk(testInfo, 2.99, HIGH)

        assertRisk(testInfo, 0.99, LOW)

        assertRisk(testInfo, 1.00, MEDIUM)

        assertRisk(testInfo, 0.5, LOW)

        assertRisk(testInfo, 1.1, MEDIUM)

        assertRisk(testInfo, 4.1, HIGH)

        assertRisk(testInfo, 1.5, HIGH)

        assertRisk(testInfo, 1.99, HIGH)
    }

    @Test
    fun residualChlorineRiskIndicator() {

        val testInfo = getTestInfo(RESIDUAL_CHLORINE_ID)!!

        Assert.assertEquals(12, testInfo.values.size)

        Assert.assertEquals(3, testInfo.risks.size)

        assertRisk(testInfo, 0.0, MEDIUM)

        assertRisk(testInfo, 3.0, HIGH)

        assertRisk(testInfo, 2.0, HIGH)

        assertRisk(testInfo, 0.8, LOW)

        assertRisk(testInfo, 0.801, LOW)

        assertRisk(testInfo, 0.81, HIGH)

        assertRisk(testInfo, 0.9, HIGH)

        assertRisk(testInfo, 2.99, HIGH)

        assertRisk(testInfo, 0.99, HIGH)

        assertRisk(testInfo, 1.00, HIGH)

        assertRisk(testInfo, 0.5, LOW)

        assertRisk(testInfo, 1.1, HIGH)

        assertRisk(testInfo, 4.1, HIGH)

        assertRisk(testInfo, 1.5, HIGH)

        assertRisk(testInfo, 1.99, HIGH)
    }

    @Test
    fun pHRiskIndicator() {

        val testInfo = getTestInfo(PH_ID)!!

        Assert.assertEquals(14, testInfo.values.size)

        Assert.assertEquals(3, testInfo.risks.size)

        assertRisk(testInfo, 0.0, LOW)

        assertRisk(testInfo, 3.0, LOW)

        assertRisk(testInfo, 2.0, LOW)

        assertRisk(testInfo, 0.9, LOW)

        assertRisk(testInfo, 6.8, MEDIUM)

        assertRisk(testInfo, 6.5, LOW)

        assertRisk(testInfo, 6.6, LOW)

        assertRisk(testInfo, 7.1, MEDIUM)

        assertRisk(testInfo, 7.0, MEDIUM)

        assertRisk(testInfo, 8.5, HIGH)

        assertRisk(testInfo, 9.0, HIGH)

        assertRisk(testInfo, 10.5, HIGH)

        assertRisk(testInfo, 5.8, LOW)
    }

    @Test
    fun nitrateRiskIndicator() {

        val testInfo = getTestInfo(NITRATE_ID)!!

        Assert.assertEquals(10, testInfo.values.size)

        Assert.assertEquals(3, testInfo.risks.size)

        assertRisk(testInfo, 50.0, HIGH)

        assertRisk(testInfo, 55.0, HIGH)

        assertRisk(testInfo, 75.0, HIGH)

        assertRisk(testInfo, 30.0, MEDIUM)

        assertRisk(testInfo, 10.0, LOW)

        assertRisk(testInfo, 65.0, HIGH)

        assertRisk(testInfo, 12.0, LOW)

        assertRisk(testInfo, 20.0, LOW)

        assertRisk(testInfo, 35.0, MEDIUM)

        assertRisk(testInfo, 80.0, HIGH)

        assertRisk(testInfo, 100.0, HIGH)

        assertRisk(testInfo, 99.9, HIGH)
    }

    @Test
    fun ironRiskIndicator() {

        val testInfo = getTestInfo(IRON_ID)!!

        Assert.assertEquals(8, testInfo.values.size)

        Assert.assertEquals(3, testInfo.risks.size)

        assertRisk(testInfo, 0.30, HIGH)

        assertRisk(testInfo, 0.5, HIGH)

        assertRisk(testInfo, 1.0, HIGH)

        assertRisk(testInfo, 0.21, MEDIUM)

        assertRisk(testInfo, 0.12, LOW)

        assertRisk(testInfo, 1.5, HIGH)

        assertRisk(testInfo, 0.0, LOW)

        assertRisk(testInfo, 0.15, LOW)

        assertRisk(testInfo, 0.1, LOW)

        assertRisk(testInfo, 0.31, HIGH)
    }

    @Test
    fun phosphateRiskIndicator() {

        val testInfo = getTestInfo(PHOSPHATE_ID)!!

        Assert.assertEquals(8, testInfo.values.size)

        Assert.assertEquals(2, testInfo.risks.size)

        assertRisk(testInfo, 0.31, HIGH)

        assertRisk(testInfo, 0.5, HIGH)

        assertRisk(testInfo, 1.0, HIGH)

        assertRisk(testInfo, 0.21, LOW)

        assertRisk(testInfo, 0.12, LOW)

        assertRisk(testInfo, 1.5, HIGH)

        assertRisk(testInfo, 0.0, LOW)

        assertRisk(testInfo, 0.15, LOW)

        assertRisk(testInfo, 0.1, LOW)

        assertRisk(testInfo, 0.9, HIGH)
    }

    private fun assertRisk(testInfo: TestInfo, result: Double, risk: RiskLevel) {
        testInfo.resultInfo.result = result
        when (risk) {
            LOW -> {
                when (testInfo.riskType) {
                    RiskType.QUANTITY -> {
                        Assert.assertEquals(
                            context.getString(R.string.low_quantity),
                            testInfo.getRisk(context)
                        )
                    }
                    RiskType.SAFETY -> {
                        Assert.assertEquals(
                            context.getString(R.string.low_safety),
                            testInfo.getRisk(context)
                        )
                    }

                    RiskType.ALKALINITY -> {
                        Assert.assertEquals(
                            context.getString(R.string.low_alkalinity),
                            testInfo.getRisk(context)
                        )
                    }

                    else -> {
                        Assert.assertEquals(
                            context.getString(R.string.low),
                            testInfo.getRisk(context)
                        )
                    }
                }
            }

            MEDIUM -> {
                when (testInfo.riskType) {
                    RiskType.QUANTITY -> {
                        Assert.assertEquals(
                            context.getString(R.string.medium_quantity),
                            testInfo.getRisk(context)
                        )
                    }
                    RiskType.SAFETY -> {
                        Assert.assertEquals(
                            context.getString(R.string.medium_safety),
                            testInfo.getRisk(context)
                        )
                    }

                    RiskType.ALKALINITY -> {
                        Assert.assertEquals(
                            context.getString(R.string.medium_alkalinity),
                            testInfo.getRisk(context)
                        )
                    }

                    else -> {
                        Assert.assertEquals(
                            context.getString(R.string.medium),
                            testInfo.getRisk(context)
                        )
                    }
                }
            }
            HIGH -> {
                when (testInfo.riskType) {
                    RiskType.QUANTITY -> {
                        Assert.assertEquals(
                            context.getString(R.string.high_quantity),
                            testInfo.getRisk(context)
                        )
                    }
                    RiskType.SAFETY -> {
                        Assert.assertEquals(
                            context.getString(R.string.high_safety),
                            testInfo.getRisk(context)
                        )
                    }

                    RiskType.ALKALINITY -> {
                        Assert.assertEquals(
                            context.getString(R.string.high_alkalinity),
                            testInfo.getRisk(context)
                        )
                    }

                    else -> {
                        Assert.assertEquals(
                            context.getString(R.string.high),
                            testInfo.getRisk(context)
                        )
                    }
                }
            }
        }
    }

    companion object {
        private val controller: ActivityController<*> =
            Robolectric.buildActivity(ResultListActivity::class.java).create().start()
        val context: Context = (controller.get() as Activity)
    }
}