package com.searchicton.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.searchicton.R;

public class StartupMenu extends AppCompatActivity {

    private final String TAG = "StartupMenu";
    private Button startButton;
    private Button instructionsButton;
    private Button optionsButton;
    private SoundPool soundPool;
    private int clickID;

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

    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool.Builder().setMaxStreams(10).build();
        }
        else {
            soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 1);
        }

        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                Log.i(TAG, "sound pool loaded");
            }
        });

        clickID = soundPool.load(this, R.raw.button_click, 1);
    }

    //Use onDestroy since onPause seems to cause soundPool to not work.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy called");

        soundPool.release();
    }

    private void startMaps() {
        Log.i(TAG, "starting map intent");
        soundPool.play(clickID, 1, 1, 1, 0, 1);
        Intent mapsIntent = new Intent(this, MapsActivity.class);
        startActivity(mapsIntent);
    }

    private void howToPlayStart() {
        Log.i(TAG, "starting instructions intent");
        soundPool.play(clickID, 1, 1, 1, 0, 1);
        Intent howToPlayIntent = new Intent(this, HowToPlayActivity.class);
        startActivity(howToPlayIntent);
    }

    private void optionsStart() {
        Log.i(TAG, "starting options intent");
        soundPool.play(clickID, 1, 1, 1, 0, 1);
        Intent optionsIntent = new Intent(this, OptionsActivity.class);
        startActivity(optionsIntent);
    }

}