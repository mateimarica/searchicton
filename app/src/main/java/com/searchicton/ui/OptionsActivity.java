package com.searchicton.ui;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.model.Marker;
import com.searchicton.R;
import com.searchicton.database.DataManager;
import com.searchicton.database.Landmark;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Point;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.Executors;

public class OptionsActivity extends AppCompatActivity {

    private final String TAG = "OptionsActivity";
    private Button resetButton;
    private Button suggestButton;
    private Button backButton;
    private final String[] ADDRESS = {"searchictonsuggest@gmail.com"};
    private SoundPool soundPool;
    private int areYouSureID;
    private int clickID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        getSupportActionBar().hide();

        resetButton = (Button) findViewById(R.id.options_reset_button);
        suggestButton = (Button) findViewById(R.id.options_suggest_button);
        backButton = (Button) findViewById(R.id.options_back_button);

        resetButton.setOnClickListener(view -> {
            Log.i(TAG, "Landmarks reset requested");
            showAlertbox();
        });

        suggestButton.setOnClickListener(view -> {
            soundPool.play(clickID, 1, 1, 1, 0, 1);
            sendSuggestion();
            Log.i(TAG, "Send suggestion button clicked");
        });

        backButton.setOnClickListener(view -> {
            soundPool.play(clickID, 1, 1, 1, 0, 1);
            finish();
        });
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
        areYouSureID = soundPool.load(this, R.raw.are_you_sure, 1);
    }

    //Use onDestroy since onPause seems to cause soundPool to not work.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy called");

        soundPool.release();
    }

    public void sendSuggestion() {
        Log.i(TAG, "Sending a suggestion to devs!");
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.putExtra(Intent.EXTRA_EMAIL, ADDRESS);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "I have a suggestion!");
        try {
            startActivity(emailIntent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Couldn't request email.");
            Toast.makeText(this, "Couldn't open email", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void showAlertbox() {

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.warning_popup);
        dialog.setCanceledOnTouchOutside(false);

        Point size = new Point();
        Window dialogWindow = dialog.getWindow();
        Display display = dialogWindow.getWindowManager().getDefaultDisplay();
        display.getSize(size);
        int width = size.x;
        dialogWindow.setLayout((int) (width * 0.95), WindowManager.LayoutParams.WRAP_CONTENT);
        dialogWindow.setGravity(Gravity.CENTER);

        Button yes = (Button) dialog.findViewById(R.id.warningbox_yes);
        Button no = (Button) dialog.findViewById(R.id.warningbox_no);

        yes.setOnClickListener(v -> {
            Log.v(TAG, "Resetting Landmarks");
            soundPool.play(clickID, 1, 1, 1, 0, 1);
            Executors.newSingleThreadExecutor().execute(() -> {
                DataManager dm = new DataManager(OptionsActivity.this);
                dm.resetAllLandmarksDiscoverable();
            });
            dialog.dismiss();
            Toast.makeText(OptionsActivity.this, "Landmarks discovered status reset!", Toast.LENGTH_LONG).show();
        });

        no.setOnClickListener(v -> {
            soundPool.play(clickID, 1, 1, 1, 0, 1);
            dialog.dismiss();
        });

        soundPool.play(areYouSureID, 1, 1, 1, 0, 1);
        dialog.show();
    }
}