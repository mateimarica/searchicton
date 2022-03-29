package com.searchicton.database;

import android.content.Context;

import java.util.List;

public class DataManager {

    private AppDatabase appDb;

    public DataManager(Context context) {
        appDb = AppDatabase.getInstance(context);
    }

    public List<Landmark> getLandmarks() {
        return appDb.landmarkDAO().getLandmarks();
    }

    public boolean landmarkExists(String id) {
        return appDb.landmarkDAO().landmarkExists(id);
    }

    public void toggleLandmarkDiscovery(String id) {
        appDb.landmarkDAO().toggleLandmarkDiscovery(id);
    }

    /** Pass in a Landmark object or Landmark[] array */
    public void insertLandmarks(Landmark... landmarks) {
        appDb.landmarkDAO().insertLandmarks(landmarks);
    }

    public void deleteAllLandmarks() {
        appDb.landmarkDAO().deleteAllLandmarks();
    }

    public int getTotalScore() { return appDb.landmarkDAO().getTotalScore();}

}