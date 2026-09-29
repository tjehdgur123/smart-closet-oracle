package com.example.smartcloset.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CoordinationDao {
    @Insert
    void insert(CoordinationEntity coordination);

    @Update
    void update(CoordinationEntity coordination);

    @Query("SELECT * FROM Coordination_Table ORDER BY coordi_id DESC")
    List<CoordinationEntity> getAll();

    @Query("SELECT * FROM Coordination_Table WHERE is_favorite = 1 ORDER BY coordi_id DESC")
    List<CoordinationEntity> getFavorites();
}
