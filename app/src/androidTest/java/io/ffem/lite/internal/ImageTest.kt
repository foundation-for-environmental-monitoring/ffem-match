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
import io.ffem.lite.common.TestUtil
import io.ffem.lite.common.TestUtil.checkMarginOfError
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
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ImageTest {

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
    fun image_000_Chlorine_2_Point_0() {
        startInternalTest(0)
    }

    @Test
    fun image_002_Iron_0_Point_5() {
        startInternalTest(2)
    }

    @Test
    fun image_003_Phosphate_1_Point_8() {
        startInternalTest(3)
    }

    @Test
    fun image_004_Iron_0_Point_5() {
        startInternalTest(4)
    }

    @Test
    fun image_005_Iron_0_Point_5() {
        startInternalTest(5)
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

            sleep(SCAN_TIME_DELAY)

            if (testData.expectedResultError != ErrorType.BAD_LIGHTING &&
                testData.expectedResultError != ErrorType.IMAGE_TILTED
            ) {
                onView(withText(R.string.continue_on)).perform(click())
            }

            onView(withText(testData.testDetails.name)).check(matches(isDisplayed()))

            if (testData.expectedResultError > NO_ERROR) {
                onView(withText(testData.expectedResultError.toLocalString(context))).check(
                    matches(isDisplayed())
                )
                onView(withText(R.string.close)).perform(click())
            } else {

                onView(withText(testData.testDetails.name)).check(
                    matches(
                        isDisplayed()
                    )
                )

                val resultTextView = onView(withId(R.id.result_txt))
                resultTextView.check(matches(checkResult(testData)))

                if (testData.testDetails == pH) {
                    onView(withId(R.id.unit_txt)).check(matches(not(isDisplayed())))
                } else {
                    onView(allOf(withId(R.id.unit_txt), withText("mg/l")))
                        .check(matches(isDisplayed()))
                }

                val marginOfErrorView = onView(withId(R.id.error_margin_txt))
                marginOfErrorView.check(matches(checkMarginOfError(testData)))

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

                onView(withText(R.string.next)).perform(click())
                TestUtil.sleep(1000)

                val textInputEditText = onView(
                    allOf(
                        withId(R.id.source_desc_edit),
                        isDisplayed()
                    )
                )
                textInputEditText.perform(
                    ViewActions.replaceText("Description"),
                    ViewActions.closeSoftKeyboard()
                )

                val appCompatAutoCompleteTextView = onView(
                    allOf(
                        withId(R.id.source_select),
                        isDisplayed()
                    )
                )
                appCompatAutoCompleteTextView.perform(
                    ViewActions.replaceText("Drinking water"),
                    ViewActions.closeSoftKeyboard()
                )

                appCompatAutoCompleteTextView.perform(ViewActions.pressImeActionButton())

                TestUtil.sleep(1000)
                onView(withText(R.string.save)).perform(click())
            }

            sleep(1000)

            onView(
                allOf(
                    withText("${context.getString(testData.testDetails.name)} (${imageNumber})"),
                    isDisplayed()
                )
            ).check(matches(withText("${context.getString(testData.testDetails.name)} (${imageNumber})")))

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
                textView.check(matches(checkResult(testData)))
            } else {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                textView.check(matches(withText(testData.expectedResultError.toLocalString(context))))
            }

            textView.perform(click())

            sleep(2000)

            onView(withId(R.id.resultScrollView))
                .perform(ViewActions.swipeUp())

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
            context = InstrumentationRegistry.getInstrumentation().targetContext
        }
    }
}