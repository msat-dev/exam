package com.example.android.rainuponarrival;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.android.rainuponarrival.data.WeatherContract;
import com.example.android.rainuponarrival.sync.RainfallSyncAdapter;

public class MainActivity extends ActionBarActivity implements SummaryFragment.Callback {
    public static final String DETAILFRAGMENT_TAG = "DetailFragment";

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final long DELETE_TARGET_PAST_TIME = 30 * 60 * 1000; // 30 minutes

    private boolean mTwoPane = false;
    private String mHomeStationCode;
    private String mOfficeStationCode;
    private int mTravelTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (findViewById(R.id.weather_detail_container) != null) {
            Log.i(LOG_TAG, "two panels");
            mTwoPane = true;
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container, new DetailFragment(), DETAILFRAGMENT_TAG)
                        .commit();
            }
        } else {
            Log.i(LOG_TAG, "one panel");
            mTwoPane = false;
            getSupportActionBar().setElevation(0f);
        }

        mHomeStationCode = Utility.getHomeStationCode(this);
        mOfficeStationCode = Utility.getOfficeStationCode(this);
        mTravelTime = Utility.getTravelTime(this);

        deleteOldData();

        RainfallSyncAdapter.initializeSyncAdapter(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(LOG_TAG, "onStart");
        String currentHomeStationCode = Utility.getHomeStationCode(this);
        String currentOfficeStationCode = Utility.getOfficeStationCode(this);
        int currentTravelTime = Utility.getTravelTime(this);
        boolean needsSync = false;
        if (mHomeStationCode == null || !mHomeStationCode.equals(currentHomeStationCode)) {
            mHomeStationCode = currentHomeStationCode;
            needsSync = true;
        }
        if (mOfficeStationCode == null || !mOfficeStationCode.equals(currentOfficeStationCode)) {
            mOfficeStationCode = currentOfficeStationCode;
            needsSync = true;
        }
        if (mTravelTime != currentTravelTime) {
            mTravelTime = currentTravelTime;
            needsSync = true;
        }
        if (needsSync) {
//            RainfallSyncAdapter.syncImmediately(this);
            notifySettingChangeToFragments();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSummaryClicked(String stationCode) {
        Log.i(LOG_TAG, "onSummaryClicked");
        if (mTwoPane) {
            Bundle args = new Bundle();
            args.putString(DetailFragment.STATION_CODE, stationCode);

            DetailFragment df = new DetailFragment();
            df.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, df, DETAILFRAGMENT_TAG)
                    .commit();
        } else {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(DetailFragment.STATION_CODE, stationCode);
            startActivity(intent);
        }
    }

    private void deleteOldData() {
        long deleteTargetTime = System.currentTimeMillis() - DELETE_TARGET_PAST_TIME;
        String deleteTargetTimeStr = Long.toString(deleteTargetTime);
        getContentResolver().delete(WeatherContract.WeatherEntry.CONTENT_URI,
                "(" + WeatherContract.WeatherEntry.COLUMN_LOC_KEY + " == ? OR "
                        + WeatherContract.WeatherEntry.COLUMN_LOC_KEY + " == ?) AND "
                        + WeatherContract.WeatherEntry.COLUMN_DATE + " < ? ",
                new String[]{mHomeStationCode, mOfficeStationCode, deleteTargetTimeStr});
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void notifySettingChangeToFragments() {
        SummaryFragment sf = (SummaryFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_summary);
        if (sf != null) {
            sf.onSettingChanged();
        }
//        DetailFragment df = (DetailFragment)getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
//        if ( null != df ) {
//            df.onLocationChanged(location);
//        }
    }
}
