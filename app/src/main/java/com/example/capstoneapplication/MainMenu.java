package com.example.capstoneapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainMenu extends MainActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);

        // Back Button to return to Initial Page
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent backIntent = new Intent(MainMenu.this, MainActivity.class);
                startActivity(backIntent);
            }
        });

        // Button to go to preset page
        Button presetButton = findViewById(R.id.presetButton);
        presetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMenu.this, PresetList.class);
                startActivity(intent);
            }
        });

        // Button to go to customize page
        Button customizeButton = findViewById(R.id.customizeButton);
        customizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMenu.this, CustomizePage.class);
                startActivity(intent);
            }
        });
    }
}
