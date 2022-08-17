package io.ffem.lite.internal


import android.content.Context
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
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
import io.ffem.lite.common.TestUtil.sleep
import io.ffem.lite.common.clearData
import io.ffem.lite.ui.ResultListActivity
import io.ffem.lite.util.PreferencesUtil
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.*
import org.junit.runner.RunWith
import java.io.File

const val SCAN_TIME_DELAY = 15000

@LargeTest
@RunWith(AndroidJUnit4::class)
class ResultListTest {

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
            TestHelper.clearPreferences()
            clearData()
            initialized = true
        }
    }

    @Test
    fun resultListTest() {
        PreferencesUtil.setString(
            ApplicationProvider.getApplicationContext(),
            R.string.testImageNumberKey, 0.toString()
        )

        val floatingActionButton = onView(
            allOf(
                withId(R.id.start_test_fab), withContentDescription(R.string.start_test),
                childAtPosition(
                    allOf(
                        withId(R.id.mainLayout),
                        childAtPosition(
                            withId(R.id.coordinator_lyt),
                            0
                        )
                    ),
                    5
                ),
                isDisplayed()
            )
        )
        floatingActionButton.perform(click())

        val materialButton2 = onView(
            allOf(
                withId(R.id.start_test_btn), withText(R.string.start),
                childAtPosition(
                    allOf(
                        withId(R.id.instruction_lyt),
                        childAtPosition(
                            withId(R.id.coordinator_lyt),
                            0
                        )
                    ),
                    6
                ),
                isDisplayed()
            )
        )
        materialButton2.perform(click())

        sleep(SCAN_TIME_DELAY)

        val materialButton3 = onView(
            allOf(
                withId(R.id.accept_btn), withText(R.string.continue_on),
                childAtPosition(
                    allOf(
                        withId(R.id.instruction_lyt),
                        childAtPosition(
                            withClassName(`is`("android.widget.FrameLayout")),
                            0
                        )
                    ),
                    4
                ),
                isDisplayed()
            )
        )
        materialButton3.perform(click())

        onView(withText(R.string.next)).perform(click())
        sleep(1000)

        val textInputEditText = onView(
            allOf(
                withId(R.id.source_desc_edit),
                isDisplayed()
            )
        )
        textInputEditText.perform(
            replaceText("Description"),
            closeSoftKeyboard()
        )

        val appCompatAutoCompleteTextView = onView(
            allOf(
                withId(R.id.source_select),
                isDisplayed()
            )
        )
        appCompatAutoCompleteTextView.perform(
            replaceText("Drinking water"),
            closeSoftKeyboard()
        )

        appCompatAutoCompleteTextView.perform(pressImeActionButton())

        sleep(1000)
        onView(withText(R.string.save)).perform(click())

        val resultName = context.getString(R.string.residual_chlorine) + " [0]"
        onView(
            allOf(
                withText(resultName),
                isDisplayed()
            )
        ).check(matches(withText(resultName)))

        sleep(3000)

        val linearLayout = onView(
            allOf(
                withId(R.id.layout),
                childAtPosition(
                    childAtPosition(
                        allOf(
                            withId(R.id.test_results_lst)
                        ),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        linearLayout.perform(click())

        sleep(2000)

        val textView3 = onView(
            allOf(
                withId(R.id.value_txt), withText(R.string.high_quantity),
                withParent(withParent(withId(R.id.result_lyt))),
                isDisplayed()
            )
        )
        textView3.check(matches(withText(R.string.high_quantity)))

        val imageView = onView(
            allOf(
                withId(R.id.result_img), withContentDescription(R.string.result_image),
                withParent(withParent(withId(R.id.result_lyt))),
                isDisplayed()
            )
        )
        imageView.check(matches(isDisplayed()))

        onView(withId(R.id.error_margin_text))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

        onView(
            allOf(
                withId(R.id.name_txt), withText(R.string.residual_chlorine),
                isDisplayed()
            )
        ).check(matches(withText(R.string.residual_chlorine)))

        Espresso.pressBack()

        sleep(3000)

        val linearLayout2 = onView(
            allOf(
                withId(R.id.layout),
                childAtPosition(
                    childAtPosition(
                        allOf(
                            withId(R.id.test_results_lst)
                        ),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        linearLayout2.perform(longClick())

        val materialButton5 = onView(
            allOf(
                withId(android.R.id.button2), withText(android.R.string.cancel),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.buttonPanel),
                        0
                    ),
                    2
                )
            )
        )
        materialButton5.perform(scrollTo(), click())

        val linearLayout3 = onView(
            allOf(
                withId(R.id.layout),
                childAtPosition(
                    childAtPosition(
                        allOf(
                            withId(R.id.test_results_lst)
                        ),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        linearLayout3.perform(longClick())

        val materialButton6 = onView(
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
        materialButton6.perform(scrollTo(), click())

        val textView6 = onView(
            allOf(
                withId(R.id.no_result_txt), withText(R.string.no_data),
                withParent(
                    allOf(
                        withId(R.id.mainLayout),
                        withParent(withId(R.id.coordinator_lyt))
                    )
                ),
                isDisplayed()
            )
        )
        textView6.check(matches(withText(R.string.no_data)))
    }

    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }

    companion object {

        @JvmStatic
        var initialized = false
        private lateinit var context: Context

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
            context = InstrumentationRegistry.getInstrumentation().targetContext
        }
    }
}
