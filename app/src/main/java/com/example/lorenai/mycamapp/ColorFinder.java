package com.example.lorenai.mycamapp;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;

import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.support.v7.graphics.Palette;
import android.support.v7.app.AppCompatActivity;

public class ColorFinder extends AppCompatActivity {

    private Bitmap original;
    public RelativeLayout outerLayout;
    public TextView titleText;
    public TextView bodyText;
    private ImageView scannedImageView;
    private Button doneButton;
    private Button saveColorDrive;

    private File mImageFolderB; // albastru
    private File mImageFolderG; // verde
    private File mImageFolderR; // rosu
    private File mImageFolderYO; // galben + portocaliu
    private File mImageFolderPP; //purple pink

    static public float[] color ;
    static public String colorName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_finder);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        init();
    }

    public void init() {
        outerLayout = (RelativeLayout) findViewById(R.id.layout);
        titleText = (TextView) findViewById(R.id.title);
        bodyText = (TextView) findViewById(R.id.body);
        scannedImageView = (ImageView) findViewById(R.id.theImage);
        doneButton = (Button) findViewById(R.id.saveButton);
        doneButton.setOnClickListener(new ColorFinder.SaveButtonClickListener());
        saveColorDrive = (Button) findViewById(R.id.saveColorDrive);
        saveColorDrive.setOnClickListener(new ColorFinder.SaveDriveClickListener());
        original = getBitmap();

        scannedImageView.setImageBitmap(original);

        createImageFolder();

        Palette.from(original).generate(new Palette.PaletteAsyncListener() {
            public void onGenerated(Palette palette) {
                Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
                if (vibrantSwatch != null) {
                    outerLayout.setBackgroundColor(vibrantSwatch.getRgb());
                    color = vibrantSwatch.getHsl();
                    titleText.setTextColor(vibrantSwatch.getTitleTextColor());
                    bodyText.setTextColor(vibrantSwatch.getBodyTextColor());

                    if (color[0] >= 63 && color[0] <= 179) { // Green
                        bodyText.setText("Green");
                        colorName = "Green";
                    } else if (color[0] >= 180 && color[0] <= 252) { // Blue
                        bodyText.setText("Blue");
                        colorName = "Blue";
                    } else if (color[0] >= 0  && color[0] <= 11) { // Red Ver. 1
                        bodyText.setText("Red");
                        colorName = "Red";
                    } else if (color[0] >= 353 && color[0] <= 360) {  // Red Ver. 2
                        bodyText.setText("Red");
                        colorName = "Red";
                    } else if (color[0] >= 12 && color[0] <= 62 ) { // Yellow&Orange
                        bodyText.setText("Yellow/Orange");
                        colorName = "Yellow/Orange";
                    } else if (color[0] > 252 && color[0] <= 352) { // Purple&Pink
                        bodyText.setText("Purple/Pink");
                        colorName = "Purple/Pink";
                    } else { // Everything else
                        bodyText.setText("No color found");
                        colorName = "No color found";
                    }
                }
            }
        });
    }

    private Bitmap getBitmap() {
        Uri uri = getIntent().getExtras().getParcelable("bitmap");;
        try {
            original = Util.getBitmap(this, uri);
            this.getContentResolver().delete(uri, null, null);
            return original;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class SaveButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (colorName == "Red") {
                            createImageFileName(original, mImageFolderR);
                        }
                        if (colorName == "Green") {
                            createImageFileName(original, mImageFolderG);
                        }
                        if (colorName == "Blue") {
                            createImageFileName(original, mImageFolderB);
                        }
                        if (colorName == "Yellow/Orange") {
                            createImageFileName(original, mImageFolderYO);
                        }
                        if (colorName == "Purple/Pink") {
                            createImageFileName(original, mImageFolderPP);
                        }
                        original.recycle();
                        System.gc();
                        finish();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public void createDrive() {

    }

    private class SaveDriveClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        original.recycle();
                        System.gc();
                        finish();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void createImageFolder() {
        mImageFolderR = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"MyCamAppRed");
        mImageFolderB = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"MyCamAppBlue");
        mImageFolderG = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"MyCamAppGreen");
        mImageFolderYO = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"MyCamAppYellow&Orange");
        mImageFolderPP = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"MyCamAppPurple&Pink");

        if (!mImageFolderB.exists()) {
            mImageFolderB.mkdir();
        }
        if (!mImageFolderG.exists()) {
            mImageFolderG.mkdir();
        }
        if (!mImageFolderR.exists()) {
            mImageFolderR.mkdir();
        }
        if (!mImageFolderYO.exists()) {
            mImageFolderYO.mkdir();
        }
        if (!mImageFolderPP.exists()) {
            mImageFolderB.mkdir();
        }
    }

    private void createImageFileName(Bitmap bitmap, File folder) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date());
        String prepend = "IMAGE" + timestamp + "_";
        File file = File.createTempFile(prepend, ".jpg", folder);
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90 , out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        MediaScannerConnection.scanFile(getApplicationContext(), new String[]{file.toString()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
