package com.example.capstoneapplication;

public class CurrentSettings {

    private static float tempValues;

    public static float getTempValues() {
        return tempValues;
    }
    public static void setTempValues(float temp) {
        tempValues = temp;
    }

    private static CurrentSettings instance;

    public static CurrentSettings getInstance() {
        if (instance == null)
            instance = new CurrentSettings();
        return instance;
    }

    private CurrentSettings() { }

}
