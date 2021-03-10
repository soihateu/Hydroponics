package com.example.capstoneapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.slider.Slider;

public class TemperatureSettings extends MainPage {

    private float tempValues = 0;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.temperature_settings);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TemperatureSettings.this, MainPage.class);
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
                float setTemperature = 0;

                setTemperature = getTempValues();

                TextView textView = findViewById(R.id.setTemp);
                textView.setText(String.valueOf(setTemperature) + "°C");

            }
        });

        slider.addOnChangeListener((slider1, value, fromUser) -> {
            tempValues = slider.getValue();
            System.out.println("The temp is: " + tempValues);

            setTempValues(tempValues);
        });

    }

    public void setTempValues(float temp) {
        this.tempValues = temp;
    }

    public float getTempValues() {
        return tempValues;
    }
}
