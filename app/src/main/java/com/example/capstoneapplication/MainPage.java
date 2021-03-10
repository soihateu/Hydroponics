package com.example.capstoneapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainPage extends MainActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent backIntent = new Intent(MainPage.this, MainActivity.class);
                startActivity(backIntent);
            }
        });

        Button lightButton = findViewById(R.id.lightButton);
        lightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent tempIntent = new Intent(MainPage.this, LightSettings.class);
                startActivity(tempIntent);
            }
        });

        Button humidityButton = findViewById(R.id.humidityButton);
        humidityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent tempIntent = new Intent(MainPage.this, HumiditySettings.class);
                startActivity(tempIntent);
            }
        });

        Button temperatureButton = findViewById(R.id.tempButton);
        temperatureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent tempIntent = new Intent(MainPage.this, TemperatureSettings.class);
                startActivity(tempIntent);
            }
        });
    }
}
