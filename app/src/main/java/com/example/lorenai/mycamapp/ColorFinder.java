package com.example.lorenai.mycamapp;

import java.util.Date;
import java.text.SimpleDateFormat;

import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.os.StrictMode;
import android.support.v7.graphics.Palette;
import android.media.MediaScannerConnection;
import android.preference.PreferenceManager;

import android.content.Intent;
import android.content.DialogInterface;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import android.os.Bundle;
import android.os.AsyncTask;
import android.os.Environment;

import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.SharedPreferences;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;

import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.GoogleApiAvailability;
import com.microsoft.onedrivesdk.saver.ISaver;
import com.microsoft.onedrivesdk.saver.Saver;
import com.microsoft.onedrivesdk.saver.SaverException;

public class ColorFinder extends ConnectDriveService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public Uri savedUri;
    public RelativeLayout outerLayout;

    private Bitmap original;
    private ImageView scannedImageView;

    public TextView bodyText;
    public TextView titleText;

    private int bodyTextOriginal;
    private int titleTextOriginal;
    private int outerLayoutOriginal;

    private Button doneButton;
    private Button saveColorDrive;
    private Button changeRed;
    private Button changeGreen;
    private Button changeBlue;
    private Button changeYO;
    private Button changePP;
    private Button changeReset;

    private File mImageFolderB; // Blue
    private File mImageFolderG; // Green
    private File mImageFolderR; // Red
    private File mImageFolderYO; // Yellow&Orange
    private File mImageFolderPP; // Purple&Pink
    private File mImageFolder; // Everything else(Default)

    static public float[] color;
    static public String colorName;
    static public String colorNameOriginal;

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
        changeRed = (Button) findViewById(R.id.changeRed);
        changeRed.setOnClickListener(new ColorFinder.ChangeRedColorListener());
        changeGreen = (Button) findViewById(R.id.changeGreen);
        changeGreen.setOnClickListener(new ColorFinder.ChangeGreenColorListener());
        changeBlue = (Button) findViewById(R.id.changeBlue);
        changeBlue.setOnClickListener(new ColorFinder.ChangeBlueColorListener());
        changeYO = (Button) findViewById(R.id.changeYellowOrange);
        changeYO.setOnClickListener(new ColorFinder.ChangeYOColorListener());
        changePP = (Button) findViewById(R.id.changePurplePink);
        changePP.setOnClickListener(new ColorFinder.ChangePPColorListener());
        changeReset = (Button) findViewById(R.id.changeResetColor);
        changeReset.setOnClickListener(new ColorFinder.ChangeResetColorListener());

        original = getBitmap();
        scannedImageView.setImageBitmap(original);

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_drive, false);

        createImageFolder();

        Palette.from(original).generate(new Palette.PaletteAsyncListener() {
            public void onGenerated(Palette palette) {
                Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
                if (vibrantSwatch != null) {
                    titleTextOriginal = vibrantSwatch.getTitleTextColor();
                    bodyTextOriginal = vibrantSwatch.getBodyTextColor();
                    outerLayoutOriginal = vibrantSwatch.getRgb();

                    outerLayout.setBackgroundColor(outerLayoutOriginal);
                    color = vibrantSwatch.getHsl();
                    titleText.setTextColor(titleTextOriginal);
                    bodyText.setTextColor(bodyTextOriginal);

                    if (color[0] >= 63 && color[0] <= 179) { // Green
                        bodyText.setText("Green");
                        colorName = "Green";
                        colorNameOriginal = colorName;
                    } else if (color[0] >= 180 && color[0] <= 252) { // Blue
                        bodyText.setText("Blue");
                        colorName = "Blue";
                        colorNameOriginal = colorName;
                    } else if (color[0] >= 0 && color[0] <= 11) { // Red Ver. 1
                        bodyText.setText("Red");
                        colorName = "Red";
                        colorNameOriginal = colorName;
                    } else if (color[0] >= 353 && color[0] <= 360) {  // Red Ver. 2
                        bodyText.setText("Red");
                        colorName = "Red";
                        colorNameOriginal = colorName;
                    } else if (color[0] >= 12 && color[0] <= 62) { // Yellow&Orange
                        bodyText.setText("Yellow/Orange");
                        colorName = "Yellow/Orange";
                        colorNameOriginal = colorName;
                    } else if (color[0] > 252 && color[0] <= 352) { // Purple&Pink
                        bodyText.setText("Purple/Pink");
                        colorName = "Purple/Pink";
                        colorNameOriginal = colorName;
                    } else {
                        colorName = "NoColorFound";
                        colorNameOriginal = colorName;
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
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        Boolean sendEmail = preferences.getBoolean("send_email", false);

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

                        if (sendEmail == true) {
                            Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                            emailIntent.setType("plain/text");
                            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "MyCamApp Email");
                            emailIntent.putExtra(Intent.EXTRA_TEXT, "Main Color: " + colorName);
                            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                            StrictMode.setVmPolicy(builder.build());
                            if (savedUri != null) {
                                emailIntent.putExtra(Intent.EXTRA_STREAM, savedUri);
                            }
                            startActivity(emailIntent);
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
            createFile();
        }
    }

    private void createImageFolder() {
        mImageFolderR = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyCamAppRed");
        mImageFolderB = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyCamAppBlue");
        mImageFolderG = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyCamAppGreen");
        mImageFolderYO = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyCamAppYellow&Orange");
        mImageFolderPP = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyCamAppPurple&Pink");
        mImageFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ScanConstants.FOLDER_NAME);

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
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
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

        savedUri = Uri.fromFile(file);

    }

    private class ChangeResetColorListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            bodyText.setText(colorNameOriginal);
            colorName = colorNameOriginal;
            outerLayout.setBackgroundColor(outerLayoutOriginal);
            titleText.setTextColor(titleTextOriginal);
            bodyText.setTextColor(bodyTextOriginal);
        }
    }

    private class ChangeRedColorListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            bodyText.setText("Red");
            colorName = "Red";
            outerLayout.setBackgroundColor(getResources().getColor(R.color.light_red));
            titleText.setTextColor(getResources().getColor(R.color.black));
            bodyText.setTextColor(getResources().getColor(R.color.red));
        }
    }

    private class ChangeGreenColorListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            bodyText.setText("Green");
            colorName = "Green";
            outerLayout.setBackgroundColor(getResources().getColor(R.color.light_green));
            titleText.setTextColor(getResources().getColor(R.color.black));
            bodyText.setTextColor(getResources().getColor(R.color.green));

        }
    }

    private class ChangeBlueColorListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            bodyText.setText("Blue");
            colorName = "Blue";
            outerLayout.setBackgroundColor(getResources().getColor(R.color.blue));
            titleText.setTextColor(getResources().getColor(R.color.black));
            bodyText.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        }
    }

    private class ChangeYOColorListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            bodyText.setText("Yellow/Orange");
            colorName = "Yellow/Orange";
            outerLayout.setBackgroundColor(getResources().getColor(R.color.yellow_orange));
            titleText.setTextColor(getResources().getColor(R.color.black));
            bodyText.setTextColor(getResources().getColor(R.color.just_orange));
        }
    }

    private class ChangePPColorListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            bodyText.setText("Purple/Pink");
            colorName = "Purple/Pink";
            outerLayout.setBackgroundColor(getResources().getColor(R.color.pink_purple));
            titleText.setTextColor(getResources().getColor(R.color.black));
            bodyText.setTextColor(getResources().getColor(R.color.just_purple));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (one_saver == true) {
            try {
                mSaver.handleSave(requestCode, resultCode, data);
                Toast.makeText(this, "File saved to OneDrive!", Toast.LENGTH_LONG).show();
                one_saver = false;
            } catch (final SaverException e) {
                Log.e("OneDriveSaver", e.getErrorType().toString());
                Toast.makeText(this, "OneDrive saving failed!", Toast.LENGTH_LONG).show();
                one_saver = false;
                e.getDebugErrorInfo();
            }
        }
    }

    // Drive Options Here

    public DriveFile file;

    private GoogleApiClient mGoogleApiClient;

    private String drive_email;

    private static final int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;

    private ISaver mSaver;
    private static boolean one_saver = false;
    final static String ONEDRIVE_Client_ID = "2894a74e-f2c9-483e-b437-f6730a18c068";

    public void saveFileToOneDrive() {
        one_saver = true;

        final Bitmap image = ((BitmapDrawable) scannedImageView.getDrawable()).getBitmap();
        String timestamp = new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date());
        String prepend = "MyCamApp" + "_" + timestamp;

        mSaver = Saver.createSaver(ONEDRIVE_Client_ID);
        mSaver.startSaving(this, prepend, getImageUri(this, image) );
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public void connectDude() {
        if (drive_email.isEmpty()) {
            Log.i(TAG, "There is no email given");
        } else {
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
    }

    public void createFile() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        ScanConstants.FOLDER_NAME = preferences.getString("folder_name", " ");
        drive_email = preferences.getString("drive_email", " ");
        String driveOption = preferences.getString("choosing_drive", " ");
        if (driveOption.equalsIgnoreCase("Google Drive")) {
            connectDude();
            if (drive_email.isEmpty()) {
                android.support.v7.app.AlertDialog alertDialog = new android.support.v7.app.AlertDialog.Builder(this).create();
                alertDialog.setTitle("Invalid Account");
                alertDialog.setMessage("The specified account does not exist on this device. Please choose a different account in the Settings Menu.");
                alertDialog.setButton(android.support.v7.app.AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            } else {
                Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show();
                saveFileToGoogleDrive();
            }
        } else if (driveOption.equalsIgnoreCase("OneDrive")) {
            saveFileToOneDrive();
        } else {
            Toast.makeText(this, "Select a drive option from the settings.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveFileToGoogleDrive() {
        Log.i(TAG, "Creating new contents.");
        final Bitmap image = ((BitmapDrawable)scannedImageView.getDrawable()).getBitmap();
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                    @Override
                    public void onResult(DriveApi.DriveContentsResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.i(TAG, "Failed to create new contents.");
                            return;
                        }
                        Log.i(TAG, "New contents created.");
                        OutputStream outputStream = result.getDriveContents().getOutputStream();
                        ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
                        image.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream);
                        try {
                            outputStream.write(bitmapStream.toByteArray());
                        } catch (IOException e1) {
                            Log.i(TAG, "Unable to write file contents.");
                        }
                        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder().setMimeType("image/jpeg").setTitle("Android Photo.png").build();
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
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
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
    public void onConnected(Bundle connectionHint) {
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    @Override
    protected void onDriveClientReady() { }

}
