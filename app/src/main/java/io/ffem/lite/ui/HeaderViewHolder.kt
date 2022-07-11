package io.ffem.lite.ui

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.ffem.lite.R

internal class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val titleText: TextView = view.findViewById(R.id.title_txt)
}