package com.example.android.rainuponarrival.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class RainfallSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static RainfallSyncAdapter sRainfallSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("RainfallSyncService", "onCreate - RainfallSyncService");
        synchronized (sSyncAdapterLock) {
            if (sRainfallSyncAdapter == null) {
                sRainfallSyncAdapter = new RainfallSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sRainfallSyncAdapter.getSyncAdapterBinder();
    }
}