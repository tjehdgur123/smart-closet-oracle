package com.example.smartcloset.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.smartcloset.R;
import com.example.smartcloset.data.ClothesEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ClothesAdapter extends RecyclerView.Adapter<ClothesAdapter.ClothesViewHolder> {
    public interface FavoriteListener {
        void onFavoriteChanged(ClothesEntity clothes, boolean isFavorite);
    }

    public interface DeleteListener {
        void onDeleteClicked(ClothesEntity clothes);
    }

    private final FavoriteListener favoriteListener;
    private final DeleteListener deleteListener;
    private final List<ClothesEntity> items = new ArrayList<>();

    public ClothesAdapter(FavoriteListener favoriteListener) {
        this(favoriteListener, null);
    }

    public ClothesAdapter(FavoriteListener favoriteListener, DeleteListener deleteListener) {
        this.favoriteListener = favoriteListener;
        this.deleteListener = deleteListener;
    }

    public void submitList(List<ClothesEntity> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ClothesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_clothes, parent, false);
        return new ClothesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClothesViewHolder holder, int position) {
        ClothesEntity item = items.get(position);
        holder.txtCategory.setText(item.category);
        holder.txtMeta.setText(item.color + " / " + item.season);
        Glide.with(holder.itemView).load(item.imagePath.startsWith("http") ? item.imagePath : new File(item.imagePath))
                .centerCrop().into(holder.imgClothes);

        holder.checkFavorite.setOnCheckedChangeListener(null);
        holder.checkFavorite.setChecked(item.isFavorite);
        holder.checkFavorite.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.isFavorite = isChecked;
            favoriteListener.onFavoriteChanged(item, isChecked);
        });

        if (deleteListener == null) {
            holder.btnDelete.setVisibility(View.GONE);
        } else {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> deleteListener.onDeleteClicked(item));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ClothesViewHolder extends RecyclerView.ViewHolder {
        ImageView imgClothes;
        TextView txtCategory;
        TextView txtMeta;
        CheckBox checkFavorite;
        Button btnDelete;

        ClothesViewHolder(@NonNull View itemView) {
            super(itemView);
            imgClothes = itemView.findViewById(R.id.imgClothes);
            txtCategory = itemView.findViewById(R.id.txtCategory);
            txtMeta = itemView.findViewById(R.id.txtMeta);
            checkFavorite = itemView.findViewById(R.id.checkFavorite);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
