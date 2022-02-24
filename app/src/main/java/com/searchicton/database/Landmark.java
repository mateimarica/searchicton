package com.searchicton.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(
    tableName = "landmarks",
    primaryKeys = {"longitude","latitude"}
)
public class Landmark {

    @NonNull public final String title, description, category, id;
    public final int points;
    public final double longitude, latitude;
    public boolean isDiscovered;

    public Landmark(String title, String description, int points, String category,
                    String id, double longitude, double latitude) {
        this.title = title;
        this.description = description;
        this.points = points;
        this.category = category;
        this.id = id;
        this.longitude = longitude;
        this.latitude = latitude;
        this.isDiscovered = false;
    }
}