package io.ffem.lite.common

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import kotlin.math.abs

object TestUtil {

    val isEmulator: Boolean
        get() = ((Build.ID.contains("KOT49H") && Build.MODEL.contains("MLA-AL10"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.HOST.startsWith("SWDG2909")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic")
                && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT)

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
