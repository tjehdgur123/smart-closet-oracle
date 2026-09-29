package com.example.smartcloset.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartcloset.R;
import com.example.smartcloset.data.ClothesEntity;
import com.example.smartcloset.data.CoordinationEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoordinationAdapter extends RecyclerView.Adapter<CoordinationAdapter.CoordinationViewHolder> {
    public interface FavoriteListener {
        void onFavoriteChanged(CoordinationEntity coordination, boolean isFavorite);
    }

    public interface ClickListener {
        void onCoordinationClicked(CoordinationEntity coordination, List<ClothesEntity> clothes);
    }

    private final FavoriteListener favoriteListener;
    private final ClickListener clickListener;
    private final List<CoordinationEntity> items = new ArrayList<>();
    private final Map<Integer, ClothesEntity> clothesById = new HashMap<>();

    public CoordinationAdapter(FavoriteListener favoriteListener) {
        this(favoriteListener, null);
    }

    public CoordinationAdapter(FavoriteListener favoriteListener, ClickListener clickListener) {
        this.favoriteListener = favoriteListener;
        this.clickListener = clickListener;
    }

    public void submitList(List<CoordinationEntity> coordinations, List<ClothesEntity> clothes) {
        items.clear();
        items.addAll(coordinations);
        clothesById.clear();
        for (ClothesEntity item : clothes) {
            clothesById.put(item.id, item);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CoordinationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_coordination, parent, false);
        return new CoordinationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CoordinationViewHolder holder, int position) {
        CoordinationEntity item = items.get(position);
        holder.txtName.setText(item.coordiName);
        holder.checkFavorite.setOnCheckedChangeListener(null);
        holder.checkFavorite.setChecked(item.isFavorite);
        holder.checkFavorite.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.isFavorite = isChecked;
            favoriteListener.onFavoriteChanged(item, isChecked);
        });
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onCoordinationClicked(item, clothesFor(item));
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private List<ClothesEntity> clothesFor(CoordinationEntity coordination) {
        List<ClothesEntity> result = new ArrayList<>();
        String ids = coordination.clothesIds;
        if (ids == null || ids.trim().isEmpty()) {
            ids = coordination.topClothesId + "," + coordination.bottomClothesId;
        }

        String[] parts = ids.split(",");
        for (String part : parts) {
            try {
                ClothesEntity item = clothesById.get(Integer.parseInt(part.trim()));
                if (item != null) {
                    result.add(item);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    static class CoordinationViewHolder extends RecyclerView.ViewHolder {
        TextView txtName;
        CheckBox checkFavorite;

        CoordinationViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtCoordinationName);
            checkFavorite = itemView.findViewById(R.id.checkCoordinationFavorite);
        }
    }
}
