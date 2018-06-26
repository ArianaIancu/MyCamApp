package com.example.lorenai.mycamapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements IScanner, ComponentCallbacks2, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private String mCurrentPhotoPath;
    private File mImageFolder;
    public Button drive;
    Bundle CODE_BUNDLE = new Bundle();

    public ImageView scannedImageView;

    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
        drive = (Button) findViewById(R.id.drive_button);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
       // navigation.setSelectedItemId(R.id.camera);

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_data_sync, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, false);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        ScanConstants.FOLDER_NAME = preferences.getString("folder_name"," ");

    }

    private void init() {
        setContentView(R.layout.scan_layout);
        PickImageFragment fragment = new PickImageFragment(); //here is where it starts to select an img
        fragment.setArguments(CODE_BUNDLE);

        android.app.FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.content, fragment);
        fragmentTransaction.commit();
    }

    /*
    protected int getPreferenceContent() {
        return getIntent().getIntExtra(ScanConstants.OPEN_INTENT_PREFERENCE, 0);
    }
    */

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onBitmapSelect(Uri uri) {
        ScanFragment fragment = new ScanFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ScanConstants.SELECTED_BITMAP, uri);
        fragment.setArguments(bundle);

        android.app.FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.content, fragment);
        fragmentTransaction.addToBackStack(ScanFragment.class.toString());
        fragmentTransaction.commit();
    }

    @Override
    public void onScanFinish(Uri uri) {
        ResultFragment fragment = new ResultFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ScanConstants.SCANNED_RESULT, uri);
        fragment.setArguments(bundle);
        android.app.FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.content, fragment);
        fragmentTransaction.addToBackStack(ResultFragment.class.toString());
        fragmentTransaction.commit();
    }

    @Override
    public void onTrimMemory(int level) {
        switch (level) {
            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:
                /*
                   Release any UI objects that currently hold memory.

                   The user interface has moved to the background.
                */
                break;
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
                /*
                   Release any memory that your app doesn't need to run.

                   The device is running low on memory while the app is running.
                   The event raised indicates the severity of the memory-related event.
                   If the event is TRIM_MEMORY_RUNNING_CRITICAL, then the system will
                   begin killing background processes.
                */
                break;
            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
                /*
                   Release as much memory as the process can.

                   The app is on the LRU list and the system is running low on memory.
                   The event raised indicates where the app sits within the LRU list.
                   If the event is TRIM_MEMORY_COMPLETE, the process will be one of
                   the first to be terminated.
                */
                new AlertDialog.Builder(this)
                        .setTitle(R.string.low_memory)
                        .setMessage(R.string.low_memory_message)
                        .create()
                        .show();
                break;
            default:
                /*
                  Release any non-critical data structures.

                  The app received an unrecognized memory level value
                  from the system. Treat this as a generic low-memory message.
                */
                break;
        }
    }

    /*
    public native Bitmap getScannedBitmap(Bitmap bitmap, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4);

    public native Bitmap getGrayBitmap(Bitmap bitmap);

    public native Bitmap getMagicColorBitmap(Bitmap bitmap);

    public native Bitmap getBWBitmap(Bitmap bitmap);

    public native float[] getPoints(Bitmap bitmap);
  */

    static {
        System.loadLibrary("opencv_java3");
//        System.loadLibrary("scanStuf");
       // System.loadLibrary("jniLibs")
       // System.loadLibrary("Scanner");
    }

    private void createImageFolder() {
        mImageFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),ScanConstants.FOLDER_NAME);
        if (!mImageFolder.exists()) {
            mImageFolder.mkdir();
        }

    }

    private File createImageFileName() throws IOException {
        createImageFolder();

        String timestamp = new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date());
        String prepend = "IMAGE" + timestamp + "_";
        File imageFile = File.createTempFile(prepend, ".jpg", mImageFolder);
        mCurrentPhotoPath = imageFile.getAbsolutePath();
        return imageFile;
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.galery:
                    CODE_BUNDLE.putInt("Gal",5);
                    init();
                    return true;
                case R.id.camera:
                    openCamera();
                    return true;
                case R.id.settings:
                    Intent sIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                    startActivity(sIntent);
                    return true;
            }
            return false;
        }
    };

    public void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFileName();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.i("MyAppTag", "IOException"+ex.toString());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                //cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getApplicationContext().getPackageName() + ".my.package.name.provider", photoFile));
                addPicToGallery(Uri.parse(mCurrentPhotoPath), photoFile);
                startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
            }
        }

    }

    /*
    public  Bitmap uriToBitmap(Context c, Uri uri) {
        if (c == null && uri == null) {
            return null;
        }
        try {
            //mImageBitmap = MediaStore.Images.Media.getBitmap(c.getContentResolver(), Uri.fromFile(new File(uri.getPath())));
            mImageBitmap = MediaStore.Images.Media.getBitmap(c.getContentResolver(), FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getApplicationContext().getPackageName() + ".my.package.name.provider", new File(uri.getPath())));
            return mImageBitmap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            try {
                if(requestCode == REQUEST_CODE_CREATOR) {
                    Log.i(TAG, "Image successfully saved.");
                }
                if(requestCode == REQUEST_IMAGE_CAPTURE) {
                    CODE_BUNDLE.putInt("Gal", 4);
                    CODE_BUNDLE.putString("IMG", mCurrentPhotoPath);
                    init();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    private void addPicToGallery(Uri uri, File imageFile) {

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
        mediaScanIntent.setData(Uri.fromFile(imageFile));
        sendBroadcast(mediaScanIntent);
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    // GOOGLE DRIVE HERE

    private static final String TAG = "Google Drive Activity";
    private static final int REQUEST_CODE_RESOLUTION = 3;
    private static final  int REQUEST_CODE_CREATOR = 2;
    private GoogleApiClient mGoogleApiClient;
    private boolean fileOperation = false;
    public DriveFile file;

    /**
     * Called when the activity will start interacting with the user.
     * At this point your activity is at the top of the activity stack,
     * with user input going to it.
     */

    public void connectDude() {
        if (mGoogleApiClient == null) {
            /**
             * Create the API client and bind it to an instance variable.
             * We use this instance as the callback for connection and connection failures.
             * Since no account name is passed, the user is prompted to choose.
             */
            mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient != null) {
            // disconnect Google API client connection
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
        /**
         *  The failure has a resolution. Resolve it.
         *  Called typically when the app is not yet authorized, and an  authorization
         *  dialog is displayed to the user.
         */
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    /**
     * It invoked when Google API client connected
     * @param connectionHint
     */
    @Override
    public void onConnected(Bundle connectionHint) { }
    /**
     * It invoked when connection suspend
     * @param cause
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    public void onClickCreateFile(View view){
        connectDude();
        saveFileToDrive();
    }

    public void onClickChangeDrive(View view) {
        mGoogleApiClient.clearDefaultAccountAndReconnect();
    }


    /**
     * This is Result result handler of Drive contents.
     * this callback method call CreateFileOnGoogleDrive() method
     * and also call OpenFileFromGoogleDrive() method,
     * send intent onActivityResult() method to handle result.
     */

    /*
    final ResultCallback <DriveApi.DriveContentsResult> driveContentsCallback = new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(DriveApi.DriveContentsResult result) {
                    if (result.getStatus().isSuccess()) {
                        if (fileOperation == true) {
                            CreateFileOnGoogleDrive(result);
                        }
                    }
                }
            };
    */

    /**
     * Create a file in root folder using MetadataChangeSet object.
     * @param result
     */
    /*
    public void CreateFileOnGoogleDrive(DriveApi.DriveContentsResult result){

        final DriveContents driveContents = result.getDriveContents();

        // Perform I/O off the UI thread.
        new Thread() {
            @Override
            public void run() {
                // write content to DriveContents
                OutputStream outputStream = driveContents.getOutputStream();
                Writer writer = new OutputStreamWriter(outputStream);
                try {
                    writer.write("Hello! I am just a text file :) ");
                    writer.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }

                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                        .setTitle("MyCamApp TEST FILE")
                        .setMimeType("text/plain")
                        .setStarred(true).build();

                // create a file in root folder
                Drive.DriveApi.getRootFolder(mGoogleApiClient)
                        .createFile(mGoogleApiClient, changeSet, driveContents)
                        .setResultCallback(fileCallback);
            }
        }.start();
    }
   */

    /**
     * Handle result of Created file
     */
    /*
    final private ResultCallback<DriveFolder.DriveFileResult> fileCallback = new ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(DriveFolder.DriveFileResult result) {
                    if (result.getStatus().isSuccess()) {
                        Toast.makeText(getApplicationContext(), "File created", Toast.LENGTH_LONG).show();
                    }
                    return;
                }
            };
    */

    private void saveFileToDrive() {
        // Start by creating a new contents, and setting a callback.
        scannedImageView = (ImageView) findViewById(R.id.scannedImage);
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
                        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                .setMimeType("image/jpeg").setTitle("Android Photo.png").build();
                        // Create an intent for the file chooser, and start it.
                        IntentSender intentSender = Drive.DriveApi
                                .newCreateFileActivityBuilder()
                                .setInitialMetadata(metadataChangeSet)
                                .setInitialDriveContents(result.getDriveContents())
                                .build(mGoogleApiClient);
                        try {
                            startIntentSenderForResult(
                                    intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "Failed to launch file chooser.");
                        }
                    }
                });
    }
}