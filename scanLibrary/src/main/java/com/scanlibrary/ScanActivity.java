package com.scanlibrary;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Bitmap;

/**
 * Created by jhansi on 28/03/15.
 */

public class ScanActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_layout);
    }

    public native Bitmap getGrayBitmap(Bitmap bitmap);

    public native Bitmap getMagicColorBitmap(Bitmap bitmap);

    public native Bitmap getBWBitmap(Bitmap bitmap);

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("Scanner");
    }
}