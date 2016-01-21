package me.heron.safefoodscanner.Parse;

import com.google.android.gms.vision.barcode.Barcode;

public interface ParseAPICallback {
    public void checkedIsTransFatContained(boolean isTransFatContained, String name);
    public void productNotFound(Barcode barcode);
}
