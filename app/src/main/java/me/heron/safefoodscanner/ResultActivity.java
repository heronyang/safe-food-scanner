package me.heron.safefoodscanner;

import android.app.ActionBar;
import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.api.CommonStatusCodes;

public class ResultActivity extends AppCompatActivity {

    private static final String TAG = "ResultActivity";

    private boolean isSafe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        isSafe = getIntent().getBooleanExtra("isSafe", false);
        showResultLayout();

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

    }

    private void showResultLayout() {
        TextView resultText = (TextView) findViewById(R.id.textView);
        resultText.setText(isSafe? "Safe" : "Not Safe");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                // NOTE: one can pass extra back to main activity
                finish();
        }

        return super.onOptionsItemSelected(item);

    }
}
