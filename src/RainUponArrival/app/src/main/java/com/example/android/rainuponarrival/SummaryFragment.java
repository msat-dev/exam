package com.example.android.rainuponarrival;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.rainuponarrival.data.WeatherContract;
import com.example.android.rainuponarrival.sync.RainfallSyncAdapter;

public class SummaryFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = SummaryFragment.class.getSimpleName();

    private static final int SUMMARY_LOADER = 0;

    private static final String[] SUMMARY_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_RAINFALL,
            WeatherContract.LocationEntry.COLUMN_STATION_CODE,
            WeatherContract.LocationEntry.COLUMN_NAME,
            WeatherContract.LocationEntry.COLUMN_LAT,
            WeatherContract.LocationEntry.COLUMN_LON
    };

    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_RAINFALL = 2;
    static final int COL_STATION_CODE = 3;
    static final int COL_NAME = 4;
    static final int COL_LAT = 5;
    static final int COL_LON = 6;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        public void onSummaryClicked(String stationCode);
    }

    private TextView  mDepartureLocationView;
    private TextView  mDepartureTimeView;
    private TextView  mDepartureTimeLagView;
    private TextView  mDepartureRainfallView;
    private ImageView mDepartureIconView;
    private TextView  mDestinationLocationView;
    private TextView  mDestinationTimeView;
    private TextView  mDestinationTimeLagView;
    private TextView  mDestinationRainfallView;
    private ImageView mDestinationIconView;
//    private ListView mSummaryListView;

//    private RainfallAdapter mForecastAdapter;
    private int mPosition = -1;

    private String mDepartureStationCode;
    private String mDestinationStationCode;

    public SummaryFragment() {}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(SUMMARY_LOADER, savedInstanceState, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View rootView = inflater.inflate(R.layout.fragment_summary, container, false);

        View departureLayout   = rootView.findViewById(R.id.departure_layout);
        mDepartureLocationView = (TextView)  departureLayout.findViewById(R.id.summary_location);
        mDepartureTimeView     = (TextView)  departureLayout.findViewById(R.id.summary_time);
        mDepartureTimeLagView  = (TextView)  departureLayout.findViewById(R.id.summary_time_lag);
        mDepartureRainfallView = (TextView)  departureLayout.findViewById(R.id.summary_rainfall);
        mDepartureIconView     = (ImageView) departureLayout.findViewById(R.id.summary_icon);

        View destinationLayout   = rootView.findViewById(R.id.destination_layout);
        mDestinationLocationView = (TextView)  destinationLayout.findViewById(R.id.summary_location);
        mDestinationTimeView     = (TextView)  destinationLayout.findViewById(R.id.summary_time);
        mDestinationTimeLagView  = (TextView)  destinationLayout.findViewById(R.id.summary_time_lag);
        mDestinationRainfallView = (TextView)  destinationLayout.findViewById(R.id.summary_rainfall);
        mDestinationIconView     = (ImageView) destinationLayout.findViewById(R.id.summary_icon);

        mDepartureStationCode = Utility.getDepartureStationCode(getActivity());
        mDestinationStationCode = Utility.getDestinationStationCode(getActivity());
        departureLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ((Callback) getActivity()).onSummaryClicked(mDepartureStationCode);
                return false;
            }
        });
        destinationLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ((Callback) getActivity()).onSummaryClicked(mDestinationStationCode);
                return false;
            }
        });
