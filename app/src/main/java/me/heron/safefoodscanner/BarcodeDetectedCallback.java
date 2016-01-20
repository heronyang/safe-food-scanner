package me.heron.safefoodscanner;

import com.google.android.gms.vision.barcode.Barcode;

/**
 * Created by heron on 1/20/16.
 */
public interface BarcodeDetectedCallback {
    void barcodeDetectedCallback(Barcode barcode);
}
