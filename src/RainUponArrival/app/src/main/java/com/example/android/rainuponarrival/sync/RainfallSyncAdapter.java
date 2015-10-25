package com.example.android.rainuponarrival.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.example.android.rainuponarrival.MainActivity;
import com.example.android.rainuponarrival.R;
import com.example.android.rainuponarrival.Utility;
import com.example.android.rainuponarrival.data.RainfallLocationUtil;
import com.example.android.rainuponarrival.data.RainfallLocationUtil.Station;
import com.example.android.rainuponarrival.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

public class RainfallSyncAdapter extends AbstractThreadedSyncAdapter {
    public final String LOG_TAG = RainfallSyncAdapter.class.getSimpleName();

    // Interval at which to sync with the weather, in seconds.
    // 60 seconds (1 minute) * 5 = 5 minutes
    public static final int SYNC_INTERVAL = 60 * 5; // 5 minitus
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;

    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[] {
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_RAINFALL,
    };

//    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int HOME_RAINFALL_NOTIFICATION_ID = 3004;
    private static final int OFFICE_RAINFALL_NOTIFICATION_ID = 3005;

    private static HashMap<String, Boolean> sWillRain = new HashMap<String, Boolean>();

    public RainfallSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(LOG_TAG, "Starting sync");

        String homeStationCode = Utility.getHomeStationCode(getContext());
        List<ContentValues> data = null;
        if (homeStationCode != null && homeStationCode.length() > 0) {
            data = getSyncWeatherValues(homeStationCode);
//            if (data != null && data.size() > 0)
//                deletePastData(homeStationCode);
        }
        String officeStationCode = Utility.getOfficeStationCode(getContext());
        if (officeStationCode != null && officeStationCode.length() > 0) {
            List<ContentValues> officeData = getSyncWeatherValues(officeStationCode);
            if (officeData != null) {
//                deletePastData(officeStationCode);
                if (data != null) {
                    data.addAll(officeData);
                } else {
                    data = officeData;
                }
            }
        }
        if (data != null) {
            ContentValues[] cvArray = new ContentValues[data.size()];
            data.toArray(cvArray);
            getContext().getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, cvArray);
        }
    }

    private List<ContentValues> getSyncWeatherValues(String stationCode) {
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String weatherJsonStr = null;

        String format = "json";
        String appid = "dj0zaiZpPUFQS0xsb1lwZlpDbyZzPWNvbnN1bWVyc2VjcmV0Jng9ODI-";
        String interval = "5";
        Station station = RainfallLocationUtil.getStation(getContext(), stationCode);
        String coordinatesParam = new StringBuilder().append(station.lon).append(',').append(station.lat).toString();
        if (coordinatesParam == null || coordinatesParam.length() == 0)
            return null;

        try {
            // Construct the URL for the Yahoo Open Local Platform query
            // Possible parameters are avaiable at 's forecast API page
            // http://developer.yahoo.co.jp/webapi/map/openlocalplatform/v1/weather.html
            final String WEATHER_BASE_URL =
                    "http://weather.olp.yahooapis.jp/v1/place?";
//                    "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String APPID_PARAM = "appid";
            final String COODINATES_PARAM = "coordinates"; // Required. longitude,latitude (WGS) devided by space charactor
            final String OUTPUT_PARAM = "output"; // xml or json
            final String DATE_PARAM = "date"; // YYYYMMDDHHMI (later than 2 hours ago)
            final String PAST_PARAM = "past"; // 0: not get(default), 1: from 1 hour begore, 2 from 2 hours begore
            final String INTERVAL_PARAM = "interval";

            Uri.Builder builder = Uri.parse(WEATHER_BASE_URL).buildUpon();
            builder.appendQueryParameter(APPID_PARAM, appid);
            builder.appendQueryParameter(COODINATES_PARAM, coordinatesParam);
            builder.appendQueryParameter(OUTPUT_PARAM, format);
            builder.appendQueryParameter(INTERVAL_PARAM, interval);

            URL url = new URL(builder.build().toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            weatherJsonStr = buffer.toString();
            Log.i(LOG_TAG, "jsonStr:" + weatherJsonStr);
            return getWeatherDataFromJson(weatherJsonStr, station);

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attempting
            // to parse it.
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
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

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     * <p/>
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private List<ContentValues> getWeatherDataFromJson(String responseJsonStr, Station station)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.

        final String YOLP_YDF = "ydf";

        // ResultInfo - Summary
        final String YOLP_RESULT_INFO   = "ResultInfo";
        final String YOLP_COUNT         = "Count";
        final String YOLP_TOTAL         = "Total";
        final String YOLP_START         = "Start";
        final String YOLP_STATUS        = "Status";
        final String YOLP_LATENCY       = "Latency";
        final String YOLP_DESCRIPTION   = "Description";
        final String YOLP_COPYRIGHT     = "Copyright";

        // Feature - Each interval's observation or forecast info
        final String YOLP_FEATURE       = "Feature";
        final String YOLP_ID            = "Id";
        final String YOLP_NAME          = "Name";
        // Geometry
        final String YOLP_GEOMETRY      = "Geometry";
        final String YOLP_GEO_TYPE      = "Type";
        final String YOLP_COORDINATES   = "Coordinates";
        // Property
        final String YOLP_PROPERTY      = "Property";
//        final String YOLP_WEATHER_AREA_CODE = "WeatherAreaCode";
        // Weather
        final String YOLP_WEATHER_LIST  = "WeatherList";
        final String YOLP_WEATHER       = "Weather";
        final String YOLP_WEATHER_TYPE  = "Type";
        final String YOLP_DATE          = "Date";
        final String YOLP_RAINFALL      = "Rainfall";

        try {
            JSONObject ydfJson = new JSONObject(responseJsonStr);
            JSONObject resultInfoJson = ydfJson.getJSONObject(YOLP_RESULT_INFO);
//            JSONObject featureJson = ydfJson.getJSONObject(YOLP_FEATURE);
            JSONArray featureArray = ydfJson.getJSONArray(YOLP_FEATURE);
            JSONObject featureJson = featureArray.getJSONObject(0);

//            JSONArray featureArray = new JSONArray(responseJsonStr);
//            JSONObject featureJson = featureArray.getJSONObject(0);
            JSONObject geometoryJson = featureJson.getJSONObject(YOLP_GEOMETRY);
            String coordinates = geometoryJson.getString(YOLP_COORDINATES);

            JSONObject propertyJson = featureJson.getJSONObject(YOLP_PROPERTY);
//            int areaCode = propertyJson.getInt(YOLP_WEATHER_AREA_CODE);

            JSONObject weatherListJson = propertyJson.getJSONObject(YOLP_WEATHER_LIST);
            JSONArray weatherArray = weatherListJson.getJSONArray(YOLP_WEATHER);

            // Insert the new weather information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());

            boolean willRain = false;
            for (int i = 0; i < weatherArray.length(); i++) {
                JSONObject weatherJson = weatherArray.getJSONObject(i);
                String type = weatherJson.getString(YOLP_WEATHER_TYPE);
                String dateStr = weatherJson.getString(YOLP_DATE);
//                String formatedDateStr = null;
//                if (dateStr.length() == 12) {
//                    formatedDateStr = new StringBuilder(16).append(dateStr.substring(0,4)).append('/')
//                            .append(dateStr.substring(4,6)).append('/')
//                            .append(dateStr.substring(6,8)).append(' ')
//                            .append(dateStr.substring(8,10)).append(':')
//                            .append(dateStr.substring(10)).toString();
//                    Log.i(LOG_TAG, "formated date str:" + formatedDateStr);
//                }
//                Date date;
//                try {
//                    if (formatedDateStr == null) {
//                        date = df.parse(dateStr);
//                        Log.i(LOG_TAG, "df str:" + dateStr + ", date:" + date.toString() + ", millis:" + date.getTime());
//                    } else {
//                        date = df2.parse(formatedDateStr);
//                        Log.i(LOG_TAG, "df2 str:" + formatedDateStr + ", date:" + date.toString() + ", millis:" + date.getTime());
//                    }
//                } catch (ParseException e) {
//                    Log.e(LOG_TAG, "Error ", e);
//                    continue;
//                }
                Calendar cal = Calendar.getInstance(Locale.JAPAN);
                cal.clear();
                int year   = Integer.parseInt(dateStr.substring(0, 4));
                int month  = Integer.parseInt(dateStr.substring(4, 6)) - 1;
                int day    = Integer.parseInt(dateStr.substring(6, 8));
                int hour   = Integer.parseInt(dateStr.substring(8, 10));
                int minute = Integer.parseInt(dateStr.substring(10, 12));
                cal.set(year, month, day, hour, minute, 0);

                double rainfall = weatherJson.getDouble(YOLP_RAINFALL);
                if (rainfall > 0) {
                    willRain = true;
                    if (!sWillRain.containsKey(station.code) || !sWillRain.get(station.code))
                        notifyRainfall(station, cal, rainfall);
                }

                ContentValues weatherValues = new ContentValues();
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, station.code);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, cal.getTimeInMillis());
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_RAINFALL, rainfall);

                cVVector.add(weatherValues);
            }
            if (!willRain) {
                sWillRain.put(station.code, false);
                clearNotification(station);
            }
//            notifyRainfall(station, Calendar.getInstance(), 1.0); // for test

//            int inserted = 0;
//            // add to database
//            if (cVVector.size() > 0) {
//                getContext().getContentResolver().delete(WeatherContract.WeatherEntry.CONTENT_URI,
//                        WeatherContract.WeatherEntry.COLUMN_LOC_KEY + "== ?",
//                        new String[]{stationCode});
//
//                ContentValues[] cvArray = new ContentValues[cVVector.size()];
//                cVVector.toArray(cvArray);
//                getContext().getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, cvArray);
//                notifyWeather();

// //                getContext().getContentResolver().delete(WeatherContract.WeatherEntry.CONTENT_URI,
// //                        WeatherContract.WeatherEntry.COLUMN_DATE + "<= ?",
// //                        new String[]{Long.toString(System.currentTimeMillis() - 1000 * 60 * 60 * 2)});
//            }

//            Log.d(LOG_TAG, "Sync Complete. " + cVVector.size() + " Inserted");
            return cVVector;

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return null;
    }

    private void deletePastData(String stationCode) {
        getContext().getContentResolver().delete(WeatherContract.WeatherEntry.CONTENT_URI,
                WeatherContract.WeatherEntry.COLUMN_LOC_KEY + "== ?",
                new String[]{stationCode});
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */
            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        RainfallSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }

    private void notifyRainfall(Station station, Calendar cal, double rainfall) {
        Log.d(LOG_TAG, "notifyRainfall");
        Context context = getContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // checking use notificaiton or not
        if (!prefs.getBoolean(context.getString(R.string.pref_enable_notifications_key),
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)))) {
            return;
        }

        //checking the last update and notify if it' the first of the day
        String lastNotificationKey = context.getString(R.string.pref_last_notification);
        long lastNotification = prefs.getLong(lastNotificationKey, 0);

        //build your notification here.
        PendingIntent ntfIntent;
        Intent intent = new Intent(context, MainActivity.class);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
            // taskStackBuilder.addParentStack(MainActivity.class);
            taskStackBuilder.addNextIntent(intent);
            ntfIntent = taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            ntfIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        String title;
        int notificationId;
        if (station.code.equals(Utility.getHomeStationCode(context))) {
            title = context.getString(R.string.title_home_rainfall_notification);
            notificationId = HOME_RAINFALL_NOTIFICATION_ID;
        } else {
            title = context.getString(R.string.title_office_rainfall_notification);
            notificationId = OFFICE_RAINFALL_NOTIFICATION_ID;
        }

        String contentText = Utility.formatNotificationContent(context, cal.getTime(), station.name);
        NotificationManager ntfMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker("")
                .setWhen(System.currentTimeMillis())
                .setContentTitle(title)
                        .setContentText(contentText)
                .setContentIntent(ntfIntent)
                .build();
        ntfMgr.notify(notificationId, notification);

        //refreshing last sync
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(lastNotificationKey, System.currentTimeMillis());
        editor.commit();

        sWillRain.put(station.code, true);
    }

    private void clearNotification(Station station) {
        Context context = getContext();
        int notificationId;
        if (station.code.equals(Utility.getHomeStationCode(context))) {
            notificationId = HOME_RAINFALL_NOTIFICATION_ID;
        } else {
            notificationId = OFFICE_RAINFALL_NOTIFICATION_ID;
        }

        NotificationManager ntfMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        ntfMgr.cancel(notificationId);
    }
}
