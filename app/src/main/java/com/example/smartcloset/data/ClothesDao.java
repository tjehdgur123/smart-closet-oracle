package com.example.smartcloset.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ClothesDao {
    @Insert
    void insert(ClothesEntity clothes);

    @Update
    void update(ClothesEntity clothes);

    @Delete
    void delete(ClothesEntity clothes);

    @Query("SELECT * FROM Clothes_Table ORDER BY id DESC")
    List<ClothesEntity> getAll();

    @Query("SELECT * FROM Clothes_Table WHERE category = :category ORDER BY id DESC")
    List<ClothesEntity> getByCategory(String category);

    @Query("SELECT * FROM Clothes_Table WHERE color = :color ORDER BY id DESC")
    List<ClothesEntity> getByColor(String color);

    @Query("SELECT * FROM Clothes_Table WHERE category = :category AND color = :color ORDER BY id DESC")
    List<ClothesEntity> getByCategoryAndColor(String category, String color);

    @Query("SELECT * FROM Clothes_Table WHERE season = :season ORDER BY id DESC")
    List<ClothesEntity> getBySeason(String season);

    @Query("SELECT * FROM Clothes_Table WHERE is_favorite = 1 ORDER BY id DESC")
    List<ClothesEntity> getFavorites();

    @Query("SELECT * FROM Clothes_Table WHERE id = :id LIMIT 1")
    ClothesEntity getById(int id);
}
