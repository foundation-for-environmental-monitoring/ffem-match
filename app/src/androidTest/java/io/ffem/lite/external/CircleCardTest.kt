package io.ffem.lite.external


import android.content.Context
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.common.*
import io.ffem.lite.common.TestHelper.clearPreferences
import io.ffem.lite.common.TestHelper.startDiagnosticMode
import io.ffem.lite.common.TestUtil.sleep
import io.ffem.lite.internal.SCAN_TIME_DELAY
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.toLocalString
import io.ffem.lite.ui.MainActivity
import io.ffem.lite.util.PreferencesUtil
import org.hamcrest.Matchers.allOf
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File

@Suppress("SameParameterValue")
@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class CircleCardTest {

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
        if (!initialized) {
            clearPreferences()
            clearData()
            setDiagnostics()
            initialized = true
        }
    }

    private fun setDiagnostics() {
        sleep(2000)
        startDiagnosticMode()
        sleep(400)

        Espresso.pressBack()
        sleep(400)

        Espresso.pressBack()
        sleep(400)

//        onView(withText("Run color card test")).perform(click())
//        sleep(200)
//
//        onView(withText("Circle Swatch")).perform(click())
//        sleep(200)
//
//        Espresso.pressBack()
//        sleep(200)
//
//        Espresso.pressBack()
//        sleep(200)
    }

    @Ignore("Measurements changed")
    @Test
    fun image_000_Fluoride_1_Point_5() {
        startInternalTest(0)
    }

    @Test
    fun image_001_Fluoride_1_Point_0() {
        startInternalTest(1)
    }

    @Ignore("Measurements changed")
    @Test
    fun image_002_Fluoride_1_Point_0() {
        startInternalTest(2)
    }

    @Test
    fun image_003_pH_1_Point_0() {
        startInternalTest(3)
    }
//
//    @Ignore
//    @Test
//    fun image_004_Fe_1_Point_0() {
//        startInternalTest(4)
//    }

    private fun startInternalTest(imageNumber: Int) {
        val testData = testDataList[imageNumber]!!

        PreferencesUtil.setString(
            ApplicationProvider.getApplicationContext(),
            R.string.testImageNumberKey, imageNumber.toString()
        )

        sleep(500)
        onView(withId(R.id.card_test_button)).perform(click())

        sleep(2000)
        TestHelper.mDevice.findObject(By.text(getString(R.string.enter_data))).click()

        sleep(1000)

        try {
            TestHelper.mDevice.findObject(By.text(TEST_SURVEY_NAME)).click()
        } catch (e: Exception) {
            ViewActions.swipeUp()
            TestHelper.mDevice.findObject(By.text(TEST_SURVEY_NAME)).click()
        }
        sleep(2000)
        TestHelper.mDevice.findObject(By.text(getString(R.string.next).uppercase())).click()

        sleep(500)
        TestHelper.mDevice.findObject(By.text(getString(R.string.fluoride_card))).click()

        sleep(500)
        onView(withText(R.string.start_test)).perform(click())

        sleep(500)
        onView(withId(R.id.noDilution_btn)).perform(click())

        sleep(500)
        onView(withText(R.string.start)).perform(click())

        sleep(500)

        if (testData.expectedScanError == -1) {

            sleep(SCAN_TIME_DELAY)

            if (testData.expectedResultError != ErrorType.BAD_LIGHTING &&
                testData.expectedResultError != ErrorType.IMAGE_TILTED
            ) {
                onView(withText(R.string.continue_on)).perform(click())
            }

//            onView(withText(testData.testDetails.name)).check(matches(isDisplayed()))

            if (testData.expectedResultError > ErrorType.NO_ERROR) {
                onView(withText(testData.expectedResultError.toLocalString())).check(
                    matches(isDisplayed())
                )
                onView(withText(R.string.close)).perform(click())
            } else {

//                onView(withText(testData.testDetails.name)).check(
//                    matches(
//                        isDisplayed()
//                    )
//                )

                val resultTextView = onView(withId(R.id.result_txt))
                resultTextView.check(matches(TestUtil.checkResult(testData)))

//                if (testData.testDetails == pH) {
//                    onView(withId(R.id.unit_txt)).check(matches(not(isDisplayed())))
//                } else {
//                    onView(allOf(withId(R.id.unit_txt), withText("mg/l")))
//                        .check(matches(isDisplayed()))
//                }

//                onView(withId(R.id.error_margin_text))
//                    .check(matches(withEffectiveVisibility(Visibility.GONE)))

//                onView(
//                    withText(
//                        testData.risk.toResourceId(
//                            ApplicationProvider.getApplicationContext(),
//                            testData.testDetails.riskType
//                        )
//                    )
//                ).check(
//                    matches(isDisplayed())
//                )

                onView(withId(R.id.resultScrollView))
                    .perform(ViewActions.swipeUp())

                onView(
                    allOf(
                        TestUtil.withIndex(withText(R.string.next), 1),
                    )
                ).perform(click())

//                onView(withText(R.string.next)).perform(click())
                sleep(1000)

//                onView(withText(R.string.done)).perform(click())
//                sleep(1000)
            }
        } else {

            sleep(SCAN_TIME_DELAY)

            onView(withText(testData.expectedScanError)).check(matches(isDisplayed()))

            Espresso.pressBack()
        }
    }


    companion object {

        @JvmStatic
        var initialized = false
        lateinit var context: Context

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
            clearPreferences()
        }

        @JvmStatic
        @BeforeClass
        fun initialize() {
            BuildConfig.INSTRUMENTED_TEST_RUNNING.set(true)
            BuildConfig.USE_SCREEN_PINNING.set(false)
            context = InstrumentationRegistry.getInstrumentation().targetContext
            if (!TestHelper.isDeviceInitialized()) {
                TestHelper.mDevice =
                    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            }
        }
    }
}