package io.ffem.lite.preference;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import androidx.annotation.NonNull;
import io.ffem.lite.R;
import io.ffem.lite.app.App;
import io.ffem.lite.ui.AboutActivity;
import io.ffem.lite.util.ListViewUtil;

public class OtherPreferenceFragment extends PreferenceFragment {

    private ListView list;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_other);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.card_row, container, false);

        Preference aboutPreference = findPreference("about");
        if (aboutPreference != null) {
            aboutPreference.setSummary(App.Companion.getAppVersion());
            aboutPreference.setOnPreferenceClickListener(preference -> {
                final Intent intent = new Intent(getActivity(), AboutActivity.class);
                getActivity().startActivity(intent);
                return true;
            });
        }
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        list = view.findViewById(android.R.id.list);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ListViewUtil.setListViewHeightBasedOnChildren(list, 0);
    }
}
