package com.example.lorenai.mycamapp;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.scanlibrary.ScanActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.app.Activity.RESULT_OK;

/**
 * Created by jhansi on 29/03/15.
 */

public class ResultFragment extends Fragment {

    private View view;
    private ImageView scannedImageView;
    private static ProgressDialogFragment progressDialogFragment;
    public Uri savedUri;

    private Bitmap original;
    private Bitmap transformed;

    public Button drive;
    private Button bwButton;
    private Button doneButton;
    private Button originalButton;
    private Button grayModeButton;
    private Button MagicColorButton;
    public Button savePDF;

    public ResultFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.result_layout, null);
        init();
        return view;
    }

    private void init() {
        scannedImageView = (ImageView) view.findViewById(R.id.scannedImage);
        originalButton = (Button) view.findViewById(R.id.original);
        originalButton.setOnClickListener(new OriginalButtonClickListener());
        MagicColorButton = (Button) view.findViewById(R.id.magicColor);
        MagicColorButton.setOnClickListener(new MagicColorButtonClickListener());
        grayModeButton = (Button) view.findViewById(R.id.grayMode);
        grayModeButton.setOnClickListener(new GrayButtonClickListener());
        bwButton = (Button) view.findViewById(R.id.BWMode);
        bwButton.setOnClickListener(new BWButtonClickListener());
        Bitmap bitmap = getBitmap();
        setScannedImage(bitmap);
        doneButton = (Button) view.findViewById(R.id.doneButton);
        doneButton.setOnClickListener(new DoneButtonClickListener());
        drive = (Button) view.findViewById(R.id.drive_button);
        savePDF = (Button) view.findViewById(R.id.savePDF);
        savePDF.setOnClickListener(new SavePDFListener());

        PreferenceManager.setDefaultValues(getContext(), R.xml.pref_general, false);
    }

    private Bitmap getBitmap() {
        Uri uri = getUri();
        try {
            original = Util.getBitmap(getActivity(), uri);
            return original;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public class SavePDFListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
           /* android.support.v7.app.AlertDialog alertDialog = new android.support.v7.app.AlertDialog.Builder(getContext()).create();
            alertDialog.setTitle("Invalid Account");
            alertDialog.setMessage("The specified account does not exist on this device. Please choose a different account in the Settings Menu.");
            alertDialog.setButton(android.support.v7.app.AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
            */
            Bitmap bitmap = transformed;
            if (bitmap == null) {
                bitmap = original;
            }
            File mImageFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ScanConstants.FOLDER_NAME);
            try {
                createImageFileName(bitmap, mImageFolder);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Document document=new Document();
            String pdfPath = android.os.Environment.getExternalStorageDirectory().toString();
            try {
                String timestamp = new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date());
                String yourPDF = "/PDF" + timestamp + ".pdf";
                PdfWriter.getInstance(document, new FileOutputStream(pdfPath + yourPDF));
                document.open();
                Image image;
                image = Image.getInstance(fileName);
                try {
                    document.add(new Paragraph("MyCamApp Image Result"));
                    document.add(image);
                    document.close();
                } catch (DocumentException e) {
                    e.printStackTrace();
                }
            } catch (DocumentException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(getContext(),"PDF Created", Toast.LENGTH_SHORT);

            MediaScannerConnection.scanFile(getContext(), new String[]{document.toString()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        }
                    });
        }

        /*
         document.open();
            Image image = null;
            try {
                try {
                    image = Image.getInstance(fileName);
                } catch (BadElementException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                document.add(new Paragraph("Your Heading for the Image Goes Here"));
                document.add(image);
            } catch (DocumentException e) {
                e.printStackTrace();
            }
            document.close();
        *
        * */

             /*
        Document document = new Document();
        String directoryPath = android.os.Environment.getExternalStorageDirectory().toString();

        try {
            PdfWriter.getInstance(document, new FileOutputStream(directoryPath + "/example.pdf"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        document.open();
        Image image = null;

        try {
            image = Image.getInstance(directoryPath + "/" + "example.jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }

        float scaler = ((document.getPageSize().getWidth() - document.leftMargin()
                - document.rightMargin() - 0) / image.getWidth()) * 100; // 0 means you have no indentation. If you have any, change it.
        image.scalePercent(scaler);
        image.setAlignment(Image.ALIGN_CENTER | Image.ALIGN_TOP);

        document.add(image);
        document.close();
        */

    }

    private Uri getUri() {
        Uri uri = getArguments().getParcelable(ScanConstants.SCANNED_RESULT);
        return uri;
    }

    public void setScannedImage(Bitmap scannedImage) {
        scannedImageView.setImageBitmap(scannedImage);
    }

    public String fileName;

    private void createImageFileName(Bitmap bitmap, File folder) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date());
        String prepend = "IMAGE" + timestamp + "_";
        File file = File.createTempFile(prepend, ".jpg", folder);
        fileName = file.getAbsolutePath();
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

        savedUri = Uri.fromFile(file);
    }

    private class DoneButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            showProgressDialog(getResources().getString(R.string.loading));
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Intent data = new Intent();
                        Bitmap bitmap = transformed;
                        if (bitmap == null) {
                            bitmap = original;
                        }
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                        Boolean sendColor = preferences.getBoolean("example_switch", true);
                        File mImageFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ScanConstants.FOLDER_NAME);
                        createImageFileName(bitmap, mImageFolder);
                        if (sendColor == true) {
                            final Uri uri = Util.getUri(getActivity(), bitmap);
                            data.putExtra(ScanConstants.SCANNED_RESULT, uri);
                            getActivity().setResult(RESULT_OK, data);
                            System.gc();
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dismissDialog();
                                    Intent intent = new Intent(getActivity(), ColorFinder.class);
                                    Bundle bundle = new Bundle();
                                    bundle.putParcelable("bitmap", uri);
                                    intent.putExtras(bundle);
                                    startActivity(intent);
                                }
                            });
                        } else {
                            getActivity().finish();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private class BWButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {
           final ScanActivity scanA = new ScanActivity();
            showProgressDialog(getResources().getString(R.string.applying_filter));
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        transformed = scanA.getBWBitmap(original);
                    } catch (final OutOfMemoryError e) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                transformed = original;
                                scannedImageView.setImageBitmap(original);
                                e.printStackTrace();
                                dismissDialog();
                                onClick(v);
                            }
                        });
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scannedImageView.setImageBitmap(transformed);
                            dismissDialog();
                        }
                    });
                }
            });
        }
    }

    private class MagicColorButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {
            final ScanActivity scanA = new ScanActivity();
            showProgressDialog(getResources().getString(R.string.applying_filter));
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        transformed = scanA.getMagicColorBitmap(original);
                    } catch (final OutOfMemoryError e) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                transformed = original;
                                scannedImageView.setImageBitmap(original);
                                e.printStackTrace();
                                dismissDialog();
                                onClick(v);
                            }
                        });
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scannedImageView.setImageBitmap(transformed);
                            dismissDialog();
                        }
                    });
                }
            });
        }
    }

    private class OriginalButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            try {
                showProgressDialog(getResources().getString(R.string.applying_filter));
                transformed = original;
                scannedImageView.setImageBitmap(original);
                dismissDialog();
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                dismissDialog();
            }
        }
    }

    private class GrayButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {
            final ScanActivity scanA = new ScanActivity();
            showProgressDialog(getResources().getString(R.string.applying_filter));
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        transformed = scanA.getGrayBitmap(original);
                    } catch (final OutOfMemoryError e) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                transformed = original;
                                scannedImageView.setImageBitmap(original);
                                e.printStackTrace();
                                dismissDialog();
                                onClick(v);
                            }
                        });
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scannedImageView.setImageBitmap(transformed);
                            dismissDialog();
                        }
                    });
                }
            });
        }
    }

    protected synchronized void showProgressDialog(String message) {
        if (progressDialogFragment != null && progressDialogFragment.isVisible()) {
            // Before creating another loading dialog, close all opened loading dialogs (if any)
            progressDialogFragment.dismissAllowingStateLoss();
        }
        progressDialogFragment = null;
        progressDialogFragment = new ProgressDialogFragment(message);
        FragmentManager fm = getFragmentManager();
        progressDialogFragment.show(fm, ProgressDialogFragment.class.toString());
    }

    protected synchronized void dismissDialog() {
        progressDialogFragment.dismissAllowingStateLoss();
    }
}