//        mSummaryListView = (ListView) rootView.findViewById(R.id.summary_list);
//        mSummaryListView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
//                if (position == 0) {
//                    ((Callback) getActivity()).onItemSelected();
//                }
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> adapterView) {
//            }
//        });

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.d(LOG_TAG, "onCreateLoader");
//        Uri departureUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
//                mDepartureStationCode, System.currentTimeMillis());
//        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
//        return new CursorLoader(getActivity(),
//                departureUri, // uri
//                SUMMARY_COLUMNS, // projection
//                null, // selection
//                null, // selection args
//                sortOrder); // sort order
        return new CursorLoader(getActivity(),
                WeatherContract.WeatherEntry.CONTENT_URI, // uri
                SUMMARY_COLUMNS, // projection
                null, // selection
                null, // selection args
                null); // sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.i(LOG_TAG, "onLoadFinished");
        mDepartureStationCode = Utility.getDepartureStationCode(getActivity());
        mDestinationStationCode = Utility.getDestinationStationCode(getActivity());

//        if (cursor != null && cursor.moveToFirst()) {
//            Log.i(LOG_TAG, "loader cursor date:" + cursor.getLong(COL_WEATHER_DATE));
//        setDepartureViewValues(cursor);
//        cursor.close();
//        }
        setDepartureViewValues();
        setDestinationViewValues();

//        Calendar cal = Calendar.getInstance();
//        if (cal.get(Calendar.HOUR_OF_DAY) >= 12) {
//
//        }

//        mForecastAdapter.swapCursor(cursor);
//        if (mPosition >= 0 && mPosition < mForecastAdapter.getCount()) {
//            if (BuildConfig.VERSION_CODE >= 11) {
//                mListView.smoothScrollByOffset(mPosition);
//            } else {
//                mListView.setSelection(mPosition);
//            }
//        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
//        mForecastAdapter.swapCursor(null);
    }

    public void onSettingChanged() {
        RainfallSyncAdapter.syncImmediately(getActivity());
        getLoaderManager().restartLoader(SUMMARY_LOADER, null, this);
    }

    private void setDepartureViewValues(Cursor c) {
        if (c == null)
            return;
        if (c.moveToFirst()) {
            Log.i(LOG_TAG, "departure count:" + c.getCount());
            mDepartureLocationView.setText(c.getString(COL_NAME));
            long time = c.getLong(COL_WEATHER_DATE);
            mDepartureTimeView.setText(Utility.formatTimeForDisplay(time).toString());
            long timeLag = time - System.currentTimeMillis();
            mDepartureTimeLagView.setText(Utility.formatTimeLag(getActivity(), timeLag));
            mDepartureRainfallView.setText(Utility.formatRainfall(getActivity(), c.getDouble(COL_RAINFALL)));
            mDepartureIconView.setImageResource(Utility.getArtResouceId(c.getDouble(COL_RAINFALL)));
            Log.i(LOG_TAG, "Successfully referesh the departure. " + mDepartureLocationView.getText());
        }
    }

    private void setDepartureViewValues() {
        long targetTime = Utility.getFiveMintesSeparatedTime(System.currentTimeMillis(), 5);
        Log.i(LOG_TAG, "targetTime:" + targetTime);
        Cursor c = getActivity().getContentResolver().query(
                WeatherContract.WeatherEntry.CONTENT_URI,
                SUMMARY_COLUMNS,
                WeatherContract.LocationEntry.COLUMN_STATION_CODE + " = ? AND "
                        + WeatherContract.WeatherEntry.COLUMN_DATE + " = ? ",
                new String[]{mDepartureStationCode, Long.toString(targetTime)},
//                WeatherContract.WeatherEntry.COLUMN_DATE + " ASC");
                null);
        if (c == null)
            return;
        if (c.moveToFirst()) {
            Log.i(LOG_TAG, "departure count:" + c.getCount());
            mDepartureLocationView.setText(c.getString(COL_NAME));
            long time = c.getLong(COL_WEATHER_DATE);
            mDepartureTimeView.setText(Utility.formatTimeForDisplay(time).toString());
            long timeLag = time - System.currentTimeMillis();
            mDepartureTimeLagView.setText(Utility.formatTimeLag(getActivity(), timeLag));
            mDepartureRainfallView.setText(Utility.formatRainfall(getActivity(), c.getDouble(COL_RAINFALL)));
            mDepartureIconView.setImageResource(Utility.getArtResouceId(c.getDouble(COL_RAINFALL)));
            Log.i(LOG_TAG, "Successfully referesh the departure. " + mDepartureLocationView.getText());
        }
        c.close();
    }

    private void setDestinationViewValues() {
        int delayMinutes = Utility.getTravelTime(getActivity());
        Log.i(LOG_TAG, "delayMinutes:" + delayMinutes);
        long targetTime = Utility.getFiveMintesSeparatedTime(System.currentTimeMillis(), delayMinutes);
        Log.i(LOG_TAG, "targetTime:" + targetTime);
        Cursor c = getActivity().getContentResolver().query(
                WeatherContract.WeatherEntry.CONTENT_URI,
                SUMMARY_COLUMNS,
                WeatherContract.LocationEntry.COLUMN_STATION_CODE + " = ?  AND "
                        + WeatherContract.WeatherEntry.COLUMN_DATE + " = ? ",
                new String[]{ mDestinationStationCode, Long.toString(targetTime) },
//                WeatherContract.WeatherEntry.COLUMN_DATE + " DESC");
                null);
        if (c == null)
            return;
        if (c.moveToFirst()) {
            Log.i(LOG_TAG, "destination count:" + c.getCount());
            mDestinationLocationView.setText(c.getString(COL_NAME));
            long time = c.getLong(COL_WEATHER_DATE);
            mDestinationTimeView.setText(Utility.formatTimeForDisplay(time).toString());
            long timeLag = time - System.currentTimeMillis();
            mDestinationTimeLagView.setText(Utility.formatTimeLag(getActivity(), timeLag));
            mDestinationRainfallView.setText(Utility.formatRainfall(getActivity(), c.getDouble(COL_RAINFALL)));
            mDestinationIconView.setImageResource(Utility.getArtResouceId(c.getDouble(COL_RAINFALL)));
            Log.i(LOG_TAG, "Successfully referesh the destination. " + mDestinationLocationView.getText());
        }
        c.close();
    }
}
