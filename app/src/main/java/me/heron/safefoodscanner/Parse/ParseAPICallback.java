package me.heron.safefoodscanner.Parse;

import com.google.android.gms.vision.barcode.Barcode;
import com.parse.ParseObject;

public interface ParseAPICallback {
    public void checkedIsTransFatContained(ParseObject productItem);
    public void productNotFound(Barcode barcode);
}
