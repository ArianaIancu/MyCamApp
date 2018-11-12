package com.example.lorenai.mycamapp;

import android.net.Uri;

public interface IScanner {

    void onBitmapSelect(Uri uri);

    void onScanFinish(Uri uri);

}
