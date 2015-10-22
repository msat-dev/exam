package com.example.android.rainuponarrival;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.example.android.rainuponarrival.data.RainfallLocationUtil;
import com.example.android.rainuponarrival.data.RainfallLocationUtil.Station;
import com.example.android.rainuponarrival.data.WeatherContract;

//public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
public class DetailFragment extends Fragment {
    public static final String STATION_CODE = "stationCode";
    public static final String STATION = "station";
    private static final long DISPLAY_TARGET_PAST_TIME = 10 * 60 * 1000; // 10 minutes
    private static final String YAHOO_ZOOM_RADAR_ZOOM = "14";

    DetailAdapter mRainfallAdapter;

    private static final String LOG_TAG = DetailFragment.class.getSimpleName();
//    private static final int DETAIL_LOADER = 0;

    private static final String[] DETAIL_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_RAINFALL,
//            WeatherContract.LocationEntry.COLUMN_STATION_CODE,
//            WeatherContract.LocationEntry.COLUMN_NAME,
//            WeatherContract.LocationEntry.COLUMN_LAT,
//            WeatherContract.LocationEntry.COLUMN_LON
    };

    static final int COL_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_RAINFALL = 2;
//    static final int COL_STATION_CODE = 3;
//    static final int COL_NAME = 4;
//    static final int COL_LAT = 5;
//    static final int COL_LON = 6;

    private TextView mLocationView;
    private ListView mRainfallListView;

    private Station mStation;

    public static DetailFragment newInstance(int index) {
        DetailFragment f = new DetailFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt("index", index);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        mLocationView = (TextView) rootView.findViewById(R.id.detail_location_label);
        mRainfallListView = (ListView) rootView.findViewById(R.id.detail_rainfall_list);

        if (savedInstanceState == null) {
            Bundle args = getArguments();
            if (args != null) {
                String stationCode = args.getString(STATION_CODE);
                if (stationCode != null && stationCode.length() > 0) {
                    mStation = RainfallLocationUtil.getStation(getActivity(), stationCode);
                }
            }
        } else {
            mStation = (Station) savedInstanceState.getSerializable(STATION);
        }

        if (mStation != null) {
            mLocationView.setText(mStation.name);
            Uri startTimeUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                    mStation.code, System.currentTimeMillis() - DISPLAY_TARGET_PAST_TIME);
            Cursor c = getActivity().getContentResolver().query(
                    startTimeUri,
                    DETAIL_COLUMNS,
                    null,
                    null,
                    null);
            mRainfallAdapter = new DetailAdapter(getActivity(), c, 0);
            mRainfallListView.setAdapter(mRainfallAdapter);
        }

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATION, mStation);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_detail, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_map) {
            openYahooMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

//    @Override
//    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
//        Log.d(LOG_TAG, "onCreateLoader");
//        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
//        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocation(mStation.code);
//        return new CursorLoader(getActivity(),
//                weatherForLocationUri, // uri
//                DETAIL_COLUMNS, // projection
//                null, // selection
//                null, // selection args
//                sortOrder); // sort order
//    }
//
//    @Override
//    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
//        mRainfallAdapter.swapCursor(cursor);
//        // TODO 移動時間後をフォーカス
////        if (mPosition >= 0 && mPosition < mForecastAdapter.getCount()) {
////            if (BuildConfig.VERSION_CODE >= 11) {
////                mListView.smoothScrollByOffset(mPosition);
////            } else {
////                mListView.setSelection(mPosition);
////            }
////        }
//    }
//
//    @Override
//    public void onLoaderReset(Loader<Cursor> cursorLoader) {
//        mRainfallAdapter.swapCursor(null);
//    }

    private static final String YAHOO_ZOOM_RADAR_URL = "http://weather.yahoo.co.jp/weather/zoomradar/";
    private static final String YAHOO_ZOOM_RADAR_PARAM_LAT = "lat";
    private static final String YAHOO_ZOOM_RADAR_PARAM_LON = "lon";
    private static final String YAHOO_ZOOM_RADAR_PARAM_ZOOM = "z";
    private void openYahooMap() {
        Uri uri = Uri.parse(YAHOO_ZOOM_RADAR_URL).buildUpon()
                .appendQueryParameter(YAHOO_ZOOM_RADAR_PARAM_LAT, mStation.lat)
                .appendQueryParameter(YAHOO_ZOOM_RADAR_PARAM_LON, mStation.lon)
                .appendQueryParameter(YAHOO_ZOOM_RADAR_PARAM_ZOOM, YAHOO_ZOOM_RADAR_ZOOM)
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);

        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(LOG_TAG, "Couldn't call " + uri.toString() + ", no receiving apps installed!");
        }
    }
}
