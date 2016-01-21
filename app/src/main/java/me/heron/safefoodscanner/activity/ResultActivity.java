package me.heron.safefoodscanner.activity;

import android.app.ActionBar;
import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.api.CommonStatusCodes;

import me.heron.safefoodscanner.R;

public class ResultActivity extends AppCompatActivity {

    private static final String TAG = "ResultActivity";

    private boolean isSafe;
    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        isSafe = getIntent().getBooleanExtra("isSafe", false);
        name = getIntent().getStringExtra("name");

        setupActionBar();
        showResultLayout();

    }

    private void showResultLayout() {
        String resultText = name + " ";
        if (isSafe) {
            resultText += "Safe";
        } else {
            resultText += "Not Safe";
        }
        TextView resultTextView = (TextView) findViewById(R.id.textView);
        resultTextView.setText(resultText);
    }

    private void setupActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
        }

        return super.onOptionsItemSelected(item);

    }
}
