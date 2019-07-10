package io.ffem.lite.preference

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.ui.AboutActivity
import io.ffem.lite.util.ListViewUtil

class OtherPreferenceFragment : PreferenceFragment() {

    private var list: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.pref_other)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.card_row, container, false)

        val aboutPreference = findPreference("about")
        if (aboutPreference != null) {
            aboutPreference.summary = App.getAppVersion()
            aboutPreference.setOnPreferenceClickListener { preference ->
                val intent = Intent(activity, AboutActivity::class.java)
                activity.startActivity(intent)
                true
            }
        }
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list = view.findViewById(android.R.id.list)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        ListViewUtil.setListViewHeightBasedOnChildren(list!!, 0)
    }
}
