package com.example.lorenai.mycamapp;

import static android.app.Activity.RESULT_OK;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;

import android.net.Uri;
import android.util.Log;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.media.MediaScannerConnection;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;
import android.widget.Button;
import android.widget.ImageView;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;

/**
 * Created by jhansi on 29/03/15.
 */

public class ScanFragment extends Fragment implements CropImageView.OnSetImageUriCompleteListener, CropImageView.OnCropImageCompleteListener{

    private View view;
    public Uri savedUri;
    private Bitmap original;
    private IScanner scanner;
    private Button scanButton;
    private ImageView sourceImageView;
    public CropImageView cropImageView;

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
        view = inflater.inflate(R.layout.scan_fragment_layout, null);
        init();
        return view;
    }

    public ScanFragment() { }

    private void createImageFileName(Bitmap bitmap) throws IOException {
        File mImageFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),ScanConstants.FOLDER_NAME);
        String timestamp = new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date());
        String prepend = "IMAGE" + timestamp + "_";
        File file = File.createTempFile(prepend, ".jpg", mImageFolder);
        savedUri = Uri.fromFile(file);

        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90 , out);
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

    private void init() {
        sourceImageView = (ImageView) view.findViewById(R.id.sourceImageView);
        scanButton = (Button) view.findViewById(R.id.scanButton);
        scanButton.setOnClickListener(new ScanButtonClickListener());
        CropImage.activity(getUri()).start(getContext(), this);
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
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            }
        }
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
}

