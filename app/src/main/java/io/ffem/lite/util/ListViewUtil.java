package io.ffem.lite.util;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * Utility functions for ListView manipulation.
 */
public final class ListViewUtil {

    private ListViewUtil() {
    }

    /**
     * Set the ListView height based on the number of rows and their height
     * <p/>
     * Height for ListView needs to be set if used as a child of another ListView.
     * The child ListView will not display any scrollbars so the height needs to be
     * set so that all the rows are visible
     *
     * @param listView    the list view
     * @param extraHeight extra bottom padding
     */
    @SuppressWarnings("SameParameterValue")
    public static void setListViewHeightBasedOnChildren(ListView listView, int extraHeight) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = extraHeight + totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }
}
