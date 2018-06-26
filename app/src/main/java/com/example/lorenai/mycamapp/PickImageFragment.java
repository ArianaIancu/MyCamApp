package com.example.lorenai.mycamapp;

import java.io.File;
import java.io.IOException;

import android.net.Uri;
import android.util.Log;
import android.os.Bundle;
import android.widget.ImageButton;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.content.Intent;
import android.content.Context;
import android.content.res.AssetFileDescriptor;

/**
 * Created by jhansi on 04/04/15.
 */
public class PickImageFragment extends Fragment {

    private View view;
    private Uri fileUri;
    private IScanner scanner;

    private ImageButton cameraButton;
    private ImageButton galleryButton;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof IScanner)) {
            throw new ClassCastException("Activity must implement IScanner");
        }
        this.scanner = (IScanner) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.pick_image_fragment, null);
        init();
        return view;
    }

    private void init() {
        cameraButton = (ImageButton) view.findViewById(R.id.cameraButton2);
        cameraButton.setOnClickListener(new CameraButtonClickListener());
        galleryButton = (ImageButton) view.findViewById(R.id.selectButton2);
        galleryButton.setOnClickListener(new GalleryClickListener());

        handleIntentPreference();
    }

    private void handleIntentPreference() {
        int preference = getArguments().getInt("Gal");
        if (preference == ScanConstants.OPEN_CAMERA) {
            openCamera();
        } else if (preference == ScanConstants.OPEN_MEDIA) {
            openMediaContent();
        }
    }

    private class CameraButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent i = new Intent(getActivity(), MainActivity.class);
            startActivity(i);
        }
    }

    private class GalleryClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            openMediaContent();
        }
    }

    public void openMediaContent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, ScanConstants.PICKFILE_REQUEST_CODE);
    }

    public void openCamera() {
        fileUri = Uri.parse(getArguments().getString("IMG"));
        Bitmap bitmap = uriToBitmap(getContext() , fileUri);
        postImagePick(bitmap);
    }

    public  Bitmap uriToBitmap(Context c, Uri uri) {
        if (c == null && uri == null) {
            return null;
        }
        Bitmap mImageBitmap;
        try {
            mImageBitmap = MediaStore.Images.Media.getBitmap(c.getContentResolver(), FileProvider.getUriForFile(getContext(), getContext().getApplicationContext().getPackageName() + ".my.package.name.provider", new File(uri.getPath())));
            return mImageBitmap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("", "onActivityResult" + resultCode);
        Bitmap bitmap = null;
        if (resultCode == Activity.RESULT_OK) {
            try {
                switch (requestCode) {
                    case ScanConstants.START_CAMERA_REQUEST_CODE:
                        bitmap = getBitmap(fileUri);
                        break;
                    case ScanConstants.PICKFILE_REQUEST_CODE:
                        bitmap = getBitmap(data.getData());
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            getActivity().finish();
        }
        if (bitmap != null) {
            postImagePick(bitmap);
        }
    }

    public void postImagePick(Bitmap bitmap) {
        Uri uri = Util.getUri(getActivity(), bitmap);
        bitmap.recycle();
        scanner.onBitmapSelect(uri);
    }

    public Bitmap getBitmap(Uri selectedImg) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 3;
        AssetFileDescriptor fileDescriptor = null;
        fileDescriptor = getActivity().getContentResolver().openAssetFileDescriptor(selectedImg, "r");
        Bitmap original = BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(), null, options);
        return original;
    }
}