package com.example.lorenai.mycamapp;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.ArrayList;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

import android.app.Activity;
import android.net.Uri;
import android.util.Log;
import android.Manifest;
import android.view.ViewGroup;
import android.provider.MediaStore;
import android.preference.PreferenceManager;

import android.os.Bundle;
import android.os.Environment;

import android.view.View;
import android.view.MenuItem;

import android.app.AlertDialog;
import android.app.FragmentTransaction;

import android.content.IntentSender;
import android.content.pm.ActivityInfo;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import android.widget.Toast;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;

import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.design.widget.BottomNavigationView;

import android.content.Intent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.ComponentCallbacks2;

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


public class MainActivity extends AppCompatActivity implements IScanner, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private java.io.File mImageFolder;
    public java.io.File photoFile;
    Bundle CODE_BUNDLE = new Bundle();
    public ImageView scannedImageView;

    public int PERMISSION_ALL = 1;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    private String mCurrentPhotoPath;
    public String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.INTERNET};

    private GridView gv;
    private ArrayList<File> list;
    private ArrayList<File> imageReader(File root) {
        ArrayList<File> a = new ArrayList<>();

        File[] files = root.listFiles();
        for(int i = 0; i < files.length; i++) {
            if(files[i].isDirectory()) {
                a.addAll(imageReader(files[i]));
            } else {
                if(files[i].getName().endsWith(".jpg")) {
                    a.add(files[i]);
                }
            }
        }
        return a;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

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

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_drive, false);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        ScanConstants.FOLDER_NAME = preferences.getString("folder_name"," ");
        drive_email = preferences.getString("drive_email", " ");

        createImageFolder();

        if(ScanConstants.CROP_URI == "notYet") {
            File[] checkEmptiness = mImageFolder.listFiles();
            if (checkEmptiness == null) {
                list = imageReader(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
            } else if (checkEmptiness.length == 0) {
                list = imageReader(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
            } else {
                list = imageReader(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/" + ScanConstants.FOLDER_NAME));
            }

            gv = (GridView) findViewById(R.id.gridView);
            gv.setAdapter(new GridAdapter());

            gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Uri uriImg = Uri.fromFile(list.get(position));
                    String uri = uriImg.toString();
                    startActivity(new Intent(getApplicationContext(), CropImageMainAct.class).putExtra("uri", uri));
                }
            });
        } else {
            Uri uri = Uri.parse(ScanConstants.CROP_URI);
            onScanFinish(uri);
        }
    }

    class GridAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            convertView = getLayoutInflater().inflate(R.layout.single_grid, parent,false);
            ImageView iv = (ImageView) convertView.findViewById(R.id.imageViewPic);

            iv.setImageURI( Uri.parse(getItem(position).toString()));

            return convertView;
        }
    }

    private void init() {
        setContentView(R.layout.scan_layout);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
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
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
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
        if(ScanConstants.CROP_URI != "notYet") {
            setContentView(R.layout.scan_layout);
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
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
        mImageFolder = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),ScanConstants.FOLDER_NAME);
        if (!mImageFolder.exists()) {
            mImageFolder.mkdir();
        }
    }

    private java.io.File createImageFileName() throws IOException {
        createImageFolder();

        String timestamp = new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date());
        String prepend = "IMAGE" + timestamp + "_";
        java.io.File imageFile = java.io.File.createTempFile(prepend, ".jpg", mImageFolder);
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
            mImageBitmap = MediaStore.Images.Media.getBitmap(c.getContentResolver(), FileProvider.getUriForFile(c, getPackageName() + ".my.package.name.provider", new java.io.File(uri.getPath())));
            return mImageBitmap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void addPicToGallery(Uri uri, java.io.File imageFile) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
        mediaScanIntent.setData(Uri.fromFile(imageFile));
        sendBroadcast(mediaScanIntent);
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        try {
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

    private static final  int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;

    private static String drive_email;
    private static final String TAG = "Google Drive Activity";

    private ISaver mSaver;
    private static boolean one_saver = false;
    final static String ONEDRIVE_Client_ID = "2894a74e-f2c9-483e-b437-f6730a18c068";

    public void connectDudeGoogle() {
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

    public void onClickCreateFile(View view) throws IOException {
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_drive, false);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String driveOption = preferences.getString("choosing_drive"," ");

        if (driveOption.equalsIgnoreCase("Google Drive")) {
            connectDudeGoogle();
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
        } else if (driveOption.equalsIgnoreCase("OneDrive")) { // Put the function here
            saveFileToOneDrive(view);
        } else {
            Toast.makeText(this, "Select a drive option from the settings.", Toast.LENGTH_SHORT).show();
        }

    }

    private void saveFileToGoogleDrive() {
        scannedImageView = (ImageView) findViewById(R.id.scannedImage);
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

    // NOT SAVING!!!
    public void saveFileToOneDrive(View view) throws IOException {
        one_saver = true;

        /*
        scannedImageView = (ImageView) findViewById(R.id.scannedImage);
        final Bitmap image = ((BitmapDrawable)scannedImageView.getDrawable()).getBitmap();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream);
        byte[] bitmapdata = bos.toByteArray();

        String timestamp = new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date());
        String prepend = "MyCamApp" + timestamp + "_";
        File file = new File(Environment.getExternalStorageDirectory() + "/Pictures/", prepend);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        */
        final String filename = "testingOneDrive.txt";
        final File f = new File(getApplicationContext().getFilesDir(), filename);


        mSaver = Saver.createSaver(ONEDRIVE_Client_ID);
        mSaver.startSaving((Activity)view.getContext(), filename, Uri.parse("file://" + f.getAbsolutePath())); //prepend, Uri.parse(file.getAbsolutePath()));
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
    public void onConnected(Bundle connectionHint) { }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

}

// Alert Dialog Example

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