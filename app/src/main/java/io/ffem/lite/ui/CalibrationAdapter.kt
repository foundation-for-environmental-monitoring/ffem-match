package io.ffem.lite.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.ffem.lite.R
import io.ffem.lite.model.TestInfo
import io.ffem.lite.preference.AppPreferences.getShowDebugInfo
import io.ffem.lite.util.ColorUtil
import java.util.*

class CalibrationAdapter internal constructor(
    private val clickListener: (Int) -> Unit,
    private val testInfo: TestInfo
) :
    RecyclerView.Adapter<CalibrationAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calibration, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val result = testInfo.subTest()

        val format = "%.2f"
        holder.textValue.text = String.format(
            Locale.getDefault(), format,
            result.colors[position].value
        )

        if (position < result.colors.size) {
            val color = result.colors[position].color
            holder.buttonColor.background = ColorDrawable(result.colors[position].color)
            holder.textUnit.text = result.unit.toString()
            holder.layoutRow.setOnClickListener { clickListener(position) }

            //display additional information if we are in diagnostic mode
            if (getShowDebugInfo(holder.textValue.context)) {
                holder.textUnit.visibility = View.GONE
                if (color != Color.TRANSPARENT) {
                    holder.textRgb.text = String.format("r: %s", ColorUtil.getColorRgbString(color))
                    holder.textRgb.visibility = View.VISIBLE
                    val colorHsv = FloatArray(3)
                    Color.colorToHSV(color, colorHsv)
                    holder.textHsv.text = String.format(
                        Locale.getDefault(),
                        "h: %.0f  %.2f  %.2f", colorHsv[0], colorHsv[1], colorHsv[2]
                    )
                    holder.textHsv.visibility = View.VISIBLE
                    var distance = 0.0
                    if (position > 0) {
                        val previousColor = result.colors[position - 1].color
                        distance = ColorUtil.getColorDistance(previousColor, color)
                    }
                    holder.textBrightness.text = String.format(
                        Locale.getDefault(),
                        "d:%.2f  b: %d", distance, ColorUtil.getBrightness(color)
                    )
                    holder.textBrightness.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return testInfo.subTest().values.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val buttonColor: View = view.findViewById(R.id.swatch_view)
        val textValue: TextView = view.findViewById(R.id.value_txt)
        val textUnit: TextView = view.findViewById(R.id.unit_txt)
        val layoutRow: LinearLayout = view.findViewById(R.id.layout_row)
        val textRgb: TextView = view.findViewById(R.id.textRgb)
        val textHsv: TextView = view.findViewById(R.id.textHsv)
        val textBrightness: TextView = view.findViewById(R.id.textBrightness)
    }
}