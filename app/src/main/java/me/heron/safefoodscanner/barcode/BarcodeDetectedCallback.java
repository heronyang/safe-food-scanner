package me.heron.safefoodscanner.barcode;

import com.google.android.gms.vision.barcode.Barcode;

public interface BarcodeDetectedCallback {
    void getBarcode(Barcode barcode);
}
