package me.heron.safefoodscanner;

import com.google.android.gms.vision.barcode.Barcode;

public interface BarcodeDetectedCallback {
    void getBarcode(Barcode barcode);
}
