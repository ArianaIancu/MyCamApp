package com.example.lorenai.mycamapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements IScanner, ComponentCallbacks2 {

    private TextView mTextMessage;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private Bitmap mImageBitmap;
    private String mCurrentPhotoPath;
    private File mImageFolder;
    int REQUEST_CODE;
    int preference;
    Bundle CODE_BUNDLE = new Bundle();

    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    //    navigation.setSelectedItemId(R.id.camera);

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_data_sync, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, false);

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


    protected int getPreferenceContent() {
        return getIntent().getIntExtra(ScanConstants.OPEN_INTENT_PREFERENCE, 0);
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


    public native Bitmap getScannedBitmap(Bitmap bitmap, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4); // RESULT FRAGMENT

    public native Bitmap getGrayBitmap(Bitmap bitmap); // RESULT FRAGMENT

    public native Bitmap getMagicColorBitmap(Bitmap bitmap); // RESULT FRAGMENT

    public native Bitmap getBWBitmap(Bitmap bitmap); // RESULT FRAGMENT

    public native float[] getPoints(Bitmap bitmap); // PICK IMAGE FRAGMENT


    static {
        System.loadLibrary("opencv_java3");
       // System.loadLibrary("jniLibs");
        //System.loadLibrary("Scanner");
    }

    private void createImageFolder() throws IOException{

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        ScanConstants.FOLDER_NAME = preferences.getString("folder_name"," ");

        //Toast.makeText(this,ScanConstants.FOLDER_NAME,Toast.LENGTH_SHORT).show();

        mImageFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),ScanConstants.FOLDER_NAME);
        //File imageFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        //mImageFolder = new File(imageFile,  ScanConstants.FOLDER_NAME);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            try {
             //  mImageBitmap = uriToBitmap(this, Uri.parse(mCurrentPhotoPath));
           //     CODE_BUNDLE.putParcelable("IMG", mImageBitmap);
               //mImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(mCurrentPhotoPath));
            //   onBitmapSelect( Uri.parse(mCurrentPhotoPath));

                CODE_BUNDLE.putInt("Gal",4);
                //CODE_BUNDLE.putString("img", mCurrentPhotoPath);
               // mImageBitmap = uriToBitmap(getApplicationContext(), Uri.parse(mCurrentPhotoPath));
                //CODE_BUNDLE.putParcelable("IMG", mImageBitmap);
                CODE_BUNDLE.putString("IMG", mCurrentPhotoPath);
                init();
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

}

