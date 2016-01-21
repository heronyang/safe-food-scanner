package me.heron.safefoodscanner.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import me.heron.safefoodscanner.R;

public class ErrorReportActivity extends AppCompatActivity {

    private static final String TAG = "ErrorReportActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error_report);

        Intent intent = getIntent();
        String existingProductItemParseId = intent.getStringExtra("existingProductItemParseId");
        Log.d(TAG, "existing product item parse id: " + existingProductItemParseId);
    }

}
