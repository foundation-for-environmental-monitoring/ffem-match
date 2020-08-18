package io.ffem.lite.internal


import android.os.Environment
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.common.TestHelper
import io.ffem.lite.common.TestHelper.takeScreenshot
import io.ffem.lite.common.TestUtil
import io.ffem.lite.common.TestUtil.childAtPosition
import io.ffem.lite.common.TestUtil.sleep
import io.ffem.lite.common.clearData
import io.ffem.lite.common.testDataList
import io.ffem.lite.model.toResourceId
import io.ffem.lite.ui.ResultListActivity
import io.ffem.lite.util.PreferencesUtil
import io.ffem.lite.util.toLocalString
import org.hamcrest.Matchers.allOf
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File


@LargeTest
@RunWith(AndroidJUnit4::class)
class CalibrationTest {

    @get:Rule
    val mActivityTestRule = activityScenarioRule<ResultListActivity>()

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.CAMERA"
        )

    @Test
    fun image_024_Fluoride_0_Point_5() {
        startCalibrationTest(24)
    }

    private fun startCalibrationTest(@Suppress("SameParameterValue") imageNumber: Int) {
        val testData = testDataList[imageNumber]!!
        val screenshotName = "calibration"

        PreferencesUtil.setString(
            ApplicationProvider.getApplicationContext(),
            R.string.testImageNumberKey, imageNumber.toString()
        )

        onView(
            allOf(
                withId(R.id.fab), withContentDescription(R.string.start_test),
                childAtPosition(
                    childAtPosition(
                        withId(android.R.id.content),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        ).perform(click())

        SystemClock.sleep(TIME_DELAY)

        onView(withText(testData.testDetails.name.toLocalString())).check(matches(isDisplayed()))

        val resultTextView = onView(withId(R.id.text_result))
        resultTextView.check(matches(TestUtil.checkResult(testData.expectedResult)))

        onView(allOf(withId(R.id.text_unit), withText("mg/l")))
            .check(matches(isDisplayed()))

        val marginOfErrorView = onView(withId(R.id.text_error_margin))
        marginOfErrorView.check(matches(TestUtil.checkResult(testData.expectedMarginOfError)))

        onView(
            withText(
                testData.risk.toResourceId(
                    ApplicationProvider.getApplicationContext(), testData.testDetails.riskType
                )
            )
        ).check(
            matches(isDisplayed())
        )

        onView(withText(R.string.submit_result)).perform(click())

        sleep(2000)

        takeScreenshot(screenshotName)

        onView(
            allOf(
                withId(R.id.action_settings), withContentDescription(R.string.settings),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.toolbar),
                        1
                    ),
                    0
                ),
                isDisplayed()
            )
        ).perform(click())

        sleep(400)

        takeScreenshot(screenshotName)

        onView(withText(R.string.calibrate)).perform(click())

        sleep(TIME_DELAY / 2)

        takeScreenshot(screenshotName)

        sleep(TIME_DELAY / 2)

        onView(withText(testData.testDetails.name.toLocalString())).check(matches(isDisplayed()))

        takeScreenshot(screenshotName)

        onView(withText(R.string.select_calibration_point)).check(matches(isDisplayed()))
        onView(withText("0.50")).perform(click())

        takeScreenshot(screenshotName)
        onView(withText(R.string.confirm)).perform(click())

        sleep(1000)

        takeScreenshot(screenshotName)

        Espresso.pressBack()

        onView(
            allOf(
                withId(R.id.fab), withContentDescription(R.string.start_test),
                childAtPosition(
                    childAtPosition(
                        withId(android.R.id.content),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        ).perform(click())

        SystemClock.sleep(TIME_DELAY)

        onView(withText(testData.testDetails.name.toLocalString())).check(matches(isDisplayed()))

        val resultTextView2 = onView(withId(R.id.text_result))
        resultTextView2.check(matches(TestUtil.checkResult(testData.expectedResult)))

        onView(allOf(withId(R.id.text_unit), withText("mg/l")))
            .check(matches(isDisplayed()))

        val marginOfErrorView2 = onView(withId(R.id.text_error_margin))
        marginOfErrorView2.check(matches(TestUtil.checkResult(testData.expectedMarginOfError)))

        onView(
            withText(
                testData.risk.toResourceId(
                    ApplicationProvider.getApplicationContext(), testData.testDetails.riskType
                )
            )
        ).check(
            matches(isDisplayed())
        )

        onView(withText(R.string.submit_result)).perform(click())

        sleep(2000)
    }

    companion object {

        @JvmStatic
        @AfterClass
        fun teardown() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val folder = File(
                context.getExternalFilesDir(
                    Environment.DIRECTORY_PICTURES
                ).toString() + File.separator + "captures"
            )
            if (folder.exists() && folder.isDirectory) {
                folder.deleteRecursively()
            }
            clearData()
        }

        @JvmStatic
        @BeforeClass
        fun initialize() {
            BuildConfig.INSTRUMENTED_TEST_RUNNING.set(true)
            TestHelper.screenshotEnabled = true

            if (!TestHelper.isDeviceInitialized()) {
                TestHelper.mDevice =
                    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            }
        }
    }
}
