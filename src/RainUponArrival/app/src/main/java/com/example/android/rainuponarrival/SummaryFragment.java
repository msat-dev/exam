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

import com.example.android.rainuponarrival.data.RainfallLocationUtil;
import com.example.android.rainuponarrival.data.RainfallLocationUtil.Station;
import com.example.android.rainuponarrival.data.WeatherContract;
import com.example.android.rainuponarrival.sync.RainfallSyncAdapter;

public class SummaryFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, View.OnLongClickListener {
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

    private TextView  mInitialComment;
    private View      mDepartureLayout;
    private TextView  mDepartureLocationView;
    private TextView  mDepartureTimeView;
    private TextView  mDepartureTimeLagView;
    private TextView  mDepartureRainfallView;
    private ImageView mDepartureIconView;
    private View      mDestinationLayout;
    private TextView  mDestinationLocationView;
    private TextView  mDestinationTimeView;
    private TextView  mDestinationTimeLagView;
    private TextView  mDestinationRainfallView;
    private ImageView mDestinationIconView;
//    private ListView mSummaryListView;

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

        mInitialComment = (TextView) rootView.findViewById(R.id.initial_comment);

        mDepartureLayout   = rootView.findViewById(R.id.departure_layout);
        mDepartureLocationView = (TextView)  mDepartureLayout.findViewById(R.id.summary_location);
//        mDepartureLocationView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        mDepartureTimeView     = (TextView)  mDepartureLayout.findViewById(R.id.summary_time);
        mDepartureTimeLagView  = (TextView)  mDepartureLayout.findViewById(R.id.summary_time_lag);
        mDepartureRainfallView = (TextView)  mDepartureLayout.findViewById(R.id.summary_rainfall);
        mDepartureIconView     = (ImageView) mDepartureLayout.findViewById(R.id.summary_icon);

        mDestinationLayout   = rootView.findViewById(R.id.destination_layout);
        mDestinationLocationView = (TextView)  mDestinationLayout.findViewById(R.id.summary_location);
//        mDestinationLocationView.measure(View.MeasureSpec.EXACTLY, View.MeasureSpec.EXACTLY);
        mDestinationTimeView     = (TextView)  mDestinationLayout.findViewById(R.id.summary_time);
        mDestinationTimeLagView  = (TextView)  mDestinationLayout.findViewById(R.id.summary_time_lag);
        mDestinationRainfallView = (TextView)  mDestinationLayout.findViewById(R.id.summary_rainfall);
        mDestinationIconView     = (ImageView) mDestinationLayout.findViewById(R.id.summary_icon);

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
    public void onStart() {
        super.onStart();
        if (!Utility.stationHasRegistered(getActivity())) {
            Log.i(LOG_TAG, "station has not yet registered.");
            mInitialComment.setVisibility(View.VISIBLE);
            mDepartureLayout.setVisibility(View.INVISIBLE);
            mDestinationLayout.setVisibility(View.INVISIBLE);

        } else {
            mInitialComment.setVisibility(View.GONE);
            mDepartureLayout.setVisibility(View.VISIBLE);
            mDestinationLayout.setVisibility(View.VISIBLE);

            if (Utility.homeIsDepartureStation()) {
                if (Utility.homeStationHasRegistered(getActivity())) {
//                    mDepartureLayout.setBackgroundColor(Color.TRANSPARENT);
                    mDepartureLayout.setOnLongClickListener(this);
                } else {
//                    mDepartureLayout.setBackgroundColor(Color.LTGRAY);
                    mDepartureLocationView.setText(getString(R.string.pref_home_station_summary));
                }
                if (Utility.officeStationHasRegistered(getActivity())) {
//                    mDestinationLayout.setBackgroundColor(Color.TRANSPARENT);
                    mDestinationLayout.setOnLongClickListener(this);
                } else {
//                    mDestinationLayout.setBackgroundColor(Color.LTGRAY);
                    mDestinationLocationView.setText(getString(R.string.pref_office_station_summary));
                }
            } else {
                if (Utility.homeStationHasRegistered(getActivity())) {
//                    mDestinationLayout.setBackgroundColor(Color.TRANSPARENT);
                    mDestinationLayout.setOnLongClickListener(this);
                } else {
//                    mDestinationLayout.setBackgroundColor(Color.LTGRAY);
                    mDestinationLocationView.setText(getString(R.string.pref_home_station_summary));
                }
                if (Utility.officeStationHasRegistered(getActivity())) {
//                    mDepartureLayout.setBackgroundColor(Color.TRANSPARENT);
                    mDepartureLayout.setOnLongClickListener(this);
                } else {
//                    mDepartureLayout.setBackgroundColor(Color.LTGRAY);
                    mDepartureLocationView.setText(getString(R.string.pref_office_station_summary));
                }
            }
        }
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
        // Queryが2種類あるのでCONTENT_URIでLoadFinishedを検知する
        return new CursorLoader(getActivity(),
                WeatherContract.WeatherEntry.CONTENT_URI, // uri
                SUMMARY_COLUMNS, // projection
                null, // selection
                null, // selection args
                null); // sort order
    }

