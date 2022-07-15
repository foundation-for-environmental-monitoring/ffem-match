package io.ffem.lite.ui

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import io.ffem.lite.common.TEST_INFO
import io.ffem.lite.databinding.ActivityCalibrationGraphBinding
import io.ffem.lite.model.ColorInfo
import io.ffem.lite.model.TestInfo

class CalibrationGraphActivity : BaseActivity() {

    private lateinit var b: ActivityCalibrationGraphBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityCalibrationGraphBinding.inflate(layoutInflater)
        val view = b.root
        setContentView(view)

        val testInfo: TestInfo = intent.getParcelableExtra(TEST_INFO)!!
        val calibrations = testInfo.subTest().colors

        val seriesRed = LineDataSet(getDataPoints(calibrations, Color.RED), "")
        seriesRed.color = Color.RED
        seriesRed.lineWidth = 2.5f
        seriesRed.setCircleColor(Color.RED)
        seriesRed.circleHoleColor = Color.BLACK
        seriesRed.valueTextColor = Color.BLACK
        seriesRed.valueTextSize = 18f
        b.graphRed.data = LineData(seriesRed)
        b.graphRed.setVisibleYRangeMaximum(255f, YAxis.AxisDependency.LEFT)
        b.graphRed.axisLeft.setDrawLabels(false)
        b.graphRed.axisLeft.setDrawAxisLine(false)
        b.graphRed.axisLeft.setDrawZeroLine(false)
        b.graphRed.axisRight.isEnabled = false
        b.graphRed.xAxis.setDrawAxisLine(false)
        b.graphRed.xAxis.setDrawGridLines(false)
        b.graphRed.description.isEnabled = false
        b.graphRed.legend.isEnabled = false

        val seriesGreen = LineDataSet(getDataPoints(calibrations, Color.GREEN), "")
        seriesGreen.color = Color.GREEN
        seriesGreen.lineWidth = 2.5f
        seriesGreen.setCircleColor(Color.GREEN)
        seriesGreen.circleHoleColor = Color.BLACK
        seriesGreen.valueTextColor = Color.BLACK
        seriesGreen.valueTextSize = 18f
        b.graphGreen.data = LineData(seriesGreen)
        b.graphGreen.setVisibleYRange(0f, 255f, YAxis.AxisDependency.LEFT)
        b.graphGreen.axisLeft.setDrawLabels(false)
        b.graphGreen.axisLeft.setDrawAxisLine(false)
        b.graphGreen.axisLeft.setDrawZeroLine(false)
        b.graphGreen.axisRight.isEnabled = false
        b.graphGreen.xAxis.isEnabled = false
        b.graphGreen.description.isEnabled = false
        b.graphGreen.legend.isEnabled = false

        val seriesBlue = LineDataSet(getDataPoints(calibrations, Color.BLUE), "")
        seriesBlue.color = Color.BLUE
        seriesBlue.lineWidth = 2.5f
        seriesBlue.setCircleColor(Color.BLUE)
        seriesBlue.circleHoleColor = Color.BLACK
        seriesBlue.valueTextColor = Color.BLACK
        seriesBlue.valueTextSize = 18f
        b.graphBlue.data = LineData(seriesBlue)
        b.graphBlue.setVisibleYRange(0f, 255f, YAxis.AxisDependency.LEFT)
        b.graphBlue.axisLeft.setDrawLabels(false)
        b.graphBlue.axisLeft.setDrawAxisLine(false)
        b.graphBlue.axisLeft.setDrawZeroLine(false)
        b.graphBlue.axisRight.isEnabled = false
        b.graphBlue.xAxis.isEnabled = false
        b.graphBlue.xAxis.setDrawAxisLine(false)
        b.graphBlue.xAxis.setDrawGridLines(false)
        b.graphBlue.description.isEnabled = false
        b.graphBlue.legend.isEnabled = false

        b.titleText.text = testInfo.name
    }

    private fun getDataPoints(calibrations: ArrayList<ColorInfo>, color: Int): ArrayList<Entry> {
        val lineEntries = ArrayList<Entry>()
        var value = 0
        for (i in calibrations.indices) {
            when (color) {
                Color.RED -> value = Color.red(calibrations[i].color)
                Color.GREEN -> value = Color.green(calibrations[i].color)
                Color.BLUE -> value = Color.blue(calibrations[i].color)
            }
            lineEntries.add(Entry(calibrations[i].value.toFloat(), value.toFloat()))
        }
        return lineEntries
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}