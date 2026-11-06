package com.example.attendease;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class StudentReportActivity extends AppCompatActivity {

    public static final String EXTRA_COURSE_ID = "course_id";
    public static final String EXTRA_COURSE_NAME = "course_name";
    public static final String EXTRA_STUDENT_ID = "student_id";

    private String courseId, courseName, studentId;
    private Student student;

    private final List<AttendanceRecord> records = new ArrayList<>();
    private HistoryAdapter adapter;
    private int presentCount = 0;

    private DatabaseReference studentRef, attendanceRef;
    private ValueEventListener studentListener, attendanceListener;

    private ShapeableImageView ivPhoto;
    private TextView tvName, tvRoll, tvGuardian, tvPercent, tvEligibility, tvCounts, tvEmptyHistory;
    private CircularProgressIndicator progressAttendance;
    private RecyclerView rvHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_report);

        courseId = getIntent().getStringExtra(EXTRA_COURSE_ID);
        courseName = getIntent().getStringExtra(EXTRA_COURSE_NAME);
        studentId = getIntent().getStringExtra(EXTRA_STUDENT_ID);
        if (courseId == null || studentId == null) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Student report");
            getSupportActionBar().setSubtitle(courseName);
        }

        studentRef = FirebaseHelper.getDatabase().getReference("students")
                .child(courseId).child(studentId);
        attendanceRef = FirebaseHelper.getDatabase().getReference("attendance").child(courseId);

        ivPhoto = findViewById(R.id.ivPhoto);
        tvName = findViewById(R.id.tvName);
        tvRoll = findViewById(R.id.tvRoll);
        tvGuardian = findViewById(R.id.tvGuardian);
        tvPercent = findViewById(R.id.tvPercent);
        tvEligibility = findViewById(R.id.tvEligibility);
        tvCounts = findViewById(R.id.tvCounts);
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory);
        progressAttendance = findViewById(R.id.progressAttendance);

        rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapter = new HistoryAdapter(records);
        rvHistory.setAdapter(adapter);

        findViewById(R.id.btnCall).setOnClickListener(v -> callGuardian());
        findViewById(R.id.btnShare).setOnClickListener(v -> shareReport());
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
        if (studentRef == null) return;

        // Student profile (updates after editing)
        studentListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Student s = snapshot.getValue(Student.class);
                if (s == null) { // deleted
                    Toast.makeText(StudentReportActivity.this, "Student not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                s.setId(snapshot.getKey());
                student = s;
                bindStudent();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showSnackbar("Error: " + error.getMessage());
            }
        };
        studentRef.addValueEventListener(studentListener);

        // This student's record on every class date
        attendanceListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                records.clear();
                presentCount = 0;
                for (DataSnapshot day : snapshot.getChildren()) {
                    DataSnapshot record = day.child(studentId);
                    if (!record.exists()) continue; // student wasn't in the class list that day

                    boolean present = Boolean.TRUE.equals(record.getValue(Boolean.class));
                    records.add(new AttendanceRecord(day.getKey(), present));
                    if (present) presentCount++;
                }
                // Newest first (yyyy-MM-dd sorts correctly as text)
                records.sort((a, b) -> b.getDate().compareTo(a.getDate()));

                adapter.notifyDataSetChanged();
                tvEmptyHistory.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);
                bindStats();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showSnackbar("Error: " + error.getMessage());
            }
        };
        attendanceRef.addValueEventListener(attendanceListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (studentRef != null && studentListener != null) {
            studentRef.removeEventListener(studentListener);
        }
        if (attendanceRef != null && attendanceListener != null) {
            attendanceRef.removeEventListener(attendanceListener);
        }
    }

    // ---------- Show data ----------

    private void bindStudent() {
        tvName.setText(student.getName());
        tvRoll.setText("Roll: " + student.getRoll());
        tvGuardian.setText("Guardian: " + student.getGuardianPhone());

        Bitmap photo = ImageUtils.base64ToBitmap(student.getPhoto());
        if (photo != null) {
            ivPhoto.setImageBitmap(photo);
        } else {
            ivPhoto.setImageResource(R.drawable.ic_person);
        }
    }

    private void bindStats() {
        int total = records.size();
        if (total == 0) {
            progressAttendance.setProgressCompat(0, false);
            tvPercent.setText("--");
            tvEligibility.setText("No classes yet");
            tvEligibility.setTextColor(Color.GRAY);
            tvCounts.setText("");
            return;
        }

        int percent = presentCount * 100 / total;
        int color = StudentAdapter.colorFor(percent);

        progressAttendance.setIndicatorColor(color);
        progressAttendance.setProgressCompat(percent, true); // animated
        tvPercent.setText(percent + "%");

        tvEligibility.setText(percent >= 75 ? "Eligible (75%+)" : "Below 75% — at risk");
        tvEligibility.setTextColor(color);
        tvCounts.setText("Present: " + presentCount
                + "  •  Absent: " + (total - presentCount)
                + "\nTotal classes: " + total);
    }

    // ---------- Implicit intents ----------

    private void callGuardian() {
        if (student == null || TextUtils.isEmpty(student.getGuardianPhone())) {
            showSnackbar("No guardian phone saved");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + student.getGuardianPhone()));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            showSnackbar("No dialer app found");
        }
    }

    private void shareReport() {
        if (student == null) return;

        int total = records.size();
        int percent = total > 0 ? presentCount * 100 / total : 0;

        // List the absent dates (newest first)
        StringBuilder absentDates = new StringBuilder();
        for (AttendanceRecord r : records) {
            if (!r.isPresent()) {
                if (absentDates.length() > 0) absentDates.append(", ");
                absentDates.append(DateUtils.keyToShort(r.getDate()));
            }
        }

        String report = "📋 Attendance Report\n\n"
                + "Course: " + courseName + "\n"
                + "Student: " + student.getName() + " (Roll " + student.getRoll() + ")\n\n"
                + "Attendance: " + percent + "% (" + presentCount + "/" + total + ")\n"
                + "Present: " + presentCount + "  |  Absent: " + (total - presentCount) + "\n"
                + "Absent on: " + (absentDates.length() > 0 ? absentDates : "None") + "\n\n"
                + "— Sent from AttendEase";

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Attendance report - " + student.getName());
        intent.putExtra(Intent.EXTRA_TEXT, report);
        startActivity(Intent.createChooser(intent, "Share report via"));
    }

    // ---------- Toolbar menu (Edit) ----------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_student_report, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_edit) {
            Intent intent = new Intent(this, AddEditStudentActivity.class);
            intent.putExtra(AddEditStudentActivity.EXTRA_COURSE_ID, courseId);
            intent.putExtra(AddEditStudentActivity.EXTRA_STUDENT_ID, studentId);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSnackbar(String message) {
        Snackbar.make(rvHistory, message, Snackbar.LENGTH_SHORT).show();
    }
}