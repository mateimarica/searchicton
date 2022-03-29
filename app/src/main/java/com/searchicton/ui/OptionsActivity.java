package com.searchicton.ui;

import androidx.appcompat.app.AppCompatActivity;
import com.searchicton.R;
import com.searchicton.database.DataManager;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.concurrent.Executors;

public class OptionsActivity extends AppCompatActivity {

    private Button resetButton;
    private Button suggestButton;
    private Button backButton;
    private final String[] ADDRESS = {"searchictonsuggest@gmail.com"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        getSupportActionBar().hide();

        resetButton = (Button) findViewById(R.id.options_reset_button);
        suggestButton = (Button) findViewById(R.id.options_suggest_button);
        backButton = (Button) findViewById(R.id.options_back_button);

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("OptionsActivity", "Landmarks reset requested");
                Executors.newSingleThreadExecutor().execute(() -> {
                    DataManager dm = new DataManager(OptionsActivity.this);
                    dm.resetAllLandmarksDiscoverable();
                });
                Toast.makeText(OptionsActivity.this, "Landmarks discovered status reset!", Toast.LENGTH_LONG).show();
            }
        });

        suggestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendSuggestion();
                Log.i("OptionsActivity", "Send suggestion button clicked");
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
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
            Log.e("DetailActvity-openWikiPage", "Couldn't open wikipedia page");
            e.printStackTrace();
        }
    }
}