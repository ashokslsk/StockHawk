# StockHawk
Project 3 of udacity Nano degree project.

Getting Started
---------------
This sample uses the Gradle build system.  To build this project, use the
"gradlew build" command or use "Import Project" in Android Studio.



## User Feedback for Stock Hawk:

* Right now I can't use this app with my screen reader. My friends love it, so I would love to download it, but the buttons don't tell my screen reader what they do.
* We need to prepare Stock Hawk for the Egypt release. Make sure our translators know what to change and make sure the Arabic script will format nicely.
* Stock Hawk allows me to track the current price of stocks, but to track their prices over time, I need to use an external program. It would be wonderful if you could show more detail on a stock, including its price over time.
* I use a lot of widgets on my Android device, and I would love to have a widget that displays my stock quotes on my home screen.
* I found a bug in your app. Right now when I search for a stock quote that doesn't exist, the app crashes.
* When I opened this app for the first time without a network connection, it was a confusing blank screen. I would love a message that tells me why the screen is blank or whether my stock quotes are out of date.

# Rubric

### Required Components

* Each stock quote on the main screen is clickable and leads to a new screen which graphs the stock’s value over time.
* Stock Hawk does not crash when a user searches for a non-existent stock.
* Stock Hawk Stocks can be displayed in a collection widget.
* Stock Hawk app has content descriptions for all buttons.
* Stock Hawk app supports layout mirroring using both the LTR attribute and the start/end tags.
* Strings are all included in the strings.xml file and untranslatable strings have a translatable tag marked to false.
* Stock Hawk displays a default text on screen when offline, to inform users that the list is empty or out of date.


##Code Samples
**Widget Helper codes**

```
public class QuoteWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_collection);

            // Create intent to launch MainActivity
            Intent intent = new Intent(context, MyStocksActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            // Set up collection
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                setRemoteAdapter(context, views);
            } else {
                setRemoteAdapterV11(context, views);
            }

            // Set up collection items
            Intent clickIntentTemplate = new Intent(context, MyStocksActivity.class);
            PendingIntent clickPendingIntentTemplate = TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(clickIntentTemplate)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setPendingIntentTemplate(R.id.widget_list, clickPendingIntentTemplate);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    /**
     * Sets the remote adapter used to fill in the list items
     *
     * @param context the context used to launch the intent
     * @param views RemoteViews to set the RemoteAdapter
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setRemoteAdapter(Context context, @NonNull final RemoteViews views) {
        views.setRemoteAdapter(R.id.widget_list,
                new Intent(context, QuoteWidgetRemoteViewsService.class));
    }

    /**
     * Sets the remote adapter used to fill in the list items
     *
     * @param context the context to launch the intent
     * @param views RemoteViews to set the RemoteAdapter
     */
    @SuppressWarnings("deprecation")
    private void setRemoteAdapterV11(Context context, @NonNull final RemoteViews views) {
        views.setRemoteAdapter(0, R.id.widget_list,
                new Intent(context, QuoteWidgetRemoteViewsService.class));
    }
}

// New class 
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

```

**Internet Connection check**
```
ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
                
        public void networkToast() {
        Snackbar.make(coordinatorLayout, getResources().getString(R.string.network_toast), Snackbar.LENGTH_LONG).show();
//        Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
    }
    
    
```
**Widget Info XML**
```
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:initialLayout="@layout/widget_large"
    android:minHeight="@dimen/widget_default_height"
    android:minWidth="@dimen/widget_default_width"
    android:updatePeriodMillis="0"/>
```

**Check out the repository for more codes**




##Screens
![Screen_after_response](https://github.com/ashokslsk/StockHawk/blob/master/Screens/Screens_1.png)
![Screen_after_toast](https://github.com/ashokslsk/StockHawk/blob/master/Screens/Screen_2.png)
![Screen_after_toast](https://github.com/ashokslsk/StockHawk/blob/master/Screens/Screen_3.png)
![Screen_after_toast](https://github.com/ashokslsk/StockHawk/blob/master/Screens/Screen_4.png)
![Screen_after_toast](https://github.com/ashokslsk/StockHawk/blob/master/Screens/Screen_5.png)


### Required Behavior

* App conforms to common standards found in the Android Nanodegree General Project Guidelines

```
MIT License

Copyright (c) 2016 Ashok Kumar S

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
