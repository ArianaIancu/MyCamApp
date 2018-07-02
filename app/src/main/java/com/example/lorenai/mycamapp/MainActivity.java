package com.example.lorenai.mycamapp;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.GoogleApiAvailability;

import android.net.Uri;
import android.util.Log;
import android.Manifest;
import android.provider.MediaStore;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.ComponentCallbacks2;

public class MainActivity extends AppCompatActivity implements IScanner, ComponentCallbacks2, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public Button drive;
    private File mImageFolder;
    public File photoFile;
    Bundle CODE_BUNDLE = new Bundle();
    public ImageView scannedImageView;

    public int PERMISSION_ALL = 1;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    private String mCurrentPhotoPath;
    public String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean agreed = sharedPreferences.getBoolean("agreed",false);
        if (!agreed) {
            new AlertDialog.Builder(this).setTitle("License agreement").setPositiveButton("Agree", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("agreed", true);
                            editor.commit();
                        }
                    })
                    .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                    })
                    .setMessage("TERMS")
                    .setCancelable(false)
                    .show();
        }

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        drive = (Button) findViewById(R.id.drive_button);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        //navigation.setSelectedItemId(R.id.camera);

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);

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

    static {
        System.loadLibrary("opencv_java3");
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

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
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
            try {
                photoFile = createImageFileName();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.i("MyAppTag", "IOException"+ex.toString());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getApplicationContext().getPackageName() + ".my.package.name.provider", photoFile));
                addPicToGallery(Uri.parse(mCurrentPhotoPath), photoFile);
                startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    public  Bitmap uriToBitmap(Context c, Uri uri) {
        if (c == null && uri == null) {
            return null;
        }
        Bitmap mImageBitmap;
        try {
            mImageBitmap = MediaStore.Images.Media.getBitmap(c.getContentResolver(), FileProvider.getUriForFile(c, getPackageName() + ".my.package.name.provider", new File(uri.getPath())));
            return mImageBitmap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            try {
                if(requestCode == REQUEST_CODE_CREATOR) {
                    Log.i(TAG, "Image successfully saved.");
                }
                if(requestCode == REQUEST_IMAGE_CAPTURE) {
                    Bitmap bitmap = uriToBitmap(getApplicationContext() , Uri.parse(mCurrentPhotoPath));
                    if(bitmap != null) {
                        bitmap.recycle();
                        CODE_BUNDLE.putInt("Gal", 4);
                        CODE_BUNDLE.putString("IMG", mCurrentPhotoPath);
                        init();
                    }
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

    public DriveFile file;
    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "Google Drive Activity";

    private static final  int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;

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
     *  Saves the bitmap to drive, can choose the name and folder in Google Drive
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