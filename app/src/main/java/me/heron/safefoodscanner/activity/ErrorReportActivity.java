package me.heron.safefoodscanner.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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

import me.heron.safefoodscanner.R;

public class ErrorReportActivity extends AppCompatActivity {

    private static final String TAG = "ErrorReportActivity";

    private static final int REQUEST_IMAGE_CAPTURE = 23;

    private ImageView mImageView;
    private String existingProductItemParseId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error_report);

        mImageView = (ImageView) findViewById(R.id.imageView);

        getIntentExtras();
        startTakePicture();

    }

    private void getIntentExtras() {

        Intent intent = getIntent();
        existingProductItemParseId = intent.getStringExtra("existingProductItemParseId");

    }

    private void startTakePicture() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, ex.getMessage());
            }

            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap bitmap = (Bitmap) extras.get("data");
            mImageView.setImageBitmap(bitmap);
            uploadImage(bitmap);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploadImage(Bitmap bitmap) {

        byte[] image = compressImage(bitmap);

        ParseFile file = new ParseFile("tmp.png", image);
        file.saveInBackground(
                new SaveCallback() {
                    public void done(ParseException e) {
                        Log.d(TAG, "done " + e.getMessage());
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
        unverifiedProductItem.put("existingProductItem", existingProductItemParseId);
        unverifiedProductItem.saveInBackground();

    }

    private byte[] compressImage(Bitmap bitmap) {

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();

    }

    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

}
