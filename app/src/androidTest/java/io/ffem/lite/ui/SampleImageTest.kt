package io.ffem.lite.ui


import android.content.Context
import android.os.Environment
import android.os.SystemClock
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.app.AppDatabase
import io.ffem.lite.common.TestHelper.clearPreferences
import io.ffem.lite.common.TestUtil.checkResult
import io.ffem.lite.common.TestUtil.childAtPosition
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.ErrorType.*
import io.ffem.lite.model.toLocalString
import io.ffem.lite.util.PreferencesUtil
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.hamcrest.core.IsInstanceOf
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File

const val pH = "pH"
const val residualChlorine = "Residual Chlorine"
const val fluoride: String = "Fluoride"
const val fluorideHighRange: String = "Fluoride - High Range"

fun clearData(context: Context) {
    val db = AppDatabase.getDatabase(context)
    db.resultDao().deleteAll()
}

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SampleImageTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(ResultListActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        )

    @Before
    fun setUp() {
        if (!initialized) {
            clearPreferences(mActivityTestRule)
            clearData(mActivityTestRule.activity)
            initialized = true
        }
    }

    @Test
    fun image_000_Chlorine_0_Point_5() {
        startTest(residualChlorine, 0, "0.5")
    }

    @Test
    fun image_001_Chlorine_0() {
        startTest(residualChlorine, 1, "0.0")
    }

    @Test
    fun image_002_InvalidBarcode() {
        startTest(residualChlorine, 2, expectedScanError = R.string.invalid_barcode)
    }

    @Test
    fun image_003_InvalidBarcode() {
        startTest(fluoride, 3, expectedScanError = R.string.invalid_barcode)
    }

    @Test
    fun image_004_pH_NoMatch() {
        startTest(pH, 4, expectedResultError = NO_MATCH)
    }

    @Test
    fun image_005_Waiting() {
        startTest(residualChlorine, 5, expectedScanError = R.string.place_color_card)
    }

    @Test
    fun image_006_Chlorine_NoMatch() {
        startTest(residualChlorine, 6, expectedResultError = NO_MATCH)
    }

    @Test
    fun image_007_Chlorine_Point_5() {
        startTest(residualChlorine, 7, "0.5")
    }

    @Test
    fun image_008_Chlorine_1_Point_5() {
        startTest(residualChlorine, 8, "1.5")
    }

    @Test
    fun image_009_BadLight() {
        startTest(residualChlorine, 9, expectedScanError = R.string.place_color_card)
    }

    @Test
    fun image_010_Chlorine_CalibrationError() {
        startTest(residualChlorine, 10, expectedResultError = CALIBRATION_ERROR)
    }

    @Test
    fun image_011_Tilted() {
        startTest(residualChlorine, 11, expectedScanError = R.string.correct_camera_tilt)
    }

    @Test
    fun image_012_Waiting() {
        startTest(residualChlorine, 12, expectedScanError = R.string.place_color_card)
    }

    @Test
    fun image_013_Waiting() {
        startTest(residualChlorine, 13, expectedScanError = R.string.place_color_card)
    }

    @Test
    fun image_014_pH_6_Point_5() {
        startTest(pH, 14, "6.5")
    }

    @Test
    fun image_015_Chlorine_4_Point_3() {
        startTest(residualChlorine, 15, "0.43")
    }

    @Test
    fun image_016_Chlorine_CalibrationError() {
        startTest(residualChlorine, 16, expectedResultError = CALIBRATION_ERROR)
    }

    @Test
    fun image_017_BadLighting() {
        startTest(residualChlorine, 17, expectedScanError = R.string.place_color_card)
    }

    @Test
    fun image_018_Waiting() {
        startTest(residualChlorine, 18, expectedScanError = R.string.place_color_card)
    }

    @Test
    fun image_019_Chlorine_3_Point_0() {
        startTest(residualChlorine, 19, "3.0")
    }

    @Test
    fun image_020_Waiting() {
        startTest(residualChlorine, 20, expectedScanError = R.string.place_color_card)
    }

    @Test
    fun image_021_Waiting() {
        startTest(residualChlorine, 21, expectedScanError = R.string.place_color_card)
    }

    @Test
    fun image_022_Waiting() {
        startTest(pH, 22, expectedScanError = R.string.place_color_card)
    }

    @Test
    fun image_023_FluorideHighRange_NoMatch() {
        startTest(fluorideHighRange, 23, expectedResultError = NO_MATCH)
    }

