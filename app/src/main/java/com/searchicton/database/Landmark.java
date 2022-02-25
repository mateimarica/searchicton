package com.searchicton.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Entity;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@Entity(
    tableName = "landmarks",
    primaryKeys = {"longitude","latitude"}
)
public class Landmark {

    @NonNull String title, description, category, id;
    int points;
    double longitude, latitude;
    boolean isDiscovered;

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

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getId() {
        return id;
    }

    public int getPoints() {
        return points;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public boolean isDiscovered() {
        return isDiscovered;
    }

    /**
     * @return A MarkerOptions object that includes the coordinates and title of the landmark.
     */
    public MarkerOptions getMarkerOptions() {
        LatLng latLng = new LatLng(latitude, longitude);
        return new MarkerOptions().position(latLng).title(title);
    }

    /**
     * Converts a JSON array of Landmark objects to a Landmark[]
     * @param jsonArr A string containing a JSON array of landmark objects.
     * @return An array of Landmark objects.
     * @throws JSONException If the JSON string is malformed.
     */
    public static Landmark[] convertFromJsonArr(String jsonArr) throws JSONException {
        JSONArray landmarksJsonArr = new JSONArray(jsonArr);
        Landmark[] landmarks = new Landmark[landmarksJsonArr.length()];
        for (int i = 0; i < landmarksJsonArr.length(); i++) {
            JSONObject landmarkJson = landmarksJsonArr.getJSONObject(i);
            landmarks[i] = new Landmark(
                    landmarkJson.getString("title"),
                    landmarkJson.getString("description"),
                    landmarkJson.getInt("points"),
                    landmarkJson.getString("category"),
                    landmarkJson.getString("id"),
                    landmarkJson.getDouble("longitude"),
                    landmarkJson.getDouble("latitude")
            );
        }
        return landmarks;
    }
}