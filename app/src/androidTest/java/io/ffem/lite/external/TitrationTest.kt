package io.ffem.lite.external


import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import io.ffem.lite.R
import io.ffem.lite.common.TEST_SURVEY_NAME
import io.ffem.lite.common.TestHelper
import io.ffem.lite.common.TestHelper.mDevice
import io.ffem.lite.common.TestUtil
import io.ffem.lite.common.TestUtil.sleep
import io.ffem.lite.common.getString
import io.ffem.lite.ui.MainActivity
import junit.framework.TestCase.assertNotNull
import org.hamcrest.Matchers.allOf
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TitrationTest {

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
        TestHelper.clearPreferences()
    }

    @Ignore
    @Test
    fun titrationTest() {

        onView(withId(R.id.titration_button)).perform(click())

        val materialButton3 = onView(
            allOf(withText(R.string.enter_data))
        )
        materialButton3.perform(scrollTo(), click())

        sleep(1000)

        onView(withText(TEST_SURVEY_NAME)).perform(scrollTo(), click())
        sleep(2000)

        onView(withText(R.string.next)).perform(click())

        sleep(500)

        onView(allOf(withText(R.string.total_alkalinity), isDisplayed()))

        try {
            onView(withText(R.string.total_alkalinity)).perform(click())
            sleep(1000)
            onView(withId(R.id.editTitration1)).perform(pressImeActionButton())
        } catch (e: Exception) {
            onView(withText(R.string.redo)).perform(click())
            sleep(500)
            onView(withText(R.string.redo)).perform(click())
            sleep(1000)
            onView(withId(R.id.editTitration1)).perform(pressImeActionButton())
        }
        sleep(500)

        onView(withId(R.id.editTitration1)).check(matches(isDisplayed()))
            .perform(replaceText("10"), closeSoftKeyboard())

        onView(allOf(withId(R.id.editTitration1), withText("10"), isDisplayed()))
            .perform(pressImeActionButton())

        sleep(500)

        onView(withId(R.id.editTitration2)).check(matches(isDisplayed()))
            .perform(replaceText("20"), closeSoftKeyboard())

        onView(allOf(withId(R.id.editTitration2), withText("20"), isDisplayed()))
            .perform(pressImeActionButton())

        sleep(1000)

        onView(withText(R.string.accept_result)).perform(click())

        onView(
            allOf(
                withText(R.string.total_alkalinity),
                isDisplayed()
            )
        ).check(matches(withText(R.string.total_alkalinity)))

        onView(
            allOf(
                withText(getString(R.string.p_alkalinity) + ": "),
                isDisplayed()
            )
        ).check(matches(withText(getString(R.string.p_alkalinity) + ": ")))

        onView(
            allOf(
                withText(getString(R.string.t_alkalinity) + ": "),
                isDisplayed()
            )
        ).check(matches(withText(getString(R.string.t_alkalinity) + ": ")))

        onView(
            allOf(
                withText("100"),
                isDisplayed()
            )
        ).check(matches(withText("100")))

        onView(
            allOf(
                TestUtil.withIndex(
                    withText("mg/l"),
                    0
                ),
                isDisplayed()
            )
        ).check(matches(withText("mg/l")))

        onView(
            allOf(
                withText(R.string.redo_test),
                isDisplayed()
            )
        ).check(matches(withText(R.string.redo_test)))
    }

    @Test
    fun calciumTitrationTest() {

        onView(withId(R.id.titration_button)).perform(click())

        sleep(3000)
        mDevice.findObject(By.text(getString(R.string.enter_data))).click()

        sleep(1000)

        try {
            mDevice.findObject(By.text(TEST_SURVEY_NAME)).click()
        } catch (e: Exception) {
            swipeUp()
            mDevice.findObject(By.text(TEST_SURVEY_NAME)).click()
        }
        sleep(2000)

        mDevice.findObject(By.text(getString(R.string.next).uppercase())).click()

        sleep(500)

        mDevice.findObject(By.text(getString(R.string.calcium_and_magnesium))).click()

        sleep(500)

        try {
            onView(withId(R.id.editTitration1)).perform(pressImeActionButton())
        } catch (e: Exception) {
            mDevice.findObject(By.text(getString(R.string.redo))).click()
            sleep(500)
            mDevice.findObject(By.text(getString(R.string.redo))).click()
        }

        onView(withId(R.id.editTitration1)).perform(pressImeActionButton())
        sleep(500)
        onView(withId(R.id.editTitration1)).check(matches(isDisplayed()))
            .perform(replaceText("10"), closeSoftKeyboard())

        onView(allOf(withId(R.id.editTitration1), withText("10"), isDisplayed()))
            .perform(pressImeActionButton())

        sleep(500)

        onView(withId(R.id.editTitration2)).check(matches(isDisplayed()))
            .perform(replaceText("20"), closeSoftKeyboard())

        onView(allOf(withId(R.id.editTitration2), withText("20"), isDisplayed()))
            .perform(pressImeActionButton())

        sleep(1000)

        onView(withText(R.string.close)).perform(click())

        assertNotNull(mDevice.findObject(By.text(getString(R.string.calcium_and_magnesium))))
        assertNotNull(mDevice.findObject(By.text("Calcium: ")))
        assertNotNull(mDevice.findObject(By.text("Magnesium: ")))
        assertNotNull(mDevice.findObject(By.text("83.33")))
        assertNotNull(mDevice.findObject(By.text("50")))
        assertNotNull(mDevice.findObject(By.text(getString(R.string.redo))))
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun initialize() {
            if (!TestHelper.isDeviceInitialized()) {
                mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            }
        }
    }
}
