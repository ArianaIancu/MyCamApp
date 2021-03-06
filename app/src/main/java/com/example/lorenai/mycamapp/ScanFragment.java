package com.example.lorenai.mycamapp;

import java.util.Date;
import java.text.SimpleDateFormat;
import static android.app.Activity.RESULT_OK;

import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import android.app.Activity;
import android.app.Fragment;

import android.os.Bundle;
import android.os.Environment;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;

import android.widget.Toast;
import android.widget.Button;
import android.widget.ImageView;

import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;

import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.media.MediaScannerConnection;

public class ScanFragment extends Fragment implements CropImageView.OnSetImageUriCompleteListener, CropImageView.OnCropImageCompleteListener{

    public View view;
    public Uri savedUri;
    public CropImageView cropImageView;

    private Bitmap original;
    private IScanner scanner;
    private Button scanButton;
    private ImageView sourceImageView;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof IScanner)) {
            throw new ClassCastException("Activity must implement IScanner");
        }
        this.scanner = (IScanner) activity;
    }

    public ScanFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.scan_fragment_layout, null);
        init();
        return view;
    }
    private void init() {
        sourceImageView = (ImageView) view.findViewById(R.id.sourceImageView);
        scanButton = (Button) view.findViewById(R.id.scanButton);
        scanButton.setOnClickListener(new ScanButtonClickListener());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String cropOption = preferences.getString("choosing_cropping"," ");
        if(cropOption.equalsIgnoreCase("2")) {
            UCrop.Options options = new UCrop.Options();
            options.setToolbarColor(getResources().getColor(R.color.colorPrimary));
            options.setActiveWidgetColor(getResources().getColor(R.color.colorPrimary));
            UCrop.of(getUri(), getUri())
                    .withOptions(options)
                    .start(getActivity());
        } else {
            CropImage.activity(getUri()).start(getContext(), this);
        }
    }

    private class ScanButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (original == null) {
                Intent intent = new Intent(getContext(), MainActivity.class);
                startActivity(intent);
            } else {
                scanner.onScanFinish(savedUri);
            }
        }
    }

    private void createImageFileName(Bitmap bitmap) throws  IOException {
        File mImageFolder = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ScanConstants.FOLDER_NAME);
        String timestamp = new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date());
        String prepend = "IMAGE" + timestamp + "_";
        File file = File.createTempFile(prepend,".jpg", mImageFolder);
        savedUri = Uri.fromFile(file);
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        MediaScannerConnection.scanFile(getContext(), new String[]{file.toString()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });


    }

    private Uri getUri() {
        Uri uri = getArguments().getParcelable(ScanConstants.SELECTED_BITMAP);
        return uri;
    }

    @Override
    public void onCropImageComplete(CropImageView view, CropImageView.CropResult result) { }

    @Override
    public void onSetImageUriComplete(CropImageView view, Uri uri, Exception error) {
        if (error == null) {
            Toast.makeText(getActivity(), "Image load successful", Toast.LENGTH_SHORT).show();
        } else {
            Log.e("AIC", "Failed to load image by URI", error);
            Toast.makeText(getActivity(), "Image load failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                try {
                    original = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), resultUri);
                    sourceImageView.setImageBitmap(original);
                    createImageFileName(original);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) { }
        }
    }

}

