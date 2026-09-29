package com.example.smartcloset.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectableClothesAdapter extends RecyclerView.Adapter<SelectableClothesAdapter.SelectableViewHolder> {
    public interface SelectionListener {
        void onSelectionChanged(List<ClothesEntity> selectedItems);
    }

    private final List<ClothesEntity> items = new ArrayList<>();
    private final Set<Integer> selectedIds = new HashSet<>();
    private final SelectionListener selectionListener;

    public SelectableClothesAdapter(List<ClothesEntity> clothes, List<ClothesEntity> selectedItems, SelectionListener selectionListener) {
        items.addAll(clothes);
        for (ClothesEntity item : selectedItems) {
            selectedIds.add(item.id);
        }
        this.selectionListener = selectionListener;
    }

    @NonNull
    @Override
    public SelectableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_select_clothes, parent, false);
        return new SelectableViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SelectableViewHolder holder, int position) {
        ClothesEntity item = items.get(position);
        holder.txtTitle.setText(item.category + " #" + item.id);
        holder.txtMeta.setText(item.color + " / " + item.season);
        Glide.with(holder.itemView).load(item.imagePath.startsWith("http") ? item.imagePath : new File(item.imagePath))
                .centerCrop().into(holder.imgClothes);

        holder.checkSelected.setOnCheckedChangeListener(null);
        holder.checkSelected.setChecked(selectedIds.contains(item.id));
        holder.itemView.setOnClickListener(v -> toggle(item));
        holder.checkSelected.setOnClickListener(v -> toggle(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void toggle(ClothesEntity item) {
        if (selectedIds.contains(item.id)) {
            selectedIds.remove(item.id);
        } else {
            selectedIds.add(item.id);
        }
        notifyDataSetChanged();
        selectionListener.onSelectionChanged(getSelectedItems());
    }

    private List<ClothesEntity> getSelectedItems() {
        List<ClothesEntity> selectedItems = new ArrayList<>();
        for (ClothesEntity item : items) {
            if (selectedIds.contains(item.id)) {
                selectedItems.add(item);
            }
        }
        return selectedItems;
    }

    static class SelectableViewHolder extends RecyclerView.ViewHolder {
        ImageView imgClothes;
        TextView txtTitle;
        TextView txtMeta;
        CheckBox checkSelected;

        SelectableViewHolder(@NonNull View itemView) {
            super(itemView);
            imgClothes = itemView.findViewById(R.id.imgSelectableClothes);
            txtTitle = itemView.findViewById(R.id.txtSelectableTitle);
            txtMeta = itemView.findViewById(R.id.txtSelectableMeta);
            checkSelected = itemView.findViewById(R.id.checkSelectable);
        }
    }
}
