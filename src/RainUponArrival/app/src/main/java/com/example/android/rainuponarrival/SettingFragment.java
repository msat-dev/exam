package com.example.android.rainuponarrival;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);
    }

//    @Override
//    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
//        String key = preference.getKey();
//        if (key.equals(getString(R.string.pref_home_station_key))
//                || key.equals(getString(R.string.pref_office_station_key))) {
//            Intent intent = new Intent(getActivity(), SelectStationActivity.class);
//            intent.putExtra(SelectStationActivity.FROM_KEY, key);
//            startActivity(intent);
//            return true;
//        }
//        return super.onPreferenceTreeClick(preferenceScreen, preference);
//    }

    @Override
    public void onStart() {
        super.onStart();
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_home_station_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_office_station_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_travel_time_key)));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String stringValue = value.toString();

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else {
            // For other preferences, set the summary to the value's simple string representation.
            preference.setSummary(stringValue);
        }
//        RainfallSyncAdapter.syncImmediately(getActivity());
        return true;
    }

    /**
     * Attaches a listener so the summary is always updated with the preference value.
     * Also fires the listener once, to initialize the summary (so it shows up before the value
     * is changed.)
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        // Trigger the listener immediately with the preference's
        // current value.
        onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }
}
