package com.searchicton.ui;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.searchicton.R;
import com.searchicton.database.DataManager;
import com.searchicton.database.Landmark;
import com.searchicton.databinding.ActivityMapsBinding;
import com.searchicton.util.Action;
import com.searchicton.util.LocationHelper;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private ActivityMapsBinding binding;
    private static final String TAG = "MapsActivity",
            LANDMARKS_ENDPOINT = "https://searchicton.mateimarica.dev/landmarks",
            PREF_LANDMARKS_ETAG = "pref_landmarks_etag";

    private static final long MINIMUM_DISTANCE_CHANGE_FOR_UPDATE = 1, // meters
            MINIMUM_TIME_BETWEEN_UPDATE = 2000; // milliseconds

    private static final int LANDMARK_CLAIMABLE_DISTANCE = 250; // meters

    private Toolbar bottomToolbar, topToolbar;
    private TextView bottomToolbarTextView, topToolbarTextView;
    private LocationManager locationManager;
    private DataManager dataManager;
    private List<Landmark> landmarks;
    private Button backButton;

    private LocationHelper locationHelper;

    //SoundPool
    private SoundPool soundPool;
    private int gameFinishedID;
    private int clickID;
    private int landmarkClaimID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate() invoked");

        locationHelper = new LocationHelper(this);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bottomToolbar = (Toolbar) findViewById(R.id.bottom_toolbar);
        bottomToolbarTextView = (TextView) findViewById(R.id.bottom_toolbar_textview);
        topToolbar = (Toolbar) findViewById(R.id.top_toolbar);
        topToolbarTextView = (TextView) findViewById(R.id.top_toolbar_textview);
        backButton = (Button) findViewById(R.id.maps_back_button);

        dataManager = new DataManager(this);

        backButton.setOnClickListener(view -> {
            soundPool.play(clickID, 1, 1, 1, 0, 1);
            finish();
        });
    }

    @Override
    @SuppressLint("MissingPermission")
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume() invoked");

        locationHelper.requestLocationPermission(() -> onLocationPermissionGranted());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool.Builder().setMaxStreams(10).build();
        } else {
            soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 1);
        }

        soundPool.setOnLoadCompleteListener((soundPool, i, i1) -> Log.v(TAG, "soundPool loaded audio"));

        gameFinishedID = soundPool.load(this, R.raw.game_finish, 1);
        clickID = soundPool.load(this, R.raw.button_click, 1);
        landmarkClaimID = soundPool.load(this, R.raw.landmark_claim, 1);
    }

    //Use onDestroy since onPause seems to cause soundPool to not work.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy called");

        soundPool.release();
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
        if (locationHelper.checkLocationEnabled()) {
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
                            public void onProviderEnabled(String provider) {} // Must override this method or may crash at runtime

                            @Override
                            public void onProviderDisabled(String provider) {}  // Must override this method or may crash at runtime
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
        Log.v(TAG, "checking closest landmark");
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
                Log.v(TAG, "close landmark found");
            } else {
                bottomToolbarTextView.setText("No more landmarks ♥");
                Log.v(TAG, "no landmarks nearby");
            }
        }
    }


    /**
     * Checks the landmarks endpoint for changes and updates local database accordingly.
     * Stores the resource ETag in SharedPreferences to save on processing if landmarks are unchanged.
     * @param callback The function to invoke when the request is completed.
     */
    private void checkForLandmarkUpdates(Action callback) {
        Log.v(TAG, "checking for landmark updates");
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Log.v(TAG, "connectiong");
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
                            Landmark[] newLandmarks = Landmark.convertFromJsonArr(response);
                            List<Landmark> oldLandmarks = dataManager.getLandmarks();
                            List<Landmark> removedLandmarks = new ArrayList<>();

                            // Create hashmap of new landmark set
                            HashMap<Integer, Landmark> newLandmarksMap = new HashMap<>();
                            for (Landmark landmark : newLandmarks) {
                                newLandmarksMap.put(landmark.getId(), landmark);
                            }

                            // Keep track of every landmark that is in existing landmarks but isn't in new landmark set.
                            // These will be removed
                            for (int i = 0; i < oldLandmarks.size(); i++) {
                                Landmark landmark = oldLandmarks.get(i);
                                if (!newLandmarksMap.containsKey(landmark.getId())) {
                                    removedLandmarks.add(landmark);
                                }
                            }

                            dataManager.updateLandmarks(newLandmarks, removedLandmarks);

                        } catch (JSONException je) {
                            Log.e(TAG, "Couldn't parse JSON response.\nError: " + je + "\nResponse: " + response);
                            break;
                        }

                        sharedPrefs.edit()
                                .putString(PREF_LANDMARKS_ETAG, con.getHeaderField("ETag"))
                                .apply();

                        Log.v(TAG, "Landmark data updated.");
                        break;
                    case 304:
                        Log.v(TAG, "Landmark data unchanged since last request.");
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
        final LatLngBounds FREDDYBOUNDS = new LatLngBounds(new LatLng(45.881983444219394, -66.79317920377393), new LatLng(46.021781, -66.516908));
        map.setLatLngBoundsForCameraTarget(FREDDYBOUNDS);

        // Get user location and center map to user's location
        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);
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
            landmarks = dataManager.getLandmarks();


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

    @SuppressLint("MissingPermission")
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
        alertbox_desc.setText(focusedLandmark.getDescription() + "\nPoints: " + focusedLandmark.getPoints());

        Button yes = (Button) dialog.findViewById(R.id.alertbox_yes);
        Button no = (Button) dialog.findViewById(R.id.alertbox_no);


        if (!focusedLandmark.isClaimable()) {
            yes.setEnabled(false);
        }


        yes.setOnClickListener(v -> {
            Log.v(TAG, "Landmark claim requested");
            Executors.newSingleThreadExecutor().execute(() -> {
                dataManager.discoverLandmark(focusedLandmark);
                updateScore();

                boolean landmarksRemaining = false;
                for (Landmark landmark : landmarks) {
                    if (!landmark.isDiscovered()) {
                        landmarksRemaining = true;
                        break;
                    }
                }

                // Only play this sound if it's not the last one
                if (landmarksRemaining) {
                    soundPool.play(clickID, 1, 1, 1, 0, 1);
                }

                boolean finalLandmarksRemaining = landmarksRemaining;
                new Handler(Looper.getMainLooper()).post(() -> {
                    focusedLandmark.setUnclaimable(); // Need this line so waving animation stops
                    checkClosestLandmark(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));

                    if (!finalLandmarksRemaining) {
                        showGameFinished();
                    }
                });
            });



            marker.setVisible(false);
            dialog.dismiss();
            soundPool.play(landmarkClaimID, (float)0.5, (float)0.5, 1, 0, 1);
            Toast.makeText(MapsActivity.this, "+" + focusedLandmark.getPoints() + " points" , Toast.LENGTH_LONG).show();
        });

        no.setOnClickListener(v -> {
            soundPool.play(clickID, 1, 1, 1, 0, 1);
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Update the score through a database query. Search for all "discovered" landmarks, sum all the points
     */
    private void updateScore() {
        int score = -1;
        Executors.newSingleThreadExecutor().execute(() -> {
            Handler handler = new Handler(Looper.getMainLooper());
            final int setscore = dataManager.getTotalScore();
            handler.post(() -> topToolbarTextView.setText("Score: " + String.valueOf(setscore)));
        });

        Log.i("MapsActivity", "Updated Score: " + score);
    }

    private void showGameFinished() {

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.finished_game_popup);
        dialog.setCanceledOnTouchOutside(false);

        Point size = new Point();
        Window dialogWindow = dialog.getWindow();
        Display display = dialogWindow.getWindowManager().getDefaultDisplay();
        display.getSize(size);
        int width = size.x;
        dialogWindow.setLayout((int) (width * 0.95), WindowManager.LayoutParams.WRAP_CONTENT);
        dialogWindow.setGravity(Gravity.CENTER);

        TextView message = (TextView) dialog.findViewById(R.id.finished_text);
        Executors.newSingleThreadExecutor().execute(() -> {
            final int score = dataManager.getTotalScore();
            message.setText("Congratulations! You finished the game!\nYou can reset your progress in options if you want to play again, or wait until more landmarks are added. :)\n\nScore: " + score + "\n");
        });

        Button ok = (Button) dialog.findViewById(R.id.finished_ok);

        ok.setOnClickListener(v -> {
            Log.v(TAG, "Landmark claim requested");
            soundPool.play(clickID, 1, 1, 1, 0, 1);
            dialog.dismiss();
            finish();
        });

        soundPool.play(gameFinishedID, 1, 1, 1, 0, 1);
        dialog.show();
    }

}