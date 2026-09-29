package com.example.smartcloset.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.smartcloset.R;
import com.example.smartcloset.data.ClothesEntity;
import com.example.smartcloset.data.CoordinationEntity;
import com.example.smartcloset.data.OracleApi;
import com.example.smartcloset.util.AppExecutors;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CoordinationActivity extends AppCompatActivity {
    private EditText editName;
    private TextView txtSelectedClothes;
    private CheckBox checkFavorite;
    private CoordinationAdapter adapter;

    private final List<ClothesEntity> allClothes = new ArrayList<>();
    private final List<ClothesEntity> selectedClothes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coordination);

        editName = findViewById(R.id.editCoordinationName);
        txtSelectedClothes = findViewById(R.id.txtSelectedClothes);
        checkFavorite = findViewById(R.id.checkCoordinationFavorite);

        RecyclerView recyclerView = findViewById(R.id.recyclerCoordination);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnChooseClothes).setOnClickListener(v -> showClothesPicker());
        findViewById(R.id.btnSaveCoordination).setOnClickListener(v -> saveCoordination());

        adapter = new CoordinationAdapter(
                (coordination, isFavorite) ->
                        AppExecutors.io().execute(() -> {
                            try {
                                OracleApi.setOutfitFavorite(coordination.coordiId, isFavorite);
                            } catch (Exception e) {
                                showServerError();
                                loadData();
                            }
                        }),
                this::showCoordinationDetail
        );
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        AppExecutors.io().execute(() -> {
            try {
                List<ClothesEntity> clothes = OracleApi.getClothes();
                List<CoordinationEntity> coordinations = OracleApi.getOutfits();
                runOnUiThread(() -> {
                    allClothes.clear();
                    allClothes.addAll(clothes);
                    adapter.submitList(coordinations, clothes);
                });
            } catch (Exception e) {
                showServerError();
            }
        });
    }

    private void showClothesPicker() {
        if (allClothes.isEmpty()) {
            Toast.makeText(this, "\uBA3C\uC800 \uC758\uB958\uB97C \uB4F1\uB85D\uD558\uC138\uC694.", Toast.LENGTH_SHORT).show();
            return;
        }

        RecyclerView pickerRecycler = new RecyclerView(this);
        int padding = (int) (12 * getResources().getDisplayMetrics().density);
        pickerRecycler.setPadding(padding, padding, padding, padding);
        pickerRecycler.setLayoutManager(new LinearLayoutManager(this));

        List<ClothesEntity> draftSelection = new ArrayList<>(selectedClothes);
        SelectableClothesAdapter selectableAdapter = new SelectableClothesAdapter(
                allClothes,
                selectedClothes,
                newSelectedItems -> {
                    draftSelection.clear();
                    draftSelection.addAll(newSelectedItems);
                }
        );
        pickerRecycler.setAdapter(selectableAdapter);

        new AlertDialog.Builder(this)
                .setTitle("\uCF54\uB514\uC5D0 \uB123\uC744 \uC637 \uC120\uD0DD")
                .setView(pickerRecycler)
                .setNegativeButton("\uCDE8\uC18C", null)
                .setPositiveButton("\uD655\uC778", (dialog, which) -> {
                    selectedClothes.clear();
                    selectedClothes.addAll(draftSelection);
                    updateSelectedText();
                })
                .show();
    }

    private void updateSelectedText() {
        if (selectedClothes.isEmpty()) {
            txtSelectedClothes.setText("\uC120\uD0DD\uB41C \uC637: \uC5C6\uC74C");
            return;
        }

        StringBuilder builder = new StringBuilder("\uC120\uD0DD\uB41C \uC637: ");
        for (int i = 0; i < selectedClothes.size(); i++) {
            ClothesEntity item = selectedClothes.get(i);
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(item.category).append(" #").append(item.id);
        }
        txtSelectedClothes.setText(builder.toString());
    }

    private void saveCoordination() {
        String name = editName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "\uCF54\uB514 \uC774\uB984\uC744 \uC785\uB825\uD558\uC138\uC694.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedClothes.isEmpty()) {
            Toast.makeText(this, "\uCF54\uB514\uC5D0 \uB123\uC744 \uC637\uC744 \uC120\uD0DD\uD558\uC138\uC694.", Toast.LENGTH_SHORT).show();
            return;
        }

        int firstId = selectedClothes.get(0).id;
        int secondId = selectedClothes.size() > 1 ? selectedClothes.get(1).id : firstId;
        CoordinationEntity coordination = new CoordinationEntity(
                name,
                firstId,
                secondId,
                checkFavorite.isChecked(),
                joinSelectedIds()
        );

        AppExecutors.io().execute(() -> {
            try {
                OracleApi.createOutfit(coordination);
                runOnUiThread(() -> {
                    Toast.makeText(this, "\uCF54\uB514\uAC00 \uC800\uC7A5\uB418\uC5C8\uC2B5\uB2C8\uB2E4.", Toast.LENGTH_SHORT).show();
                    editName.setText("");
                    checkFavorite.setChecked(false);
                    selectedClothes.clear();
                    updateSelectedText();
                    loadData();
                });
            } catch (Exception e) {
                showServerError();
            }
        });
    }

    private String joinSelectedIds() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < selectedClothes.size(); i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(selectedClothes.get(i).id);
        }
        return builder.toString();
    }

    private void showCoordinationDetail(CoordinationEntity coordination, List<ClothesEntity> clothes) {
        View view = getLayoutInflater().inflate(R.layout.dialog_coordination_detail, null);
        TextView txtName = view.findViewById(R.id.txtDetailName);
        LinearLayout container = view.findViewById(R.id.layoutDetailContainer);

        txtName.setText(coordination.coordiName);
        if (clothes.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("\uC800\uC7A5\uB41C \uC758\uB958\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.");
            emptyText.setTextColor(getResources().getColor(R.color.text_muted));
            emptyText.setTextSize(14);
            container.addView(emptyText);
        } else {
            for (ClothesEntity item : clothes) {
                addDetailClothesView(container, item);
            }
        }

        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("\uD655\uC778", null)
                .show();
    }

    private void addDetailClothesView(LinearLayout container, ClothesEntity clothes) {
        int imageHeight = (int) (150 * getResources().getDisplayMetrics().density);
        int topMargin = (int) (10 * getResources().getDisplayMetrics().density);

        ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                imageHeight
        );
        imageParams.topMargin = topMargin;
        imageView.setLayoutParams(imageParams);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setVisibility(View.VISIBLE);
        Glide.with(this).load(clothes.imagePath.startsWith("http") ? clothes.imagePath : new File(clothes.imagePath))
                .centerCrop().into(imageView);
        container.addView(imageView);

        TextView textView = new TextView(this);
        textView.setText(clothes.category + " / " + clothes.color + " / " + clothes.season);
        textView.setTextColor(getResources().getColor(R.color.text_muted));
        textView.setTextSize(14);
        container.addView(textView);
    }

    private void showServerError() {
        runOnUiThread(() -> Toast.makeText(this, "Oracle \uC11C\uBC84 \uC5F0\uACB0\uC744 \uD655\uC778\uD558\uC138\uC694.", Toast.LENGTH_LONG).show());
    }
}
