package com.searchicton.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.List;

@Database(entities = {Landmark.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract LandmarkDAO landmarkDAO();

    public static synchronized AppDatabase getInstance(Context context) {
        return Room.databaseBuilder(context.getApplicationContext(),
                AppDatabase.class, "Database").build();
    }
}