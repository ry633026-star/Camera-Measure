package com.example.memo.camerameasure;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImageAccess extends AppCompatActivity {

    private static final String TAG = "ImageAccess";
    private static final String FILE_PROVIDER_AUTHORITY = "com.example.memo.camerameasure.fileprovider";
    private static final String MESSAGE_KEY = "message.message"; // Connects to Offline activity

    private Uri photoURI;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity1);

        Button btnTakeImage = findViewById(R.id.btn_take_image);
        Button btnLoadImage = findViewById(R.id.btn_load_image);

        // Apply border to buttons
        ShapeDrawable shapeDrawable = new ShapeDrawable(new RectShape());
        shapeDrawable.getPaint().setColor(Color.LTGRAY);
        shapeDrawable.getPaint().setStrokeWidth(10f);
        shapeDrawable.getPaint().setStyle(Paint.Style.STROKE);
        btnTakeImage.setBackground(shapeDrawable);
        btnLoadImage.setBackground(shapeDrawable);

        // Capture image button
        btnTakeImage.setOnClickListener(v -> requestCameraPermission());

        // Load image from gallery
        btnLoadImage.setOnClickListener(v -> requestStoragePermission());
    }

    // 🔹 Request Camera Permission
    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            takePicture();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        }
    }

    // 🔹 Open Camera to Take Picture
    private void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraLauncher.launch(takePictureIntent);
            }
        } else {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
        }
    }

    // 🔹 Create Image File
    private File createImageFile() {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File imageFile = new File(storageDir, "IMG_" + timeStamp + ".jpg");
        currentPhotoPath = imageFile.getAbsolutePath();
        return imageFile;
    }

    // 🔹 Handle Camera Result
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    galleryAddPic();
                    Intent intent = new Intent(this, Offline.class);
                    intent.putExtra(MESSAGE_KEY, photoURI.toString()); // Pass URI instead of file path
                    startActivity(intent);
                } else {
                    Log.e(TAG, "Camera capture failed");
                }
            }
    );

    // 🔹 Add Captured Image to Gallery
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }

    // 🔹 Request Storage Permission
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
        } else {
            selectImageFromGallery();
        }
    }

    // 🔹 Select Image from Gallery
    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    // 🔹 Handle Gallery Selection
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();

                    if (selectedImageUri != null) {
                        Log.d(TAG, "Selected Image URI: " + selectedImageUri.toString());
                        Intent intent = new Intent(this, Offline.class);
                        intent.putExtra(MESSAGE_KEY, selectedImageUri.toString()); // Pass URI instead of path
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Failed to get image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );
}
