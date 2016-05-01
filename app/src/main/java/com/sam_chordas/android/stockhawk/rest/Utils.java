package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.util.Log;

import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

  private static String LOG_TAG = Utils.class.getSimpleName();

  public static boolean showPercent = true;
  private static final String KEY_QUERY = "query";
  private static final String KEY_COUNT = "count";
  private static final String KEY_RESULTS = "results";
  private static final String KEY_QUOTE = "quote";
  private static final String KEY_CHANGE = "Change";
  private static final String KEY_CHANGE_PERCENTAGE = "ChangeinPercent";
  private static final String KEY_BID = "Bid";

  public static ArrayList quoteJsonToContentVals(String JSON){
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    JSONObject jsonObject = null;
    JSONArray resultsArray = null;
    Log.i(LOG_TAG, "GET FB: " +JSON);
    try{
      jsonObject = new JSONObject(JSON);
      if (jsonObject != null && jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject(KEY_QUERY);
        int count = Integer.parseInt(jsonObject.getString(KEY_COUNT));
        if (count == 1){
          jsonObject = jsonObject.getJSONObject(KEY_RESULTS)
                  .getJSONObject(KEY_QUOTE);
          ContentProviderOperation obj =  buildBatchOperation(jsonObject);
          if(obj!=null)
            batchOperations.add(obj);
        } else{
          resultsArray = jsonObject.getJSONObject(KEY_RESULTS).getJSONArray(KEY_QUOTE);

          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = 0; i < resultsArray.length(); i++){
              jsonObject = resultsArray.getJSONObject(i);
              batchOperations.add(buildBatchOperation(jsonObject));
            }
          }
        }
      }
    } catch (JSONException e){
      Log.e(LOG_TAG, "String to JSON failed: " + e);
    }
    return batchOperations;
  }

  public static String truncateBidPrice(String bidPrice){
    bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
    return bidPrice;
  }

  public static String truncateChange(String change, boolean isPercentChange){
    String weight = change.substring(0,1);
    String ampersand = "";
    if (isPercentChange){
      ampersand = change.substring(change.length() - 1, change.length());
      change = change.substring(0, change.length() - 1);
    }
    change = change.substring(1, change.length());
    double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
    change = String.format("%.2f", round);
    StringBuffer changeBuffer = new StringBuffer(change);
    changeBuffer.insert(0, weight);
    changeBuffer.append(ampersand);
    change = changeBuffer.toString();
    return change;
  }

  public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) {
    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
            QuoteProvider.Quotes.CONTENT_URI);
    String changes = null;
    try {
      changes = jsonObject.getString(KEY_CHANGE);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    Log.d("values", changes);
    if (changes != null && changes != "null") {
      try {
        String change = jsonObject.getString(KEY_CHANGE);
        builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString(QuoteColumns.SYMBOL));
        builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString(KEY_BID)));
        builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                jsonObject.getString(KEY_CHANGE_PERCENTAGE), true));
        builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
        builder.withValue(QuoteColumns.ISCURRENT, 1);
        if (change.charAt(0) == '-') {
          builder.withValue(QuoteColumns.ISUP, 0);
        } else {
          builder.withValue(QuoteColumns.ISUP, 1);
        }

      } catch (JSONException e) {
        e.printStackTrace();
      }
      return builder.build();
    }
    return null;
  }
}