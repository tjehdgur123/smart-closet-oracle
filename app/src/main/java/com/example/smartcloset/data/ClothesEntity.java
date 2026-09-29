package com.example.smartcloset.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Clothes_Table")
public class ClothesEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "image_path")
    public String imagePath;

    public String category;
    public String color;
    public String season;

    @ColumnInfo(name = "is_favorite")
    public boolean isFavorite;

    public ClothesEntity(String imagePath, String category, String color, String season, boolean isFavorite) {
        this.imagePath = imagePath;
        this.category = category;
        this.color = color;
        this.season = season;
        this.isFavorite = isFavorite;
    }
}
