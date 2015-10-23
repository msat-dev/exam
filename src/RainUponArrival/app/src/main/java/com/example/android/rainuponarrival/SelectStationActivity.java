package com.example.android.rainuponarrival;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

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
import java.util.ArrayList;

public class SelectStationActivity extends ActionBarActivity {
    public static final String FROM_KEY = "fromKey";
    private static final String LOG_TAG = SelectStationActivity.class.getSimpleName();

    private class Line {
        public String code;
        public String name;

        private Line() {}

        public Line(String _code, String _name) {
            this.code = _code;
            this.name = _name;
        }
    }

    private Spinner mPrefsSpinner;
    private Spinner mLinesSpinner;
    private Spinner mStationsSpinner;

    private boolean mIsHome = false;
    private String mInitialStationCode = null;

    private String mSelectedPrefCode;
    private String mSelectedPrefName;

    private ArrayList<Line> mLines;
    private Line mSelectedLine;

    private ArrayList<Station> mStations;
    private Station mSelectedStation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_station);

        String fromKey = getIntent().getStringExtra(FROM_KEY);
        if (fromKey != null && fromKey.equals(getString(R.string.pref_home_station_key))) {
            mInitialStationCode  = Utility.getHomeStationCode(this);
            mIsHome = true;
        } else {
            mInitialStationCode = Utility.getOfficeStationCode(this);
        }

        mPrefsSpinner = (Spinner) findViewById(R.id.prefs_spinner);
        ArrayAdapter prefAdapter = ArrayAdapter.createFromResource(
                this, R.array.prefectures, android.R.layout.simple_spinner_item);
        prefAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPrefsSpinner.setAdapter(prefAdapter);

        mLinesSpinner = (Spinner) findViewById(R.id.lines_spinner);
        mStationsSpinner = (Spinner) findViewById(R.id.stations_spinner);
        setStation(mInitialStationCode);

        mPrefsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                int selectedPosition = ((Spinner) adapterView).getSelectedItemPosition();
                Log.i(LOG_TAG, "selected position:" + selectedPosition);
                mSelectedPrefCode = Integer.toString(selectedPosition);
                if (selectedPosition == 0) {
                    mSelectedPrefName = "";
                    mLinesSpinner.setSelection(0);
                    mLinesSpinner.setEnabled(false);
                    mStationsSpinner.setSelection(0);
                    mStationsSpinner.setEnabled(false);
                    return;
                }
                mSelectedPrefName = (String) ((Spinner) adapterView).getSelectedItem();
                resetLine(mSelectedPrefCode, null);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSelectedStation != null) {
            Log.i(LOG_TAG, "Station is changed. " + mInitialStationCode + " to " + mSelectedStation.code);
            RainfallLocationUtil.addStation(this, mSelectedStation);
            if (mInitialStationCode == null || !mSelectedStation.code.equals(mInitialStationCode)) {
                if (mIsHome) {
                    Utility.storeHomeStationCode(this, mSelectedStation.code);
                } else {
                    Utility.storeOfficeStationCode(this, mSelectedStation.code);
                }
            }
//            RainfallSyncAdapter.syncImmediately(this);
        }
    }

    private void setStation(String stationCode) {
        Station station = RainfallLocationUtil.getStation(this, stationCode);
        if (station == null)
            return;

        Log.i(LOG_TAG, "initial station:" + station);
        mSelectedPrefCode = station.prefCode;
        mSelectedPrefName =  (String) mPrefsSpinner.getSelectedItem();
        mSelectedLine = new Line(station.lineCode, station.lineName);
        mSelectedStation = station;

        mPrefsSpinner.setSelection(Integer.parseInt(station.prefCode), false);
        resetLine(station.prefCode, station.lineCode);
        resetStation(station.lineCode, station.code);
    }

    private void resetLine(String prefCode, String initialLineCode) {
//        mLinesSpinner.setAdapter(null);
//        mLinesSpinner.setEnabled(false);
//        mStationsSpinner.setAdapter(null);
//        mStationsSpinner.setEnabled(false);
        GetLinesTask task = new GetLinesTask();
        task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, prefCode, initialLineCode);
    }

    private void resetStation(String lineCode, String initialStationCode) {
//        mStationsSpinner.setAdapter(null);
//        mStationsSpinner.setEnabled(false);
        GetStationsTask task = new GetStationsTask();
        task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, lineCode, initialStationCode);
    }

