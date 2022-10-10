package io.ffem.lite.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import io.ffem.lite.R
import io.ffem.lite.common.Constants.CUSTOMER_ID
import io.ffem.lite.data.CalibrationDatabase
import io.ffem.lite.data.DataHelper.getParametersFromTheCloud
import io.ffem.lite.databinding.FragmentTestListBinding
import io.ffem.lite.model.TestInfo
import io.ffem.lite.model.TestType
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.util.PreferencesUtil
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter
import kotlinx.coroutines.*
import timber.log.Timber


class TestListFragment : BaseFragment() {
    private var _binding: FragmentTestListBinding? = null
    private val b get() = _binding!!
    private val mainScope = MainScope()
    private var itemClicked: Boolean = false

    override fun onResume() {
        super.onResume()
        itemClicked = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTestListBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(b.testsLst) {
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
        }

        b.sampleTypeTab.getTabAt(0)?.tag = "compost"
        b.sampleTypeTab.getTabAt(1)?.tag = "soil"
        b.sampleTypeTab.getTabAt(2)?.tag = "water"

        val lastTabSelected = AppPreferences.getLastSelectedTestsTab(requireContext())
        if (b.sampleTypeTab.getTabAt(0)?.tag == lastTabSelected) {
            b.sampleTypeTab.getTabAt(0)?.select()
        } else if (b.sampleTypeTab.getTabAt(1)?.tag == lastTabSelected) {
            b.sampleTypeTab.getTabAt(1)?.select()
        } else if (b.sampleTypeTab.getTabAt(2)?.tag == lastTabSelected) {
            b.sampleTypeTab.getTabAt(2)?.select()
        }

        b.sampleTypeTab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                b.sampleTypeTab.getTabAt(b.sampleTypeTab.selectedTabPosition)?.let {
                    AppPreferences.setLastSelectedTestsTab(
                        it.tag as String,
                        requireContext()
                    )
                }
                setAdapter()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        setAdapter()
    }

    private fun setAdapter() {
        var compostList: ArrayList<TestInfo> = ArrayList()
        var waterList: ArrayList<TestInfo> = ArrayList()
        var soilList: ArrayList<TestInfo> = ArrayList()
        runBlocking {
            launch {
                val testType = AppPreferences.getTestType().toString().lowercase()
                try {
                    compostList = getParametersFromTheCloud(CUSTOMER_ID, "compost_$testType")
                    waterList = getParametersFromTheCloud(CUSTOMER_ID, "water_$testType")
                    soilList = getParametersFromTheCloud(CUSTOMER_ID, "soil_$testType")
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.check_internet),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        if (compostList.isEmpty() && soilList.isEmpty() ||
            soilList.isEmpty() && waterList.isEmpty() ||
            compostList.isEmpty() && waterList.isEmpty()
        ) {
            b.sampleTypeTab.visibility = GONE
        }

        if (compostList.isNotEmpty()) {
            for (i in compostList.indices.reversed()) {
                if (compostList[i].subtype == TestType.API) {
                    compostList.removeAt(i)
                }
            }

            compostList.sortWith { object1: TestInfo, object2: TestInfo ->
                object1.name!!.compareTo(object2.name!!, ignoreCase = true)
            }
        } else {
            if (b.sampleTypeTab.getTabAt(0)?.tag == "compost") {
                b.sampleTypeTab.getTabAt(0)?.let { b.sampleTypeTab.removeTab(it) }
            }
        }

        if (soilList.isNotEmpty()) {
            for (i in soilList.indices.reversed()) {
                if (soilList[i].subtype == TestType.API) {
                    soilList.removeAt(i)
                }
            }

            soilList.sortWith { object1: TestInfo, object2: TestInfo ->
                object1.name!!.compareTo(object2.name!!, ignoreCase = true)
            }
        } else {
            b.sampleTypeTab.getTabAt(1)?.let { b.sampleTypeTab.removeTab(it) }
        }

        if (waterList.isNotEmpty()) {
            for (i in waterList.indices.reversed()) {
                if (waterList[i].subtype == TestType.API) {
                    waterList.removeAt(i)
                }
            }

            waterList.sortWith { object1: TestInfo, object2: TestInfo ->
                object1.name!!.compareTo(object2.name!!, ignoreCase = true)
            }
        } else {
            b.sampleTypeTab.getTabAt(2)?.let { b.sampleTypeTab.removeTab(it) }
        }

        val lastTabSelected = AppPreferences.getLastSelectedTestsTab(requireContext())
        if (b.sampleTypeTab.getTabAt(0)?.tag == lastTabSelected) {
            b.sampleTypeTab.getTabAt(0)?.select()
        } else if (b.sampleTypeTab.getTabAt(1)?.tag == lastTabSelected) {
            b.sampleTypeTab.getTabAt(1)?.select()
        } else if (b.sampleTypeTab.getTabAt(2)?.tag == lastTabSelected) {
            b.sampleTypeTab.getTabAt(2)?.select()
        }

        val tests = when (b.sampleTypeTab.getTabAt(b.sampleTypeTab.selectedTabPosition)?.tag) {
            "compost" -> {
                compostList
            }
            "soil" -> {
                soilList
            }
            else -> {
                waterList
            }
        }

        for (i in tests.indices.reversed()) {
            if (tests[i].subtype == TestType.API) {
                tests.removeAt(i)
            }
        }

        tests.sortWith { object1: TestInfo, object2: TestInfo ->
            object1.name!!.compareTo(object2.name!!, ignoreCase = true)
        }

        val db: CalibrationDatabase = CalibrationDatabase.getDatabase(requireContext())
        val calibratedList = ArrayList<TestInfo>()
        try {
            val dao = db.calibrationDao()
            for (i in tests.indices) {
                val calibrationInfo = dao.getCalibrations(tests[i].uuid)
                if (calibrationInfo != null) {
                    if (calibrationInfo.calibrations.isNotEmpty()) {
                        calibratedList.add(tests[i])
                        continue
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            db.close()
        }

        val sectionAdapter = SectionedRecyclerViewAdapter()
        val testInfoSection = TestInfoSection(
            getString(R.string.all),
            tests,
            this@TestListFragment::onItemRootViewClicked
        )

        if (calibratedList.size > 0 && calibratedList.size < tests.size * 0.8) {
            sectionAdapter.addSection(
                TestInfoSection(
                    getString(R.string.recent),
                    calibratedList,
                    this@TestListFragment::onItemRootViewClicked
                )
            )
        } else {
            testInfoSection.setHasHeader(false)
        }

        sectionAdapter.addSection(testInfoSection)

        b.testsLst.layoutManager = LinearLayoutManager(context)
        b.testsLst.adapter = sectionAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }

    private fun onItemRootViewClicked(section: TestInfoSection, position: Int) {
        if (itemClicked) return
        itemClicked = true
        val testInfo = section.getItemAt(position)
        PreferencesUtil.setString(requireContext(), "lastSelectedTestId", testInfo.uuid)
        PreferencesUtil.setString(requireContext(), "lastSelectedTestName", testInfo.name)

        mainScope.launch {
            delay(350)
            (activity as TestActivity).onTestSelected(testInfo, false)
        }
    }
}