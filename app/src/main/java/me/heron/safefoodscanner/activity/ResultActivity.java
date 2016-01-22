package me.heron.safefoodscanner.activity;

import android.app.ActionBar;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import me.heron.safefoodscanner.Constants;
import me.heron.safefoodscanner.Parse.ParseProxyObject;
import me.heron.safefoodscanner.R;

public class ResultActivity extends AppCompatActivity {

    private static final String TAG = Constants.LOG_PREFIX + "ResultAct";

    private boolean isProductNotFound;

    private static final int REQUEST_CODE_ERROR_REPORT_ACTIVITY = 22;
    private ParseProxyObject productItem;
    private String barcodeValue;

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

        isProductNotFound = intent.getBooleanExtra("isProductNotFound", false);
        barcodeValue = intent.getStringExtra("barcodeValue");

        if (!isProductNotFound) {
            productItem = (ParseProxyObject) intent.getSerializableExtra("productItem");
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

        String name = productItem.getString("name");
        boolean isSafe = !productItem.getBoolean("isTransFatContained");

        setResultTextView(name, isSafe);
        setResultImageView(isSafe);

    }

    private void setResultTextView(String name, boolean isSafe) {

        int colorHappy = ContextCompat.getColor(this, R.color.colorHappy);
        int colorSad = ContextCompat.getColor(this, R.color.colorSad);

        TextView resultTextView = (TextView) findViewById(R.id.resultTextView);

        String resultText = name + " ";
        if (isSafe) {
            resultText += getString(R.string.isNotTransFatContainedText);
            resultTextView.setTextColor(colorHappy);
        } else {
            resultText += getString(R.string.isTransFatContainedText);
            resultTextView.setTextColor(colorSad);
        }

        resultTextView.setText(resultText);

    }

    private void setResultImageView(boolean isSafe) {

        ImageView resultImageView = (ImageView) findViewById(R.id.resultImageView);
        if (isSafe) {
            resultImageView.setImageResource(R.drawable.happy_face);
        } else {
            resultImageView.setImageResource(R.drawable.sad_face);
        }

    }

    private void setResultImageViewToNotFound() {

        ImageView resultImageView = (ImageView) findViewById(R.id.resultImageView);
        resultImageView.setImageResource(R.drawable.heart);

    }

    private void showNotFoundResultLayout() {

        setResultTextViewToNotFound();
        setResultImageViewToNotFound();
        setReportButtonText();

    }

    private void setResultTextViewToNotFound() {

        int colorHeart = ContextCompat.getColor(this, R.color.colorHeart);

        String resultText = getString(R.string.productNotFoundText);
        TextView resultTextView = (TextView) findViewById(R.id.resultTextView);
        resultTextView.setText(resultText);
        resultTextView.setTextColor(colorHeart);

    }

    private void setReportButtonText() {

        Button reportErrorButton = (Button) findViewById(R.id.reportErrorButton);
        reportErrorButton.setText(R.string.helpUsButtonText);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
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
        intent.putExtra("productItem", productItem);
        intent.putExtra("barcodeValue", barcodeValue);
        startActivityForResult(intent, REQUEST_CODE_ERROR_REPORT_ACTIVITY);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d(TAG, "onActivityResult, get code: " + REQUEST_CODE_ERROR_REPORT_ACTIVITY);

        if (requestCode == REQUEST_CODE_ERROR_REPORT_ACTIVITY) {
            finish();
        }

        super.onActivityResult(requestCode, resultCode, data);

    }

}
