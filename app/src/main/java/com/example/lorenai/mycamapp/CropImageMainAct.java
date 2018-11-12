package com.example.lorenai.mycamapp;

import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.media.MediaScannerConnection;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Environment;

import java.util.Date;
import java.text.SimpleDateFormat;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;

public class CropImageMainAct extends AppCompatActivity implements CropImageView.OnSetImageUriCompleteListener, CropImageView.OnCropImageCompleteListener {

    public Uri savedUri;
    private Bitmap original;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_image_main);

        Intent i = getIntent();
        Uri uri = Uri.parse(i.getStringExtra("uri"));

        CropImage.activity(uri).start(this);
    }

    private void createImageFileName(Bitmap bitmap) throws  IOException {
        File mImageFolder = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ScanConstants.FOLDER_NAME);
        String timestamp = new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date());
        String prepend = "IMAGE" + timestamp + "_";
        File file = File.createTempFile(prepend,".jpg", mImageFolder);
        savedUri = Uri.fromFile(file);
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        MediaScannerConnection.scanFile(this, new String[]{file.toString()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                try {
                    original = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                    createImageFileName(original);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ScanConstants.CROP_URI = resultUri.toString();
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                finish();
            }
        }
    }

    @Override
    public void onCropImageComplete(CropImageView view, CropImageView.CropResult result) { }

    @Override
    public void onSetImageUriComplete(CropImageView view, Uri uri, Exception error) {
        if (error == null) {
            Toast.makeText(this, "Image load successful", Toast.LENGTH_SHORT).show();
        } else {
            Log.e("AIC", "Failed to load image by URI", error);
            Toast.makeText(this, "Image load failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        finish();
    }
}
