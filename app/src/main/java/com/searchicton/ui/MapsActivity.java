package com.searchicton.ui;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.Task;
import com.searchicton.R;
import com.searchicton.database.DataManager;
import com.searchicton.database.Landmark;
import com.searchicton.databinding.ActivityMapsBinding;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executors;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap map;
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
        map = googleMap;
        map.setInfoWindowAdapter(new MyInfoWindowAdapter(this));
        map.setMinZoomPreference(0.5F);
        LatLng freddy = new LatLng(45.961658502432456, -66.64279337439932);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(freddy, 15.0f));

        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));


        //get user location used, and center map to user's location
        enableMyLocation();
        FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(this);
        Task locationTask = fusedClient.getLastLocation().addOnSuccessListener(location -> {
            LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 15.0f));
        });

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Landmark> landmarks = new DataManager(this).getLandmarks();
            new Handler(Looper.getMainLooper()).post(() -> {
                for (Landmark landmark : landmarks) {
                    map.addMarker(landmark.getMarkerOptions()).setTag(landmark);
                }
            });
        });

        //map.setOnInfoWindowClickListener(this::onInfoWindowClick);
        map.setOnMarkerClickListener(marker -> {
            return false;
        });
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (map != null) {
                map.setMyLocationEnabled(true);
            }
        } else {
            Toast.makeText(this, "Poop", Toast.LENGTH_LONG).show();
        }
    }

    class MyInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private final View myContentsView;
        private final Context context;

        MyInfoWindowAdapter(Context context){
            this.context = context;
            myContentsView = getLayoutInflater().inflate(R.layout.info_window, null);
        }

        @Override
        public View getInfoContents(Marker marker) {
            Landmark landmark = (Landmark) marker.getTag();
            TextView tvTitle = ((TextView)myContentsView.findViewById(R.id.title));
            tvTitle.setText(landmark.getTitle());
            TextView tvDesc = ((TextView)myContentsView.findViewById(R.id.description));
            tvDesc.setText(landmark.getDescription());

            TextView tvPoints = ((TextView)myContentsView.findViewById(R.id.points));
            tvPoints.setText(landmark.getPoints() + "");

            Button tvButton = ((Button)myContentsView.findViewById(R.id.discoverButton));
            tvButton.setOnClickListener(view -> {
                Toast.makeText(context, "discoverButton clicked", Toast.LENGTH_LONG).show();
            });

            return myContentsView;
        }

        @Override
        public View getInfoWindow(Marker marker) {
//            View v = inflater.inflate(R.layout.balloon, null);
//            if (marker != null) {
//                textViewTitle = (TextView) v.findViewById(R.id.textViewTitle);
//                textViewTitle.setText(marker.getTitle());
//            }
//            return (v);
            return null;
        }

    }

}