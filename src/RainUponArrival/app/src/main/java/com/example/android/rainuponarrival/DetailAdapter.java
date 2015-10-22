package com.example.android.rainuponarrival;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * {@link DetailAdapter} exposes a list of weather forecasts
 * from a {@link Cursor} to a {@link android.widget.ListView}.
 */
public class DetailAdapter extends CursorAdapter {
    private final String LOG_TAG = DetailAdapter.class.getSimpleName();

//    private final int VIEW_TYPE_COUNT = 2;
//    private final int VIEW_TYPE_DEPARTURE = 0;
//    private final int VIEW_TYPE_DESTINATION = 1;

    /**
     * Cache of the children views for a forecast list item.
     */
    public static class ViewHolder {
        public final ImageView iconView;
        public final TextView timeView;
        public final TextView rainfallView;

        public ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.detail_icon);
            timeView =(TextView) view.findViewById(R.id.detail_time);
            rainfallView =(TextView) view.findViewById(R.id.detail_rainfall);
        }
    }

    public DetailAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

//    @Override
//    public int getItemViewType(int position) {
//        return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
//    }

//    @Override
//    public int getViewTypeCount() {
//        return VIEW_TYPE_COUNT;
//    }

    /*
        Remember that these views are reused as needed.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_detail, parent, false);
        ViewHolder holder = new ViewHolder(view);
        view.setTag(holder);
        return view;
    }

//    /*
//        This is where we fill-in the views with the contents of the cursor.
//     */
//    @Override
//    public void bindView(View view, Context context, Cursor cursor) {
//        // our view is pretty simple here --- just a text view
//        // we'll keep the UI functional with a simple (and slow!) binding.
//
//        TextView tv = (TextView)view;
//        tv.setText(convertCursorRowToUXFormat(cursor));
//    }

    /*
    This is where we fill-in the views with the contents of the cursor.
 */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        // Read weather icon ID from cursor
        long time = cursor.getLong(DetailFragment.COL_WEATHER_DATE);
        double rainfall = cursor.getDouble(DetailFragment.COL_RAINFALL);
        viewHolder.timeView.setText(Utility.formatTimeForDisplay(time));
        viewHolder.rainfallView.setText(Utility.formatRainfall(context, rainfall));
        viewHolder.iconView.setImageResource(Utility.getIconResouceId(rainfall));
    }
}
