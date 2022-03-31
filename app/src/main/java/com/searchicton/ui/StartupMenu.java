package com.searchicton.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.searchicton.R;

public class StartupMenu extends AppCompatActivity {

    private Button startButton;
    private Button instructionsButton;
    private Button optionsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If device is running Android 9+, then make activity fullscreen (past the notch cutout, if the phone has one)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        setContentView(R.layout.activity_startup_menu);

        getSupportActionBar().hide();

        startButton = (Button) findViewById(R.id.menu_start);
        instructionsButton = (Button) findViewById(R.id.menu_instructions);
        optionsButton = (Button) findViewById(R.id.menu_options);

        startButton.setOnClickListener(view -> startMaps());
        instructionsButton.setOnClickListener(view -> howToPlayStart());
        optionsButton.setOnClickListener(view -> optionsStart());
    }

    private void startMaps() {
        Log.i("StartupMenu", "starting map intent");
        Intent mapsIntent = new Intent(this, MapsActivity.class);
        startActivity(mapsIntent);
    }

    private void howToPlayStart() {
        Log.i("StartupMenu", "starting instructions intent");
        Intent howToPlayIntent = new Intent(this, HowToPlayActivity.class);
        startActivity(howToPlayIntent);
    }

    private void optionsStart() {
        Log.i("StartupMenu", "starting options intent");
        Intent optionsIntent = new Intent(this, OptionsActivity.class);
        startActivity(optionsIntent);
    }

}