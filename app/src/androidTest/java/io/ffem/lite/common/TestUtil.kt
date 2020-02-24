package io.ffem.lite.common

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import kotlin.math.abs

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

    fun checkResult(value: Float): Matcher<View?>? {
        return object : TypeSafeMatcher<View?>() {
            override fun matchesSafely(item: View?): Boolean {
                if (item !is TextView) return false
                val convertedValue =
                    java.lang.Float.valueOf(item.text.toString())
                val delta = abs(convertedValue - value)
                return delta < 0.20f
            }

            override fun describeTo(description: Description) {
                description.appendText("Value expected is wrong")
            }
        }
    }
}
