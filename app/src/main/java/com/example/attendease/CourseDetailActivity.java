package com.example.attendease;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourseDetailActivity extends AppCompatActivity
        implements StudentAdapter.OnStudentActionListener {

    public static final String EXTRA_COURSE_ID = "course_id";
    public static final String EXTRA_COURSE_NAME = "course_name";
    public static final String EXTRA_COURSE_CODE = "course_code";

    private String courseId, courseName, courseCode;

    private DatabaseReference studentsRef, attendanceRef;
    private ValueEventListener studentsListener, attendanceListener;

    private final List<Student> studentList = new ArrayList<>();
    private StudentAdapter adapter;

    private int classCount = 0;

    private RecyclerView rvStudents;
    private TextView tvEmpty;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);

        courseId = getIntent().getStringExtra(EXTRA_COURSE_ID);
        courseName = getIntent().getStringExtra(EXTRA_COURSE_NAME);
        courseCode = getIntent().getStringExtra(EXTRA_COURSE_CODE);
        if (courseId == null) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(courseName);
            getSupportActionBar().setSubtitle(courseCode);
        }

        studentsRef = FirebaseHelper.getDatabase().getReference("students").child(courseId);
        attendanceRef = FirebaseHelper.getDatabase().getReference("attendance").child(courseId);

        tvEmpty = findViewById(R.id.tvEmpty);
        progressBar = findViewById(R.id.progressBar);

        rvStudents = findViewById(R.id.rvStudents);
        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentAdapter(studentList, this);
        rvStudents.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAddStudent);
        fab.setOnClickListener(v -> openAddEdit(null));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // ---------- Real-time listeners ----------

    @Override
    protected void onStart() {
        super.onStart();
        if (studentsRef == null) return;

        // 1) Students
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
                updateSubtitle();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                showSnackbar("Error: " + error.getMessage());
            }
        };
        studentsRef.addValueEventListener(studentsListener);

        // 2) Attendance -> percentage for each student
        attendanceListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, int[]> stats = new HashMap<>();   // studentId -> {present, total}
                for (DataSnapshot day : snapshot.getChildren()) {          // each date
                    for (DataSnapshot record : day.getChildren()) {        // each student that day
                        Boolean present = record.getValue(Boolean.class);
                        int[] s = stats.computeIfAbsent(record.getKey(), k -> new int[2]);
                        s[1]++;                                            // total classes
                        if (Boolean.TRUE.equals(present)) s[0]++;          // present count
                    }
                }
                classCount = (int) snapshot.getChildrenCount();
                adapter.setStats(stats);
                updateSubtitle();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };
        attendanceRef.addValueEventListener(attendanceListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (studentsRef != null && studentsListener != null) {
            studentsRef.removeEventListener(studentsListener);
        }
        if (attendanceRef != null && attendanceListener != null) {
            attendanceRef.removeEventListener(attendanceListener);
        }
    }

    private void updateSubtitle() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(courseCode + "  •  " + studentList.size()
                    + " students  •  " + classCount + " classes");
        }
    }

    // Sort rolls as numbers when possible (2 before 10), otherwise as text.
    // Not private: TakeAttendanceActivity uses it too.
    static int compareRoll(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        try {
            return Long.compare(Long.parseLong(a), Long.parseLong(b));
        } catch (NumberFormatException e) {
            return a.compareToIgnoreCase(b);
        }
    }

    // ---------- Toolbar menu ----------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_course_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_take_attendance) {
            if (studentList.isEmpty()) {
                showSnackbar("Add students first");
                return true;
            }
            Intent intent = new Intent(this, TakeAttendanceActivity.class);
            intent.putExtra(TakeAttendanceActivity.EXTRA_COURSE_ID, courseId);
            intent.putExtra(TakeAttendanceActivity.EXTRA_COURSE_NAME, courseName);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ---------- Navigation ----------

    private void openAddEdit(String studentId) {
        Intent intent = new Intent(this, AddEditStudentActivity.class);
        intent.putExtra(AddEditStudentActivity.EXTRA_COURSE_ID, courseId);
        intent.putExtra(AddEditStudentActivity.EXTRA_STUDENT_ID, studentId);
        startActivity(intent);
    }

    // ---------- Adapter callbacks ----------

    @Override
    public void onStudentClick(Student student) {
        Intent intent = new Intent(this, StudentReportActivity.class);
        intent.putExtra(StudentReportActivity.EXTRA_COURSE_ID, courseId);
        intent.putExtra(StudentReportActivity.EXTRA_COURSE_NAME, courseName);
        intent.putExtra(StudentReportActivity.EXTRA_STUDENT_ID, student.getId());
        startActivity(intent);
    }

    @Override
    public void onCallClick(Student student) {
        String phone = student.getGuardianPhone();
        if (TextUtils.isEmpty(phone)) {
            showSnackbar("No guardian phone saved");
            return;
        }
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
        Snackbar.make(rvStudents, message, Snackbar.LENGTH_SHORT).show();
    }
}