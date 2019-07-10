package io.ffem.lite.util

import android.widget.ListView

/**
 * Utility functions for ListView manipulation.
 */
object ListViewUtil {

    /**
     * Set the ListView height based on the number of rows and their height
     *
     *
     * Height for ListView needs to be set if used as a child of another ListView.
     * The child ListView will not display any scrollbars so the height needs to be
     * set so that all the rows are visible
     *
     * @param listView    the list view
     * @param extraHeight extra bottom padding
     */
    fun setListViewHeightBasedOnChildren(listView: ListView, extraHeight: Int) {
        val listAdapter = listView.adapter
            ?: // pre-condition
            return

        var totalHeight = 0
        for (i in 0 until listAdapter.count) {
            val listItem = listAdapter.getView(i, null, listView)
            listItem.measure(0, 0)
            totalHeight += listItem.measuredHeight
        }

        val params = listView.layoutParams
        params.height = extraHeight + totalHeight + listView.dividerHeight * (listAdapter.count - 1)
        listView.layoutParams = params
    }
}
