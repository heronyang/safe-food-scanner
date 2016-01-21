package me.heron.safefoodscanner.activity;

import android.app.ActionBar;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import me.heron.safefoodscanner.R;

public class ResultActivity extends AppCompatActivity {

    private static final String TAG = "ResultActivity";

    private boolean isProductNotFound;

    private boolean isSafe;
    private String name;
    private String parseId;

    private static final int REQUEST_CODE_ERROR_REPORT_ACTIVITY = 22;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        getIntentExtras();

        setupActionBar();
        setupContent();

    }

    private void setupActionBar() {

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

    }

    private void getIntentExtras() {

        Intent intent = getIntent();

        isProductNotFound = getIntent().getBooleanExtra("isProductNotFound", false);
        Log.d(TAG, "product not found");

        if (!isProductNotFound) {
            isSafe = intent.getBooleanExtra("isSafe", false);
            name = intent.getStringExtra("name");
            parseId = intent.getStringExtra("parseId");
        }

    }

    private void setupContent() {

        if (isProductNotFound) {
            showNotFoundResultLayout();
        } else {
            showNormalResultLayout();
        }

    }

    private void showNormalResultLayout() {
        String resultText = name + " ";
        if (isSafe) {
            resultText += "Safe";
        } else {
            resultText += "Not Safe";
        }
        TextView resultTextView = (TextView) findViewById(R.id.resultTextView);
        resultTextView.setText(resultText);
    }

    private void showNotFoundResultLayout() {
        setResultTextViewToNotFound();
        hideReportErrorButton();
    }

    private void setResultTextViewToNotFound() {

        String resultText = getString(R.string.productNotFoundText);
        TextView resultTextView = (TextView) findViewById(R.id.resultTextView);
        resultTextView.setText(resultText);

    }

    private void hideReportErrorButton() {

        Button reportErrorButton = (Button) findViewById(R.id.reportErrorButton);
        reportErrorButton.setVisibility(View.GONE);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
        }

        return super.onOptionsItemSelected(item);

    }

    public void scanAgainClickHandler(View view) {
        Log.d(TAG, "scan again clicked");
        finish();
    }

    public void reportErrorClickHandler(View view) {

        Log.d(TAG, "report error clicked");

        Intent intent = new Intent(this, ErrorReportActivity.class);
        intent.putExtra("existingProductItemParseId", parseId);
        startActivityForResult(intent, REQUEST_CODE_ERROR_REPORT_ACTIVITY);

    }
}
