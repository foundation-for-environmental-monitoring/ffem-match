package io.ffem.lite.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import io.ffem.lite.R
import io.ffem.lite.model.CalibrationInfo
import io.ffem.lite.widget.SwatchView
import java.text.DateFormat
import java.util.*

class CalibrationsAdapter internal constructor(
    private val clickListener: (Long) -> Unit,
    private val longClickListener: (Long) -> Boolean,
    private val calibrationList: List<CalibrationInfo?>?
) :
    RecyclerView.Adapter<CalibrationsAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.calibration_load_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val calibration = calibrationList!![position]!!.details
        holder.titleText.text = calibration.name
        holder.swatchView.setCalibrations(calibrationList[position]!!.calibrations)
        holder.descText.text = calibration.desc
        holder.dateText.text =
            DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(calibration.date))
        holder.layoutRow.setOnClickListener { clickListener(calibration.calibrationId) }
        holder.layoutRow.setOnLongClickListener { longClickListener(calibration.calibrationId) }
    }

    override fun getItemCount(): Int {
        return calibrationList!!.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.title_text)
        val swatchView: SwatchView = view.findViewById(R.id.swatch_view)
        val descText: TextView = view.findViewById(R.id.desc_text)
        val dateText: TextView = view.findViewById(R.id.date_text)
        val layoutRow: ConstraintLayout = view.findViewById(R.id.layout_row)
    }
}