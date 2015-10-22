package com.example.android.rainuponarrival.data;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class WeatherProvider extends ContentProvider {
    private static final String LOG_TAG = WeatherProvider.class.getSimpleName();

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private WeatherDbHelper mOpenHelper;

    static final int WEATHER = 100;
    static final int WEATHER_WITH_STATION_CODE = 101;
    static final int WEATHER_WITH_STATION_CODE_AND_TIME = 102;
    static final int LOCATION = 300;

    private static final SQLiteQueryBuilder sWeatherByLocationQueryBuilder;
    private static final SQLiteQueryBuilder sLocationQueryBuilder;

    static{
        //This is an inner join which looks like
        //weather INNER JOIN location ON weather.location_id = location._id
        sWeatherByLocationQueryBuilder = new SQLiteQueryBuilder();
        sWeatherByLocationQueryBuilder.setTables(
                WeatherContract.WeatherEntry.TABLE_NAME + " INNER JOIN " +
                        WeatherContract.LocationEntry.TABLE_NAME +
                        " ON " + WeatherContract.WeatherEntry.TABLE_NAME +
                        "." + WeatherContract.WeatherEntry.COLUMN_LOC_KEY +
                        " = " + WeatherContract.LocationEntry.TABLE_NAME +
                        "." + WeatherContract.LocationEntry.COLUMN_STATION_CODE);

        sLocationQueryBuilder = new SQLiteQueryBuilder();
        sLocationQueryBuilder.setTables(WeatherContract.LocationEntry.TABLE_NAME);
    }

    //location.station_code = ?
    private static final String sStationCodeSelection =
            WeatherContract.LocationEntry.COLUMN_STATION_CODE + " = ? ";

    //location.station_code = ? AND date = ?
    private static final String sStationCodeAndTimeSelection =
            WeatherContract.LocationEntry.COLUMN_STATION_CODE + " = ? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATE + " = ? ";

    //location.station_code = ? AND date > ?
    private static final String sStationCodeAndStartTimeSelection =
            WeatherContract.LocationEntry.COLUMN_STATION_CODE + " = ? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATE + " >= ? ";
    /*
        Students: Here is where you need to create the UriMatcher. This UriMatcher will
        match each URI to the WEATHER, WEATHER_WITH_LOCATION, WEATHER_WITH_LOCATION_AND_DATE,
        and LOCATION integer constants defined above.  You can test this by uncommenting the
        testUriMatcher test within TestUriMatcher.
     */
    static UriMatcher buildUriMatcher() {
        final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = WeatherContract.CONTENT_AUTHORITY;

        uriMatcher.addURI(authority, WeatherContract.PATH_WEATHER, WEATHER);
        uriMatcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*", WEATHER_WITH_STATION_CODE);
        uriMatcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*/#", WEATHER_WITH_STATION_CODE_AND_TIME);
        uriMatcher.addURI(authority, WeatherContract.PATH_LOCATION, LOCATION);

        return uriMatcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new WeatherDbHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            // Student: Uncomment and fill out these two cases
//            case WEATHER_WITH_LOCATION_AND_DATE:
//            case WEATHER_WITH_LOCATION:
            case WEATHER:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case WEATHER_WITH_STATION_CODE:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case WEATHER_WITH_STATION_CODE_AND_TIME:
                return WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE;
            case LOCATION:
                return WeatherContract.LocationEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            // "weather/*/*"
            case WEATHER_WITH_STATION_CODE_AND_TIME: {
                retCursor = getWeatherByStationCodeAndTime(uri, projection, sortOrder);
                break;
            }
            // "weather/*/*

            // "weather/*"
            case WEATHER_WITH_STATION_CODE: {
                retCursor = getWeatherByStationCode(uri, projection, sortOrder);
                break;
            }
            // "weather"
            case WEATHER: {
                retCursor = getWeather(uri, projection, selection, selectionArgs, sortOrder);
                break;
            }
            // "location"
            case LOCATION: {
                retCursor = getLocation(uri, projection, selection, selectionArgs, sortOrder);
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    /*
        Student: Add the ability to insert Locations to the implementation of this function.
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case WEATHER: {
                normalizeDate(values);
                long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = WeatherContract.WeatherEntry.buildWeatherUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case LOCATION: {
                long _id = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, values);
                if (_id > 0) {
                    returnUri = WeatherContract.LocationEntry.buildLocationUri(_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        db.close();
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Student: Start by getting a writable database
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int returnCount = 0;

        // Student: Use the uriMatcher to match the WEATHER and LOCATION URI's we are going to
        // handle.  If it doesn't match these, throw an UnsupportedOperationException.
        switch(match) {
            case WEATHER: {
                returnCount = db.delete(WeatherContract.WeatherEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            case LOCATION: {
                returnCount = db.delete(WeatherContract.LocationEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri:" + uri);
        }

        // Student: A null value deletes all rows.  In my implementation of this, I only notified
        // the uri listeners (using the content resolver) if the rowsDeleted != 0 or the selection
        // is null.
        // Oh, and you should notify the listeners here.
        if (returnCount > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        db.close();


        // Student: return the actual rows deleted
        return returnCount;
    }

    private void normalizeDate(ContentValues values) {
        // normalize the date value
//        if (values.containsKey(WeatherContract.WeatherEntry.COLUMN_DATE)) {
//            long dateValue = values.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE);
//            values.put(WeatherContract.WeatherEntry.COLUMN_DATE, WeatherContract.normalizeDate(dateValue));
//        }
    }

    @Override
    public int update(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // Student: This is a lot like the delete function.  We return the number of rows impacted
        // by the update.
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int returnCount = 0;

        switch (match) {
            case WEATHER: {
                returnCount = db.update(WeatherContract.WeatherEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            }
            case LOCATION: {
                returnCount = db.update(WeatherContract.LocationEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri. uri:" + uri);
        }
        if (returnCount > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        db.close();
        return returnCount;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case WEATHER:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        normalizeDate(value);
                        long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                Log.d(LOG_TAG, "provider notifyChange");
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }

    // You do not need to call this method. This is a method specifically to assist the testing
    // framework in running smoothly. You can read more at:
    // http://developer.android.com/reference/android/content/ContentProvider.html#shutdown()
    @Override
    @TargetApi(11)
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }

    private Cursor getWeather(
            Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return sWeatherByLocationQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    private Cursor getWeatherByStationCode(
            Uri uri, String[] projection, String sortOrder) {
        String stationCode = WeatherContract.WeatherEntry.getStationCodeFromUri(uri);
        long startDate = WeatherContract.WeatherEntry.getStartDateFromUri(uri);

        String selection;
        String[] selectionArgs;
        if (startDate == 0) {
            selection = sStationCodeSelection;
            selectionArgs = new String[]{stationCode};
        } else {
            selectionArgs = new String[]{stationCode, Long.toString(startDate)};
            selection = sStationCodeAndStartTimeSelection;
        }

        return sWeatherByLocationQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    private Cursor getWeatherByStationCodeAndTime(
            Uri uri, String[] projection, String sortOrder) {
        String stationCode = WeatherContract.WeatherEntry.getStationCodeFromUri(uri);
        long time = WeatherContract.WeatherEntry.getDateFromUri(uri);

        return sWeatherByLocationQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sStationCodeAndTimeSelection,
                new String[]{stationCode, Long.toString(time)},
                null,
                null,
                sortOrder
        );
    }

    private Cursor getWeatherByStationCodeAndStartTime(
            Uri uri, String[] projection, String sortOrder) {
        String stationCode = WeatherContract.WeatherEntry.getStationCodeFromUri(uri);
        long time = WeatherContract.WeatherEntry.getDateFromUri(uri);

        return sWeatherByLocationQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sStationCodeAndStartTimeSelection,
                new String[]{stationCode, Long.toString(time)},
                null,
                null,
                sortOrder
        );
    }

    private Cursor getLocation(
            Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        return sLocationQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }
}
