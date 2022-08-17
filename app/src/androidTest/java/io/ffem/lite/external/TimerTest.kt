package io.ffem.lite.external


import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.common.TEST_SURVEY_NAME
import io.ffem.lite.common.TestHelper
import io.ffem.lite.common.TestHelper.clearPreferences
import io.ffem.lite.common.TestHelper.mDevice
import io.ffem.lite.common.TestHelper.nextSurveyPage
import io.ffem.lite.common.TestHelper.startDiagnosticMode
import io.ffem.lite.common.TestHelper.waitForTestCompletion
import io.ffem.lite.common.TestUtil.sleep
import io.ffem.lite.common.getString
import io.ffem.lite.data.clearData
import io.ffem.lite.ui.ResultListActivity
import org.hamcrest.Matchers.allOf
import org.junit.*
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class TimerTest {

    @get:Rule
    val mActivityTestRule = activityScenarioRule<ResultListActivity>()

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.CAMERA"
        )

    @Before
    fun setUp() {
        clearPreferences()
    }

    @Test
    fun timerTest() {
        startDiagnosticMode()

        sleep(500)

        onView(allOf(withContentDescription(R.string.abc_action_bar_up_description))).perform(click())
        sleep(500)
        Espresso.pressBack()
        sleep(500)
        onView(withId(R.id.start_test_fab)).perform(click())

        sleep(3000)
        mDevice.findObject(By.text(getString(R.string.enter_data))).click()

        sleep(1000)

        try {
            mDevice.findObject(By.text(TEST_SURVEY_NAME)).click()
        } catch (e: Exception) {
            ViewActions.swipeUp()
            mDevice.findObject(By.text(TEST_SURVEY_NAME)).click()
        }
        sleep(2000)

        nextSurveyPage(4, "Water Tests 2")
        sleep(1000)

        mDevice.findObject(By.text(getString(R.string.ph))).click()

        sleep(2000)

        onView(withText(R.string.start_test)).perform(click())

        sleep(1000)

        onView(withText(R.string.start)).perform(click())

        onView(
            allOf(
                withId(R.id.start_timer_btn), withText(R.string.start_timer)
            )
        ).perform(click())

        sleep(1000)

        onView(
            allOf(
                withId(R.id.countdown_tmr),
                isDisplayed()
            )
        ).check(matches(isDisplayed()))

        sleep(30000)

        waitForTestCompletion()

    }

    companion object {
        @JvmStatic
        var initialized = false

        @JvmStatic
        @BeforeClass
        fun initialize() {
            BuildConfig.USE_SCREEN_PINNING.set(false)
            if (!TestHelper.isDeviceInitialized()) {
                mDevice =
                    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            }
            clearData(ApplicationProvider.getApplicationContext())
        }

        @JvmStatic
        @AfterClass
        fun teardown() {
            clearData(ApplicationProvider.getApplicationContext())
        }
    }
}
