package com.example.android.rainuponarrival;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.android.rainuponarrival.data.RainfallLocationUtil;
import com.example.android.rainuponarrival.data.RainfallLocationUtil.Station;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;

public class SelectStationFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    public static final String FROM_KEY = "fromKey";

    private static final String LOG_TAG = SelectStationFragment.class.getSimpleName();

    private class Line {
        public String code;
        public String name;

        private Line() {}

        public Line(String _code, String _name) {
            this.code = _code;
            this.name = _name;
        }
    }

    private ListPreference prefecturePreference;
    private ListPreference linePreference;
    private ListPreference stationPreference;

    private boolean mIsHome = false;
    private String mInitialStationCode = null;

    private String mSelectedPrefCode;
    private String mSelectedPrefName;

    private LinkedHashMap<String, Line> mLines;
    private Line mSelectedLine;

    private LinkedHashMap<String, Station> mStations;
    private Station mSelectedStation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_station_select);

        prefecturePreference = (ListPreference) findPreference(getString(R.string.pref_prefecture_key));
        linePreference =       (ListPreference) findPreference(getString(R.string.pref_line_key));
        stationPreference =    (ListPreference) findPreference(getString(R.string.pref_station_key));
        prefecturePreference.setOnPreferenceChangeListener(this);
        linePreference.setOnPreferenceChangeListener(this);
        stationPreference.setOnPreferenceChangeListener(this);

        if (savedInstanceState == null) {
            Bundle args = getArguments();
            if (args != null) {
                String fromKey = args.getString(FROM_KEY);
                if (fromKey != null && fromKey.equals(getString(R.string.pref_home_station_key))) {
                    mInitialStationCode = Utility.getHomeStationCode(getActivity());
                    mIsHome = true;
                } else {
                    mInitialStationCode = Utility.getOfficeStationCode(getActivity());
                }
            }
            initStation(mInitialStationCode);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mSelectedStation != null) {
            Log.i(LOG_TAG, "Station is changed. " + mInitialStationCode + " to " + mSelectedStation.code);
            RainfallLocationUtil.addStation(getActivity(), mSelectedStation);
            if (mInitialStationCode == null || !mSelectedStation.code.equals(mInitialStationCode)) {
                if (mIsHome) {
                    Utility.storeHomeStationCode(getActivity(), mSelectedStation.code);
                } else {
                    Utility.storeOfficeStationCode(getActivity(), mSelectedStation.code);
                }
            }
//            RainfallSyncAdapter.syncImmediately(this);
        }
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
//        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_prefecture_key)));
//        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_line_key)));
//        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_station_key)));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        Log.d(LOG_TAG, "onPreferenceChange " + preference.getKey());
        String stringValue = value.toString();

        ListPreference listPreference = (ListPreference) preference;
        int prefIndex = listPreference.findIndexOfValue(stringValue);
        String entry = "";
        if (prefIndex >= 0) {
            entry = (String) listPreference.getEntries()[prefIndex];
            preference.setSummary(entry);
        }

        if (preference.getKey().equals(getString(R.string.pref_prefecture_key))) {
            mSelectedPrefCode = stringValue;
            mSelectedPrefName = entry;

//            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
//            prefs.edit().putString(getString(R.string.pref_line_key), "").commit();
//            prefs.edit().putString(getString(R.string.pref_station_key), "").commit();
            clearLinePreference();
            clearStaionPreference();

            GetLinesTask task = new GetLinesTask();
            task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, mSelectedPrefCode, "");

        } else if (preference.getKey().equals(getString(R.string.pref_line_key))) {
//            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
//            prefs.edit().putString(getString(R.string.pref_station_key), "").commit();
            clearStaionPreference();

            String selectedLineCode = (String) value;
            mSelectedLine = mLines.get(selectedLineCode);

            GetStationsTask task = new GetStationsTask();
            task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, mSelectedLine.code, "");

        } else if (preference.getKey().equals(getString(R.string.pref_station_key))) {
            String selectedStationCode = (String) value;
            mSelectedStation = mStations.get(selectedStationCode);
        }
        return true;
    }

    private void initStation(String stationCode) {
        Station station = RainfallLocationUtil.getStation(getActivity(), mInitialStationCode);
        if (station != null) {
            mSelectedPrefCode = station.prefCode;
            mSelectedPrefName = station.prefName;
            prefecturePreference.setValue(mSelectedPrefCode);
            prefecturePreference.setSummary(station.prefName);

            linePreference.setSummary(station.lineName);
            mSelectedLine = new Line(station.lineCode, station.lineName);
            GetLinesTask getLineTask = new GetLinesTask();
            getLineTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, station.prefCode, station.lineCode);

            stationPreference.setSummary(station.name);
            mSelectedStation = station;
            GetStationsTask getStationTask = new GetStationsTask();
            getStationTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, station.lineCode, station.code);

        } else {
            prefecturePreference.setValue("");
            clearLinePreference();
            clearStaionPreference();
        }
    }

    private void clearLinePreference() {
        linePreference.setEnabled(false);
        linePreference.setValue("");
        linePreference.setEntryValues(null);
        linePreference.setEntries(null);
        linePreference.setSummary(getString(R.string.pref_line_summary));
    }

    private void clearStaionPreference() {
        stationPreference.setEnabled(false);
        stationPreference.setValue("");
        stationPreference.setEntryValues(null);
        stationPreference.setEntries(null);
        stationPreference.setSummary(getString(R.string.pref_station_summary));
    }

    /**
     * Attaches a listener so the summary is always updated with the preference value.
     * Also fires the listener once, to initialize the summary (so it shows up before the value
     * is changed.)
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        if (preference.getOnPreferenceChangeListener() == null)
            preference.setOnPreferenceChangeListener(this);

        String key = preference.getKey();
        String value = PreferenceManager.getDefaultSharedPreferences(preference.getContext())
                .getString(preference.getKey(), "");
        onPreferenceChange(preference, value);
    }

    private class GetLinesTask extends AsyncTask<String, Void, LinkedHashMap<String, Line>> {
        final String LOG_TAG = GetLinesTask.class.getSimpleName();
        private String mmInitialLineCode;

        @Override
        protected LinkedHashMap<String, Line> doInBackground(String... params) {
            String prefCode = params[0];
            String initialLineCode =null;
            if (params.length > 1)
                mmInitialLineCode = params[1];
            Log.i(LOG_TAG, "GetStationsTask lineCode:" + prefCode);
            Log.i(LOG_TAG, "GetStationsTask initialStationCode:" + initialLineCode);

            final String PREFECTURES_API_URL_STR = "http://www.ekidata.jp/api/p/";
            Uri uri = Uri.parse(PREFECTURES_API_URL_STR).buildUpon()
                    .appendPath(prefCode + ".json").build();

            String jsonStr = getEkiDataViaApi(uri.toString());
            if (jsonStr == null || jsonStr.length() == 0) {
                return null;
            }

            // // Prefecture
            // final String EKIP_PREF      = "pref";
            // final String EKIP_PREF_CODE = "code";
            // final String EKIP_PREF_NAME = "name";
            // Line
            final String EKI_LINE = "line";
            final String EKI_LINE_CODE = "line_cd";
            final String EKI_LINE_NAME = "line_name";

            try {
                JSONObject linesJson = new JSONObject(jsonStr);
                JSONArray lineArray = linesJson.getJSONArray(EKI_LINE);
                mLines = new LinkedHashMap<>(lineArray.length());
//                ArrayList<String> lineNames = new ArrayList<String>();
//                lineNames.add(getString(R.string.comment_select_line));
//                mLines.put("0", new Line("0", ""));
                for (int i = 0; i < lineArray.length(); i++) {
                    JSONObject lineJson = lineArray.getJSONObject(i);
                    String lineCode = lineJson.getString(EKI_LINE_CODE);
                    String lineName = lineJson.getString(EKI_LINE_NAME);
                    mLines.put(lineCode, new Line(lineCode, lineName));
//                    lineNames.add(lineName);
                }
                return mLines;

            } catch (JSONException e) {
                Log.w(LOG_TAG, "Error ", e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(LinkedHashMap<String, Line> lines) {
            super.onPostExecute(lines);

            if (lines == null)
                return;

            ListPreference linePreference = (ListPreference) findPreference(getString(R.string.pref_line_key));
            String[] entryValues = lines.keySet().toArray(new String[0]);
            String[] entories = new String[entryValues.length];
            for (int i = 0; i < entryValues.length; i++)
                entories[i] = ((Line) lines.get(entryValues[i])).name;
            linePreference.setEntries(entories);
            linePreference.setEntryValues(entryValues);
            if (mmInitialLineCode != null)
                linePreference.setValue(mmInitialLineCode);
            linePreference.setEnabled(true);
        }
    }

    private class GetStationsTask extends AsyncTask<String, Void, LinkedHashMap<String, Station>> {
        final String LOG_TAG = GetStationsTask.class.getSimpleName();
        private String mmInitialStationCode;

        @Override
        protected LinkedHashMap<String, Station> doInBackground(String... params) {
            String lineCode = params[0];
            String initialStationCode = null;
            if (params.length > 1)
                mmInitialStationCode = params[1];
            Log.i(LOG_TAG, "GetStationsTask lineCode:" + lineCode);
            Log.i(LOG_TAG, "GetStationsTask initialStationCode:" + initialStationCode);

            final String LINES_API_URL_STR = "http://www.ekidata.jp/api/l/";
            Uri uri = Uri.parse(LINES_API_URL_STR).buildUpon()
                    .appendPath(lineCode + ".json").build();

            String jsonStr = getEkiDataViaApi(uri.toString());
            if (jsonStr == null || jsonStr.length() == 0) {
                return null;
            }

//                // Line
//                final String EKI_LINE       = "line";
//                final String EKI_LINE_CODE  = "line_cd";
//                final String EKI_LINE_NAME  = "line_name";
            // Station
            final String EKI_STATION = "station_l";
            final String EKI_STATION_CODE = "station_cd";
            final String EKI_STATION_GROUP_CODE = "station_g_cd";
            final String EKI_STATION_NAME = "station_name";
            final String EKI_STATION_LON = "lon";
            final String EKI_STATION_LAT = "lat";

            try {
                JSONObject stationsJson = new JSONObject(jsonStr);
                JSONArray stationArray = stationsJson.getJSONArray(EKI_STATION);
                mStations = new LinkedHashMap<>(stationArray.length());
//                ArrayList<String> stationNames = new ArrayList<String>();
//                mStations.put("0", new Station("0", "", "", "", "", "", "", "", ""));
//                stationNames.add(getString(R.string.comment_select_station));
                for (int i = 0; i < stationArray.length(); i++) {
                    JSONObject stationJson = stationArray.getJSONObject(i);
                    String stationCode = stationJson.getString(EKI_STATION_CODE);
                    String stationGroupCode = stationJson.getString(EKI_STATION_GROUP_CODE);
                    String stationName = stationJson.getString(EKI_STATION_NAME);
                    String lat = stationJson.getString(EKI_STATION_LAT);
                    String lon = stationJson.getString(EKI_STATION_LON);
                    mStations.put(stationCode, new Station(stationCode, stationGroupCode,
                            mSelectedPrefCode, mSelectedLine.code,
                            stationName, mSelectedPrefName, mSelectedLine.name, lat, lon));
//                    stationNames.add(stationName);
                }
                return mStations;

            } catch (JSONException e) {
                Log.w(LOG_TAG, "Error ", e);
            }

            return null;
        }


        @Override
        protected void onPostExecute(LinkedHashMap<String, Station> stations) {
            super.onPostExecute(stations);

            if (stations == null)
                return;

            String[] entryValues = stations.keySet().toArray(new String[0]);
            String[] entories = new String[entryValues.length];
            for (int i = 0; i < entryValues.length; i++)
                entories[i] = ((Station) stations.get(entryValues[i])).name;
            stationPreference.setEntries(entories);
            stationPreference.setEntryValues(entryValues);
            if (mmInitialStationCode != null)
                stationPreference.setValue(mmInitialStationCode);
            stationPreference.setEnabled(true);
        }
    }

    private String getEkiDataViaApi(String uriStr) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(uriStr);

            // Create the request to ekidata, and open the connection
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // Read the input stream into a string
            InputStream is = connection.getInputStream();
            if (is == null) {
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(is));

            StringBuffer buffer = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("if(typeof")) {
                    continue;
                } else if (line.startsWith("xml.data")) {
                    line = line.replace("xml.data = ", "");
                }
                buffer.append(line);
            }

            return buffer.toString();

        } catch (IOException e) {
            Log.e(LOG_TAG, e.toString());

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return null;
    }
}
