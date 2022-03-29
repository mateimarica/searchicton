package com.searchicton.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
        setContentView(R.layout.activity_startup_menu);

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
        Toast.makeText(this, "How to Play Placeholder", Toast.LENGTH_SHORT).show();
    }

    private void optionsStart() {
        Log.i("StartupMenu", "starting options intent");
        Toast.makeText(this, "Options Placeholder", Toast.LENGTH_SHORT).show();
    }

}