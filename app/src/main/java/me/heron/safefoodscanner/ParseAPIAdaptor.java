package me.heron.safefoodscanner;

import android.util.Log;

import com.google.android.gms.vision.barcode.Barcode;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class ParseAPIAdaptor {

    private static final String TAG = "ParseAPIAdaptor";
    private static ParseAPICallback mParseAPICallback;

    ParseAPIAdaptor(ParseAPICallback parseAPICallback) {
        mParseAPICallback = parseAPICallback;
    }

    public void checkIsTransFatContained(final Barcode barcode) {

        String barcodeValue = barcode.rawValue;

        Log.d(TAG, "barcode raw value: " + barcodeValue);

        ParseQuery<ParseObject> query = ParseQuery.getQuery("ProductItem");
        query.whereEqualTo("barcode", barcodeValue);
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            public void done(ParseObject productItem, ParseException e) {
                if (e == null) {
                    Log.d(TAG, "Retrieved " + productItem.get("objectId"));
                    mParseAPICallback.checkedIsTransFatContained(productItem.getBoolean("isTransFatContained"),
                            productItem.getString("name"));
                } else {
                    Log.d(TAG, "Error: " + e.getMessage());
                    if (e.getCode() == ParseException.OBJECT_NOT_FOUND) {
                        mParseAPICallback.productNotFound(barcode);
                    } else {
                    }
                }
            }
        });

    }

}
