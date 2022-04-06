package com.searchicton.util;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.searchicton.util.Action;

/**
 * Class for handling the requesting of Location permissions and enabling location services
 */
public class LocationHelper {

    private static final String TAG = "LocationHelper";

    private FragmentActivity activity;

    private Action locationPermGrantedCallback;

    private ActivityResultLauncher<String> requestPermissionLauncher;


    public LocationHelper(FragmentActivity activity) {
        this.activity = activity;

        requestPermissionLauncher = activity.registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.v(TAG, "Location permission granted by user");
                    if (locationPermGrantedCallback != null) {
                        locationPermGrantedCallback.invoke();
                    }
                } else {
                    Log.v(TAG, "Location permission not granted by user");
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their decision.

                    // Maybe show a screen that says "Location must be granted to use this app"
                }
            }
        );
    }

    /** Checks if location services are turned on is turned on.
     * If they aren't, shows an alert that leads to the system location activity.
     * @return True if location services are turned on, false otherwise.
     */
    public boolean checkLocationEnabled() {
        LocationManager locationManager = (LocationManager) activity.getSystemService(activity.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                !locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER)) {

            Log.v(TAG, "Location not enabled. Showing alert to user...");

            // Build the alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("Location Services Not Active");
            builder.setMessage("Searchicton needs location services to function. Location mode must use GPS.");
            builder.setPositiveButton("Enable", (dialogInterface, i) -> {
                // Show location settings when the user acknowledges the alert dialog
                dialogInterface.dismiss();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                activity.startActivity(intent);
            });
            Dialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
            return false;
        }
        return true;
    }

    /** Checks if the location permission is granted. If it is, invoke the callback.
     * If not, request that user grant permission.
     * @param callback The function to be invoked if the permission is granted.
     */
    public void requestLocationPermission(Action callback) {
        locationPermGrantedCallback = callback;
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG, "Location permission already granted");
            callback.invoke();
        } else {
            // Directly asks user for permission.
            // The registered ActivityResultCallback gets the result of this request.
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            // This calls onPause(), then onResume() when user taps Allow or Deny
        }
    }




}
