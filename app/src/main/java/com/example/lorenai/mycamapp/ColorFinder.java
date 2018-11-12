package com.example.lorenai.mycamapp;

import java.util.Date;
import java.text.SimpleDateFormat;

import android.net.Uri;
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
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.GoogleApiAvailability;

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

    // Google Drive Here

    public DriveFile file;

    private GoogleApiClient mGoogleApiClient;

    private String drive_email;
    private String drive_created;

    public DriveId fId;
    public DriveId fIdRed;
    public DriveId fIdBlue;
    public DriveId fIdGreen;
    public DriveId fIdPP;
    public DriveId fIdYO;
    public DriveId fIdDefault;

    public String folderRed = "MyCamAppRed";
    public String folderBlue = "MyCamAppBlue";
    public String folderGreen = "MyCamAppGreen";
    public String folderPP = "MyCamAppPurple&Pink";
    public String folderYO = "MyCamAppYellow&Orange";

    private static final int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;
    private static final String TAG = "Google Drive Activity";

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
        drive_created = preferences.getString("fIdCreated", " ");
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
            if (drive_created.equals("true") && ScanConstants.EMAIL_CHANGED.equals("no")) {
                Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show();
                saveFileToDrive();
            } else if (ScanConstants.EMAIL_GOOD.equals("bad")) {
                Log.i(TAG, "GoogleApiClient is not connected!");
            } else if (!drive_email.equals(" ")) {
                createFolderDefault();
                createFolderRed();
                createFolderBlue();
                createFolderGreen();
                createFolderPP();
                createFolderYO();

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("fIdCreated", "true");
                editor.apply();
                ScanConstants.EMAIL_CHANGED = "no";

                Log.i(TAG, "Created all Drive folders.");
                Toast.makeText(this, "Drive folders created. Try again to save photo.", Toast.LENGTH_LONG).show();

            }
        }
    }

    private void saveFileToDrive() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (colorName == "Red") {
            fId = DriveId.decodeFromString(preferences.getString("fIdRed", ""));
        } else if (colorName == "Green") {
            fId = DriveId.decodeFromString(preferences.getString("fIdGreen", ""));
        } else if (colorName == "Blue") {
            fId = DriveId.decodeFromString(preferences.getString("fIdBlue", ""));
        } else if (colorName == "Yellow/Orange") {
            fId = DriveId.decodeFromString(preferences.getString("fIdYO", ""));
        } else if (colorName == "Purple/Pink") {
            fId = DriveId.decodeFromString(preferences.getString("fIdPP", ""));
        } else {
            fId = DriveId.decodeFromString(preferences.getString("fIdDefault", ""));
        }
        Log.i(TAG, "Creating new contents.");
        final Bitmap image = ((BitmapDrawable) scannedImageView.getDrawable()).getBitmap();
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
                                .setActivityStartFolder(fId)
                                .build(mGoogleApiClient);
                        try {
                            startIntentSenderForResult(intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "Failed to launch file chooser.");
                        }
                    }
                });
    }

    // Create Drive Folders

    public void createFolderRed() {
        getDriveResourceClient().getRootFolder().continueWithTask(task -> {
            DriveFolder parentFolder = task.getResult();
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle(folderRed)
                    .setMimeType(DriveFolder.MIME_TYPE)
                    .setStarred(true)
                    .build();
            return getDriveResourceClient().createFolder(parentFolder, changeSet);
        }).addOnSuccessListener(this,
                driveFolder -> { fIdRed = driveFolder.getDriveId();
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("fIdRed", String.valueOf(fIdRed));
                    editor.apply();
                    Log.i(TAG, "Created folder");
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Unable to create folder", e);
                });
    }

    public void createFolderBlue() {
        getDriveResourceClient().getRootFolder().continueWithTask(task -> {
            DriveFolder parentFolder = task.getResult();
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle(folderBlue)
                    .setMimeType(DriveFolder.MIME_TYPE)
                    .setStarred(true)
                    .build();
            return getDriveResourceClient().createFolder(parentFolder, changeSet);
        }).addOnSuccessListener(this,
                driveFolder -> { fIdBlue = driveFolder.getDriveId();
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("fIdBlue", String.valueOf(fIdBlue));
                    editor.apply();
                    Log.i(TAG, "Created folder");
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Unable to create folder", e);
                });
    }

    public void createFolderGreen() {
        getDriveResourceClient().getRootFolder().continueWithTask(task -> {
            DriveFolder parentFolder = task.getResult();
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle(folderGreen)
                    .setMimeType(DriveFolder.MIME_TYPE)
                    .setStarred(true)
                    .build();
            return getDriveResourceClient().createFolder(parentFolder, changeSet);
        }).addOnSuccessListener(this,
                driveFolder -> { fIdGreen = driveFolder.getDriveId();
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("fIdGreen", String.valueOf(fIdGreen));
                    editor.apply();
                    Log.i(TAG, "Created folder");
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Unable to create folder", e);
                });
    }

    public void createFolderPP() {
        getDriveResourceClient().getRootFolder().continueWithTask(task -> {
            DriveFolder parentFolder = task.getResult();
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle(folderPP)
                    .setMimeType(DriveFolder.MIME_TYPE)
                    .setStarred(true)
                    .build();
            return getDriveResourceClient().createFolder(parentFolder, changeSet);
        }).addOnSuccessListener(this,
                driveFolder -> { fIdPP = driveFolder.getDriveId();
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("fIdPP", String.valueOf(fIdPP));
                    editor.apply();
                    Log.i(TAG, "Created folder");
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Unable to create folder", e);
                });
    }

    public void createFolderYO() {
        getDriveResourceClient().getRootFolder().continueWithTask(task -> {
            DriveFolder parentFolder = task.getResult();
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle(folderYO)
                    .setMimeType(DriveFolder.MIME_TYPE)
                    .setStarred(true)
                    .build();
            return getDriveResourceClient().createFolder(parentFolder, changeSet);
        }).addOnSuccessListener(this,
                driveFolder -> { fIdYO = driveFolder.getDriveId();
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("fIdYO", String.valueOf(fIdYO));
                    editor.apply();
                    Log.i(TAG, "Created folder");
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Unable to create folder", e);
                });
    }

    public void createFolderDefault() {
        getDriveResourceClient().getRootFolder().continueWithTask(task -> {
            DriveFolder parentFolder = task.getResult();
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle(ScanConstants.FOLDER_NAME)
                    .setMimeType(DriveFolder.MIME_TYPE)
                    .setStarred(true)
                    .build();
            return getDriveResourceClient().createFolder(parentFolder, changeSet);
        }).addOnSuccessListener(this,
                driveFolder -> { fIdDefault = driveFolder.getDriveId();
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("fIdDefault", String.valueOf(fIdDefault));
                    editor.apply();
                    Log.i(TAG, "Created folder");
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Unable to create folder", e);
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
