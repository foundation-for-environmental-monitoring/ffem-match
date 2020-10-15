package io.ffem.lite.external


import android.content.Context
import android.os.Environment
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.common.*
import io.ffem.lite.common.TestHelper.clearPreferences
import io.ffem.lite.common.TestHelper.clickLaunchButton
import io.ffem.lite.common.TestHelper.gotoSurveyForm
import io.ffem.lite.common.TestHelper.gotoSurveySelection
import io.ffem.lite.common.TestHelper.isDeviceInitialized
import io.ffem.lite.common.TestHelper.mDevice
import io.ffem.lite.common.TestHelper.nextSurveyPage
import io.ffem.lite.common.TestHelper.startSurveyApp
import io.ffem.lite.common.TestUtil.sleep
import io.ffem.lite.internal.TIME_DELAY
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.ErrorType.NO_ERROR
import io.ffem.lite.model.ErrorType.WRONG_CARD
import io.ffem.lite.model.toLocalString
import io.ffem.lite.ui.ResultListActivity
import io.ffem.lite.util.PreferencesUtil
import io.ffem.lite.util.toLocalString
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File

@LargeTest
@RequiresExternalApp
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SampleImageSurveyTest : BaseTest() {

    @get:Rule
    val mActivityTestRule = activityScenarioRule<ResultListActivity>()

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.CAMERA"
        )

    @Before
    override fun setUp() {
        if (!initialized) {
            clearPreferences()
            clearData()
            initialized = true
        }
    }

    @Test
    fun image_000_Chlorine_0_Point_5() {
        startTest(0)
    }

    @Test
    fun image_001_Chlorine_0() {
        startTest(1)
    }

    @Test
    fun image_002_InvalidBarcode() {
        startTest(2)
    }

    @Test
    fun image_003_InvalidBarcode() {
        startTest(3)
    }

    @Test
    fun image_004_pH_NoMatch() {
        startTest(4)
    }

    @Test
    fun image_005_Waiting() {
        startTest(5)
    }

    @Test
    fun image_006_Chlorine_NoMatch() {
        startTest(6)
    }

    @Test
    fun image_007_Chlorine_Point_5() {
        startTest(7)
    }

    @Test
    fun image_008_Chlorine_1_Point_5() {
        startTest(8)
    }

    @Test
    fun image_009_BadLight() {
        startTest(9)
    }

    @Test
    fun image_010_Chlorine_CalibrationError() {
        startTest(10)
    }

    @Test
    fun image_011_Tilted() {
        startTest(11)
    }

    @Test
    fun image_012_Waiting() {
        startTest(12)
    }

    @Test
    fun image_013_Waiting() {
        startTest(13)
    }

    @Test
    fun image_014_pH_6_Point_5() {
        startTest(14)
    }

    @Test
    fun image_015_Chlorine_4_Point_3() {
        startTest(15)
    }

    @Test
    fun image_016_Chlorine_CalibrationError() {
        startTest(16)
    }

    @Test
    fun image_017_BadLighting() {
        startTest(17)
    }

    @Test
    fun image_018_Waiting() {
        startTest(18)
    }

    @Test
    fun image_019_Chlorine_3_Point_0() {
        startTest(19)
    }

    @Test
    fun image_020_Waiting() {
        startTest(20)
    }

    @Test
    fun image_021_Waiting() {
        startTest(21)
    }

    @Test
    fun image_022_Waiting() {
        startTest(22)
    }

    @Test
    fun image_023_FluorideHighRange_NoMatch() {
        startTest(23)
    }
    @Test
    fun image_024_Fluoride_1_Point_0() {
        startTest(24)
    }

    @Test
    fun invalidTest() {
        validityTest(invalidTest, 1, WRONG_CARD)
    }

    @Test
    fun invalidCardTest() {
        validityTest(fluoride, 0, WRONG_CARD)
    }

    @Test
    fun imageX_Waiting() {
        startTest(500)
    }

    private fun validityTest(
        testDetails: TestDetails,
        imageNumber: Int,
        @Suppress("SameParameterValue") expectedResultError: ErrorType = NO_ERROR
    ) {

        PreferencesUtil.setString(
            context, R.string.testImageNumberKey, imageNumber.toString()
        )

        sleep(2000)

        startSurveyApp()
        gotoSurveySelection()
        gotoSurveyForm()

        nextSurveyPage(3, context.getString(testDetails.group))

        sleep(2000)
        clickLaunchButton(testDetails.buttonIndex)

        sleep(TIME_DELAY)

        onView(withText(testDetails.name.toLocalString())).check(matches(isDisplayed()))

        if (expectedResultError > NO_ERROR) {
            onView(withText(expectedResultError.toLocalString(context))).check(
                matches(isDisplayed())
            )
            onView(withText(R.string.close)).perform(click())
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

            if (!isDeviceInitialized()) {
                mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            }
        }
    }
}