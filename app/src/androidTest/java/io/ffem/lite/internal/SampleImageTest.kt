package io.ffem.lite.internal


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
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.common.TestHelper.clearPreferences
import io.ffem.lite.common.TestHelper.sleep
import io.ffem.lite.common.TestUtil.checkResult
import io.ffem.lite.common.TestUtil.childAtPosition
import io.ffem.lite.common.clearData
import io.ffem.lite.common.pH
import io.ffem.lite.common.testDataList
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.ErrorType.NO_ERROR
import io.ffem.lite.model.toLocalString
import io.ffem.lite.model.toResourceId
import io.ffem.lite.ui.ResultListActivity
import io.ffem.lite.util.PreferencesUtil
import io.ffem.lite.util.toLocalString
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.hamcrest.core.IsInstanceOf
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File

const val TIME_DELAY = 10000L

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SampleImageTest {

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
        if (!initialized) {
            clearPreferences()
            clearData()
            initialized = true
        }
    }

    @Test
    fun image_000_Chlorine_0_Point_5() {
        startInternalTest(0)
    }

    @Test
    fun image_001_Chlorine_0() {
        startInternalTest(1)
    }

    @Test
    fun image_002_InvalidBarcode() {
        startInternalTest(2)
    }

    @Test
    fun image_003_InvalidBarcode() {
        startInternalTest(3)
    }

    @Test
    fun image_004_pH_NoMatch() {
        startInternalTest(4)
    }

    @Test
    fun image_005_Waiting() {
        startInternalTest(5)
    }

    @Test
    fun image_006_Chlorine_NoMatch() {
        startInternalTest(6)
    }

    @Test
    fun image_007_Chlorine_Point_5() {
        startInternalTest(7)
    }

    @Test
    fun image_008_Chlorine_1_Point_5() {
        startInternalTest(8)
    }

    @Test
    fun image_009_BadLight() {
        startInternalTest(9)
    }

    @Test
    fun image_010_Chlorine_CalibrationError() {
        startInternalTest(10)
    }

    @Test
    fun image_011_Tilted() {
        startInternalTest(11)
    }

    @Test
    fun image_012_Waiting() {
        startInternalTest(12)
    }

    @Test
    fun image_013_Waiting() {
        startInternalTest(13)
    }

    @Test
    fun image_014_pH_6_Point_5() {
        startInternalTest(14)
    }

    @Test
    fun image_015_Chlorine_4_Point_3() {
        startInternalTest(15)
    }

    @Test
    fun image_016_Chlorine_CalibrationError() {
        startInternalTest(16)
    }

    @Test
    fun image_017_BadLighting() {
        startInternalTest(17)
    }

    @Test
    fun image_018_Waiting() {
        startInternalTest(18)
    }

    @Test
    fun image_019_Chlorine_3_Point_0() {
        startInternalTest(19)
    }

    @Test
    fun image_020_Waiting() {
        startInternalTest(20)
    }

    @Test
    fun image_021_Waiting() {
        startInternalTest(21)
    }

    @Test
    fun image_022_Waiting() {
        startInternalTest(22)
    }

    @Test
    fun image_023_FluorideHighRange_NoMatch() {
        startInternalTest(23)
    }

    @Test
    fun image_024_Fluoride_1_Point_0() {
        startInternalTest(24)
    }

    @Test
    fun image_025_Nitrate_0() {
        startInternalTest(25)
    }

    @Test
    fun image_026_Iron_0() {
        startInternalTest(26)
    }

    @Test
    fun image_027_Fluoride_0_Point_5() {
        startInternalTest(27)
    }

    @Test
    fun image_028_pH_7_Point_0() {
        startInternalTest(28)
    }

    @Test
    fun image_029_Phosphate_1_Point_6() {
        startInternalTest(29)
    }

    @Test
    fun imageX_Waiting() {
        startInternalTest(500)
    }

    private fun startInternalTest(imageNumber: Int) {
        val testData = testDataList[imageNumber]!!

        PreferencesUtil.setString(
            ApplicationProvider.getApplicationContext(),
            R.string.testImageNumberKey, imageNumber.toString()
        )

        sleep(2000)

        onView(withId(R.id.start_test_fab)).perform(click())

        onView(withText(R.string.start)).perform(click())

        if (testData.expectedScanError == -1) {

            sleep(TIME_DELAY)

            if (testData.expectedResultError != ErrorType.BAD_LIGHTING &&
                testData.expectedResultError != ErrorType.IMAGE_TILTED
            ) {
                onView(withText(R.string.accept)).perform(click())
            }

            onView(withText(testData.testDetails.name.toLocalString())).check(matches(isDisplayed()))

            if (testData.expectedResultError > NO_ERROR) {
                onView(withText(testData.expectedResultError.toLocalString(context))).check(
                    matches(isDisplayed())
                )
                onView(withText(R.string.close)).perform(click())
            } else {

                onView(withText(testData.testDetails.name.toLocalString())).check(
                    matches(
                        isDisplayed()
                    )
                )

                val resultTextView = onView(withId(R.id.result_txt))
                resultTextView.check(matches(checkResult(testData.expectedResult)))

                if (testData.testDetails == pH) {
                    onView(withId(R.id.unit_txt)).check(matches(not(isDisplayed())))
                } else {
                    onView(allOf(withId(R.id.unit_txt), withText("mg/l")))
                        .check(matches(isDisplayed()))
                }

                val marginOfErrorView = onView(withId(R.id.error_margin_txt))
                marginOfErrorView.check(matches(checkResult(testData.expectedMarginOfError)))

                onView(
                    withText(
                        testData.risk.toResourceId(
                            ApplicationProvider.getApplicationContext(),
                            testData.testDetails.riskType
                        )
                    )
                ).check(
                    matches(isDisplayed())
                )

                onView(withText(R.string.close)).perform(click())
            }

            sleep(1000)

            onView(
                allOf(
                    withId(R.id.text_title),
                    withText("${testData.testDetails.name.toLocalString()} (${imageNumber})"),
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
            ).check(matches(withText("${testData.testDetails.name.toLocalString()} (${imageNumber})")))

            val textView = onView(
                allOf(
                    withId(R.id.textResultValue),
                    childAtPosition(
                        childAtPosition(
                            allOf(
                                withId(R.id.test_results_lst),
                                withContentDescription(R.string.result_list)
                            ),
                            0
                        ),
                        1
                    ),
                    isDisplayed()
                )
            )

            sleep(3000)

            if (testData.expectedResultError == NO_ERROR) {
                textView.check(matches(checkResult(testData.expectedResult)))
            } else {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                textView.check(matches(withText(testData.expectedResultError.toLocalString(context))))
            }

            textView.perform(click())

            sleep(2000)

            val imageView = onView(
                allOf(
                    withId(R.id.extract_img), withContentDescription(R.string.analyzed_image),
                    isDisplayed()
                )
            )
            imageView.check(matches(isDisplayed()))

            onView(withId(R.id.resultScrollView))
                .perform(ViewActions.swipeUp())

            val imageView2 = onView(
                allOf(
                    withId(R.id.full_photo_img), withContentDescription(R.string.analyzed_image),
                    isDisplayed()
                )
            )
            imageView2.check(matches(isDisplayed()))

        } else {

            sleep(TIME_DELAY)

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
        }

        @JvmStatic
        @BeforeClass
        fun initialize() {
            BuildConfig.INSTRUMENTED_TEST_RUNNING.set(true)
            context = InstrumentationRegistry.getInstrumentation().targetContext
        }
    }
}