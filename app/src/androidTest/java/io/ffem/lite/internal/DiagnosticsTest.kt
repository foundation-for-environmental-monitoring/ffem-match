package io.ffem.lite.internal


import android.os.Environment
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.common.TestHelper
import io.ffem.lite.common.TestHelper.startDiagnosticMode
import io.ffem.lite.common.TestUtil.childAtPosition
import io.ffem.lite.common.TestUtil.sleep
import io.ffem.lite.common.clearData
import io.ffem.lite.common.fluoride
import io.ffem.lite.ui.ResultListActivity
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.junit.*
import org.junit.runner.RunWith
import java.io.File


@LargeTest
@RunWith(AndroidJUnit4::class)
class DiagnosticsTest {

    @get:Rule
    val mActivityTestRule = activityScenarioRule<ResultListActivity>()

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.CAMERA"
        )

    @Before
    fun setUp() {
        if (!CalibrationTest.initialized) {
            TestHelper.clearPreferences()
            clearData()
            CalibrationTest.initialized = true
        }
    }

    @Test
    fun deleteDataTest() {
        startDiagnosticMode()

        sleep(400)

        Espresso.pressBack()

        sleep(400)

        onView(withId(R.id.scrollViewSettings)).perform(swipeUp())

        sleep(400)

        onView(withText(R.string.delete_data)).perform(click())

        sleep(400)

        val appCompatButton2 = onView(
            allOf(
                withId(android.R.id.button1), withText(R.string.delete),
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
        TestHelper.clearPreferences()

        startDiagnosticMode()

        sleep(400)

        Espresso.pressBack()

        sleep(400)

        onView(withId(R.id.scrollViewSettings)).perform(swipeUp())

        sleep(400)

        pressBack()

        onView(withText("Test Image Number")).perform(click())

        sleep(400)

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
        ).perform(scrollTo(), replaceText("0"))

        val appCompatEditText2 = onView(
            allOf(
                withId(android.R.id.edit), withText("0"),
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
                withId(android.R.id.button1), withText(android.R.string.ok),
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
                withId(R.id.colorimetric_button), withContentDescription(R.string.start_test),
                isDisplayed()
            )
        )
        floatingActionButton.perform(click())

        onView(withText(R.string.start)).perform(click())

        sleep(SCAN_TIME_DELAY)

        onView(withText(R.string.continue_on)).perform(click())

        onView(
            allOf(
                withId(R.id.name_txt), withText(fluoride.name),
                isDisplayed()
            )
        ).check(matches(withText(fluoride.name)))

//        onView(allOf(withId(R.id.result_txt))).check(matches(withText("1.99")))
//
//        onView(allOf(withId(R.id.unit_txt))).check(matches(withText("mg/l")))

//        val textView4 = onView(
//            allOf(
//                withId(R.id.value_txt), withText(R.string.high_quantity),
//                isDisplayed()
//            )
//        )
//        textView4.check(matches(withText(R.string.high_quantity)))
//
//        onView(allOf(withText(R.string.margin_of_error), isDisplayed()))
//
//        onView(
//            allOf(
//                withId(R.id.error_margin_text),
//                isDisplayed()
//            )
//        ).check(matches(TestUtil.checkResult(testDataList[0]!!, 0.25)))

        onView(withId(R.id.resultScrollView))
            .perform(ViewActions.swipeUp())

        onView(withText(R.string.next)).perform(click())
    }

    companion object {

        @JvmStatic
        @AfterClass
        fun teardown() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val folder = File(
                context.getExternalFilesDir(
                    Environment.DIRECTORY_PICTURES
                ).toString() + File.separator + "captures"
            )
            if (folder.exists() && folder.isDirectory) {
                folder.deleteRecursively()
            }
            clearData()
            TestHelper.clearPreferences()
        }

        @JvmStatic
        @BeforeClass
        fun initialize() {
            BuildConfig.INSTRUMENTED_TEST_RUNNING.set(true)
        }
    }
}

