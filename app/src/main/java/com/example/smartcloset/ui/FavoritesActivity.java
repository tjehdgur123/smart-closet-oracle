package com.example.smartcloset.ui;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartcloset.R;
import com.example.smartcloset.data.ClothesEntity;
import com.example.smartcloset.data.CoordinationEntity;
import com.example.smartcloset.data.OracleApi;
import com.example.smartcloset.util.AppExecutors;

import java.util.List;

public class FavoritesActivity extends AppCompatActivity {
    private ClothesAdapter clothesAdapter;
    private CoordinationAdapter coordinationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView clothesRecyclerView = findViewById(R.id.recyclerFavoriteClothes);
        clothesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        clothesAdapter = new ClothesAdapter((clothes, isFavorite) ->
                AppExecutors.io().execute(() -> {
                    try {
                        OracleApi.setClothesFavorite(clothes.id, isFavorite);
                        loadFavorites();
                    } catch (Exception e) {
                        showServerError();
                    }
                }));
        clothesRecyclerView.setAdapter(clothesAdapter);

        RecyclerView coordinationRecyclerView = findViewById(R.id.recyclerFavoriteCoordination);
        coordinationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        coordinationAdapter = new CoordinationAdapter((coordination, isFavorite) ->
                AppExecutors.io().execute(() -> {
                    try {
                        OracleApi.setOutfitFavorite(coordination.coordiId, isFavorite);
                        loadFavorites();
                    } catch (Exception e) {
                        showServerError();
                    }
                }));
        coordinationRecyclerView.setAdapter(coordinationAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void loadFavorites() {
        AppExecutors.io().execute(() -> {
            try {
                List<ClothesEntity> allClothes = OracleApi.getClothes();
                List<CoordinationEntity> allOutfits = OracleApi.getOutfits();
                java.util.ArrayList<ClothesEntity> favoriteClothes = new java.util.ArrayList<>();
                java.util.ArrayList<CoordinationEntity> favoriteOutfits = new java.util.ArrayList<>();
                for (ClothesEntity item : allClothes) if (item.isFavorite) favoriteClothes.add(item);
                for (CoordinationEntity item : allOutfits) if (item.isFavorite) favoriteOutfits.add(item);
                runOnUiThread(() -> {
                    clothesAdapter.submitList(favoriteClothes);
                    coordinationAdapter.submitList(favoriteOutfits, allClothes);
                });
            } catch (Exception e) {
                showServerError();
            }
        });
    }

    private void showServerError() {
        runOnUiThread(() -> Toast.makeText(this, "Oracle \uC11C\uBC84 \uC5F0\uACB0\uC744 \uD655\uC778\uD558\uC138\uC694.", Toast.LENGTH_LONG).show());
    }
}
