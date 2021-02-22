package io.ffem.lite.util

import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import io.ffem.lite.R

object WidgetUtil {
    fun CardView.setStatusColor(completed: Boolean, required: Boolean) {
        foreground = if (completed) {
            ResourcesCompat.getDrawable(resources, R.drawable.status_completed, null)
        } else {
            if (required) {
                ResourcesCompat.getDrawable(resources, R.drawable.status_incomplete_required, null)
            } else {
                ResourcesCompat.getDrawable(resources, R.drawable.status_incomplete, null)
            }
        }
    }
}
