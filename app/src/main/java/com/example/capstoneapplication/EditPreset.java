package com.example.capstoneapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.slider.Slider;

public class EditPreset extends PresetList {

    private float lightingValues = 0;
    private float humidityValues = 0;
    private float temperatureValues = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_preset);

        // Setup entries for Database
        DatabaseHelper db = new DatabaseHelper(this);
        EditText addName = findViewById(R.id.presetName);

        // Lighting Slider
        Slider sliderLighting = findViewById(R.id.sliderLighting);
        sliderLighting.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {

            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                lightingValues = sliderLighting.getValue();
                System.out.println(lightingValues);
            }
        });

        // Humidity Slider
        Slider sliderHumidity = findViewById(R.id.sliderHumidity);
        sliderHumidity.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {

            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                humidityValues = sliderHumidity.getValue();
            }
        });

        // Temperature Slider
        Slider sliderTemperature = findViewById(R.id.sliderTemperature);
        sliderTemperature.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {

            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                temperatureValues = sliderTemperature.getValue();
            }
        });

        // Button to go back to main page
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EditPreset.this, PresetList.class);
                startActivity(intent);
            }
        });

        // Button to save changes
        Button saveChanges = findViewById(R.id.saveButton);
        saveChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = addName.getText().toString();
                if (!name.equals("")) {
                db.insertData(name, lightingValues, humidityValues, temperatureValues);
                    Toast.makeText(EditPreset.this, "Preset Saved", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(EditPreset.this, PresetList.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(EditPreset.this, "Name is required", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
