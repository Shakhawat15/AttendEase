package com.example.attendease;

import com.google.firebase.database.FirebaseDatabase;

public class FirebaseHelper {

    // google-services.json already contains the correct database URL,
    // so the default instance works. All screens get the database from here.
    public static FirebaseDatabase getDatabase() {
        return FirebaseDatabase.getInstance();
    }
}