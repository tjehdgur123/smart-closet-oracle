package com.example.smartcloset.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "Coordination_Table")
public class CoordinationEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "coordi_id")
    public int coordiId;

    @ColumnInfo(name = "coordi_name")
    public String coordiName;

    @ColumnInfo(name = "top_clothes_id")
    public int topClothesId;

    @ColumnInfo(name = "bottom_clothes_id")
    public int bottomClothesId;

    @ColumnInfo(name = "is_favorite")
    public boolean isFavorite;

    @ColumnInfo(name = "clothes_ids")
    public String clothesIds;

    public CoordinationEntity(String coordiName, int topClothesId, int bottomClothesId, boolean isFavorite, String clothesIds) {
        this.coordiName = coordiName;
        this.topClothesId = topClothesId;
        this.bottomClothesId = bottomClothesId;
        this.isFavorite = isFavorite;
        this.clothesIds = clothesIds;
    }

    @Ignore
    public CoordinationEntity(String coordiName, int topClothesId, int bottomClothesId, boolean isFavorite) {
        this(coordiName, topClothesId, bottomClothesId, isFavorite, topClothesId + "," + bottomClothesId);
    }
}
