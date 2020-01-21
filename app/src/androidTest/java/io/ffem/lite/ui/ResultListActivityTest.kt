package io.ffem.lite.ui


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.AndroidJUnit4
import io.ffem.lite.R
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ResultListActivityTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(ResultListActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        )

    @Test
    fun resultListActivityTest() {
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

        val appCompatEditText = onView(
            allOf(
                withId(R.id.editExpectedValue),
                childAtPosition(
                    childAtPosition(
                        withId(android.R.id.custom),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        appCompatEditText.perform(replaceText("1"), closeSoftKeyboard())

        val appCompatButton = onView(
            allOf(
                withId(android.R.id.button1), withText("OK"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.ScrollView")),
                        0
                    ),
                    3
                )
            )
        )
        appCompatButton.perform(scrollTo(), click())

        val textView = onView(
            allOf(
                withId(R.id.text_title), withText("Residual Chlorine (1.0)"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.layout),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        textView.check(matches(withText("Residual Chlorine (1.0)")))

        val textView2 = onView(
            allOf(
                withId(R.id.textLocalValue), withText("0.0"),
                childAtPosition(
                    allOf(
                        withId(R.id.layout),
                        childAtPosition(
                            allOf(
                                withId(R.id.list_results),
                                withContentDescription("List of results")
                            ),
                            0
                        )
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        textView2.check(matches(withText("0.0")))

        val textView3 = onView(
            allOf(
                withText("just now"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.layout),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        textView3.check(matches(withText("just now")))
    }

    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
