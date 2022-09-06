package io.ffem.lite.ui

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.ffem.lite.camera.CameraFragment
import io.ffem.lite.model.*
import io.ffem.lite.preference.AppPreferences.useExternalSensor
import kotlin.math.max

/**
 * A [FragmentStateAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class TestPagerAdapter(
    val activity: TestActivity, var testInfo: TestInfo? = null
) : FragmentStateAdapter(activity) {

    var instructions: List<Instruction>? = ArrayList()
    lateinit var pageIndex: PageIndex

    override fun getItemCount(): Int {
        return if (testInfo != null && testInfo?.subtype == TestType.CARD) {
            pageIndex.totalPageCount
        } else {
            if (instructions == null) {
                1
            } else {
                max(1, instructions!!.size)
            }
        }
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            pageIndex.calibrateOptionPage -> {
                CalibrateOptionFragment()
            }
            pageIndex.instruction -> {
                CardInstructionFragment()
            }
            pageIndex.confirmationPage -> {
                ImageConfirmFragment()
            }
            pageIndex.calibrationPage -> {
                CalibrationFragment()
            }
            pageIndex.dilutionPage -> {
                SelectDilutionFragment()
            }
            pageIndex.startTimerPage -> {
                InstructionFragment.newInstance(
                    instructions!![position],
                    ButtonType.START_TIMER
                )
            }
            pageIndex.testPage -> {
                when (testInfo?.subtype) {
                    TestType.API -> {
                        RecommendationFragment(activity.intent)
                    }
                    TestType.CARD -> {
                        CameraFragment()
                    }
                    TestType.TITRATION -> {
                        TitrationFragment()
                    }
                    else -> {

                        when {
                            useExternalSensor(activity) -> {
                                SensorFragment()
                            }
                            else -> {
                                CuvetteBelowFragment()
                            }
                        }
                    }
                }
            }
            pageIndex.resultPage -> {
                if (testInfo?.subtype == TestType.CARD) {
                    if (activity.isCalibration) {
                        CalibrationResultFragment()
                    } else {
                        ResultFragment(true)
                    }
                } else {
                    ResultFragment(true)
                }
            }
            pageIndex.submitPage -> {
                FormSubmitFragment.newInstance()
            }
            pageIndex.listPage -> {
                TestListFragment()
            }
            else -> {
                InstructionFragment.newInstance(
                    instructions!![position],
                    ButtonType.NONE
                )
            }
        }
    }
}
