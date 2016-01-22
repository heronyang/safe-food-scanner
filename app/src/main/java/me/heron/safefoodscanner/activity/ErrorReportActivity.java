package me.heron.safefoodscanner.activity;

import android.app.ActionBar;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ProgressCallback;
import com.parse.SaveCallback;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import me.heron.safefoodscanner.Constants;
import me.heron.safefoodscanner.Parse.ParseProxyObject;
import me.heron.safefoodscanner.R;

public class ErrorReportActivity extends AppCompatActivity {

    private static final String TAG = Constants.LOG_PREFIX + "ErrorReportAct";

    private static final int REQUEST_IMAGE_CAPTURE = 23;

    private ImageView mImageView;
    private ParseProxyObject productItem;
    private String tempImageFilepath;
    private String barcodeValue;

    private ParseFile uploadedParseImage;

    private enum ResponseIsTransFatContained {
        YES, NO, DONT_KNOW
    }

    private ResponseIsTransFatContained responseIsTransFatContained;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error_report);

        mImageView = (ImageView) findViewById(R.id.imageView);

        setupActionBar();
        getIntentExtras();
        startTakePicture();

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
        productItem = (ParseProxyObject) intent.getSerializableExtra("productItem");
        barcodeValue = intent.getStringExtra("barcodeValue");

    }

    private void startTakePicture() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) == null) {
            return;
        }

        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        if (photoFile != null) {
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                    Uri.fromFile(photoFile));
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Log.d(TAG, "photo file is null");
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            showDialogForIsTransFatContained();

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showDialogForIsTransFatContained() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("")
                .setTitle("您認為這商品含有反式脂肪嗎？");

        builder.setPositiveButton("有的", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                responseIsTransFatContained = ResponseIsTransFatContained.YES;
                uploadErrorReport();
            }
        });

        builder.setNegativeButton("沒有", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                responseIsTransFatContained = ResponseIsTransFatContained.NO;
                uploadErrorReport();
            }
        });

        builder.setNeutralButton("不知道", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                responseIsTransFatContained = ResponseIsTransFatContained.DONT_KNOW;
                uploadErrorReport();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void uploadErrorReport() {
        Bitmap bitmap = readImage();
        mImageView.setImageBitmap(bitmap);
        uploadImage(bitmap);
    }

    private Bitmap readImage() {

        Bitmap bitmap = null;

        try {
            bitmap = BitmapFactory.decodeStream(new java.net.URL(tempImageFilepath).openStream());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "image file read error");
        }

        Log.d(TAG, "current photo path: " + tempImageFilepath);
        return bitmap;

    }

    private void uploadImage(Bitmap bitmap) {

        byte[] image = compressImage(bitmap);

        uploadedParseImage = uploadParseImage(image);
        saveNewUnverifiedProductItem();

    }

    private ParseFile uploadParseImage(byte[] image) {

        ParseFile file = new ParseFile(barcodeValue + ".jpg", image);
        file.saveInBackground(
                new SaveCallback() {
                    public void done(ParseException e) {

                        if (e != null) {
                            Log.d(TAG, e.getMessage());
                        }

                        deleteDeviceSavedImage();
                        showUploadComplete();
                        finish();

                    }
                }, new ProgressCallback() {
                    public void done(Integer percentDone) {
                        Log.d(TAG, "progress callback: " + percentDone);
                        updateProgress(percentDone);
                    }
                }
        );

        return file;

    }

    private void deleteDeviceSavedImage() {

        File file = new File(tempImageFilepath);
        boolean deleted = file.delete();

        if (deleted) {
            Log.d(TAG, "temp file is deleted");
        } else {
            String reason = getReasonForFileDeletionFailure(file);
            Log.e(TAG, "can't delete temp file: " + reason);
        }

    }

    private String getReasonForFileDeletionFailure(File file) {

        try {
            if (!file.exists()) {
                return "It doesn't exist in the first place.";
            } else if (file.isDirectory() && file.list().length > 0) {
                return "It's a directory and it's not empty.";
            } else {
                return "Somebody else has it open, we don't have write permissions, or somebody stole my disk.";
            }
        } catch (SecurityException e) {
            return "Security exception error.";
        }

    }

    private void showUploadComplete() {
        Snackbar.make(findViewById(android.R.id.content), R.string.uploadCompletedText, Snackbar.LENGTH_LONG).show();
    }

    private void updateProgress(Integer percentDone) {

        TextView reportErrorTextView = (TextView) findViewById(R.id.reportErrorTextView);
        reportErrorTextView.setText(getString(R.string.uploadingText) + percentDone + "%");

    }


    private void saveNewUnverifiedProductItem() {

        if (productItem == null) {

            saveNewUnverifiedProductItemWithoutExistingProductItem();

        } else {

            ParseQuery<ParseObject> query = ParseQuery.getQuery("ProductItem");
            query.getInBackground(productItem.getObjectId(), new GetCallback<ParseObject>() {
                public void done(ParseObject existingProductItem, ParseException e) {
                    if (e == null) {

                        saveNewUnverifiedProductItemWithExistingProductItem(existingProductItem);

                    } else {

                        Log.e(TAG, e.getMessage());

                    }
                }
            });

        }

    }

    private void saveNewUnverifiedProductItemWithoutExistingProductItem() {

        ParseObject unverifiedProductItem = new ParseObject("UnverifiedProductItem");

        putGeneralNewUnverifiedProductItemColumns(unverifiedProductItem);
        unverifiedProductItem.saveInBackground();

    }

    private void saveNewUnverifiedProductItemWithExistingProductItem(ParseObject existingProductItem) {

        ParseObject unverifiedProductItem = new ParseObject("UnverifiedProductItem");

        putGeneralNewUnverifiedProductItemColumns(unverifiedProductItem);
        unverifiedProductItem.put("existingProductItem", existingProductItem);

        unverifiedProductItem.saveInBackground();

    }

    private void putGeneralNewUnverifiedProductItemColumns(ParseObject unverifiedProductItem) {

        switch (responseIsTransFatContained) {

            case YES:
                unverifiedProductItem.put("isTransFatContained", true);
                break;
            case NO:
                unverifiedProductItem.put("isTransFatContained", false);
                break;
            case DONT_KNOW:
                break;

        }

        unverifiedProductItem.put("ingredientImage", uploadedParseImage);
        unverifiedProductItem.put("barcode", barcodeValue);

    }

    private byte[] compressImage(Bitmap bitmap) {

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, Constants.COMPRESS_QUALITY, stream);
        return stream.toByteArray();

    }

    private File createImageFile() throws IOException {

        String imageFileName = getString(R.string.tempImageFilename);
        File storageDir = Environment.getExternalStorageDirectory();
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        storeTempImageFilePath(image);

        return image;

    }

    private void storeTempImageFilePath(File image)
    {
        tempImageFilepath = "file:" + image.getAbsolutePath();
        Log.d(TAG, "create image file, get path: " + tempImageFilepath);
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

}
