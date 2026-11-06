package com.example.attendease;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class SettingsActivity extends AppCompatActivity {

    // TODO: change these to your own
    private static final String GITHUB_URL = "https://github.com/Shakhawat15/AttendEase";
    private static final String DEV_EMAIL = "shsourov2000@gmail.com";

    private TextView tvAccountName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupAccount();
        setupTheme();
        setupPreferences();
        setupAbout();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // ---------- Account ----------

    private void setupAccount() {
        tvAccountName = findViewById(R.id.tvAccountName);
        TextView tvAccountEmail = findViewById(R.id.tvAccountEmail);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        tvAccountEmail.setText(user.getEmail());

        FirebaseHelper.getDatabase().getReference("users").child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User u = snapshot.getValue(User.class);
                        if (u != null) tvAccountName.setText(u.getName());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    // ---------- Theme ----------

    private void setupTheme() {
        RadioGroup rgTheme = findViewById(R.id.rgTheme);

        // Show the saved choice BEFORE adding the listener (so it doesn't fire now)
        int mode = Prefs.getThemeMode(this);
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) {
            rgTheme.check(R.id.rbLight);
        } else if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            rgTheme.check(R.id.rbDark);
        } else {
            rgTheme.check(R.id.rbSystem);
        }

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int newMode;
            if (checkedId == R.id.rbLight) {
                newMode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.rbDark) {
                newMode = AppCompatDelegate.MODE_NIGHT_YES;
            } else {
                newMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            }
            Prefs.setThemeMode(this, newMode);              // remember for next launch
            AppCompatDelegate.setDefaultNightMode(newMode); // apply now (screens recreate)
        });
    }

    // ---------- Preferences ----------

    private void setupPreferences() {
        MaterialSwitch switchDefaultPresent = findViewById(R.id.switchDefaultPresent);
        switchDefaultPresent.setChecked(Prefs.isDefaultPresent(this));
        switchDefaultPresent.setOnCheckedChangeListener((button, isChecked) -> {
            Prefs.setDefaultPresent(this, isChecked);
            showSnackbar(isChecked ? "New sheets: everyone present" : "New sheets: everyone absent");
        });

        MaterialSwitch switchRememberMe = findViewById(R.id.switchRememberMe);
        switchRememberMe.setChecked(Prefs.isRememberMe(this));
        switchRememberMe.setOnCheckedChangeListener((button, isChecked) -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (isChecked && user != null && user.getEmail() != null) {
                Prefs.setRememberMe(this, user.getEmail());
                showSnackbar("Email will be remembered");
            } else {
                Prefs.clearRememberMe(this);
                showSnackbar("Saved email cleared");
            }
        });
    }

    // ---------- About (implicit intents) ----------

    private void setupAbout() {
        TextView tvVersion = findViewById(R.id.tvVersion);
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText("Version " + version);
        } catch (PackageManager.NameNotFoundException e) {
            tvVersion.setText("Version 1.0");
        }

        // ACTION_VIEW: open the GitHub repo in the browser
        findViewById(R.id.btnGithub).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL));
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                showSnackbar("No browser found");
            }
        });

        // ACTION_SENDTO + mailto: opens only email apps
        findViewById(R.id.btnContact).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + DEV_EMAIL));
            intent.putExtra(Intent.EXTRA_SUBJECT, "AttendEase feedback");
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                showSnackbar("No email app found");
            }
        });
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(R.id.scrollView), message, Snackbar.LENGTH_SHORT).show();
    }
}