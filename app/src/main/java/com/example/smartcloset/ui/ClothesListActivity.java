package com.example.smartcloset.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartcloset.R;
import com.example.smartcloset.data.ClothesEntity;
import com.example.smartcloset.data.OracleApi;
import com.example.smartcloset.util.AppExecutors;

import java.util.List;

public class ClothesListActivity extends AppCompatActivity {
    private static final String ALL = "\uC804\uCCB4";

    private ClothesAdapter adapter;
    private Spinner spinnerTypeFilter;
    private Spinner spinnerColorFilter;
    private Spinner spinnerSeasonFilter;

    private final String[] typeOptions = new String[]{
            ALL, "\uC0C1\uC758", "\uD558\uC758", "\uC544\uC6B0\uD130", "\uC2E0\uBC1C", "\uC561\uC138\uC11C\uB9AC"
    };

    private final String[] colorOptions = new String[]{
            ALL, "\uAC80\uC815", "\uD770\uC0C9", "\uD68C\uC0C9", "\uC544\uC774\uBCF4\uB9AC", "\uBCA0\uC774\uC9C0", "\uBE0C\uB77C\uC6B4",
            "\uB124\uC774\uBE44", "\uD30C\uB791", "\uD558\uB298\uC0C9", "\uCD08\uB85D", "\uCE74\uD0A4", "\uBBFC\uD2B8",
            "\uBE68\uAC15", "\uBD84\uD64D", "\uBCF4\uB77C", "\uB178\uB791", "\uC8FC\uD669",
            "\uC2E4\uBC84", "\uACE8\uB4DC", "\uBA40\uD2F0\uCEEC\uB7EC", "\uAE30\uD0C0"
    };

    private final String[] seasonOptions = new String[]{
            ALL, "\uBD04", "\uC5EC\uB984", "\uAC00\uC744", "\uACA8\uC6B8", "\uC0AC\uACC4\uC808"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clothes_list);

        spinnerTypeFilter = findViewById(R.id.spinnerTypeFilter);
        spinnerColorFilter = findViewById(R.id.spinnerColorFilter);
        spinnerSeasonFilter = findViewById(R.id.spinnerSeasonFilter);
        RecyclerView recyclerView = findViewById(R.id.recyclerClothes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new ClothesAdapter(
                (clothes, isFavorite) ->
                        AppExecutors.io().execute(() -> {
                            try {
                                OracleApi.setClothesFavorite(clothes.id, isFavorite);
                            } catch (Exception e) {
                                showServerError();
                                runOnUiThread(this::loadClothes);
                            }
                        }),
                this::confirmDelete
        );
        recyclerView.setAdapter(adapter);

        setupSpinner(spinnerTypeFilter, typeOptions);
        setupSpinner(spinnerColorFilter, colorOptions);
        setupSpinner(spinnerSeasonFilter, seasonOptions);
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadClothes();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                loadClothes();
            }
        };
        spinnerTypeFilter.setOnItemSelectedListener(listener);
        spinnerColorFilter.setOnItemSelectedListener(listener);
        spinnerSeasonFilter.setOnItemSelectedListener(listener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadClothes();
    }

    private void setupSpinner(Spinner spinner, String[] values) {
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                values
        );
        spinner.setAdapter(filterAdapter);
    }

    private void loadClothes() {
        if (spinnerTypeFilter.getSelectedItem() == null || spinnerColorFilter.getSelectedItem() == null
                || spinnerSeasonFilter.getSelectedItem() == null) return;
        String selectedType = spinnerTypeFilter.getSelectedItem().toString();
        String selectedColor = spinnerColorFilter.getSelectedItem().toString();
        String selectedSeason = spinnerSeasonFilter.getSelectedItem().toString();
        AppExecutors.io().execute(() -> {
            try {
                List<ClothesEntity> clothes = OracleApi.getClothes();
                clothes.removeIf(item ->
                        (!ALL.equals(selectedType) && !selectedType.equals(item.category))
                                || (!ALL.equals(selectedColor) && !selectedColor.equals(item.color))
                                || (!ALL.equals(selectedSeason) && !selectedSeason.equals(item.season))
                );
                runOnUiThread(() -> adapter.submitList(clothes));
            } catch (Exception e) {
                showServerError();
            }
        });
    }

    private void confirmDelete(ClothesEntity clothes) {
        new AlertDialog.Builder(this)
                .setTitle("\uC758\uB958 \uC0AD\uC81C")
                .setMessage("\uC120\uD0DD\uD55C \uC758\uB958\uB97C \uC0AD\uC81C\uD560\uAE4C\uC694?")
                .setNegativeButton("\uCDE8\uC18C", null)
                .setPositiveButton("\uC0AD\uC81C", (dialog, which) -> deleteClothes(clothes))
                .show();
    }

    private void deleteClothes(ClothesEntity clothes) {
        AppExecutors.io().execute(() -> {
            try {
                OracleApi.deleteClothes(clothes.id);
                runOnUiThread(() -> {
                    Toast.makeText(this, "\uC0AD\uC81C\uB418\uC5C8\uC2B5\uB2C8\uB2E4.", Toast.LENGTH_SHORT).show();
                    loadClothes();
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
