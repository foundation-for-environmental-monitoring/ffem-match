package io.ffem.lite.common

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.intent.Checks
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import kotlin.math.abs

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class RequiresExternalApp

object TestUtil {

    fun childAtPosition(
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

    fun checkResult(value: Double): Matcher<View?> {
        return object : TypeSafeMatcher<View?>() {
            var convertedValue = 0.0
            override fun matchesSafely(item: View?): Boolean {
                if (item !is TextView) return false
                var text = item.text.toString()
                if (text.contains(" ")) {
                    text = text.subSequence(text.lastIndexOf(" "), text.length - 1).toString()
                }
                convertedValue = try {
                    text.toDouble()
                } catch (e: Exception) {
                    return false
                }
                val delta = abs(convertedValue - value)
                return delta < 0.30f
            }

            override fun describeTo(description: Description) {
                description.appendText("Result: $convertedValue does not match expected value: $value")
            }
        }
    }

    fun checkResult(testData: TestData, checkResult: Double = -1.0): Matcher<View?> {
        return object : TypeSafeMatcher<View?>() {
            override fun matchesSafely(item: View?): Boolean {
                if (item !is TextView) return false
                var text = item.text.toString()
                if (text.contains(" ")) {
                    text = text.subSequence(text.lastIndexOf(" ") + 1, text.length).toString()
                }
                val convertedValue = try {
                    text.toDouble()
                } catch (e: Exception) {
                    return false
                }

                val expectedResult = if (checkResult > 0) {
                    checkResult
                } else {
                    testData.expectedResult
                }

                val delta = abs(convertedValue - expectedResult)
                var resultOk = if (testData.maxResult > 0) {
                    delta <= testData.maxResult * 0.1
                } else {
                    delta <= testData.expectedMarginOfError
                }
                if (testData.maxResult > 0 && resultOk && convertedValue >= testData.maxResult * 0.9) {
                    resultOk = item.text.toString().startsWith("> ")
                }
                return resultOk
            }

            override fun describeTo(description: Description) {
                description.appendText("Result does not match expected value")
            }
        }
    }

    fun checkMarginOfError(testData: TestData): Matcher<View?> {
        return object : TypeSafeMatcher<View?>() {
            override fun matchesSafely(item: View?): Boolean {
                if (item !is TextView) return false
                var text = item.text.toString()
                if (text.contains(" ")) {
                    text = text.subSequence(text.lastIndexOf(" ") + 1, text.length).toString()
                }
                val convertedValue = try {
                    text.toDouble()
                } catch (e: Exception) {
                    return false
                }
                return abs(convertedValue - testData.expectedMarginOfError) <= 0.2
            }

            override fun describeTo(description: Description) {
                description.appendText("Margin of error does not match expected value")
            }
        }
    }

    fun sleep(millis: Int) {
        SystemClock.sleep(millis.toLong())
    }

    /**
     * Matches the view at the given index. Useful when several views have the same properties.
     * https://stackoverflow.com/questions/29378552
     */
    fun withIndex(matcher: Matcher<View?>, index: Int): Matcher<View?> {
        return object : TypeSafeMatcher<View>() {
            var currentIndex = 0
            var viewObjHash = 0

            @SuppressLint("DefaultLocale")
            override fun describeTo(description: Description) {
                description.appendText(String.format("with index: %d ", index))
                matcher.describeTo(description)
            }

            override fun matchesSafely(view: View): Boolean {
                if (matcher.matches(view) && currentIndex++ == index) {
                    viewObjHash = view.hashCode()
                }
                return view.hashCode() == viewObjHash
            }
        }
    }

    fun getBackgroundColor(matcher: Matcher<View?>?): Int {
        var colorHolder = 0
        Espresso.onView(matcher).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View?>? {
                return ViewMatchers.isAssignableFrom(View::class.java)
            }

            override fun getDescription(): String {
                return "getting color from a View"
            }

            override fun perform(uiController: UiController?, view: View) {
                val viewColor = view.background as ColorDrawable
                val colorId = viewColor.color
                colorHolder = colorId
            }
        })
        return colorHolder
    }


    fun hasBackgroundColor(color: Int): Matcher<View> {
        Checks.checkNotNull(color)

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description?) {
                description?.appendText("background color: $color")
            }

            override fun matchesSafely(item: View?): Boolean {
                val textViewColor = (item?.background as ColorDrawable).color
                return textViewColor == color
            }
        }
    }
}
