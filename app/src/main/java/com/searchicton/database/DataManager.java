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

    public void discoverLandmark(Landmark landmark) {
        appDb.landmarkDAO().discoverLandmark(landmark.getId());
        landmark.discover();
    }

    /** Pass in a Landmark object or Landmark[] array */
    public void updateLandmarks(Landmark[] newLandmarks, List<Landmark> removedLandmarks) {
        appDb.landmarkDAO().insertLandmarks(newLandmarks);
        appDb.landmarkDAO().deleteLandmarks(removedLandmarks);
    }

    public void deleteAllLandmarks() {
        appDb.landmarkDAO().deleteAllLandmarks();
    }

    public int getTotalScore() { return appDb.landmarkDAO().getTotalScore(); }

    public void resetAllLandmarksDiscoverable() { appDb.landmarkDAO().resetAllLandmarksDiscoverable(); }

}