package me.heron.safefoodscanner.Parse;

import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.vision.barcode.Barcode;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import me.heron.safefoodscanner.Constants;

public class ParseAPIAdaptor {

    // TODO: detect network availability

    private static final String TAG = Constants.LOG_PREFIX + "ParseAPIAdaptor";
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
                if (e == null) {

                    mParseAPICallback.checkedIsTransFatContained(productItem);

                } else {

                    Log.e(TAG, "Error: " + e.getMessage());
                    Log.e(TAG, "Error: code -> " + e.getCode());
                    if (e.getCode() == ParseException.OBJECT_NOT_FOUND) {
                        mParseAPICallback.productNotFound(barcode);
                    } else if (e.getCode() == ParseException.TIMEOUT) {
                        Log.e(TAG, "Network Is Down");
                    }

                }
            }
        });

    }

}
