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
import android.media.MediaPlayer;
import android.net.Uri;
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

    private Button resetButton;
    private Button suggestButton;
    private Button backButton;
    private final String[] ADDRESS = {"searchictonsuggest@gmail.com"};
    private MediaPlayer mediaPlay1;
    private MediaPlayer mediaPlay2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        getSupportActionBar().hide();

        resetButton = (Button) findViewById(R.id.options_reset_button);
        suggestButton = (Button) findViewById(R.id.options_suggest_button);
        backButton = (Button) findViewById(R.id.options_back_button);

        mediaPlay1 = MediaPlayer.create(this, R.raw.button_click);
        mediaPlay2 = MediaPlayer.create(this, R.raw.are_you_sure);

        resetButton.setOnClickListener(view -> {
            Log.i("OptionsActivity", "Landmarks reset requested");
            mediaPlay1.start();
            showAlertbox();
        });

        suggestButton.setOnClickListener(view -> {
            mediaPlay1.start();
            sendSuggestion();
            Log.i("OptionsActivity", "Send suggestion button clicked");
        });

        backButton.setOnClickListener(view -> {
            mediaPlay1.start();
            finish();
        });
    }

    public void sendSuggestion() {
        Log.i("OptionsActivity", "Sending a suggestion to devs!");
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.putExtra(Intent.EXTRA_EMAIL, ADDRESS);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "I have a suggestion!");
        try {
            startActivity(emailIntent);
        } catch (ActivityNotFoundException e) {
            Log.e("OptionsActivity", "Couldn't request email.");
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
            Log.v("OptionsActivity", "Resetting Landmarks");
            mediaPlay1.start();
            Executors.newSingleThreadExecutor().execute(() -> {
                DataManager dm = new DataManager(OptionsActivity.this);
                dm.resetAllLandmarksDiscoverable();
            });
            Toast.makeText(OptionsActivity.this, "Landmarks discovered status reset!", Toast.LENGTH_LONG).show();
            dialog.dismiss();
        });

        no.setOnClickListener(v -> {
            mediaPlay1.start();
            dialog.dismiss();
        });

        mediaPlay2.start();
        dialog.show();
    }
}