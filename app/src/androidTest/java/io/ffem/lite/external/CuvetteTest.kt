package io.ffem.lite.external


import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.common.TEST_SURVEY_NAME
import io.ffem.lite.common.TestHelper.clearPreferences
import io.ffem.lite.common.TestHelper.isDeviceInitialized
import io.ffem.lite.common.TestHelper.mDevice
import io.ffem.lite.common.TestHelper.startDiagnosticMode
import io.ffem.lite.common.TestUtil.sleep
import io.ffem.lite.common.getString
import io.ffem.lite.data.clearData
import io.ffem.lite.ui.ResultListActivity
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CuvetteTest {

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
    fun cuvetteTest() {

        startDiagnosticMode()
        sleep(200)

        Espresso.pressBack()

        sleep(200)

        onView(withId(R.id.scrollViewSettings)).perform(ViewActions.swipeUp())

        sleep(200)

        onView(withText("Return dummy results")).perform(click())

        sleep(200)

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

        mDevice.findObject(By.text(getString(R.string.next).uppercase())).click()

        sleep(500)

        mDevice.findObject(By.text(getString(R.string.fluoride))).click()

        sleep(1000)

        onView(withId(R.id.noDilution_btn)).perform(click())

        sleep(500)

        Espresso.pressBack()

        sleep(500)
        onView(withId(R.id.dilution2_btn)).perform(click())

        sleep(1000)

        onView((withText("Generate Dummy Result"))).perform(click())

        sleep(500)

        mDevice.findObject(By.text(getString(R.string.next).uppercase())).click()

    }

    companion object {
        @JvmStatic
        var initialized = false

        @JvmStatic
        @BeforeClass
        fun initialize() {
            BuildConfig.USE_SCREEN_PINNING.set(false)
            if (!isDeviceInitialized()) {
                mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            }
        }

        @JvmStatic
        @AfterClass
        fun teardown() {
            clearData(ApplicationProvider.getApplicationContext())
        }
    }
}
