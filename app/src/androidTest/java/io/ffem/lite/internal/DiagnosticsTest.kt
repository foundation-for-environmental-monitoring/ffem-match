package io.ffem.lite.internal


import android.os.Environment
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.ffem.lite.R
import io.ffem.lite.common.TestHelper
import io.ffem.lite.common.TestHelper.enterDiagnosticMode
import io.ffem.lite.common.TestHelper.leaveDiagnosticMode
import io.ffem.lite.common.TestHelper.sleep
import io.ffem.lite.common.TestUtil
import io.ffem.lite.common.TestUtil.childAtPosition
import io.ffem.lite.common.clearData
import io.ffem.lite.common.residualChlorine
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.ui.ResultListActivity
import io.ffem.lite.util.PreferencesUtil
import io.ffem.lite.util.toLocalString
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.core.IsInstanceOf
import org.junit.AfterClass
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File


@LargeTest
@RunWith(AndroidJUnit4::class)
class DiagnosticsTest {

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
        if (!CalibrationTest.initialized) {
            TestHelper.clearPreferences()
            clearData()
            PreferencesUtil.setBoolean(
                ApplicationProvider.getApplicationContext(),
                R.string.useColorCardVersion1, true
            )
            CalibrationTest.initialized = true
        }
    }

    @Test
    fun deleteDataTest() {
        startDiagnosticMode()

        sleep(400)

        Espresso.pressBack()

        sleep(400)

        onView(withId(R.id.scrollViewSettings)).perform(swipeUp())

        sleep(400)

        onView(withText(R.string.delete_data)).perform(click())

        sleep(400)

        val appCompatButton2 = onView(
            allOf(
                withId(android.R.id.button1), withText(R.string.delete),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.buttonPanel),
                        0
                    ),
                    3
                )
            )
        )
        appCompatButton2.perform(scrollTo(), click())
    }

    @Test
    fun testDiagnosticTest() {
        TestHelper.clearPreferences()

        startDiagnosticMode()

        sleep(400)

        Espresso.pressBack()

        sleep(400)

        onView(withId(R.id.scrollViewSettings)).perform(swipeUp())

        sleep(400)

        pressBack()

        onView(withText("Test Image Number")).perform(click())

        sleep(400)

        onView(
            allOf(
                withId(android.R.id.edit),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.ScrollView")),
                        0
                    ),
                    1
                )
            )
        ).perform(scrollTo(), replaceText("0"))

        val appCompatEditText2 = onView(
            allOf(
                withId(android.R.id.edit), withText("0"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.ScrollView")),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        appCompatEditText2.perform(closeSoftKeyboard())

        val appCompatButton = onView(
            allOf(
                withId(android.R.id.button1), withText(R.string.ok),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.buttonPanel),
                        0
                    ),
                    3
                )
            )
        )
        appCompatButton.perform(scrollTo(), click())

        Espresso.pressBack()

        pressBack()

        val floatingActionButton = onView(
            allOf(
                withId(R.id.start_test_fab), withContentDescription(R.string.start_test),
                isDisplayed()
            )
        )
        floatingActionButton.perform(click())

        onView(withText(R.string.start)).perform(click())

        SystemClock.sleep(TIME_DELAY)

        onView(withText(R.string.continue_on)).perform(click())

        val textView = onView(
            allOf(
                withId(R.id.name_txt), withText(residualChlorine.name.toLocalString()),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.result_lyt),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        textView.check(matches(withText(residualChlorine.name.toLocalString())))

        val textView2 = onView(
            allOf(
                withId(R.id.result_txt), withText("1.99"),
                isDisplayed()
            )
        )
        textView2.check(matches(withText("1.99")))

        val textView3 = onView(
            allOf(
                withId(R.id.unit_txt), withText("mg/l"),
                isDisplayed()
            )
        )
        textView3.check(matches(withText("mg/l")))

        val textView4 = onView(
            allOf(
                withId(R.id.value_txt), withText(R.string.high_quantity),
                isDisplayed()
            )
        )
        textView4.check(matches(withText(R.string.high_quantity)))

        val textView5 = onView(
            allOf(
                withText(R.string.margin_of_error),
                childAtPosition(
                    childAtPosition(
                        IsInstanceOf.instanceOf(android.widget.LinearLayout::class.java),
                        3
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        textView5.check(matches(withText(R.string.margin_of_error)))

        val textView6 = onView(
            allOf(
                withId(R.id.error_margin_txt),
                childAtPosition(
                    childAtPosition(
                        IsInstanceOf.instanceOf(android.widget.LinearLayout::class.java),
                        3
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        textView6.check(matches(TestUtil.checkResult(0.25)))

        onView(
            allOf(
                withId(R.id.submit_btn), withText(R.string.close),
                isDisplayed()
            )
        ).perform(click())
    }

    @Test
    fun testManualCapture() {

        startDiagnosticMode()

        sleep(400)

        Espresso.pressBack()

        sleep(400)

        onView(withId(R.id.scrollViewSettings)).perform(swipeUp())

        sleep(2000)

        onView(withText("Use the older Barcode Color card")).perform(click())

        onView(withText(R.string.manual_photo_capture)).perform(click())

        sleep(400)

        onView(withId(R.id.scrollViewSettings)).perform(swipeUp())

        Thread.sleep(400)

        pressBack()

        onView(withText("Test Image Number")).perform(click())

        sleep(400)

        onView(
            allOf(
                withId(android.R.id.edit),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.ScrollView")),
                        0
                    ),
                    1
                )
            )
        ).perform(scrollTo(), replaceText(""))

        val appCompatEditText2 = onView(
            allOf(
                withId(android.R.id.edit), withText(""),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.ScrollView")),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        appCompatEditText2.perform(closeSoftKeyboard())

        val appCompatButton = onView(
            allOf(
                withId(android.R.id.button1), withText(R.string.ok),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.buttonPanel),
                        0
                    ),
                    3
                )
            )
        )
        appCompatButton.perform(scrollTo(), click())

        Espresso.pressBack()

        pressBack()

        val floatingActionButton = onView(
            allOf(
                withId(R.id.start_test_fab), withContentDescription(R.string.start_test),
                isDisplayed()
            )
        )
        floatingActionButton.perform(click())

        onView(withText(R.string.start)).perform(click())

        sleep(TIME_DELAY / 4)

        onView(withText(R.string.take_photo)).perform(click())

        sleep(TIME_DELAY / 4)

        val textView = onView(
            allOf(
                withId(R.id.name2_txt), withText(R.string.unknown),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.error_message_lyt),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        textView.check(matches(withText(R.string.unknown)))

        val textView2 = onView(
            allOf(
                withId(R.id.error_txt), withText(R.string.bad_lighting),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.error_message_lyt),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        textView2.check(matches(withText(R.string.bad_lighting)))

        val textView3 = onView(
            allOf(
                withText(R.string.analyzed_photo),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.analyzed_photo_lyt),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        textView3.check(matches(withText(R.string.analyzed_photo)))

        val imageView = onView(
            allOf(
                withId(R.id.full_photo_img), withContentDescription(R.string.analyzed_image),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.analyzed_photo_lyt),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        imageView.check(matches(isDisplayed()))

        onView(
            allOf(
                withId(R.id.submit_btn), withText(R.string.close),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.resultScrollView),
                        0
                    ),
                    4
                )
            )
        ).perform(scrollTo(), click())
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
            TestHelper.clearPreferences()
        }
    }
}

private fun startDiagnosticMode() {
    if (isDiagnosticMode()) {
        sleep(400)

        val actionMenuItemView = onView(
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
        )
        actionMenuItemView.perform(click())

        sleep(400)

        onView(withText(R.string.about)).perform(click())

        sleep(400)

        leaveDiagnosticMode()
    } else {
        val actionMenuItemView = onView(
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
        )
        actionMenuItemView.perform(click())
    }

    sleep(500)

    onView(withText(R.string.about)).perform(click())
    sleep(500)

    enterDiagnosticMode()
}