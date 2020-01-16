package io.ffem.lite.ui


import android.content.Context
import android.os.Environment
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
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
import org.hamcrest.Matchers.allOf
import org.hamcrest.core.IsInstanceOf
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File

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
        clearPreferences(mActivityTestRule)
        clearData(mActivityTestRule.activity)
    }

    @Test
    fun image000_Result_0_Point_5() {
        startTest(0, "0.5", -1)
    }

    @Test
    fun image001_Result_0() {
        startTest(1, "0.0", -1)
    }

    @Test
    fun image002_InvalidBarcode() {
        startTest(2, "", -1, R.string.invalid_barcode)
    }

    @Test
    fun image003_Tilted() {
        startTest(3, "", -1, R.string.correct_camera_tilt)
    }

    @Test
    fun image004_Tilted() {
        startTest(4, "", -1, R.string.try_moving_well_lit)
    }

    @Test
    fun image006_NoMatch() {
        startTest(6, "", R.string.no_match)
    }

    @Test
    fun image007_Result_Point_5() {
        startTest(7, "0.5")
    }

    @Test
    fun image008_Result_1_Point_5() {
        startTest(8, "1.5")
    }

    @Test
    fun image009_BadLight() {
        startTest(9, "0.5")
    }

    @Test
    fun image010_CalibrationError() {
        startTest(10, "", R.string.calibration_error)
    }

    @Test
    fun image011_Tilted() {
        startTest(11, "", -1, R.string.correct_camera_tilt)
    }

//    @Test
//    fun imageX_Waiting() {
//        startTest(500, "", -1, R.string.place_color_card)
//    }

    private fun startTest(
        imageNumber: Int,
        result: String,
        resultError: Int = -1,
        previewError: Int = -1
    ) {

        Thread.sleep(5000)

        val floatingActionButton = onView(
            allOf(
                withId(R.id.fab), withContentDescription("Start test"),
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

        val appCompatEditText = onView(
            allOf(
                withId(R.id.editExpectedValue),
                childAtPosition(
                    childAtPosition(
                        withId(android.R.id.custom),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        appCompatEditText.perform(replaceText(imageNumber.toString()), closeSoftKeyboard())

        if (TestUtil.isEmulator) {
            Thread.sleep(3000)
        }

        val appCompatButton = onView(
            allOf(withId(android.R.id.button1), withText("OK"), isDisplayed())
        )
        appCompatButton.perform(click())

        if (previewError == -1) {

            Thread.sleep(7000)

            onView(
                allOf(
                    withId(R.id.text_title), withText("Residual Chlorine ($imageNumber.0)"),
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
            ).check(matches(withText("Residual Chlorine ($imageNumber.0)")))

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

            Thread.sleep(7000)

            onView(withText(previewError)).check(matches(isDisplayed()))
        }
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
                folder.listFiles()?.forEach {
                    it.delete()
                }
            }
            clearData(context)
        }
    }
}