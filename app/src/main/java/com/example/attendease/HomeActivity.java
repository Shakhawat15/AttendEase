package com.example.attendease;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity implements CourseAdapter.OnCourseActionListener {

    private FirebaseAuth auth;
    private String uid;
    private DatabaseReference coursesRef;
    private ValueEventListener coursesListener;

    private final List<Course> courseList = new ArrayList<>();
    private CourseAdapter adapter;

    private MaterialToolbar toolbar;
    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private TextView tvNavName;
    private TextView tvEmpty;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            goToLogin();
            return;
        }
        uid = user.getUid();
        coursesRef = FirebaseHelper.getDatabase().getReference("courses").child(uid);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupDrawer(user);

        tvEmpty = findViewById(R.id.tvEmpty);
        progressBar = findViewById(R.id.progressBar);

        RecyclerView rvCourses = findViewById(R.id.rvCourses);
        rvCourses.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CourseAdapter(courseList, this);
        rvCourses.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAddCourse);
        fab.setOnClickListener(v -> showCourseDialog(null));

        loadUserName();
    }

    // ---------- Navigation drawer ----------

    private void setupDrawer(FirebaseUser user) {
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);

        // Hamburger icon opens the drawer
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Header views live inside the NavigationView's header layout
        View header = navView.getHeaderView(0);
        tvNavName = header.findViewById(R.id.tvNavName);
        TextView tvNavEmail = header.findViewById(R.id.tvNavEmail);
        tvNavEmail.setText(user.getEmail());

        navView.setCheckedItem(R.id.nav_courses);
        navView.setNavigationItemSelectedListener(this::onDrawerItemSelected);

        // Back button closes the drawer first (instead of leaving the app)
        OnBackPressedCallback closeDrawerCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, closeDrawerCallback);

        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                closeDrawerCallback.setEnabled(true);
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                closeDrawerCallback.setEnabled(false);
            }
        });
    }

    private boolean onDrawerItemSelected(@NonNull MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START);
        int id = item.getItemId();

        if (id == R.id.nav_courses) {
            return true; // already here; keep it highlighted
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_share) {
            shareApp();
        } else if (id == R.id.nav_about) {
            showAbout();
        } else if (id == R.id.nav_logout) {
            confirmLogout();
        }
        return false; // don't leave other items highlighted
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navView != null) navView.setCheckedItem(R.id.nav_courses);
    }

    private void shareApp() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "AttendEase - attendance tracker");
        intent.putExtra(Intent.EXTRA_TEXT,
                "Track class attendance easily with AttendEase!\nDownload: "
                        + SettingsActivity.GITHUB_URL + "/releases");
        startActivity(Intent.createChooser(intent, "Share app via"));
    }

    private void showAbout() {
        String version;
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = "1.0";
        }

        new MaterialAlertDialogBuilder(this)
                .setIcon(R.mipmap.ic_launcher_round)
                .setTitle("AttendEase")
                .setMessage("Version " + version + "\n\n"
                        + "A simple attendance tracker for teachers: manage courses and students, "
                        + "take daily attendance, and share reports.\n\n"
                        + "Final lab project — Mobile Application and Development Lab (0613-410).")
                .setPositiveButton("Close", null)
                .setNeutralButton("GitHub", (d, w) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SettingsActivity.GITHUB_URL)));
                    } catch (ActivityNotFoundException e) {
                        showSnackbar("No browser found");
                    }
                })
                .show();
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (d, which) -> {
                    auth.signOut();
                    Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
                    goToLogin();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------- Real-time listener ----------

    @Override
    protected void onStart() {
        super.onStart();
        if (coursesRef == null) return;

        coursesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                courseList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Course course = child.getValue(Course.class);
                    if (course != null) {
                        course.setId(child.getKey());
                        courseList.add(course);
                    }
                }
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(courseList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                showSnackbar("Error: " + error.getMessage());
            }
        };
        coursesRef.addValueEventListener(coursesListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (coursesRef != null && coursesListener != null) {
            coursesRef.removeEventListener(coursesListener);
        }
    }

    // Read the teacher's name once: toolbar subtitle + drawer header
    private void loadUserName() {
        FirebaseHelper.getDatabase().getReference("users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null && user.getName() != null) {
                            toolbar.setSubtitle("Hello, " + user.getName());
                            tvNavName.setText(user.getName());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    // ---------- Add / Edit (dialog) ----------

    private void showCourseDialog(Course existing) {
        boolean isEdit = existing != null;

        View view = getLayoutInflater().inflate(R.layout.dialog_course, null);
        TextInputLayout tilName = view.findViewById(R.id.tilCourseName);
        TextInputLayout tilCode = view.findViewById(R.id.tilCourseCode);
        TextInputEditText etName = view.findViewById(R.id.etCourseName);
        TextInputEditText etCode = view.findViewById(R.id.etCourseCode);
        TextInputEditText etBatch = view.findViewById(R.id.etBatch);

        if (isEdit) {
            etName.setText(existing.getName());
            etCode.setText(existing.getCode());
            etBatch.setText(existing.getBatch());
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(isEdit ? "Edit course" : "Add course")
                .setView(view)
                .setPositiveButton(isEdit ? "Update" : "Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String name = textOf(etName).trim();
                    String code = textOf(etCode).trim();
                    String batch = textOf(etBatch).trim();

                    tilName.setError(null);
                    tilCode.setError(null);

                    if (TextUtils.isEmpty(name)) {
                        tilName.setError("Course name is required");
                        return;
                    }
                    if (TextUtils.isEmpty(code)) {
                        tilCode.setError("Course code is required");
                        return;
                    }

                    String id = isEdit ? existing.getId() : coursesRef.push().getKey();
                    saveCourse(new Course(id, name, code, batch), isEdit);
                    dialog.dismiss();
                }));

        dialog.show();
    }

    private void saveCourse(Course course, boolean isEdit) {
        coursesRef.child(course.getId()).setValue(course)
                .addOnSuccessListener(unused ->
                        showSnackbar(isEdit ? "Course updated" : "Course added"))
                .addOnFailureListener(e ->
                        showSnackbar("Failed: " + e.getMessage()));
    }

    // ---------- Delete ----------

    private void confirmDelete(Course course) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete course?")
                .setMessage("\"" + course.getName() + "\" and all its students and attendance records will be deleted.")
                .setPositiveButton("Delete", (d, which) -> deleteCourse(course))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCourse(Course course) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("courses/" + uid + "/" + course.getId(), null);
        updates.put("students/" + course.getId(), null);
        updates.put("attendance/" + course.getId(), null);

        FirebaseHelper.getDatabase().getReference().updateChildren(updates)
                .addOnSuccessListener(unused -> showSnackbar("Course deleted"))
                .addOnFailureListener(e -> showSnackbar("Failed: " + e.getMessage()));
    }

    // ---------- Adapter callbacks ----------

    @Override
    public void onCourseClick(Course course) {
        Intent intent = new Intent(this, CourseDetailActivity.class);
        intent.putExtra(CourseDetailActivity.EXTRA_COURSE_ID, course.getId());
        intent.putExtra(CourseDetailActivity.EXTRA_COURSE_NAME, course.getName());
        intent.putExtra(CourseDetailActivity.EXTRA_COURSE_CODE, course.getCode());
        startActivity(intent);
    }

    @Override
    public void onEditClick(Course course) {
        showCourseDialog(course);
    }

    @Override
    public void onDeleteClick(Course course) {
        confirmDelete(course);
    }

    // ---------- Helpers ----------

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(R.id.rvCourses), message, Snackbar.LENGTH_SHORT).show();
    }

    private String textOf(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }
}