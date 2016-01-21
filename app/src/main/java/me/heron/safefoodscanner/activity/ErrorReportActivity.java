package me.heron.safefoodscanner.activity;

import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ProgressCallback;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import me.heron.safefoodscanner.Parse.ParseProxyObject;
import me.heron.safefoodscanner.R;

public class ErrorReportActivity extends AppCompatActivity {

    private static final String TAG = "ErrorReportActivity";

    private static final int REQUEST_IMAGE_CAPTURE = 23;

    private ImageView mImageView;
    private ParseProxyObject productItem;

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

    }

    private void startTakePicture() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

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

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            /*
            Bundle extras = data.getExtras();
            Bitmap bitmap = (Bitmap) extras.get("data");
            mImageView.setImageBitmap(bitmap);
            uploadImage(bitmap);
            */
            Log.d(TAG, "photo saved");
            readImage();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void readImage() {
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(new java.net.URL(mCurrentPhotoPath).openStream());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "image file read error");
        }
        mImageView.setImageBitmap(bitmap);
        Log.d(TAG, "current photo path: " + mCurrentPhotoPath);
    }

    private void uploadImage(Bitmap bitmap) {

        byte[] image = compressImage(bitmap);

        ParseFile file = new ParseFile("tmp.jpg", image);
        file.saveInBackground(
                new SaveCallback() {
                    public void done(ParseException e) {
                        Log.d(TAG, "done " + e.getMessage());
                        finish();
                    }
                }, new ProgressCallback() {
                    public void done(Integer percentDone) {
                        Log.d(TAG, "progress callback: " + percentDone);
                    }
                }
        );

        ParseObject unverifiedProductItem = new ParseObject("UnverifiedProductItem");

        unverifiedProductItem.put("isTransFatContained", false);
        unverifiedProductItem.put("ingredientImage", file);
        /*unverifiedProductItem.put("existingProductItem", productItem);*/
        unverifiedProductItem.saveInBackground();

    }

    private byte[] compressImage(Bitmap bitmap) {

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        return stream.toByteArray();

    }

    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "SafeFoodScanner_" + timeStamp + "_";
        File storageDir = Environment.getExternalStorageDirectory();
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        Log.d(TAG, "create image file, get path: " + mCurrentPhotoPath);
        return image;

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
