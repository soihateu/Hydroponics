package com.example.capstoneapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.slider.Slider;

public class LightSettings extends MainPage {
    private float lightValues = 0;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.light_settings);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LightSettings.this, MainPage.class);
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
                float setHumidity = 0;

                setHumidity = getTempValues();

                TextView textView = findViewById(R.id.setTemp);
                textView.setText(String.valueOf(setHumidity) + "%");

            }
        });

        slider.addOnChangeListener((slider1, value, fromUser) -> {
            lightValues = slider.getValue();

            System.out.println("The Light is: " + lightValues);

            setTempValues(lightValues);
        });

    }

    public void setTempValues(float temp) {
        this.lightValues = temp;
    }

    public float getTempValues() {
        return lightValues;
    }
}
