package com.example.android.rainuponarrival.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Defines table and column names for the weather database.
 */
public class WeatherContract {
    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.  A convenient string to use for the
    // content authority is the package name for the app, which is guaranteed to be unique on the
    // device.
    public static final String CONTENT_AUTHORITY = "com.example.android.rainuponarrival";

    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible paths (appended to base content URI for possible URI's)
    // For instance, content://com.example.android.sunshine.app/weather/ is a valid path for
    // looking at weather data. content://com.example.android.sunshine.app/givemeroot/ will fail,
    // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
    // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.
    public static final String PATH_WEATHER= "weather";
    public static final String PATH_LOCATION = "location";

//    // To make it easy to query for the exact date, we normalize all dates that go into
//    // the database to the start of the the Julian day at UTC.
//    public static long normalizeDate(long startDate) {
//        // normalize the start date to the beginning of the (UTC) day
//        Time time = new Time();
//        time.set(startDate);
//        int julianDay = Time.getJulianDay(startDate, time.gmtoff);
//        return time.setJulianDay(julianDay);
//    }

    /*
        Inner class that defines the table contents of the point table
        Students: This is where you will add the strings.  (Similar to what has been
        done for RainFallEntry)
     */
    public static final class LocationEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCATION).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;

        public static Uri buildLocationUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
        public static Uri buildLocationWithStationCode(String stationCode) {
            return CONTENT_URI.buildUpon().appendPath(stationCode).build();
        }

        public static long getRowIdWithUri(Uri uri) {
            return ContentUris.parseId(uri);
        }

        public static String getStationCodeWithUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static final String TABLE_NAME = "location";

        // Location Name
        public static final String COLUMN_NAME = "name";
        // Latitude
        public static final String COLUMN_LAT = "lat";
        // Longitude
        public static final String COLUMN_LON = "lon";
        // Station Code
        public static final String COLUMN_STATION_CODE = "station_code";
        // Station Group Code
        public static final String COLUMN_STATION_GROUP_CODE = "station_group_code";
        // Line Code
        public static final String COLUMN_LINE_CODE = "line_code";
        // Line Name
        public static final String COLUMN_LINE_NAME = "line_name";
        // Prefecture Code
        public static final String COLUMN_PREF_CODE = "pref_code";
        // Prefecture Name
        public static final String COLUMN_PREF_NAME = "pref_name";
//        // Weather Area Code
//        public static final String COLUMN_WEATHER_AREA_CODE = "weather_area_code";
    }

    /* Inner class that defines the table contents of the weather table */
    public static final class WeatherEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_WEATHER).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_WEATHER;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_WEATHER;

        public static Uri buildWeatherUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildWeatherLocation(String stationCode) {
            return CONTENT_URI.buildUpon().appendPath(stationCode).build();
        }

        public static Uri buildWeatherLocationWithStartDate(String stationCode, long startTime) {
            return CONTENT_URI.buildUpon().appendPath(stationCode)
                    .appendQueryParameter(COLUMN_DATE, Long.toString(startTime)).build();
        }

        public static Uri buildWeatherLocationWithDate(long locationId, long time) {
//            long normalizedDate = normalizeDate(date);
            return CONTENT_URI.buildUpon().appendPath(Long.toString(locationId))
                    .appendPath(Long.toString(time)).build();
        }

        public static String getStationCodeFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static long getDateFromUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(2));
        }

        public static long getStartDateFromUri(Uri uri) {
            String dateString = uri.getQueryParameter(COLUMN_DATE);
            if (null != dateString && dateString.length() > 0)
                return Long.parseLong(dateString);
            else
                return 0;
        }

        public static final String TABLE_NAME = "weather";
        // Column with the foreign key into the location table.
        public static final String COLUMN_LOC_KEY = "location_id";
        // Date, stored as long in milliseconds since the epoch
        public static final String COLUMN_DATE = "date";
        // Rainfall is stored as a float representing mm/h
        public static final String COLUMN_RAINFALL = "rainfall";
    }
}
