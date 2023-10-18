package io.ffem.lite.internal


import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.common.TestHelper.clearPreferences
import io.ffem.lite.common.TestHelper.isDeviceInitialized
import io.ffem.lite.common.TestHelper.mDevice
import io.ffem.lite.common.TestHelper.startDiagnosticMode
import io.ffem.lite.common.TestUtil.childAtPosition
import io.ffem.lite.common.TestUtil.sleep
import io.ffem.lite.common.getString
import io.ffem.lite.data.clearData
import io.ffem.lite.ui.MainActivity
import org.hamcrest.Matchers.allOf
import org.hamcrest.core.IsInstanceOf
import org.junit.*
import org.junit.runner.RunWith
import timber.log.Timber

@LargeTest
@RunWith(AndroidJUnit4::class)
class CalibrateLoadTest {

    @get:Rule
    val mActivityTestRule = activityScenarioRule<MainActivity>()

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.CAMERA"
        )

    @Before
    fun setUp() {
        clearPreferences()
    }

    @Test
    fun calibrateTest() {
        try {
            onView(withText("YES SHARE")).perform(click())
        } catch (_: Exception) {
        }

        startDiagnosticMode()
        sleep(200)

        pressBack()

        sleep(200)
        onView(withId(R.id.scrollViewSettings)).perform(swipeUp())

        sleep(200)
        onView(withText("Return dummy results")).perform(click())

        sleep(200)

        onView(withId(R.id.scrollViewSettings)).perform(swipeDown())

        sleep(200)

        onView(withText("Colorimetric test")).perform(click())

        sleep(200)

        onView((withText(R.string.calibration_type))).perform(click())
        onView((withText("Full Calibration"))).perform(click())
        sleep(200)

        onView((withText(R.string.calibrate))).perform(click())

        sleep(400)

        onView((withText(R.string.water))).perform(click())

        try {
            onView((withText(R.string.fluoride))).perform(click())
        } catch (e: Exception) {

            val recyclerView2 = onView(
                allOf(
                    withId(R.id.tests_lst),
                )
            )
            recyclerView2.perform(actionOnItemAtPosition<ViewHolder>(7, click()))
        }

        sleep(1000)

        calibrate(0)
        calibrate(1)
        calibrate(2)
        calibrate(3)

        sleep(1000)

//        val view = onView(
//            allOf(
//                withId(R.id.color_vue),
//                withParent(withParent(withId(R.id.result_lyt))),
//                isDisplayed()
//            )
//        )
//        view.check(matches(isDisplayed()))

//        view.check(matches(hasBackgroundColor(R.color.red_500)))

//        val backgroundColor = getBackgroundColor(
//            allOf(
//                withId(R.id.color_vue),
//                withParent(withParent(withId(R.id.result_lyt)))
//            )
//        )
//
//        val textView = onView(
//            allOf(
//                withId(R.id.value_txt), withText("1.0"),
//                withParent(withParent(withId(R.id.result_lyt))),
//                isDisplayed()
//            )
//        )
//        textView.check(matches(withText("1.0")))
//
//        val textView2 = onView(
//            allOf(
//                withId(R.id.name_txt), withText(R.string.fluoride),
//                withParent(withParent(withId(R.id.result_lyt))),
//                isDisplayed()
//            )
//        )
//        textView2.check(matches(withText(R.string.fluoride)))
//
//        onView(withText(R.string.next)).perform(click())
//        onView(withText(R.string.done)).perform(click())
//
//        sleep(1000)

        sleep(1000)

        onView(
            allOf(
                withId(R.id.save_menu), withContentDescription(R.string.save)
            )
        ).perform(click())

        sleep(1000)

        val message = InstrumentationRegistry.getInstrumentation().targetContext.getString(
            R.string.error_calibration_incomplete,
            getString(R.string.fluoride)
        )
        onView(
            allOf(
                withId(com.google.android.material.R.id.snackbar_text), withText(message),
                withParent(withParent(IsInstanceOf.instanceOf(android.widget.FrameLayout::class.java))),
                isDisplayed()
            )
        ).check(matches(withText(message)))

        sleep(5000)

        calibrate(4)

        sleep(1000)

        val textView3 = onView(
            allOf(
                withId(R.id.value_txt), withText("1.00"),
                withParent(
                    allOf(
                        withId(R.id.layout_row),
                        withParent(withId(R.id.calibration_lst))
                    )
                ),
                isDisplayed()
            )
        )
        textView3.check(matches(withText("1.00")))

        sleep(1000)

//        onView(
//            allOf(
//                withIndex(withId(R.id.swatch_view), 2),
//                hasBackgroundColor(backgroundColor)
//            )
//        ).check(matches(isDisplayed()))

        onView(
            allOf(
                withId(R.id.save_menu), withContentDescription(R.string.save)
            )
        ).perform(click())

        sleep(1000)

        onView(
            allOf(
                withId(android.R.id.button2), withText(R.string.cancel)
            )
        ).perform(click())

        sleep(200)

        onView(
            allOf(
                withId(R.id.menuLoad), withContentDescription(R.string.load)
            )
        ).perform(click())

        sleep(500)

        onView(
            allOf(
                withId(com.google.android.material.R.id.snackbar_text), withText("There are no previously saved calibrations"),
                withParent(withParent(IsInstanceOf.instanceOf(android.widget.FrameLayout::class.java))),
                isDisplayed()
            )
        ).check(matches(withText("There are no previously saved calibrations")))

        sleep(500)

        onView(
            allOf(
                withId(R.id.save_menu), withContentDescription(R.string.save)
            )
        ).perform(click())

        sleep(200)

        val appCompatEditText2 = onView(
            allOf(
                withId(R.id.editName),
                isDisplayed()
            )
        )

        sleep(1000)

        appCompatEditText2.perform(
            replaceText("Test Save"),
            closeSoftKeyboard()
        )

        val appCompatEditText3 = onView(
            allOf(
                withId(R.id.desc_edit),
                isDisplayed()
            )
        )
        appCompatEditText3.perform(
            replaceText("Test description"),
            closeSoftKeyboard()
        )

        sleep(200)

        onView(
            allOf(
                withId(android.R.id.button1), withText(R.string.save),
            )
        ).perform(click())

        sleep(300)

        try {
            onView(
                allOf(
                    withId(com.google.android.material.R.id.snackbar_text), withText("Calibration saved"),
                    withParent(withParent(IsInstanceOf.instanceOf(android.widget.FrameLayout::class.java))),
                    isDisplayed()
                )
            ).check(matches(withText("Calibration saved")))
        } catch (_: Exception) {
        }

        sleep(1500)

        onView(
            allOf(
                withId(R.id.menuLoad), withContentDescription(R.string.load)
            )
        ).perform(click())

        val textView4 = onView(
            allOf(
                withId(R.id.title_text), withText("Test Save"),
                withParent(
                    allOf(
                        withId(R.id.layout_row),
                        withParent(withId(R.id.calibrations_list))
                    )
                ),
                isDisplayed()
            )
        )
        textView4.check(matches(withText("Test Save")))

        onView(
            allOf(
                withId(R.id.swatch_view),
                withParent(
                    allOf(
                        withId(R.id.layout_row),
                        withParent(withId(R.id.calibrations_list))
                    )
                ),
                isDisplayed()
            )
        ).check(matches(isDisplayed()))

        val textView5 = onView(
            allOf(
                withId(R.id.desc_text), withText("Test description"),
                withParent(
                    allOf(
                        withId(R.id.layout_row),
                        withParent(withId(R.id.calibrations_list))
                    )
                ),
                isDisplayed()
            )
        )
        textView5.check(matches(withText("Test description")))

        val recyclerView11 = onView(
            allOf(
                withId(R.id.calibrations_list)
            )
        )
        recyclerView11.perform(actionOnItemAtPosition<ViewHolder>(0, click()))

        val appCompatButton5 = onView(
            allOf(
                withId(android.R.id.button1), withText(R.string.load),
                childAtPosition(
                    childAtPosition(
                        withId(androidx.appcompat.R.id.buttonPanel),
                        0
                    ),
                    3
                )
            )
        )
        appCompatButton5.perform(scrollTo(), click())

        sleep(2500)

        onView(
            allOf(
                withId(R.id.save_menu), withContentDescription(R.string.save)
            )
        ).perform(click())

        sleep(500)

        onView(
            allOf(
                withId(com.google.android.material.R.id.snackbar_text), withText("Calibration already saved as: Test Save"),
                withParent(withParent(IsInstanceOf.instanceOf(android.widget.FrameLayout::class.java))),
                isDisplayed()
            )
        ).check(matches(withText("Calibration already saved as: Test Save")))
    }

    private fun calibrate(i: Int) {

        onView(withId(R.id.calibration_lst))
            .perform(actionOnItemAtPosition<ViewHolder>(i, click()))

        sleep(1000)

        try {
            onView(withId(R.id.editExpiryDate)).perform(click())

            onView(
                allOf(
                    withId(android.R.id.button1), withText(android.R.string.ok),
                    isDisplayed()
                )
            ).perform(click())

            onView(
                allOf(
                    withId(android.R.id.button1), withText(R.string.save),
                    isDisplayed()
                )
            ).perform(click())

            sleep(1000)
        } catch (e: Exception) {
            Timber.e(e)
        }


//        for (i in 0..4) {
//            onView(withText(R.string.next)).perform(click())
//            sleep(200)
//        }
//
//        onView((withText(R.string.skip))).perform(click())
//
//        sleep(200)

        sleep(200)

        onView((withText("Generate Dummy Result"))).perform(click())

        sleep(1000)

        onView(
            withText(R.string.close)
        ).perform(click())

    }

    companion object {
        @JvmStatic
        var initialized = false

        @JvmStatic
        @BeforeClass
        fun initialize() {
            BuildConfig.USE_SCREEN_PINNING.set(false)
            if (!isDeviceInitialized()) {
                mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            }
            clearData(ApplicationProvider.getApplicationContext())
        }

        @JvmStatic
        @AfterClass
        fun teardown() {
            clearData(ApplicationProvider.getApplicationContext())
        }
    }
}
