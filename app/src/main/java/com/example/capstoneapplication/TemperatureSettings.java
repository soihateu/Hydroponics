package com.example.capstoneapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.slider.Slider;

public class TemperatureSettings extends CustomizePage {

    // Settings for Temperature
    CurrentSettings currentSettings = CurrentSettings.getInstance();
    float temperature = currentSettings.getTempValues();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.temperature_settings);
        TextView textView = findViewById(R.id.setTemp);

        textView.setText(String.valueOf(temperature) + "°C");

        // Button to go back to main page
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TemperatureSettings.this, CustomizePage.class);
                startActivity(intent);
            }
        });

        Slider slider = findViewById(R.id.slider);
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {

            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                currentSettings.setTempValues(slider.getValue());
                textView.setText(String.valueOf(temperature) + "°C");
            }
        });

        slider.addOnChangeListener((slider1, value, fromUser) -> {
            temperature = slider.getValue();
        });

    }
}
