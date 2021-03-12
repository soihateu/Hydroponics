package com.example.capstoneapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class CustomizePage extends MainMenu {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.customize);

        // Back Button to return to main menu
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent backIntent = new Intent(CustomizePage.this, MainMenu.class);
                startActivity(backIntent);
            }
        });

        // Button to enter Light Settings
        Button lightButton = findViewById(R.id.lightButton);
        lightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent tempIntent = new Intent(CustomizePage.this, LightSettings.class);
                startActivity(tempIntent);
            }
        });

        // Button to enter Humidity Settings
        Button humidityButton = findViewById(R.id.humidityButton);
        humidityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent tempIntent = new Intent(CustomizePage.this, HumiditySettings.class);
                startActivity(tempIntent);
            }
        });

        // Button to enter Temperature settings
        Button temperatureButton = findViewById(R.id.tempButton);
        temperatureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent tempIntent = new Intent(CustomizePage.this, TemperatureSettings.class);
                startActivity(tempIntent);
            }
        });
    }
}
