package io.ffem.lite.preference

import android.app.Fragment
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import io.ffem.lite.R
import io.ffem.lite.util.ListViewUtil

/**
 * A simple [Fragment] subclass.
 */
class TestingPreferenceFragment : PreferenceFragment() {

    private var list: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.pref_testing)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.card_row, container, false)
        setBackgroundColor(view)

        val testModeOnPreference = findPreference(getString(R.string.testModeOnKey))
        testModeOnPreference?.setOnPreferenceClickListener {
            setBackgroundColor(view)
            true
        }
        return view
    }

    private fun setBackgroundColor(view: View) {
        if (isTestMode()) {
            view.setBackgroundColor(Color.rgb(255, 165, 0))
        } else {
            view.setBackgroundColor(Color.rgb(255, 240, 220))
        }
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
