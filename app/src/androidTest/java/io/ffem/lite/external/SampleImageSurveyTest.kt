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
import io.ffem.lite.common.TestHelper.clickExternalAppButton
import io.ffem.lite.common.TestHelper.gotoSurveyForm
import io.ffem.lite.common.TestHelper.gotoSurveySelection
import io.ffem.lite.common.TestHelper.isDeviceInitialized
import io.ffem.lite.common.TestHelper.mDevice
import io.ffem.lite.common.TestHelper.nextSurveyPage
import io.ffem.lite.common.TestHelper.startSurveyApp
import io.ffem.lite.common.TestUtil.sleep
import io.ffem.lite.internal.SCAN_TIME_DELAY
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.ErrorType.NO_ERROR
import io.ffem.lite.model.ErrorType.WRONG_CARD
import io.ffem.lite.model.toLocalString
import io.ffem.lite.ui.ResultListActivity
import io.ffem.lite.util.PreferencesUtil
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
        super.setUp()
        if (!initialized) {
            clearPreferences()
            clearData()
            initialized = true
        }
    }

    @Test
    fun invalidTest() {
        validityTest(invalidTest, 1, WRONG_CARD)
    }

    @Test
    fun invalidCardTest() {
        validityTest(fluoride, 0, WRONG_CARD)
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
        clickExternalAppButton(testDetails.name)

        sleep(2000)

        onView(withText(R.string.start)).perform(click())

        sleep(SCAN_TIME_DELAY)

        if (expectedResultError > NO_ERROR) {
            onView(withText(expectedResultError.toLocalString(context))).check(
                matches(isDisplayed())
            )
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