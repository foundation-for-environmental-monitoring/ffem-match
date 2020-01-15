package io.ffem.lite.ui


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import io.ffem.lite.R
import io.ffem.lite.common.TestUtil
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.IsInstanceOf
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
    var mGrantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        )

    @Test
    fun runTest0() {
        startTest(1, "0.0", -1)
    }

    @Test
    fun runTestsInvalidBarcode() {
        startTest(2, "", R.string.invalid_barcode)
    }

    @Test
    fun runTestsNoMatch() {
        startTest(6, "No match", -1)
    }

    @Test
    fun runTestsPointFive() {
        startTest(7, "0.44", -1)
    }

    @Test
    fun runTestsOnePointFive() {
        startTest(8, "1.5", -1)
    }

    @Test
    fun runTestsCalibrationError() {
        startTest(10, "", R.string.calibration_error)
    }

    @Test
    fun runTestsTilted() {
        startTest(11, "", R.string.correct_camera_tilt)
    }

//    @Test
//    fun runTestsWaiting() {
//        startTest(500, "", R.string.place_color_card)
//    }

    private fun startTest(imageNumber: Int, result: String, error: Int) {

        Thread.sleep(5000)

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
        appCompatEditText.perform(replaceText(imageNumber.toString()), closeSoftKeyboard())

        if (TestUtil.isEmulator) {
            Thread.sleep(5000)
        }

        val appCompatButton = onView(
            allOf(withId(android.R.id.button1), withText("OK"), isDisplayed())
        )
        appCompatButton.perform(click())

        if (error == -1) {

            if (TestUtil.isEmulator) {
                Thread.sleep(40000)
            } else {
                Thread.sleep(7000)
            }

            onView(
                allOf(
                    withId(R.id.text_title), withText("Residual Chlorine ($imageNumber.0)"),
                    childAtPosition(
                        allOf(
                            withId(R.id.layout),
                            childAtPosition(
                                IsInstanceOf.instanceOf(android.view.ViewGroup::class.java),
                                0
                            )
                        ),
                        0
                    ),
                    isDisplayed()
                )
            ).check(matches(withText("Residual Chlorine ($imageNumber.0)")))

            onView(
                allOf(
                    withId(R.id.textLocalValue), withText(result),
                    childAtPosition(
                        childAtPosition(
                            allOf(
                                withId(R.id.list_results),
                                withContentDescription("List of results")
                            ),
                            0
                        ),
                        1
                    ),
                    isDisplayed()
                )
            ).check(matches(withText(result)))
        } else {
            if (TestUtil.isEmulator) {
                Thread.sleep(40000)
            } else {
                Thread.sleep(7000)
            }
            onView(withText(error)).check(matches(isDisplayed()))
        }
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
