package com.example.smartcloset.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartcloset.R;
import com.example.smartcloset.data.AppDatabase;
import com.example.smartcloset.data.ClothesEntity;
import com.example.smartcloset.data.CoordinationEntity;
import com.example.smartcloset.data.OracleApi;
import com.example.smartcloset.util.AppExecutors;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnAddClothes).setOnClickListener(v ->
                startActivity(new Intent(this, AddClothesActivity.class)));
        findViewById(R.id.btnClothesList).setOnClickListener(v ->
                startActivity(new Intent(this, ClothesListActivity.class)));
        findViewById(R.id.btnCoordination).setOnClickListener(v ->
                startActivity(new Intent(this, CoordinationActivity.class)));
        findViewById(R.id.btnFavorites).setOnClickListener(v ->
                startActivity(new Intent(this, FavoritesActivity.class)));

        importLocalDataOnce();
    }

    private void importLocalDataOnce() {
        SharedPreferences preferences = getSharedPreferences("oracle_import", MODE_PRIVATE);
        if (preferences.getBoolean("complete", false)) return;
        String deviceKey = preferences.getString("device_key", null);
        if (deviceKey == null) {
            deviceKey = UUID.randomUUID().toString();
            preferences.edit().putString("device_key", deviceKey).apply();
        }
        final String importPrefix = deviceKey;
        AppExecutors.io().execute(() -> {
            try {
                AppDatabase local = AppDatabase.getInstance(this);
                Map<Integer, Integer> newIds = new HashMap<>();
                for (ClothesEntity item : local.clothesDao().getAll()) {
                    if (item.imagePath == null || !new File(item.imagePath).isFile()) {
                        throw new IOException("Missing local image for clothes #" + item.id);
                    }
                    int newId = OracleApi.createClothes(item, importPrefix + ":clothes:" + item.id);
                    newIds.put(item.id, newId);
                }
                for (CoordinationEntity outfit : local.coordinationDao().getAll()) {
                    String rawIds = outfit.clothesIds;
                    if (rawIds == null || rawIds.isEmpty()) {
                        rawIds = outfit.topClothesId + "," + outfit.bottomClothesId;
                    }
                    StringBuilder mapped = new StringBuilder();
                    for (String rawId : rawIds.split(",")) {
                        Integer newId = newIds.get(Integer.parseInt(rawId.trim()));
                        if (newId == null) continue;
                        if (mapped.length() > 0) mapped.append(',');
                        mapped.append(newId);
                    }
                    if (mapped.length() == 0) continue;
                    outfit.clothesIds = mapped.toString();
                    OracleApi.createOutfit(outfit, importPrefix + ":outfit:" + outfit.coordiId);
                }
                preferences.edit().putBoolean("complete", true).apply();
                if (!newIds.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this,
                            "\uAE30\uC874 \uC758\uB958\uB97C Oracle\uB85C \uAC00\uC838\uC654\uC2B5\uB2C8\uB2E4.", Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                // The next launch retries incomplete imports using stable import keys.
                runOnUiThread(() -> Toast.makeText(this,
                        "기존 의류를 Oracle로 가져오지 못했습니다. 사진과 서버 연결을 확인하세요.",
                        Toast.LENGTH_LONG).show());
            }
        });
    }
}
