package io.ffem.lite

import android.app.Activity
import android.content.Context
import android.os.Build
import io.ffem.lite.app.App.Companion.getTestInfo
import io.ffem.lite.model.TestInfo
import io.ffem.lite.ui.ResultListActivity
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class RiskIndicatorTest {

    @Test
    fun testRiskIndicatorForFluoride() {

        val testInfo = getTestInfo(FLUORIDE_ID)!!

        assertLowRisk(testInfo, 0.0)

        assertHighRisk(testInfo, 3.0)

        assertMediumRisk(testInfo, 2.0)

        assertLowRisk(testInfo, 0.9)

        assertMediumRisk(testInfo, 2.99)

        assertLowRisk(testInfo, 0.99)

        assertMediumRisk(testInfo, 1.00)

        assertLowRisk(testInfo, 0.5)

        assertMediumRisk(testInfo, 1.1)

        assertHighRisk(testInfo, 4.1)

        assertMediumRisk(testInfo, 1.5)

        assertMediumRisk(testInfo, 1.99)
    }

    private fun assertLowRisk(testInfo: TestInfo, result: Double) {
        testInfo.result = result
        Assert.assertEquals(low, testInfo.getRisk(context))
    }

    private fun assertMediumRisk(testInfo: TestInfo, result: Double) {
        testInfo.result = result
        Assert.assertEquals(medium, testInfo.getRisk(context))
    }

    private fun assertHighRisk(testInfo: TestInfo, result: Double) {
        testInfo.result = result
        Assert.assertEquals(high, testInfo.getRisk(context))
    }

    companion object {
        private val controller: ActivityController<*> =
            Robolectric.buildActivity(ResultListActivity::class.java).create().start()
        val context: Context = (controller.get() as Activity)

        val low = context.getString(R.string.low)
        val medium = context.getString(R.string.medium)
        val high = context.getString(R.string.high)

        const val FLUORIDE_ID = "f0f3c1dd-89af-49f1-83e7-bcc31cb61159"
    }
}