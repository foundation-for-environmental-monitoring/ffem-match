package io.ffem.lite.ui


import android.content.Context
import android.os.Environment
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import io.ffem.lite.R
import io.ffem.lite.app.AppDatabase
import io.ffem.lite.common.TestHelper.clearPreferences
import io.ffem.lite.common.TestUtil
import io.ffem.lite.common.TestUtil.checkResult
import io.ffem.lite.common.TestUtil.childAtPosition
import io.ffem.lite.util.PreferencesUtil
import org.hamcrest.Matchers.allOf
import org.hamcrest.core.IsInstanceOf
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File

const val pH = "pH"
const val residualChlorine = "Residual Chlorine"
const val fluoride: String = "Fluoride"

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
    fun image000_Result_0_Point_5() {
        startTest(residualChlorine, 0, "0.5")
    }

    @Test
    fun image001_Result_0() {
        startTest(residualChlorine, 1, "0.0")
    }

    @Test
    fun image002_InvalidBarcode() {
        startTest(residualChlorine, 2, scanError = R.string.invalid_barcode)
    }

    @Test
    fun image003_Tilted() {
        startTest(fluoride, 3, scanError = R.string.invalid_barcode)
    }

    @Test
    fun image004_NoMatch() {
        startTest(pH, 4, resultError = R.string.no_match)
    }

    @Test
    fun image005_Waiting() {
        startTest(residualChlorine, 5, scanError = R.string.place_color_card)
    }

    @Test
    fun image006_NoMatch() {
        startTest(residualChlorine, 6, resultError = R.string.no_match)
    }

    @Test
    fun image007_Result_Point_5() {
        startTest(residualChlorine, 7, "0.5")
    }

    @Test
    fun image008_Result_1_Point_5() {
        startTest(residualChlorine, 8, "1.5")
    }

    @Test
    fun image009_BadLight() {
        startTest(residualChlorine, 9, scanError = R.string.place_color_card)
    }

    @Test
    fun image010_CalibrationError() {
        startTest(residualChlorine, 10, resultError = R.string.calibration_error)
    }

    @Test
    fun image011_Tilted() {
        startTest(residualChlorine, 11, scanError = R.string.correct_camera_tilt)
    }

    @Test
    fun image012_Waiting() {
        startTest(residualChlorine, 12, scanError = R.string.place_color_card)
    }

    @Test
    fun image013_Waiting() {
        startTest(residualChlorine, 13, scanError = R.string.place_color_card)
    }

    @Test
    fun image014_Result_6_Point_5() {
        startTest(pH, 14, "6.5")
    }

    @Test
    fun image015_Result_4_Point_3() {
        startTest(residualChlorine, 15, "0.43")
    }

    @Test
    fun image016_CalibrationError() {
        startTest(residualChlorine, 16, resultError = R.string.calibration_error)
    }

    @Test
    fun image017_BadLighting() {
        startTest(residualChlorine, 17, scanError = R.string.place_color_card)
    }

    @Test
    fun image018_Waiting() {
        startTest(residualChlorine, 18, scanError = R.string.place_color_card)
    }

    @Test
    fun image019_Result_3_Point_0() {
        startTest(residualChlorine, 19, "3.0")
    }

    @Test
    fun image020_Waiting() {
        startTest(residualChlorine, 20, scanError = R.string.place_color_card)
    }

    @Test
    fun image021_Waiting() {
        startTest(residualChlorine, 21, scanError = R.string.place_color_card)
    }

    @Test
    fun imageX_Waiting() {
        startTest(pH, 500, scanError = R.string.place_color_card)
    }

    private fun startTest(
        name: String,
        imageNumber: Int,
        result: String = "",
        resultError: Int = -1,
        scanError: Int = -1
    ) {

        Thread.sleep(5000)

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

        if (TestUtil.isEmulator) {
            Thread.sleep(3000)
        }

        PreferencesUtil.setString(
            mActivityTestRule.activity,
            R.string.expectedValueKey, imageNumber.toString()
        )

        if (scanError == -1) {

            Thread.sleep(10000)

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
                    withId(R.id.textLocalValue),
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

            if (resultError == -1) {
                val floatValue = result.toFloat()
                textView.check(matches(checkResult(floatValue)))
            } else {
                textView.check(matches(withText(resultError)))
            }

        } else {

            Thread.sleep(10000)

            onView(withText(scanError)).check(matches(isDisplayed()))
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
    }
}