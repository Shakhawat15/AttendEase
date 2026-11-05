package com.example.attendease;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
    private TextView tvEmpty;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {          // safety: should never happen after Splash
            goToLogin();
            return;
        }
        uid = user.getUid();
        coursesRef = FirebaseHelper.getDatabase().getReference("courses").child(uid);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvEmpty = findViewById(R.id.tvEmpty);
        progressBar = findViewById(R.id.progressBar);

        RecyclerView rvCourses = findViewById(R.id.rvCourses);
        rvCourses.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CourseAdapter(courseList, this);
        rvCourses.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAddCourse);
        fab.setOnClickListener(v -> showCourseDialog(null)); // null = add mode

        loadUserName();
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
        // Stop listening when the screen is not visible (saves battery and data)
        if (coursesRef != null && coursesListener != null) {
            coursesRef.removeEventListener(coursesListener);
        }
    }

    // Read the teacher's name once and show it under the title
    private void loadUserName() {
        FirebaseHelper.getDatabase().getReference("users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null && user.getName() != null) {
                            toolbar.setSubtitle("Hello, " + user.getName());
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
                .setPositiveButton(isEdit ? "Update" : "Save", null) // set below
                .setNegativeButton("Cancel", null)
                .create();

        // Override the Save button so the dialog stays open when input is invalid
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
        // Multi-path delete: removes the course AND its related data in one atomic operation
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
        // Next step: open CourseDetailActivity with putExtra(courseId)
        Toast.makeText(this, "Opening " + course.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEditClick(Course course) {
        showCourseDialog(course);
    }

    @Override
    public void onDeleteClick(Course course) {
        confirmDelete(course);
    }

    // ---------- Toolbar menu (Logout) ----------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
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
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ---------- Helpers ----------

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }

    private String textOf(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }
}