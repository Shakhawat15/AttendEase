package com.example.attendease;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

// Runs once when the app process starts, BEFORE any Activity is created
public class AttendEaseApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(Prefs.getThemeMode(this));
    }
}