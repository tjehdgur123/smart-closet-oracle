package com.example.smartcloset.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.smartcloset.R;
import com.example.smartcloset.data.ClothesEntity;
import com.example.smartcloset.data.OracleApi;
import com.example.smartcloset.util.AppExecutors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AddClothesActivity extends AppCompatActivity {
    private static final int REQUEST_GALLERY = 10;
    private static final int REQUEST_CAMERA = 11;

    private ImageView imgPreview;
    private Spinner spinnerCategory;
    private Spinner spinnerColor;
    private Spinner spinnerSeason;
    private CheckBox checkFavorite;
    private String savedImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_clothes);

        imgPreview = findViewById(R.id.imgPreview);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerColor = findViewById(R.id.spinnerColor);
        spinnerSeason = findViewById(R.id.spinnerSeason);
        checkFavorite = findViewById(R.id.checkFavorite);

        setupSpinner(spinnerCategory, new String[]{
                "\uC0C1\uC758", "\uD558\uC758", "\uC544\uC6B0\uD130", "\uC2E0\uBC1C", "\uC561\uC138\uC11C\uB9AC"
        });
        setupSpinner(spinnerColor, new String[]{
                "\uAC80\uC815", "\uD770\uC0C9", "\uD68C\uC0C9", "\uC544\uC774\uBCF4\uB9AC", "\uBCA0\uC774\uC9C0", "\uBE0C\uB77C\uC6B4",
                "\uB124\uC774\uBE44", "\uD30C\uB791", "\uD558\uB298\uC0C9", "\uCD08\uB85D", "\uCE74\uD0A4", "\uBBFC\uD2B8",
                "\uBE68\uAC15", "\uBD84\uD64D", "\uBCF4\uB77C", "\uB178\uB791", "\uC8FC\uD669",
                "\uC2E4\uBC84", "\uACE8\uB4DC", "\uBA40\uD2F0\uCEEC\uB7EC", "\uAE30\uD0C0"
        });
        setupSpinner(spinnerSeason, new String[]{
                "\uBD04", "\uC5EC\uB984", "\uAC00\uC744", "\uACA8\uC6B8", "\uC0AC\uACC4\uC808"
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnGallery).setOnClickListener(v -> openGallery());
        findViewById(R.id.btnCamera).setOnClickListener(v -> openCamera());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveClothes());
    }

    private void setupSpinner(Spinner spinner, String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values);
        spinner.setAdapter(adapter);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null) {
            return;
        }

        try {
            if (requestCode == REQUEST_GALLERY && data.getData() != null) {
                savedImagePath = copyUriToInternalStorage(data.getData());
                Glide.with(this).load(new File(savedImagePath)).into(imgPreview);
            } else if (requestCode == REQUEST_CAMERA && data.getExtras() != null) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                if (bitmap != null) {
                    savedImagePath = saveBitmapToInternalStorage(bitmap);
                    Glide.with(this).load(new File(savedImagePath)).into(imgPreview);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "\uC774\uBBF8\uC9C0 \uC800\uC7A5\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.", Toast.LENGTH_SHORT).show();
        }
    }

    private String copyUriToInternalStorage(Uri uri) throws Exception {
        File dir = new File(getFilesDir(), "clothes");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, "clothes_" + System.currentTimeMillis() + ".jpg");
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(file)) {
            if (inputStream == null) {
                throw new IllegalStateException("Image input stream is null.");
            }
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        }
        return file.getAbsolutePath();
    }

    private String saveBitmapToInternalStorage(Bitmap bitmap) throws Exception {
        File dir = new File(getFilesDir(), "clothes");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, "camera_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
        }
        return file.getAbsolutePath();
    }

    private void saveClothes() {
        if (savedImagePath == null) {
            Toast.makeText(this, "\uC0AC\uC9C4\uC744 \uBA3C\uC800 \uC120\uD0DD\uD558\uC138\uC694.", Toast.LENGTH_SHORT).show();
            return;
        }

        ClothesEntity clothes = new ClothesEntity(
                savedImagePath,
                spinnerCategory.getSelectedItem().toString(),
                spinnerColor.getSelectedItem().toString(),
                spinnerSeason.getSelectedItem().toString(),
                checkFavorite.isChecked()
        );

        AppExecutors.io().execute(() -> {
            try {
                OracleApi.createClothes(clothes);
                runOnUiThread(() -> {
                    Toast.makeText(this, "\uC758\uB958\uAC00 \uB4F1\uB85D\uB418\uC5C8\uC2B5\uB2C8\uB2E4.", Toast.LENGTH_SHORT).show();
                    goToMain();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Oracle \uC11C\uBC84 \uC800\uC7A5 \uC2E4\uD328: \uC11C\uBC84 \uC5F0\uACB0\uC744 \uD655\uC778\uD558\uC138\uC694.", Toast.LENGTH_LONG).show());
            }
        });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
