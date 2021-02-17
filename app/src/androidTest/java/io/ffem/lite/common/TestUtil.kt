package io.ffem.lite.common

import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
                val delta = abs(convertedValue - value)
                return delta < 0.20f
            }

            override fun describeTo(description: Description) {
                description.appendText("Result does not match expected value")
            }
        }
    }

    fun sleep(millis: Long) {
        SystemClock.sleep(millis)
    }
}
