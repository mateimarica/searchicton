package com.searchicton.database;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Executors;

import static java.lang.Math.toDegrees;
import static java.lang.Math.sin;

@Entity(
    tableName = "landmarks",
    primaryKeys = {"longitude","latitude"}
)
public class Landmark {

    @NonNull String title, description, category;
    int points, id;
    double longitude, latitude;
    boolean isDiscovered;
    @Ignore private Marker marker;
    @Ignore private boolean isClaimable = false;
    @Ignore private final float markerColour = (int)(Math.random() * 12) * 30; // Random float between 0 and 330, markerColour % 30 = 0
    @Ignore private static final float unclaimableAlpha = 0.5F,
                                       claimableAlpha = 1F;

    @Ignore private static final double rangeRadians = Math.PI / 2.0,
                                        incrementRadians = Math.PI / 32.0;


    public Landmark(String title, String description, int points, String category,
                    int id, double longitude, double latitude) {
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

    public int getId() {
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

    /** Only call this method if you're also calling {@link LandmarkDAO#discoverLandmark(int)} ! */
    public void discover() {
        isDiscovered = true;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
        marker.setIcon(BitmapDescriptorFactory.defaultMarker(markerColour));
        marker.setAlpha(unclaimableAlpha);
    }

    public Marker getMarker() {
        return marker;
    }



    public void setClaimable() {
        if (!isClaimable) {
            marker.setAlpha(claimableAlpha);
            isClaimable = true;

            Executors.newSingleThreadExecutor().execute(() -> {
                Handler handler = new Handler(Looper.getMainLooper());
                for (float i = (float) ((Math.random() * 2 - 1) * rangeRadians); i < rangeRadians; i += incrementRadians) {
                    float degrees = (float) toDegrees(sin(i)) / 4.5F;
                    handler.post(() -> {
                        marker.setRotation(degrees);
                    });
                    sleep(40);
                }
                while (isClaimable) {
                    for (double j = rangeRadians; j > -rangeRadians; j -= incrementRadians) {
                        float degrees = (float) toDegrees(sin(j)) / 4.5F;
                        handler.post(() -> {
                            marker.setRotation(degrees);
                        });
                        sleep(40);
                    }
                    for (double j = -rangeRadians; j < rangeRadians; j += incrementRadians) {
                        float degrees = (float) toDegrees(sin(j)) / 4.5F;
                        handler.post(() -> {
                            marker.setRotation(degrees);
                        });
                        sleep(40);
                    }
                }

                handler.post(() -> {
                    marker.setRotation(0F);
                });

            });
        }
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {} // Whatever
    }

    public void setUnclaimable() {
        if (isClaimable) {
            marker.setAlpha(unclaimableAlpha);
            isClaimable = false;
        }
    }

    public boolean isClaimable() {
        return isClaimable;
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
        for (int i = 0; i < landmarks.length; i++) {
            JSONObject landmarkJson = landmarksJsonArr.getJSONObject(i);
            landmarks[i] = new Landmark(
                    landmarkJson.getString("title"),
                    landmarkJson.getString("description"),
                    landmarkJson.getInt("points"),
                    landmarkJson.getString("category"),
                    landmarkJson.getInt("id"),
                    landmarkJson.getDouble("longitude"),
                    landmarkJson.getDouble("latitude")
            );
        }
        return landmarks;
    }
}