package com.example.capstoneapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.slider.Slider;

public class LightSettings extends CustomizePage {
    private float lightValues = 0;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.light_settings);

        // Button to go back to main page
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LightSettings.this, CustomizePage.class);
                startActivity(intent);
            }
        });


        // Slider for Humidity
        Slider slider = findViewById(R.id.slider);
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {

            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                float setLight = 0;

                setLight = getLightValues();

                TextView textView = findViewById(R.id.setTemp);
                textView.setText(String.valueOf(setLight) + "%");

            }
        });

        slider.addOnChangeListener((slider1, value, fromUser) -> {
            lightValues = slider.getValue();

            System.out.println("The Light is: " + lightValues);

            setLightValues(lightValues);
        });

    }

    // Setter and Getter for lightValues
    public void setLightValues(float temp) {
        this.lightValues = temp;
    }
    public float getLightValues() {
        return lightValues;
    }
}
