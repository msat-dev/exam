package com.example.android.rainuponarrival;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class SelectStationActivity extends ActionBarActivity {
    private static final String LOG_TAG = SelectStationActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Bundle args = new Bundle();
            args.putString(SelectStationFragment.FROM_KEY, getIntent().getStringExtra(SelectStationFragment.FROM_KEY));
            SelectStationFragment ssf = new SelectStationFragment();
            ssf.setArguments(args);

            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, ssf)
                    .commit();
        }
    }
}