//    /**
//     * Helper method to handle insertion of a new location in the weather database.
//     *
//     * @param station    Station Class
//     */
//    private void addStation(Station station) {
//        // First, check if the location with this city name exists in the db
//        Cursor c = getContentResolver().query(
//                WeatherContract.LocationEntry.CONTENT_URI,
//                new String[]{WeatherContract.LocationEntry._ID},
//                WeatherContract.LocationEntry.COLUMN_STATION_CODE + " = ?",
//                new String[]{station.code},
//                null);
//
//        if (c == null)
//            return;
//        if (!c.moveToFirst()) {
//            // Now that the content provider is set up, inserting rows of data is pretty simple.
//            // First create a ContentValues object to hold the data you want to insert.
//            ContentValues locationValues = new ContentValues();
//
//            // Then add the data, along with the corresponding name of the data type,
//            // so the content provider knows what kind of value is being inserted.
//            locationValues.put(WeatherContract.LocationEntry.COLUMN_STATION_CODE, station.code);
//            locationValues.put(WeatherContract.LocationEntry.COLUMN_NAME, station.name);
//            locationValues.put(WeatherContract.LocationEntry.COLUMN_LAT, station.lat);
//            locationValues.put(WeatherContract.LocationEntry.COLUMN_LON, station.lon);
//            locationValues.put(WeatherContract.LocationEntry.COLUMN_PREF_CODE, mSelectedPrefCode);
//            locationValues.put(WeatherContract.LocationEntry.COLUMN_LINE_CODE, mSelectedLine.code);
//
//            // Finally, insert location data into the database.
//            Uri insertedUri = getContentResolver().insert(
//                    WeatherContract.LocationEntry.CONTENT_URI,
//                    locationValues
//            );
//        }
//        c.close();
//    }

    private class GetLinesTask extends AsyncTask<String, Void, ArrayList<String>> {
        final String LOG_TAG = GetLinesTask.class.getSimpleName();
        private int mmInitialPosition = 0;

        @Override
        protected ArrayList<String> doInBackground(String... params) {
            String prefCode = params[0];
            String initialLineCode =null;
            if (params.length > 1)
                initialLineCode = params[1];
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
                mLines = new ArrayList<Line>(lineArray.length());
                ArrayList<String> lineNames = new ArrayList<String>();
                lineNames.add(getString(R.string.comment_select_line));
                mLines.add(new Line("", ""));
                for (int i = 0; i < lineArray.length(); i++) {
                    JSONObject lineJson = lineArray.getJSONObject(i);
                    String lineCode = lineJson.getString(EKI_LINE_CODE);
                    String lineName = lineJson.getString(EKI_LINE_NAME);
                    if (initialLineCode != null) {
                        if (lineCode.equals(initialLineCode)) {
                            mmInitialPosition = lineNames.size();
                            Log.i("GetLinesTask", "initialPostion:" + mmInitialPosition);
                        }
                    }
                    mLines.add(new Line(lineCode, lineName));
                    lineNames.add(lineName);
                }
                return lineNames;

            } catch (JSONException e) {
                Log.w(LOG_TAG, "Error ", e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<String> lineNames) {
            super.onPostExecute(lineNames);
            if (lineNames == null)
                return;

            ArrayAdapter<String> linesAdapter =
                    new ArrayAdapter<String>(SelectStationActivity.this, android.R.layout.simple_spinner_item);
            linesAdapter.addAll(lineNames);
            linesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mLinesSpinner.setAdapter(linesAdapter);
            mLinesSpinner.setSelection(mmInitialPosition, false);
            mLinesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    int selectedPosition = ((Spinner) adapterView).getSelectedItemPosition();
                    if (selectedPosition == 0) {
                        mStationsSpinner.setSelection(0);
                        mStationsSpinner.setEnabled(false);
                        return;
                    }
                    mSelectedLine = mLines.get(selectedPosition);
                    resetStation(mSelectedLine.code, null);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
            mLinesSpinner.setEnabled(true);
        }
    }

    private class GetStationsTask extends AsyncTask<String, Void, ArrayList<String>> {
        final String LOG_TAG = GetStationsTask.class.getSimpleName();
        private int mmInitialPosition = 0;

        @Override
        protected ArrayList<String> doInBackground(String... params) {
            String lineCode = params[0];
            String initialStationCode = null;
            if (params.length > 1) {
                initialStationCode = params[1];
            }
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
                mStations = new ArrayList<Station>(stationArray.length());
                ArrayList<String> stationNames = new ArrayList<String>();
                mStations.add(new Station("", "", "", "", "", "", "", "", ""));
                stationNames.add(getString(R.string.comment_select_station));
                for (int i = 0; i < stationArray.length(); i++) {
                    JSONObject stationJson = stationArray.getJSONObject(i);
                    String stationCode = stationJson.getString(EKI_STATION_CODE);
                    String stationGroupCode = stationJson.getString(EKI_STATION_GROUP_CODE);
                    String stationName = stationJson.getString(EKI_STATION_NAME);
                    String lat = stationJson.getString(EKI_STATION_LAT);
                    String lon = stationJson.getString(EKI_STATION_LON);
                    if (initialStationCode != null) {
                        if (initialStationCode.equals(stationCode)) {
                            mmInitialPosition = stationNames.size();
                            Log.i("GetStationsTask", "initialPostion:" + mmInitialPosition);
                        }
                    }
                    mStations.add(new Station(stationCode, stationGroupCode, mSelectedPrefCode, mSelectedLine.code,
                            stationName, mSelectedPrefName, mSelectedLine.name, lat, lon));
                    stationNames.add(stationName);
                }
                return stationNames;

            } catch (JSONException e) {
                Log.w(LOG_TAG, "Error ", e);
            }

            return null;
        }


        @Override
        protected void onPostExecute(ArrayList<String> stationNames) {
            super.onPostExecute(stationNames);
            if (stationNames == null)
                return;

            ArrayAdapter<String> stationsAdapter =
                    new ArrayAdapter<String>(SelectStationActivity.this, android.R.layout.simple_spinner_item);
            stationsAdapter.addAll(stationNames);
            stationsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mStationsSpinner.setAdapter(stationsAdapter);
            mStationsSpinner.setSelection(mmInitialPosition, false);
            mStationsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    int selectedPosition = ((Spinner) adapterView).getSelectedItemPosition();
                    if (selectedPosition == 0)
                        return;
                    mSelectedStation = mStations.get(selectedPosition);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
            mStationsSpinner.setEnabled(true);
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
