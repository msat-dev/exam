package com.example.android.rainuponarrival;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;

import java.util.Calendar;

public class Utility {
    public static String getHomeStationCode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_home_station_key),
                context.getString(R.string.pref_home_station_default));
    }

    public static String getOfficeStationCode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_office_station_key),
                context.getString(R.string.pref_office_station_default));
    }

    public static int getTravelTime(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String travelTimeStr =  prefs.getString(context.getString(R.string.pref_travel_time_key),
                context.getString(R.string.pref_travel_time_default));
        if (travelTimeStr != null) {
            return Integer.parseInt(travelTimeStr);
        }
        return 0;
    }

    public static void storeHomeStationCode(Context context, String stationCode) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(context.getString(R.string.pref_home_station_key), stationCode).commit();
    }

    public static void storeOfficeStationCode(Context context, String stationCode) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(context.getString(R.string.pref_office_station_key), stationCode).commit();
    }

    public static void storeTravelTime(Context context, int travelTime) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt(context.getString(R.string.pref_travel_time_key), travelTime).commit();
    }

    public static String getDepartureStationCode(Context context) {
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.HOUR_OF_DAY) < 12) {
            return getHomeStationCode(context);
        } else {
            return getOfficeStationCode(context);
        }
    }

    public static String getDestinationStationCode(Context context) {
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.HOUR_OF_DAY) >= 12) {
            return getHomeStationCode(context);
        } else {
            return getOfficeStationCode(context);
        }
    }

    public static String formatRainfall(Context context, double rainfall) {
        return String.format(context.getString(R.string.format_rainfall), rainfall);
    }

    public static String formatTimeForDisplay(long time) {
        return (String) DateFormat.format("MM/dd kk:mm", time);
    }

    public static String formatTimeLag(Context context, long timeLagMillis) {
        int timeLagMinutes = (int) (timeLagMillis / 1000 / 60);
        return String.format(context.getString(R.string.format_time_lag), timeLagMinutes);
    }

    public static long getFiveMintesSeparatedTime(long currentTime, int delayMinutes) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(currentTime);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        int nowMinute = cal.get(Calendar.MINUTE);
        int formattedMinute = (nowMinute / 5) * 5;
        cal.set(Calendar.MINUTE, formattedMinute);

        if (delayMinutes > 0)
            cal.add(Calendar.MINUTE, delayMinutes);

        return cal.getTimeInMillis();
    }

    public static int getArtResouceId(double rainfall) {
        if (rainfall > 10)
            return R.drawable.art_storm;
        if (rainfall > 5)
            return R.drawable.art_rain;
        if (rainfall > 2)
            return R.drawable.art_light_rain;
        if (rainfall > 0)
            return R.drawable.art_clouds;

        return R.drawable.art_clear;
    }

    public static int getIconResouceId(double rainfall) {
        if (rainfall > 10)
            return R.drawable.ic_storm;
        if (rainfall > 5)
            return R.drawable.ic_rain;
        if (rainfall > 2)
            return R.drawable.ic_light_rain;
        if (rainfall > 0)
            return R.drawable.ic_cloudy;

        return R.drawable.ic_clear;
    }
}
