package com.example.lorenai.mycamapp;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;

import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.preference.PreferenceManager;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.MetadataChangeSet;

public class ColorFinder extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener  {

    private Bitmap original;
    public RelativeLayout outerLayout;
    public TextView titleText;
    public TextView bodyText;
    private ImageView scannedImageView;
    private Button doneButton;
    private Button saveColorDrive;
    private static String drive_email;

    private File mImageFolderB; // albastru
    private File mImageFolderG; // verde
    private File mImageFolderR; // rosu
    private File mImageFolderYO; // galben + portocaliu
    private File mImageFolderPP; //purple pink
    private File mImageFolder; // everything else

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

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        ScanConstants.FOLDER_NAME = preferences.getString("folder_name"," ");
        drive_email = preferences.getString("drive_email", " ");

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
                    } else {
                        colorName= "NoColorFound";
                    }
                }
            }
        });
    }

    private Bitmap getBitmap() {
        Uri uri = getIntent().getExtras().getParcelable("bitmap");
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
                        } else if (colorName == "Green") {
                            createImageFileName(original, mImageFolderG);
                        } else if (colorName == "Blue") {
                            createImageFileName(original, mImageFolderB);
                        } else if (colorName == "Yellow/Orange") {
                            createImageFileName(original, mImageFolderYO);
                        } else if (colorName == "Purple/Pink") {
                            createImageFileName(original, mImageFolderPP);
                        } else {
                            createImageFileName(original, mImageFolder);
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

    private class SaveDriveClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    createFile();
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
        mImageFolder = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),ScanConstants.FOLDER_NAME);

        if (!mImageFolder.exists()) {
            mImageFolder.mkdir();
        }
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
            mImageFolderPP.mkdir();
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

    // GOOGLE DRIVE HERE

    public DriveFile file;
    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "Google Drive Activity";

    private static final  int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;


    public void connectDude() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                    .addApi(Drive.API)
                    .setAccountName(drive_email)
                    .addConnectionCallbacks(this)
                    .addScope(Drive.SCOPE_FILE)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Called whenever the API client fails to connect.
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            return;
        }

        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) { }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    public void createFile() {
        connectDude();
        saveFileToDrive();
       /*
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("Can't connect");
            alertDialog.setMessage("You are not connected with a Google Drive account.");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
     */
    }

    private void saveFileToDrive() {
        // Start by creating a new contents, and setting a callback.
        Log.i(TAG, "Creating new contents.");
        final Bitmap image = ((BitmapDrawable)scannedImageView.getDrawable()).getBitmap();
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                    @Override
                    public void onResult(DriveApi.DriveContentsResult result) {
                        // If the operation was not successful, we cannot do anything
                        // and must
                        // fail.
                        if (!result.getStatus().isSuccess()) {
                            Log.i(TAG, "Failed to create new contents.");
                            return;
                        }
                        // Otherwise, we can write our data to the new contents.
                        Log.i(TAG, "New contents created.");
                        // Get an output stream for the contents.
                        OutputStream outputStream = result.getDriveContents().getOutputStream();
                        // Write the bitmap data from it.
                        ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
                        image.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream);
                        try {
                            outputStream.write(bitmapStream.toByteArray());
                        } catch (IOException e1) {
                            Log.i(TAG, "Unable to write file contents.");
                        }
                        // Create the initial metadata - MIME type and title.
                        // Note that the user will be able to change the title later.
                        MetadataChangeSet metaData = new MetadataChangeSet.Builder().setMimeType("application/vnd.google-apps.folder").setTitle("MyCamApp").build();
                        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder().setMimeType("image/jpeg").setTitle("Android Photo.png").build();
                        // Create an intent for the file chooser, and start it.
                        IntentSender intentSender = Drive.DriveApi
                                .newCreateFileActivityBuilder()
                                .setInitialMetadata(metadataChangeSet)
                                .setInitialDriveContents(result.getDriveContents())
                                .build(mGoogleApiClient);
                        try {
                            startIntentSenderForResult(intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "Failed to launch file chooser.");
                        }
                    }
                });
    }

}