//    @Test
//    fun imageX_Waiting() {
//        startTest(pH, 500, expectedScanError = R.string.place_color_card)
//    }

    private fun startTest(
        name: String,
        imageNumber: Int,
        expectedResult: String = "",
        expectedResultError: ErrorType = NO_ERROR,
        expectedScanError: Int = -1
    ) {

        PreferencesUtil.setString(
            mActivityTestRule.activity,
            R.string.testImageNumberKey, imageNumber.toString()
        )

        Thread.sleep(2000)

        val floatingActionButton = onView(
            allOf(
                withId(R.id.fab), withContentDescription(R.string.start_test),
                childAtPosition(
                    childAtPosition(
                        withId(android.R.id.content),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        floatingActionButton.perform(click())

        if (expectedScanError == -1) {

            Thread.sleep(9000)

            onView(withText(name)).check(matches(isDisplayed()))

            if (name == pH) {
                onView(withId(R.id.text_unit)).check(matches(not(isDisplayed())))
            } else {
                if (expectedResultError > NO_ERROR) {
                    onView(withId(R.id.text_unit)).check(matches(not(isDisplayed())))
                } else {
                    onView(allOf(withId(R.id.text_unit), withText("mg/l")))
                        .check(matches(isDisplayed()))
                }
            }

            onView(withText(R.string.submitResult)).perform(click())

            Thread.sleep(1000)

            onView(
                allOf(
                    withId(R.id.text_title), withText("$name ($imageNumber)"),
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
            ).check(matches(withText("$name ($imageNumber)")))

            val textView = onView(
                allOf(
                    withId(R.id.textResultValue),
                    childAtPosition(
                        childAtPosition(
                            allOf(
                                withId(R.id.list_results),
                                withContentDescription("List of results")
                            ),
                            0
                        ),
                        1
                    ),
                    isDisplayed()
                )
            )

            SystemClock.sleep(3000)

            if (expectedResultError == NO_ERROR) {
                val floatValue = expectedResult.toFloat()
                textView.check(matches(checkResult(floatValue)))
            } else {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                textView.check(matches(withText(expectedResultError.toLocalString(context))))
            }

            textView.perform(click())

            SystemClock.sleep(2000)

            val imageView = onView(
                allOf(
                    withId(R.id.image), withContentDescription("Analyzed image"),
                    childAtPosition(
                        childAtPosition(
                            withId(android.R.id.content),
                            0
                        ),
                        2
                    ),
                    isDisplayed()
                )
            )
            imageView.check(matches(isDisplayed()))

            val appCompatButton = onView(
                allOf(
                    withId(R.id.imageModeButton), withText("View Full"),
                    childAtPosition(
                        childAtPosition(
                            withId(android.R.id.content),
                            0
                        ),
                        1
                    ),
                    isDisplayed()
                )
            )
            appCompatButton.perform(click())

            SystemClock.sleep(2000)

            val imageView2 = onView(
                allOf(
                    withId(R.id.image), withContentDescription("Analyzed image"),
                    childAtPosition(
                        childAtPosition(
                            withId(android.R.id.content),
                            0
                        ),
                        2
                    ),
                    isDisplayed()
                )
            )
            imageView2.check(matches(isDisplayed()))

            val appCompatButton2 = onView(
                allOf(
                    withId(R.id.imageModeButton), withText("View Extracts"),
                    childAtPosition(
                        childAtPosition(
                            withId(android.R.id.content),
                            0
                        ),
                        1
                    ),
                    isDisplayed()
                )
            )
            appCompatButton2.perform(click())

            val appCompatImageButton = onView(
                allOf(
                    withContentDescription("Navigate up"),
                    childAtPosition(
                        allOf(
                            withId(R.id.app_bar),
                            childAtPosition(
                                withClassName(Matchers.`is`("androidx.constraintlayout.widget.ConstraintLayout")),
                                0
                            )
                        ),
                        0
                    ),
                    isDisplayed()
                )
            )
            appCompatImageButton.perform(click())
        } else {

            Thread.sleep(9000)

            onView(withText(expectedScanError)).check(matches(isDisplayed()))
        }
    }

    companion object {

        @JvmStatic
        var initialized = false

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
                folder.listFiles()?.forEach {
                    it.delete()
                }
            }
            clearData(context)
        }

        @JvmStatic
        @BeforeClass
        fun initialize() {
            BuildConfig.TEST_RUNNING.set(true)
        }
    }
}