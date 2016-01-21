package me.heron.safefoodscanner.Parse;

import android.util.Log;

import com.google.android.gms.vision.barcode.Barcode;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class ParseAPIAdaptor {

    // TODO: detect network availability

    private static final String TAG = "ParseAPIAdaptor";
    private static ParseAPICallback mParseAPICallback;

    public ParseAPIAdaptor(ParseAPICallback parseAPICallback) {
        mParseAPICallback = parseAPICallback;
    }

    public void checkIsTransFatContained(final Barcode barcode) {

        String barcodeValue = barcode.rawValue;

        Log.d(TAG, "barcode raw value: " + barcodeValue);

        ParseQuery<ParseObject> query = ParseQuery.getQuery("ProductItem");
        query.whereEqualTo("barcode", barcodeValue);
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            public void done(ParseObject productItem, ParseException e) {
                Log.d(TAG, "parse done");
                if (e == null) {

                    boolean isTransFatContained = productItem.getBoolean("isTransFatContained");
                    String name = productItem.getString("name");
                    String parseId = productItem.getObjectId();

                    Log.d(TAG, "name: " + name + ", objectId: " + parseId);

                    mParseAPICallback.checkedIsTransFatContained(isTransFatContained, name, parseId);

                } else {

                    Log.d(TAG, "Error: " + e.getMessage());
                    if (e.getCode() == ParseException.OBJECT_NOT_FOUND) {
                        mParseAPICallback.productNotFound(barcode);
                    }

                }
            }
        });

    }

}
