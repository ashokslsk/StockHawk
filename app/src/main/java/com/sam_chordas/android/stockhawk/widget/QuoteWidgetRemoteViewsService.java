package com.sam_chordas.android.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

/**
 * Created by yogeshmadaan on 09/03/16.
 */
public class QuoteWidgetRemoteViewsService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;
            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }

                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();

                // This is the same query from MyStocksActivity
                data = getContentResolver().query(
                        QuoteProvider.Quotes.CONTENT_URI,
                        new String[] {
                                QuoteColumns._ID,
                                QuoteColumns.SYMBOL,
                                QuoteColumns.BIDPRICE,
                                QuoteColumns.PERCENT_CHANGE,
                                QuoteColumns.CHANGE,
                                QuoteColumns.ISUP
                        },
                        QuoteColumns.ISCURRENT + " = ?",
                        new String[]{"1"},
                        null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {

            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }

                // Get the layout
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_collection_item);

                // Bind data to the views
                views.setTextViewText(R.id.stock_symbol, data.getString(data.getColumnIndex
                        (getResources().getString(R.string.string_symbol))));

                if (data.getInt(data.getColumnIndex(QuoteColumns.ISUP)) == 1) {
                    views.setInt(R.id.change, getResources().getString(R.string.string_set_background_resource), R.drawable.percent_change_pill_green);
                } else {
                    views.setInt(R.id.change, getResources().getString(R.string.string_set_background_resource), R.drawable.percent_change_pill_red);
                }

                if (Utils.showPercent) {
                    views.setTextViewText(R.id.change, data.getString(data.getColumnIndex(QuoteColumns.PERCENT_CHANGE)));
                } else {
                    views.setTextViewText(R.id.change, data.getString(data.getColumnIndex(QuoteColumns.CHANGE)));
                }

                final Intent fillInIntent = new Intent();
                fillInIntent.putExtra(getResources().getString(R.string.string_symbol), data.getString(data.getColumnIndex(QuoteColumns.SYMBOL)));
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);

                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return null; // use the default loading view
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                // Get the row ID for the view at the specified position
                if (data != null && data.moveToPosition(position)) {
                    final int QUOTES_ID_COL = 0;
                    return data.getLong(QUOTES_ID_COL);
                }
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}