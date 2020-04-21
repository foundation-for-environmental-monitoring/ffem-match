package io.ffem.lite.internal


import android.os.SystemClock
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import io.ffem.lite.R
import io.ffem.lite.common.TestHelper.enterDiagnosticMode
import io.ffem.lite.common.TestHelper.leaveDiagnosticMode
import io.ffem.lite.common.TestUtil.childAtPosition
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.ui.ResultListActivity
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.core.IsInstanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@LargeTest
@RunWith(AndroidJUnit4::class)
class DiagnosticsTest {

    @get:Rule
    val mActivityTestRule = activityScenarioRule<ResultListActivity>()

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        )

    @Test
    fun deleteDataTest() {
        startDiagnosticMode()

        Thread.sleep(400)

        Espresso.pressBack()

        Thread.sleep(400)

        onView(withId(R.id.scrollViewSettings))
            .perform(swipeUp())

        Thread.sleep(400)

        onView(withText(R.string.delete_data)).perform(click())

        Thread.sleep(400)

        val appCompatButton2 = onView(
            allOf(
                withId(android.R.id.button1), withText("Delete"),
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
        startDiagnosticMode()

        Thread.sleep(400)

        Espresso.pressBack()

        Thread.sleep(400)

        onView(withId(R.id.scrollViewSettings))
            .perform(swipeUp())

        Thread.sleep(400)

        pressBack()

        onView(withText("Test Image Number")).perform(click())

        Thread.sleep(400)

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
        ).perform(scrollTo(), replaceText("1"))

        val appCompatEditText2 = onView(
            allOf(
                withId(android.R.id.edit), withText("1"),
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
                withId(android.R.id.button1), withText("OK"),
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
                withId(R.id.fab), withContentDescription("Start test"),
                childAtPosition(
                    childAtPosition(
                        withId(android.R.id.content),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        floatingActionButton.perform(click())

        SystemClock.sleep(TIME_DELAY)

        val textView = onView(
            allOf(
                withId(R.id.text_name), withText("Residual Chlorine"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.lyt_result),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        textView.check(matches(withText("Residual Chlorine")))

        val textView2 = onView(
            allOf(
                withId(R.id.text_result), withText("0.0"),
                childAtPosition(
                    childAtPosition(
                        IsInstanceOf.instanceOf(android.widget.LinearLayout::class.java),
                        1
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        textView2.check(matches(withText("0.0")))

        val textView3 = onView(
            allOf(
                withId(R.id.text_unit), withText("mg/l"),
                childAtPosition(
                    childAtPosition(
                        IsInstanceOf.instanceOf(android.widget.LinearLayout::class.java),
                        1
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        textView3.check(matches(withText("mg/l")))

        val textView4 = onView(
            allOf(
                withId(R.id.text_risk), withText("Insufficient"),
                childAtPosition(
                    childAtPosition(
                        IsInstanceOf.instanceOf(android.widget.LinearLayout::class.java),
                        1
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        textView4.check(matches(withText("Insufficient")))

        val textView5 = onView(
            allOf(
                withText("Margin of error: ±"),
                childAtPosition(
                    childAtPosition(
                        IsInstanceOf.instanceOf(android.widget.LinearLayout::class.java),
                        2
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        textView5.check(matches(withText("Margin of error: ±")))

        val textView6 = onView(
            allOf(
                withId(R.id.text_error_margin), withText("0.30"),
                childAtPosition(
                    childAtPosition(
                        IsInstanceOf.instanceOf(android.widget.LinearLayout::class.java),
                        2
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        textView6.check(matches(withText("0.30")))

        val textView7 = onView(
            allOf(
                withText("Result"),
                childAtPosition(
                    childAtPosition(
                        IsInstanceOf.instanceOf(android.widget.LinearLayout::class.java),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        textView7.check(matches(withText("Result")))

        val appCompatButton2 = onView(
            allOf(
                withId(R.id.button_submit), withText("Submit Result"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.fragment_container),
                        0
                    ),
                    4
                ),
                isDisplayed()
            )
        )
        appCompatButton2.perform(click())
    }

    private fun startDiagnosticMode() {
        if (isDiagnosticMode()) {
            Thread.sleep(400)

            val actionMenuItemView = onView(
                allOf(
                    withId(R.id.action_settings), withContentDescription("Settings"),
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

            Thread.sleep(400)

            onView(withText(R.string.about)).perform(click())

            Thread.sleep(400)

            leaveDiagnosticMode()
        } else {
            val actionMenuItemView = onView(
                allOf(
                    withId(R.id.action_settings), withContentDescription("Settings"),
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

        Thread.sleep(500)

        onView(withText(R.string.about)).perform(click())

        Thread.sleep(500)

        enterDiagnosticMode()
    }
}
