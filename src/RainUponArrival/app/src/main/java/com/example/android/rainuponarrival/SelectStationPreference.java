package com.example.android.rainuponarrival;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;

public class SelectStationPreference extends Preference {
    public SelectStationPreference(Context context) {
        super(context);
    }
    public SelectStationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onClick() {
        String key = getKey();
        Intent intent = new Intent(getContext(), SelectStationActivity.class);
        intent.putExtra(SelectStationFragment.FROM_KEY, key);
        getContext().startActivity(intent);
    }
}
