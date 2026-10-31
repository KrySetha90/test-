package com.originos.wiggle;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class WallpaperConfigActivity extends AppCompatActivity {

    private ImageView previewImageView;
    private SharedPreferences prefs;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        prefs = getSharedPreferences("WallpaperPrefs", MODE_PRIVATE);
        previewImageView = findViewById(R.id.previewImageView);

        Button btnPickImage = findViewById(R.id.btnPickImage);
        Button btnSetWallpaper = findViewById(R.id.btnSetWallpaper);

        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        getContentResolver().takePersistableUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        prefs.edit().putString("image_uri", imageUri.toString()).apply();
                        previewImageView.setImageURI(imageUri);
                    }
                }
            }
        );

        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnSetWallpaper.setOnClickListener(v -> {
            Intent intent = new Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            intent.putExtra(android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    new android.content.ComponentName(this, WiggleWallpaperService.class));
            startActivity(intent);
        });
    }
}