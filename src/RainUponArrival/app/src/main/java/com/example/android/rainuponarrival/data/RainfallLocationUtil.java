package com.example.android.rainuponarrival.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.io.Serializable;

public class RainfallLocationUtil {
    private static final String[] LOCATION_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.LocationEntry._ID,
            WeatherContract.LocationEntry.COLUMN_STATION_CODE,
            WeatherContract.LocationEntry.COLUMN_STATION_GROUP_CODE,
            WeatherContract.LocationEntry.COLUMN_PREF_CODE,
            WeatherContract.LocationEntry.COLUMN_LINE_CODE,
            WeatherContract.LocationEntry.COLUMN_NAME,
            WeatherContract.LocationEntry.COLUMN_LAT,
            WeatherContract.LocationEntry.COLUMN_LON
    };

    static final int COL_ID = 0;
    static final int COL_STATION_CODE = 1;
    static final int COL_STATION_GROUP_CODE = 2;
    static final int COL_PREF_CODE = 3;
    static final int COL_LINE_CODE = 4;
    static final int COL_NAME = 5;
    static final int COL_LAT = 6;
    static final int COL_LON = 7;

    public static class Station implements Serializable {
        public String code;
        public String groupCode;
        public String prefCode;
        public String lineCode;
        public String name;
        public String lat;
        public String lon;

        private Station() {}

        public Station(String _code, String _groupCode, String _prefCode, String _lineCode, String _name, String _lat, String _lon) {
            this.code      = _code;
            this.groupCode = _groupCode;
            this.prefCode  = _prefCode;
            this.lineCode  = _lineCode;
            this.name      = _name;
            this.lat       = _lat;
            this.lon       = _lon;
        }

        public String toString() {
            return new StringBuilder(100)
                    .append("code:").append(code)
                    .append(", groupCode:").append(groupCode)
                    .append(", prefCode:").append(prefCode)
                    .append(", lineCode:").append(lineCode)
                    .append(", name:").append(name)
                    .append(", lat:").append(lat)
                    .append(", lon:").append(lon)
                    .toString();
        }
    }

    public static Station getStation(Context context, String stationCode) {
        Cursor c = context.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                LOCATION_COLUMNS,
                WeatherContract.LocationEntry.COLUMN_STATION_CODE + " = ?",
                new String[]{stationCode},
                null);
        if (c == null)
            return null;
        Station station = null;
        if (c.moveToFirst()) {
            String groupCode = c.getString(COL_STATION_GROUP_CODE);
            String prefCode = c.getString(COL_PREF_CODE);
            String lineCode = c.getString(COL_LINE_CODE);
            String name = c.getString(COL_NAME);
            String lat = c.getString(COL_LAT);
            String lon = c.getString(COL_LON);
            station = new Station(stationCode, groupCode, prefCode, lineCode, name, lat, lon);
        }
        c.close();

        return station;
    }

    /**
     * Helper method to handle insertion of a new location in the weather database.
     *
     * @param station    Station Class
     */
    public static void addStation(Context context, Station station) {
        // First, check if the location with this city name exists in the db
        Cursor c = context.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_STATION_CODE + " = ?",
                new String[]{station.code},
                null);

        if (c == null)
            return;
        if (!c.moveToFirst()) {
            // Now that the content provider is set up, inserting rows of data is pretty simple.
            // First create a ContentValues object to hold the data you want to insert.
            ContentValues locationValues = new ContentValues();

            // Then add the data, along with the corresponding name of the data type,
            // so the content provider knows what kind of value is being inserted.
            locationValues.put(WeatherContract.LocationEntry.COLUMN_STATION_CODE, station.code);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_STATION_GROUP_CODE, station.groupCode);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_PREF_CODE, station.prefCode);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_LINE_CODE, station.lineCode);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_NAME, station.name);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_LAT, station.lat);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_LON, station.lon);

            // Finally, insert location data into the database.
            Uri insertedUri = context.getContentResolver().insert(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    locationValues
            );
        }
        c.close();
    }
}
