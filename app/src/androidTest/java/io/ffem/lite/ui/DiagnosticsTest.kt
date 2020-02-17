package io.ffem.lite.ui


import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import io.ffem.lite.R
import io.ffem.lite.common.TestHelper.enterDiagnosticMode
import io.ffem.lite.common.TestHelper.leaveDiagnosticMode
import io.ffem.lite.common.TestUtil.childAtPosition
import io.ffem.lite.preference.isDiagnosticMode
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@LargeTest
@RunWith(AndroidJUnit4::class)
class DiagnosticsTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(ResultListActivity::class.java)

    @Test
    fun deleteDataTest() {
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

        Thread.sleep(400)

        onView(
            allOf(
                withId(android.R.id.button2), withText("Cancel")
            )
        ).perform(click())

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

        Thread.sleep(400)

        val appCompatImageButton = onView(
            allOf(
                withContentDescription("Navigate up"),
                childAtPosition(
                    allOf(
                        withId(R.id.toolbar),
                        childAtPosition(
                            withClassName(`is`("android.widget.FrameLayout")),
                            0
                        )
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        appCompatImageButton.perform(click())

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
}
