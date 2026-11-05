package com.example.attendease;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class CourseDetailActivity extends AppCompatActivity
        implements StudentAdapter.OnStudentActionListener {

    // Keys for the data passed in with putExtra()
    public static final String EXTRA_COURSE_ID = "course_id";
    public static final String EXTRA_COURSE_NAME = "course_name";
    public static final String EXTRA_COURSE_CODE = "course_code";

    private String courseId;
    private String courseCode;
    private DatabaseReference studentsRef;
    private ValueEventListener studentsListener;

    private final List<Student> studentList = new ArrayList<>();
    private StudentAdapter adapter;

    private RecyclerView rvStudents;
    private TextView tvEmpty;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);

        // Read the data sent from HomeActivity
        courseId = getIntent().getStringExtra(EXTRA_COURSE_ID);
        String courseName = getIntent().getStringExtra(EXTRA_COURSE_NAME);
        courseCode = getIntent().getStringExtra(EXTRA_COURSE_CODE);
        if (courseId == null) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // back arrow
            getSupportActionBar().setTitle(courseName);
            getSupportActionBar().setSubtitle(courseCode);
        }

        studentsRef = FirebaseHelper.getDatabase().getReference("students").child(courseId);

        tvEmpty = findViewById(R.id.tvEmpty);
        progressBar = findViewById(R.id.progressBar);

        rvStudents = findViewById(R.id.rvStudents);
        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentAdapter(studentList, this);
        rvStudents.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAddStudent);
        fab.setOnClickListener(v -> openAddEdit(null)); // null = add mode
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // toolbar back arrow
        return true;
    }

    // ---------- Real-time listener ----------

    @Override
    protected void onStart() {
        super.onStart();
        if (studentsRef == null) return;

        studentsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                studentList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Student student = child.getValue(Student.class);
                    if (student != null) {
                        student.setId(child.getKey());
                        studentList.add(student);
                    }
                }
                studentList.sort((a, b) -> compareRoll(a.getRoll(), b.getRoll()));
                adapter.notifyDataSetChanged();

                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(studentList.isEmpty() ? View.VISIBLE : View.GONE);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setSubtitle(courseCode + "  •  " + studentList.size() + " students");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                showSnackbar("Error: " + error.getMessage());
            }
        };
        studentsRef.addValueEventListener(studentsListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (studentsRef != null && studentsListener != null) {
            studentsRef.removeEventListener(studentsListener);
        }
    }

    // Sort rolls as numbers when possible (so 2 comes before 10), otherwise as text
    private static int compareRoll(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        try {
            return Long.compare(Long.parseLong(a), Long.parseLong(b));
        } catch (NumberFormatException e) {
            return a.compareToIgnoreCase(b);
        }
    }

    // ---------- Navigation ----------

    private void openAddEdit(String studentId) {
        Intent intent = new Intent(this, AddEditStudentActivity.class);
        intent.putExtra(AddEditStudentActivity.EXTRA_COURSE_ID, courseId);
        intent.putExtra(AddEditStudentActivity.EXTRA_STUDENT_ID, studentId); // null when adding
        startActivity(intent);
    }

    // ---------- Adapter callbacks ----------

    @Override
    public void onStudentClick(Student student) {
        openAddEdit(student.getId()); // edit mode
    }

    @Override
    public void onCallClick(Student student) {
        String phone = student.getGuardianPhone();
        if (TextUtils.isEmpty(phone)) {
            showSnackbar("No guardian phone saved");
            return;
        }
        // Implicit intent: opens the phone's dialer with the number filled in
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            showSnackbar("No dialer app found");
        }
    }

    @Override
    public void onDeleteClick(Student student) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete student?")
                .setMessage("Remove " + student.getName() + " (Roll " + student.getRoll() + ") from this course?")
                .setPositiveButton("Delete", (d, which) -> deleteStudent(student))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteStudent(Student student) {
        studentsRef.child(student.getId()).removeValue()
                .addOnSuccessListener(unused ->
                        Snackbar.make(rvStudents, student.getName() + " deleted", Snackbar.LENGTH_LONG)
                                .setAction("UNDO", v ->
                                        studentsRef.child(student.getId()).setValue(student))
                                .show())
                .addOnFailureListener(e -> showSnackbar("Failed: " + e.getMessage()));
    }

    private void showSnackbar(String message) {
        // Anchor to a view inside the CoordinatorLayout so the FAB moves up
        Snackbar.make(rvStudents, message, Snackbar.LENGTH_SHORT).show();
    }
}