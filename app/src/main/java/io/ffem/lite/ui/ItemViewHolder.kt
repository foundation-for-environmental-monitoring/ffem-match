package io.ffem.lite.ui

import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import io.ffem.lite.R

internal class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val rootView: View = view
    val subtitleView: TextView = view.findViewById(R.id.subtitle_text)
    val subtitle2View: TextView = view.findViewById(R.id.subtitle2_text)
    val parameterIdView: TextView = view.findViewById(R.id.parameter_id_text)
    val formulaView: TextView = view.findViewById(R.id.formula_text)
    val contentView: TextView = view.findViewById(R.id.title_text)
    val layoutRow: ConstraintLayout = view.findViewById(R.id.layout_row)
}