    private boolean mHomeIsDepartureStation = true;
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.i(LOG_TAG, "onLoadFinished");
        mHomeIsDepartureStation = Utility.homeIsDepartureStation();
        String departureStationCode = Utility.getDepartureStationCode(getActivity());
        String destinationStationCode = Utility.getDestinationStationCode(getActivity());

        setDepartureViewValues(departureStationCode);
        setDestinationViewValues(destinationStationCode);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
//        mForecastAdapter.swapCursor(null);
    }

    @Override
    public boolean onLongClick(View view) {
        String stationCode = null;
        if (view.getId() == R.id.departure_layout) {
            stationCode = Utility.getDepartureStationCode(getActivity());
        } else if (view.getId() == R.id.destination_layout) {
            stationCode = Utility.getDestinationStationCode(getActivity());
        }
        if (stationCode != null && stationCode.length() > 0)
            ((Callback) getActivity()).onSummaryClicked(stationCode);

        return false;
    }

    public void onSettingChanged() {
        RainfallSyncAdapter.syncImmediately(getActivity());
        getLoaderManager().restartLoader(SUMMARY_LOADER, null, this);
    }

//    private void setDepartureViewValues(Cursor c) {
//        if (c == null)
//            return;
//        if (c.moveToFirst()) {
//            Log.i(LOG_TAG, "departure count:" + c.getCount());
//            mDepartureLocationView.setText(c.getString(COL_NAME));
//            long time = c.getLong(COL_WEATHER_DATE);
//            mDepartureTimeView.setText(Utility.formatTimeForDisplay(time).toString());
//            long timeLag = time - System.currentTimeMillis();
//            mDepartureTimeLagView.setText(Utility.formatTimeLag(getActivity(), timeLag));
//            mDepartureRainfallView.setText(Utility.formatRainfall(getActivity(), c.getDouble(COL_RAINFALL)));
//            mDepartureIconView.setImageResource(Utility.getArtResouceId(c.getDouble(COL_RAINFALL)));
//            Log.i(LOG_TAG, "Successfully referesh the departure. " + mDepartureLocationView.getText());
//        }
//    }

    private void setDepartureViewValues(String departureStationCode) {
        long targetTime = Utility.getFiveMintesSeparatedTime(System.currentTimeMillis(), 5);
        Log.i(LOG_TAG, "targetTime:" + targetTime);
        Cursor c = getActivity().getContentResolver().query(
                WeatherContract.WeatherEntry.CONTENT_URI,
                SUMMARY_COLUMNS,
                WeatherContract.LocationEntry.COLUMN_STATION_CODE + " = ? AND "
                        + WeatherContract.WeatherEntry.COLUMN_DATE + " = ? ",
                new String[]{ departureStationCode, Long.toString(targetTime) },
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
            Log.i(LOG_TAG, "Successfully loaded the departure. " + mDepartureLocationView.getText());
        } else {
            Station station = RainfallLocationUtil.getStation(getActivity(), departureStationCode);
            if (station != null) {
                mDepartureLocationView.setText(station.name);
            }
            mDepartureTimeView.setText("");
            mDepartureTimeLagView.setText("");
            mDepartureRainfallView.setText("");
            mDepartureIconView.setImageDrawable(null);
            Log.i(LOG_TAG, "Failed to load the departure.");
        }
        c.close();
    }

    private void setDestinationViewValues(String destinationStationCode) {
        int delayMinutes = Utility.getTravelTime(getActivity());
        long targetTime = Utility.getFiveMintesSeparatedTime(System.currentTimeMillis(), delayMinutes);
        Cursor c = getActivity().getContentResolver().query(
                WeatherContract.WeatherEntry.CONTENT_URI,
                SUMMARY_COLUMNS,
                WeatherContract.LocationEntry.COLUMN_STATION_CODE + " = ?  AND "
                        + WeatherContract.WeatherEntry.COLUMN_DATE + " <= ? ", // ＝指定だと50分指定のとき取れていないときがある
                new String[]{ destinationStationCode, Long.toString(targetTime) },
                WeatherContract.WeatherEntry.COLUMN_DATE + " DESC");
//                null);
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
            Log.i(LOG_TAG, "Successfully loaded the destination. " + mDestinationLocationView.getText());
        } else {
            Station station = RainfallLocationUtil.getStation(getActivity(), destinationStationCode);
            if (station != null) {
                mDestinationLocationView.setText(station.name);
            }
            mDestinationTimeView.setText("");
            mDestinationTimeLagView.setText("");
            mDestinationRainfallView.setText("");
            mDestinationIconView.setImageDrawable(null);
            Log.i(LOG_TAG, "Failed to load the destination. " + mDestinationLocationView.getText());
        }
        c.close();
    }
}
