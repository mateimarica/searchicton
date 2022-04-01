package com.searchicton.ui;

import androidx.appcompat.app.AppCompatActivity;
import com.searchicton.R;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.concurrent.Executors;

public class HowToPlayActivity extends AppCompatActivity {

    private final String TAG = "HowToPlayActivity";
    private Button okayButton;
    private Object View;
    private SoundPool soundPool;
    private int clickID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_how_to_play);

        getSupportActionBar().hide();

        okayButton = (Button) findViewById(R.id.instructions_okay_button);

        okayButton.setOnClickListener(v -> {
            soundPool.play(clickID, 1, 1, 1, 0, 1);
            finish();
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool.Builder().setMaxStreams(10).build();
        } else {
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

}