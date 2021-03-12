package com.example.capstoneapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.slider.Slider;

public class HumiditySettings extends CustomizePage {

    private float humidityValues = 0;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.humidity_settings);

        // Button to go back to main page
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HumiditySettings.this, CustomizePage.class);
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
                float saveHumidity = 0;

                saveHumidity = getTempValues();

                TextView textView = findViewById(R.id.setTemp);
                textView.setText(String.valueOf(saveHumidity) + "%");

            }
        });

        slider.addOnChangeListener((slider1, value, fromUser) -> {
            humidityValues = slider.getValue();
            System.out.println("The Humidity is: " + humidityValues);

            setTempValues(humidityValues);
        });

    }

    public void setTempValues(float temp) {
        this.humidityValues = temp;
    }

    public float getTempValues() {
        return humidityValues;
    }
}
