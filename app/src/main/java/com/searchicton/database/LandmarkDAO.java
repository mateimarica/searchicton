package com.searchicton.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LandmarkDAO {
    @Query("SELECT * FROM landmarks")
    List<Landmark> getLandmarks();

    @Query("SELECT COUNT(1) FROM landmarks WHERE id = :id")
    boolean landmarkExists(String id);

    @Query("UPDATE landmarks SET isDiscovered = NOT isDiscovered WHERE id = :id")
    void toggleLandmarkDiscovery(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLandmarks(Landmark... landmarks);

    @Query("DELETE FROM landmarks")
    void deleteAllLandmarks();

    @Query("SELECT SUM(points) FROM landmarks WHERE isDiscovered = 1")
    int getTotalScore();
}