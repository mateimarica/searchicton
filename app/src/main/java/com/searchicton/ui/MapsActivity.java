package com.searchicton.ui;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.searchicton.R;
import com.searchicton.database.DataManager;
import com.searchicton.database.Landmark;
import com.searchicton.databinding.ActivityMapsBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executors;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private static final String TAG = "MapsActivity";
    private static final String LANDMARKS_ENDPOINT = "https://searchicton.mateimarica.dev/landmarks";
    private static final String PREF_LANDMARKS_ETAG = "pref_landmarks_etag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // This will start the map fragment when complete
        checkForLandmarkUpdates();
    }

    /**
     * Checks the landmarks endpoints for changes and updates local database accordingly.
     * Stores the resource ETag in SharedPreferences to save on processing if landmarks are unchanged.
     */
    private void checkForLandmarkUpdates() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                HttpURLConnection con = (HttpURLConnection) new URL(LANDMARKS_ENDPOINT).openConnection();

                SharedPreferences sharedPrefs = getPreferences(Context.MODE_PRIVATE);
                String landmarksETag = sharedPrefs.getString(PREF_LANDMARKS_ETAG, null);

                if (landmarksETag != null) {
                    con.setRequestProperty("If-None-Match", landmarksETag);
                }

                int statusCode = con.getResponseCode();

                switch (statusCode) {
                    case 200:
                        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

                        String response = "",
                                inputLine;

                        while ((inputLine = in.readLine()) != null) {
                            response += inputLine + "\n";
                        }

                        try {
                            Landmark[] landmarks = Landmark.convertFromJsonArr(response);
                            DataManager dm = new DataManager(this);
                            dm.deleteAllLandmarks();
                            dm.insertLandmarks(landmarks);
                        } catch (JSONException je) {
                            Log.e(TAG, "Couldn't parse JSON response.\nError: " + je + "\nResponse: " + response);
                            break;
                        }

                        sharedPrefs.edit()
                                .putString(PREF_LANDMARKS_ETAG, con.getHeaderField("ETag"))
                                .apply();

                        Log.i(TAG, "Landmark data updated.");
                        break;
                    case 304:
                        Log.i(TAG, "Landmark data unchanged since last request.");
                        break;
                    default:
                        Log.e(TAG, "Couldn't retrieve data from " + LANDMARKS_ENDPOINT + ". Status code: " + statusCode);
                }
            } catch (MalformedURLException e) {
                Log.e(TAG, "URL is messed: " + e);
            } catch (IOException e) {
                Log.e(TAG, "Couldn't open connection: " + e);
            }

            // Start up the map on the UI thread
            new Handler(Looper.getMainLooper()).post(() -> {
                // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
                mapFragment.getMapAsync(this);
            });
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    @SuppressLint("MissingPermission")
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //get user location used, and center map to user's location
        mMap.setMyLocationEnabled(true);
        FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(this);
        Task locationTask = fusedClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
            }
        });


        Executors.newSingleThreadExecutor().execute(() -> {
            List<Landmark> landmarks = new DataManager(this).getLandmarks();
            new Handler(Looper.getMainLooper()).post(() -> {
                for (Landmark landmark : landmarks) {
                    mMap.addMarker(landmark.getMarkerOptions());
                }
            });
        });
    }

    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        return false;
    }

}