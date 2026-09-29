package com.example.smartcloset.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.migration.Migration;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {ClothesEntity.class, CoordinationEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Coordination_Table ADD COLUMN clothes_ids TEXT");
            database.execSQL("UPDATE Coordination_Table SET clothes_ids = top_clothes_id || ',' || bottom_clothes_id WHERE clothes_ids IS NULL");
        }
    };

    public abstract ClothesDao clothesDao();

    public abstract CoordinationDao coordinationDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "smart_closet.db"
                    ).addMigrations(MIGRATION_1_2).build();
                }
            }
        }
        return instance;
    }
}
