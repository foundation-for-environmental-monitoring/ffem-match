package io.ffem.lite.common

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.widget.ScrollView
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.*
import io.ffem.lite.R
import timber.log.Timber

const val EXTERNAL_SURVEY_PACKAGE_NAME = "io.ffem.collect"
const val NEXT = "next"

object TestHelper {

    lateinit var mDevice: UiDevice

    private val isEmulator: Boolean
        get() = (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT)

    fun enterDiagnosticMode() {
        for (i in 0..9) {
            onView(withId(R.id.textVersion)).perform(click())
        }
    }

    fun leaveDiagnosticMode() {
        onView(withId(R.id.fabDisableDiagnostics)).perform(click())
    }

    fun clearPreferences(activityTestRule: ActivityTestRule<*>) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activityTestRule.activity)
        prefs.edit().clear().apply()
    }

    fun gotoSurveyForm() {
        val context: Context? = InstrumentationRegistry.getInstrumentation().context
        val intent =
            context!!.packageManager.getLaunchIntentForPackage(EXTERNAL_SURVEY_PACKAGE_NAME)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        mDevice.waitForIdle()
        Thread.sleep(1000)
        val addButton: UiObject? = mDevice.findObject(
            UiSelector()
                .resourceId("$EXTERNAL_SURVEY_PACKAGE_NAME:id/enter_data")
        )
        try {
            if (addButton!!.exists() && addButton.isEnabled) {
                addButton.click()
            }
        } catch (e: UiObjectNotFoundException) {
            Timber.e(e)
        }
        mDevice.waitForIdle()
        clickListViewItem("Lite Automated Testing")
        mDevice.waitForIdle()
        val goToStartButton: UiObject? = mDevice.findObject(
            UiSelector()
                .resourceId("$EXTERNAL_SURVEY_PACKAGE_NAME:id/jumpBeginningButton")
        )
        try {
            if (goToStartButton!!.exists() && goToStartButton.isEnabled) {
                goToStartButton.click()
            }
        } catch (e: UiObjectNotFoundException) {
            Timber.e(e)
        }
        mDevice.waitForIdle()
    }

    private fun clickListViewItem(@Suppress("SameParameterValue") name: String): Boolean {
        val listView = UiScrollable(UiSelector())
        listView.maxSearchSwipes = 4
        listView.waitForExists(3000)
        val listViewItem: UiObject
        try {
            if (listView.scrollTextIntoView(name)) {
                listViewItem = listView.getChildByText(
                    UiSelector()
                        .className(TextView::class.java.name), "" + name + ""
                )
                listViewItem.click()
            } else {
                return false
            }
        } catch (e: UiObjectNotFoundException) {
            return false
        }
        return true
    }

    private fun findButtonInScrollable(name: String?) {
        val listView = UiScrollable(UiSelector().className(ScrollView::class.java.name))
        listView.maxSearchSwipes = 10
        listView.waitForExists(5000)
        try {
            listView.scrollTextIntoView(name)
        } catch (ignored: Exception) {
        }
    }

    fun clickLaunchButton(index: Int) {
        var buttonText = "Launch"
        findButtonInScrollable(buttonText)
        var buttons: List<UiObject2?>? = mDevice.findObjects(By.text(buttonText))
        if (buttons?.size == 0) {
            buttonText = buttonText.toUpperCase()
        }
        buttons = mDevice.findObjects(By.text(buttonText))
        buttons!![index]!!.click()
        mDevice.waitForWindowUpdate("", 2000)
        SystemClock.sleep(4000)
    }

    private fun clickNextButton() {
        try {
            var buttonText = NEXT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                buttonText = buttonText.toUpperCase()
            }
            findButtonInScrollable(buttonText)
            mDevice.findObject(UiSelector().text(buttonText)).click()

            mDevice.waitForWindowUpdate("", 2000)
        } catch (e: UiObjectNotFoundException) {
            Timber.e(e)
        }
    }

    private fun swipeLeft() {
        if (isEmulator) {
            clickNextButton()
        } else {
            mDevice.waitForIdle()
            mDevice.swipe(500, 400, 50, 400, 4)
            mDevice.waitForIdle()
        }
    }

    private fun swipeDown() {
        for (i in 0..2) {
            mDevice.waitForIdle()
            mDevice.swipe(300, 400, 300, 750, 4)
        }
    }

    fun nextSurveyPage(times: Int, tabName: String) {
        var tab: UiObject2? = mDevice.findObject(By.text(tabName))
        if (tab == null) {
            for (i in 0..11) {
                swipeLeft()
                mDevice.waitForIdle()
                tab = mDevice.findObject(By.text(tabName))
                if (tab != null) {
                    break
                }
                tab = mDevice.findObject(By.text("Fluoride"))
                if (tab != null) {
                    for (ii in 0 until times) {
                        mDevice.waitForIdle()
                        swipeLeft()
                        SystemClock.sleep(300)
                        tab = mDevice.findObject(By.text(tabName))
                        if (tab != null) {
                            break
                        }
                    }
                    break
                }
            }
        }
        swipeDown()
        mDevice.waitForIdle()
    }

    fun isDeviceInitialized(): Boolean {
        return ::mDevice.isInitialized
    }
}
