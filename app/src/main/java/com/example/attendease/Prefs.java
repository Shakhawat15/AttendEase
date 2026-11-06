package com.example.attendease;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class Prefs {

    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_DEFAULT_PRESENT = "default_present";

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ----- Theme -----
    public static int getThemeMode(Context context) {
        return prefs(context).getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static void setThemeMode(Context context, int mode) {
        prefs(context).edit().putInt(KEY_THEME, mode).apply();
    }

    // ----- Default attendance status -----
    public static boolean isDefaultPresent(Context context) {
        return prefs(context).getBoolean(KEY_DEFAULT_PRESENT, true);
    }

    public static void setDefaultPresent(Context context, boolean present) {
        prefs(context).edit().putBoolean(KEY_DEFAULT_PRESENT, present).apply();
    }

    // ----- Remember me (keys shared with LoginActivity) -----
    public static boolean isRememberMe(Context context) {
        return prefs(context).getBoolean(LoginActivity.KEY_REMEMBER, false);
    }

    public static void setRememberMe(Context context, String email) {
        prefs(context).edit()
                .putBoolean(LoginActivity.KEY_REMEMBER, true)
                .putString(LoginActivity.KEY_EMAIL, email)
                .apply();
    }

    public static void clearRememberMe(Context context) {
        prefs(context).edit()
                .remove(LoginActivity.KEY_REMEMBER)
                .remove(LoginActivity.KEY_EMAIL)
                .apply();
    }
}