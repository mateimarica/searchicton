package com.searchicton.ui;

import androidx.appcompat.app.AppCompatActivity;
import com.searchicton.R;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.concurrent.Executors;

public class HowToPlayActivity extends AppCompatActivity {

    private Button okayButton;
    private Object View;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_how_to_play);

        getSupportActionBar().hide();

        okayButton = (Button) findViewById(R.id.instructions_okay_button);

        okayButton.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }
}