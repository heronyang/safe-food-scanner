package me.heron.safefoodscanner.Parse;

import com.google.android.gms.vision.barcode.Barcode;
import com.parse.ParseObject;

public interface ParseAPICallback {
    void checkedIsTransFatContained(ParseObject productItem);
    void productNotFound(Barcode barcode);
}
