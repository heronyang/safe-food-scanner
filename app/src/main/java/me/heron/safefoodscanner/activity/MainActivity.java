package me.heron.safefoodscanner.activity;

/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.parse.ParseObject;

import java.io.IOException;

import me.heron.safefoodscanner.Constants;
import me.heron.safefoodscanner.Parse.ParseAPIAdaptor;
import me.heron.safefoodscanner.Parse.ParseAPICallback;
import me.heron.safefoodscanner.Parse.ParseProxyObject;
import me.heron.safefoodscanner.PlayServicesUtils;
import me.heron.safefoodscanner.R;
import me.heron.safefoodscanner.barcode.BarcodeDetectedCallback;
import me.heron.safefoodscanner.barcode.BarcodeTrackerFactory;
import me.heron.safefoodscanner.ui.camera.CameraSource;
import me.heron.safefoodscanner.ui.camera.CameraSourcePreview;

public class MainActivity extends AppCompatActivity implements BarcodeDetectedCallback, ParseAPICallback {

    private static final String TAG = Constants.LOG_PREFIX + "MainAct";

    private static final int RC_HANDLE_CAMERA_PERMISSION = 2;
    private static final int REQUEST_CODE_RESULT_ACTIVITY = 20;

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;

    private ParseAPIAdaptor parseAPIAdaptor;

    private boolean isFreeForChecking = false;
    private String barcodeValue;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (!PlayServicesUtils.checkPlayServices(this)) {
            return;
        }

        setupCameraPermission();
        setupHelpers();
        setupLayout();

        checkNetworkAvailable();

    }


    private void setupLayout() {

        setContentView(R.layout.barcode_capture);
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);

        showHint();

    }

    private void showHint() {
        Snackbar.make(findViewById(android.R.id.content), R.string.scanBarcodeText, Snackbar.LENGTH_INDEFINITE).show();
    }

    private void showLoading() {
        Snackbar.make(findViewById(android.R.id.content), R.string.loadingWaitText, Snackbar.LENGTH_INDEFINITE).show();
    }

    private void setupCameraPermission() {

        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }

    }

    private void setupHelpers() {

        parseAPIAdaptor = new ParseAPIAdaptor(this);

    }

    private void checkNetworkAvailable() {
        if (!isNetworkAvailable()) {
            showNoNetworkError();
        }
    }

    private boolean isNetworkAvailable() {

        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();

    }

    private void requestCameraPermission() {

        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERMISSION);
        }

    }

    private void createCameraSource() {

        BarcodeDetector barcodeDetector = setupBarcodeDetector();
        setupCamera(barcodeDetector);

    }

    private BarcodeDetector setupBarcodeDetector() {

        Context context = getApplicationContext();

        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context).build();
        BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(this);
        barcodeDetector.setProcessor(
                new MultiProcessor.Builder<>(barcodeFactory).build());

        if (!barcodeDetector.isOperational()) {

            Log.w(TAG, "Detector dependencies are not yet available.");
            checkLowStorage();

        }

        return barcodeDetector;

    }

    private void setupCamera(BarcodeDetector barcodeDetector) {

        CameraSource.Builder builder = new CameraSource.Builder(getApplicationContext(), barcodeDetector);

        mCameraSource = builder
                .setFlashMode(Camera.Parameters.FLASH_MODE_OFF)
                .setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO)
                .setRequestedFps(Constants.BARCODE_DEFAULT_REQUESTED_FPS)
                .setRequestedPreviewSize(Constants.BARCODE_DEFAULT_PICTURE_WIDTH, Constants.BARCODE_DEFAULT_PICTURE_HEIGHT)
                .setFacing(Camera.CameraInfo.CAMERA_FACING_BACK)
                .build();
        mCameraSource.autoFocus(null);

    }

    private void checkLowStorage() {

        IntentFilter lowStorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
        boolean hasLowStorage = registerReceiver(null, lowStorageFilter) != null;

        if (hasLowStorage) {
            Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
        }

    }

    @Override
    protected void onResume() {

        super.onResume();

        if (!PlayServicesUtils.checkPlayServices(this)) {
            return;
        }

        startCameraSource();
        showHint();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERMISSION) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        showNoCameraPermissionError();

    }

    private void showNoCameraPermissionError() {

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.error_dialog_title)
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();

    }

    private void showNoNetworkError() {

        final Activity activity = this;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(R.string.error_dialog_title)
                        .setMessage(R.string.no_network_connection)
                        .setPositiveButton(R.string.ok, listener)
                        .show();

            }
        });

    }

    private void startCameraSource() throws SecurityException {

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }

    }

    @Override
    public void getBarcode(Barcode barcode) {

        if (!isFreeForChecking) {

            freeLayoutForChecking();
            showLoading();
            buzz();

            saveBarcodeValue(barcode);

            checkNetworkAvailable();
            analysisBarcode(barcode);

        }

    }

    private void freeLayoutForChecking() {
        isFreeForChecking = true;
    }

    private void stopFreeLayoutForChecking() {
        isFreeForChecking = false;
    }

    private void buzz() {
        Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(Constants.BUZZ_LENGTH_MILLISECOND);
    }

    private void saveBarcodeValue(Barcode barcode) {
        barcodeValue = barcode.rawValue;
    }

    private void analysisBarcode(Barcode barcode) {

        parseAPIAdaptor.checkIsTransFatContained(barcode);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_CODE_RESULT_ACTIVITY) {
            stopFreeLayoutForChecking();
        }

        super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    public void checkedIsTransFatContained(ParseObject productItem) {

        startResultLayout(productItem);

    }

    private void startResultLayout(ParseObject productItem) {

        ParseProxyObject productItemProxy = new ParseProxyObject(productItem);

        Intent intent = new Intent(this, ResultActivity.class);

        intent.putExtra("isProductNotFound", false);
        intent.putExtra("productItem", productItemProxy);
        intent.putExtra("barcodeValue", barcodeValue);

        startActivityForResult(intent, REQUEST_CODE_RESULT_ACTIVITY);

    }

    @Override
    public void productNotFound(Barcode barcode) {

        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("isProductNotFound", true);
        intent.putExtra("barcodeValue", barcodeValue);
        startActivityForResult(intent, REQUEST_CODE_RESULT_ACTIVITY);

    }

}

