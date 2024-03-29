package io.ffem.lite.internal


import android.os.Environment
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
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.toResourceId
import io.ffem.lite.ui.MainActivity
import io.ffem.lite.util.PreferencesUtil
import org.hamcrest.Matchers.allOf
import org.junit.*
import org.junit.runner.RunWith
import java.io.File


@LargeTest
@RunWith(AndroidJUnit4::class)
class CalibrationTest {

    @get:Rule
    val mActivityTestRule = activityScenarioRule<MainActivity>()

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.CAMERA"
        )

    @Before
    fun setUp() {
        if (!initialized) {
            TestHelper.clearPreferences()
            clearData()
            initialized = true
        }
    }

    @Test
    fun image_000_Chlorine_1_Point_0() {
        startCalibrationTest(0)
    }

    @Test
    fun image_001_pH_NoMatch() {
        TestHelper.clearPreferences()
        calibrationNoMatch(1)
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
                withId(R.id.colorimetric_button), withContentDescription(R.string.start_test),
                isDisplayed()
            )
        ).perform(click())

        onView(withText(R.string.start)).perform(click())

        sleep(SCAN_TIME_DELAY)

        if (testData.expectedResultError == ErrorType.NO_ERROR) {
            onView(withText(R.string.continue_on)).perform(click())
        }

        onView(withText(testData.testDetails.name)).check(matches(isDisplayed()))

        val resultTextView = onView(withId(R.id.result_txt))
        resultTextView.check(matches(TestUtil.checkResult(testData)))

        onView(allOf(withId(R.id.unit_txt), withText("mg/l")))
            .check(matches(isDisplayed()))

        onView(withId(R.id.error_margin_text))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

        onView(
            withText(
                testData.risk.toResourceId(
                    ApplicationProvider.getApplicationContext(), testData.testDetails.riskType
                )
            )
        ).check(
            matches(isDisplayed())
        )

        if (testData.expectedResultError == ErrorType.NO_ERROR) {
            onView(withText(R.string.next)).perform(click())
            sleep(1000)

//            val textInputEditText = onView(
//                allOf(
//                    withId(R.id.source_desc_edit),
//                    isDisplayed()
//                )
//            )
//            textInputEditText.perform(
//                ViewActions.replaceText("Description"),
//                ViewActions.closeSoftKeyboard()
//            )

//            val appCompatAutoCompleteTextView = onView(
//                allOf(
//                    withId(R.id.source_select),
//                    isDisplayed()
//                )
//            )
//            appCompatAutoCompleteTextView.perform(
//                ViewActions.replaceText("Drinking water"),
//                ViewActions.closeSoftKeyboard()
//            )
//
//            appCompatAutoCompleteTextView.perform(ViewActions.pressImeActionButton())

            sleep(1000)
            onView(withText(R.string.save)).perform(click())
        } else {
            onView(withText(R.string.close)).perform(click())
        }

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

        onView(withText(R.string.start)).perform(click())

        sleep(SCAN_TIME_DELAY / 2)

        takeScreenshot(screenshotName)

        sleep(SCAN_TIME_DELAY / 2)

        onView(withText(R.string.continue_on)).perform(click())

        onView(withText(testData.testDetails.name)).check(matches(isDisplayed()))

        takeScreenshot(screenshotName)

        onView(allOf(withId(R.id.value_txt), withText("0.2")))
            .check(matches(isDisplayed()))

        onView(withText(R.string.confirm)).perform(click())

        takeScreenshot(screenshotName)

        sleep(1000)

        takeScreenshot(screenshotName)

        Espresso.pressBack()

        onView(
            allOf(
                withId(R.id.colorimetric_button), withContentDescription(R.string.start_test),
                isDisplayed()
            )
        ).perform(click())

        onView(withText(R.string.start)).perform(click())

        sleep(SCAN_TIME_DELAY)

        onView(withText(R.string.continue_on)).perform(click())

        onView(withText(testData.testDetails.name)).check(matches(isDisplayed()))

        val resultTextView2 = onView(withId(R.id.result_txt))
        resultTextView2.check(matches(TestUtil.checkResult(testData, testData.calibratedResult)))

        onView(allOf(withId(R.id.unit_txt), withText("mg/l")))
            .check(matches(isDisplayed()))

        onView(withId(R.id.error_margin_text))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

        onView(
            withText(
                testData.calibratedRisk.toResourceId(
                    ApplicationProvider.getApplicationContext(), testData.testDetails.riskType
                )
            )
        ).check(
            matches(isDisplayed())
        )

        onView(withText(R.string.next)).perform(click())

        sleep(2000)
    }

    private fun calibrationNoMatch(@Suppress("SameParameterValue") imageNumber: Int) {
        val testData = testDataList[imageNumber]!!
        val screenshotName = "calibration"

        PreferencesUtil.setString(
            ApplicationProvider.getApplicationContext(),
            R.string.testImageNumberKey, imageNumber.toString()
        )

        onView(
            allOf(
                withId(R.id.colorimetric_button), withContentDescription(R.string.start_test),
                isDisplayed()
            )
        ).perform(click())

        onView(withText(R.string.start)).perform(click())

        sleep(SCAN_TIME_DELAY)

        if (testData.expectedResultError != ErrorType.BAD_LIGHTING &&
            testData.expectedResultError != ErrorType.IMAGE_TILTED
        ) {
            onView(withText(R.string.continue_on)).perform(click())
        }

        sleep(1000)
        onView(withText(testData.testDetails.name)).check(matches(isDisplayed()))

        onView(withText(R.string.close)).perform(click())

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

        onView(withText(R.string.start)).perform(click())

        sleep(SCAN_TIME_DELAY / 2)

        takeScreenshot(screenshotName)

        sleep(SCAN_TIME_DELAY / 2)

        if (testData.expectedResultError != ErrorType.BAD_LIGHTING &&
            testData.expectedResultError != ErrorType.IMAGE_TILTED
        ) {
            onView(withText(R.string.continue_on)).perform(click())
        }

        sleep(2000)
        onView(allOf(isDisplayed(), withText(testData.testDetails.name)))
            .check(matches(isDisplayed()))

        takeScreenshot(screenshotName)

        onView(withText(R.string.confirm)).perform(click())

        sleep(1000)

        takeScreenshot(screenshotName)

        Espresso.pressBack()

        onView(
            allOf(
                withId(R.id.colorimetric_button), withContentDescription(R.string.start_test),
                isDisplayed()
            )
        ).perform(click())

        onView(withText(R.string.start)).perform(click())

        sleep(SCAN_TIME_DELAY)

        if (testData.expectedResultError != ErrorType.BAD_LIGHTING &&
            testData.expectedResultError != ErrorType.IMAGE_TILTED
        ) {
            onView(withText(R.string.continue_on)).perform(click())
        }

        sleep(2000)
        onView(allOf(isDisplayed(), withText(testData.testDetails.name)))
            .check(matches(isDisplayed()))

        onView(withText(R.string.close)).perform(click())
    }

    companion object {

        @JvmStatic
        var initialized = false

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
