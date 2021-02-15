package io.ffem.lite.common

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.widget.ScrollView
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.data.AppDatabase
import org.junit.Assert
import timber.log.Timber
import java.io.File
import java.util.*


const val EXTERNAL_SURVEY_PACKAGE_NAME = "io.ffem.collect"
const val TEST_SURVEY_NAME = "ffem Lite Testing"

fun clearData() {
    val db = AppDatabase.getDatabase(ApplicationProvider.getApplicationContext())
    try {
        db.clearAllTables()
    } finally {
        db.close()
    }
}

object TestHelper {

    lateinit var mDevice: UiDevice
    var screenshotCount = -1
    var screenshotEnabled = false

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

    fun clearPreferences() {
        val prefs =
            PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())
        prefs.edit().clear().apply()
    }

    fun startSurveyApp() {
        val context: Context? = InstrumentationRegistry.getInstrumentation().context
        val intent =
            context!!.packageManager.getLaunchIntentForPackage(EXTERNAL_SURVEY_PACKAGE_NAME)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        mDevice.waitForIdle()
        Thread.sleep(1000)
    }

    fun gotoSurveySelection() {
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
        Thread.sleep(1000)
    }

    fun gotoSurveyForm() {
        clickListViewItem(TEST_SURVEY_NAME)
        mDevice.waitForIdle()
        Thread.sleep(1000)
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

    fun gotoFormSubmit() {
        clickListViewItem(TEST_SURVEY_NAME)
        mDevice.waitForIdle()
        Thread.sleep(1000)
        val gotoEndButton: UiObject? = mDevice.findObject(
            UiSelector()
                .resourceId("$EXTERNAL_SURVEY_PACKAGE_NAME:id/jumpEndButton")
        )
        try {
            if (gotoEndButton!!.exists() && gotoEndButton.isEnabled) {
                gotoEndButton.click()
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

    fun clickLaunchButton(text: String) {
        var buttonText = text
        findButtonInScrollable(buttonText)
        val buttons: List<UiObject2?>? = mDevice.findObjects(By.text(buttonText))
        if (buttons?.size == 0) {
            buttonText = buttonText.toUpperCase()
        }
        mDevice.findObject(By.text(buttonText)).click()
        mDevice.waitForWindowUpdate("", 2000)
        SystemClock.sleep(4000)
    }

    private fun clickNextButton() {
        try {
            var buttonText =
                ApplicationProvider.getApplicationContext<Context>().getString(R.string.next)
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

//    fun swipeUp() {
//        for (i in 0..2) {
//            mDevice.waitForIdle()
//            mDevice.swipe(300, 750, 300, 400, 4)
//        }
//    }

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

                val buttonText =
                    ApplicationProvider.getApplicationContext<Context>().getString(R.string.back)
                findButtonInScrollable(buttonText)
                val button = mDevice.findObject(UiSelector().text(buttonText))
                if (button == null) {
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

    fun takeScreenshot(name: String) {
        takeScreenshot(name, screenshotCount++)
    }

    fun takeScreenshot(name: String, page: Int) {
        if (screenshotEnabled && BuildConfig.TAKE_SCREENSHOTS
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
        ) {
            val folder = File(
                InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(
                    Environment.DIRECTORY_PICTURES
                )
                    .toString() + "/screenshots/"
            )
            val path = File(
                folder, name + "-" + Locale.getDefault().language.substring(0, 2) + "-" +
                        String.format("%02d", page + 1) + ".png"
            )

            if (!folder.exists()) {
                folder.mkdirs()
            }

            mDevice.takeScreenshot(path, 0.1f, 30)
        }
    }

    fun selectMenuItem() {
        val menuButton = mDevice.findObject(
            By.desc(
                ApplicationProvider
                    .getApplicationContext<Context>().getString(R.string.view_hierarchy)
            )
        )
        if (menuButton != null && menuButton.isClickable) {
            menuButton.click()
            menuButton.recycle()
        } else {
            Assert.fail()
        }
    }

    fun sleep(duration: Long) {
        SystemClock.sleep(duration)
    }
}
