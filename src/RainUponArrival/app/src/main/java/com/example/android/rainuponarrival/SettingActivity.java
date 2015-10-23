package com.example.android.rainuponarrival;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class SettingActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new SettingFragment()).commit();
    }
}
