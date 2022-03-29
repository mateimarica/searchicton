package com.searchicton.ui;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.searchicton.R;
import com.searchicton.database.DataManager;
import com.searchicton.database.Landmark;
import com.searchicton.databinding.ActivityMapsBinding;
import com.searchicton.util.Action;

import org.json.JSONException;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private ActivityMapsBinding binding;
    private static final String TAG = "MapsActivity",
            LANDMARKS_ENDPOINT = "https://searchicton.mateimarica.dev/landmarks",
            PREF_LANDMARKS_ETAG = "pref_landmarks_etag";

    private static final long MINIMUM_DISTANCE_CHANGE_FOR_UPDATE = 1, // meters
            MINIMUM_TIME_BETWEEN_UPDATE = 2000; // milliseconds

    private static final int LANDMARK_CLAIMABLE_DISTANCE = 50; // meters

    private Toolbar bottomToolbar;
    private TextView bottomToolbarTextView;
    private Toolbar topToolbar;
    private TextView topToolbarTextView;
    private LocationManager locationManager;
    private List<Landmark> landmarks;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate() invoked");

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bottomToolbar = (Toolbar) findViewById(R.id.bottom_toolbar);
        bottomToolbarTextView = (TextView) findViewById(R.id.bottom_toolbar_textview);
        topToolbar = (Toolbar) findViewById(R.id.top_toolbar);
        topToolbarTextView = (TextView) findViewById(R.id.top_toolbar_textview);
    }

    @Override
    @SuppressLint("MissingPermission")
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume() invoked");
        requestLocationPermission(() -> onLocationPermissionGranted());
    }

    /** To be called when location permission is confirmed to be granted.<br>
     * Checks if location is enabled. If it is:
     * <ul>
     *     <li>Initializes locationManager and sets {@link #checkClosestLandmark} to be called upon a location update.</li>
     *     <li>Calls {@link #checkForLandmarkUpdates} with a callback on the UI thread that initializes the map. This leads to {@link #onMapReady} being called.</li>
     * </ul>
     */
    @SuppressLint("MissingPermission")
    // Permission is checked prior to this method being called. Android Studio still complains. Ignore it
    private void onLocationPermissionGranted() {
        if (checkLocationEnabled()) {
            if (locationManager == null) {
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MINIMUM_TIME_BETWEEN_UPDATE,
                        MINIMUM_DISTANCE_CHANGE_FOR_UPDATE,
                        new LocationListener() {
                            @Override
                            public void onLocationChanged(Location location) {
                                checkClosestLandmark(location);
                            }

                            @Override
                            public void onProviderEnabled(String provider) {
                            } // Must override this method or may crash at runtime

                            @Override
                            public void onProviderDisabled(String provider) {
                            }  // Must override this method or may crash at runtime
                        }
                );
            }

            if (map == null) {
                // Checks if there are any landmark updates.
                checkForLandmarkUpdates((() -> startMap()));
            }
        }
    }

    private void startMap() {
        // Start up the map on the UI thread
        new Handler(Looper.getMainLooper()).post(() -> {
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        });
    }

    /** Finds the closest landmark and displays it in the bottom toolbar.
     * Also marks nearby landmarks as "claimable"
     * @param currentLocation The user's current location.
     */
    private void checkClosestLandmark(Location currentLocation) {
        if (landmarks != null && currentLocation != null) {
            double myLatitude = currentLocation.getLatitude(),
                    myLongitude = currentLocation.getLongitude();

            float[] result = new float[1];
            float smallestDistance = 1000000; // large number to start off with

            Landmark closestLandmark = null;

            // Cycles through every landmark to see which one is closest and marks each as claimable
            // or unclaimable, depending on their distance.
            for (Landmark landmark : landmarks) {
                if (!landmark.isDiscovered()) {
                    android.location.Location.distanceBetween(myLatitude, myLongitude, landmark.getLatitude(), landmark.getLongitude(), result);

                    if ((int) result[0] <= LANDMARK_CLAIMABLE_DISTANCE) {
                        landmark.setClaimable();
                    } else {
                        landmark.setUnclaimable();
                    }

                    if (smallestDistance > result[0]) {
                        smallestDistance = result[0];
                        closestLandmark = landmark;
                    }
                }
            }

            // Shows closest landmark in bottom toolbar
            if (closestLandmark != null) {
                bottomToolbarTextView.setText(closestLandmark.getTitle() + " (" + (int) smallestDistance + "m)");
            }
        }
    }


    /**
     * Checks the landmarks endpoint for changes and updates local database accordingly.
     * Stores the resource ETag in SharedPreferences to save on processing if landmarks are unchanged.
     * @param callback The function to invoke when the request is completed.
     */
    private void checkForLandmarkUpdates(Action callback) {
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

            callback.invoke();
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
        map.setMinZoomPreference(0.5F);
        LatLng freddy = new LatLng(45.961658502432456, -66.64279337439932); // Hardcoded fredericton coords to initially pan to
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(freddy, 15.0f));
        map.getUiSettings().setMapToolbarEnabled(false);

        map.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setTiltGesturesEnabled(true);

        final LatLngBounds FREDDYBOUNDS = new LatLngBounds(new LatLng(45.922518, -66.796030), new LatLng(46.021781, -66.516908));
        map.setLatLngBoundsForCameraTarget(FREDDYBOUNDS);

        // Get user location and center map to user's location
        map.setMyLocationEnabled(true);
        FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(this);
        fusedClient.getLastLocation().addOnSuccessListener(location -> {
            // Sometimes location is null and thus the map isn't centered initially. Don't know why.
            // It's usually right after enabling location services and/or GPS, so probably it takes some time to get current location
            if (location != null) {
                LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 15.0f));
            }
        });

        // Puts the markers on the map, then calls checkClosestLandmark
        Executors.newSingleThreadExecutor().execute(() -> {
            DataManager dm = new DataManager(this);
            landmarks = dm.getLandmarks();
            int score = dm.getTotalScore();
            new Handler(Looper.getMainLooper()).post(() -> {
                for (Landmark landmark : landmarks) {
                    if (!landmark.isDiscovered()) {
                        Marker marker = map.addMarker(landmark.getMarkerOptions());
                        marker.setTag(landmark);
                        landmark.setMarker(marker);
                    }
                }
                checkClosestLandmark(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
            });
        });

        map.setOnMarkerClickListener(marker -> {

            Landmark landmark = (Landmark) marker.getTag();
            LatLng landmarkCoords = new LatLng(landmark.getLatitude(), landmark.getLongitude());
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(landmarkCoords, 15.0f));

            showAlertbox(marker, landmark);
            return true; // returning true means the listener consumed the event (i.e., the default behavior should not occur, so the infowindow wouldn't appear)
        });

        updateScore();
    }

    public void showAlertbox(Marker marker, Landmark focusedLandmark) {


        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.landmark_popup);
        dialog.setCanceledOnTouchOutside(false);

        Point size = new Point();
        Window dialogWindow = dialog.getWindow();
        Display display = dialogWindow.getWindowManager().getDefaultDisplay();
        display.getSize(size);
        int width = size.x;
        dialogWindow.setLayout((int) (width * 0.95), WindowManager.LayoutParams.WRAP_CONTENT);
        dialogWindow.setGravity(Gravity.CENTER);

        TextView alertbox_title = (TextView) dialog
                .findViewById(R.id.alertbox_title);
        alertbox_title.setText(focusedLandmark.getTitle());
        TextView alertbox_desc = (TextView) dialog.findViewById(R.id.alertbox_desc);
        alertbox_desc.setText(focusedLandmark.getDescription());

        Button yes = (Button) dialog.findViewById(R.id.alertbox_yes);
        Button no = (Button) dialog.findViewById(R.id.alertbox_no);


        if (!focusedLandmark.isClaimable()) {
            yes.setEnabled(false);
        }

        DataManager dm = new DataManager(this);

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MapsActivity", "Landmark claim requested");
                Executors.newSingleThreadExecutor().execute(() -> {
                    dm.discoverLandmark(focusedLandmark.getId());
                });
                Executors.newSingleThreadExecutor().execute(() -> {
                    updateScore();
                });
                Executors.newSingleThreadExecutor().execute(() -> {

                });
                marker.setVisible(false);
                dialog.dismiss();
                Toast.makeText(MapsActivity.this, "Claimed landmark!", Toast.LENGTH_SHORT).show();
            }
        });

        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    /**
     * This launcher requests location permission requesting.
     * If it is granted, then {@link #onLocationPermissionGranted} is called.
     */
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.i(TAG, "Location permission granted by user");
                } else {
                    Log.i(TAG, "Location permission not granted by user");
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their decision.

                    // Maybe show a screen that says "Location must be granted to use this app"
                }
            });

    /** Checks if the location permission is granted. If it is, invoke the callback.
     * If not, request that user grant permission.
     * @param callback The function to be invoked if the permission is granted.
     */
    private void requestLocationPermission(Action callback) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Location permission already granted");
            callback.invoke();
        } else {
            // Directly asks user for permission.
            // The registered ActivityResultCallback gets the result of this request.
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            // This calls onPause(), then onResume() when user taps Allow or Deny
        }
    }

    /** Checks if location services are turned on is turned on.
     * If they aren't, shows an alert that leads to the system location activity.
     * @return True if location services are turned on, false otherwise.
     */
    private boolean checkLocationEnabled() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {

            Log.i(TAG, "Location not enabled. Showing alert to user...");

            // Build the alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Location Services Not Active");
            builder.setMessage("Please enable Location Services and GPS to use Searchicton");
            builder.setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Show location settings when the user acknowledges the alert dialog
                    dialogInterface.dismiss();
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);

                }
            });
            Dialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
            return false;
        }
        return true;
    }

    /**
     * Update the score through a database query. Search for all "discovered" landmarks, sum all the points
     */
    private void updateScore() {
        int score = -1;
        Executors.newSingleThreadExecutor().execute(() -> {
            Handler handler = new Handler(Looper.getMainLooper());
            final int setscore = new DataManager(this).getTotalScore();
            handler.post(() -> topToolbarTextView.setText("Score: " + String.valueOf(setscore)));
        });

        Log.i("MapsActivity", "Updated Score: " + score);
    }

}