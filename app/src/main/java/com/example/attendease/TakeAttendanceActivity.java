package com.example.attendease;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class TakeAttendanceActivity extends AppCompatActivity
        implements AttendanceAdapter.OnStatusChangeListener {

    public static final String EXTRA_COURSE_ID = "course_id";
    public static final String EXTRA_COURSE_NAME = "course_name";

    private DatabaseReference studentsRef;
    private DatabaseReference attendanceRef;

    private final List<Student> studentList = new ArrayList<>();
    private final Map<String, Boolean> statusMap = new HashMap<>();
    private AttendanceAdapter adapter;

    private long selectedDateMillis; // UTC midnight of the chosen day

    private TextView tvDate, tvExisting, tvSummary;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    private RecyclerView rvAttendance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_attendance);

        String courseId = getIntent().getStringExtra(EXTRA_COURSE_ID);
        String courseName = getIntent().getStringExtra(EXTRA_COURSE_NAME);
        if (courseId == null) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Take attendance");
            getSupportActionBar().setSubtitle(courseName);
        }

        studentsRef = FirebaseHelper.getDatabase().getReference("students").child(courseId);
        attendanceRef = FirebaseHelper.getDatabase().getReference("attendance").child(courseId);

        tvDate = findViewById(R.id.tvDate);
        tvExisting = findViewById(R.id.tvExisting);
        tvSummary = findViewById(R.id.tvSummary);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        rvAttendance = findViewById(R.id.rvAttendance);
        rvAttendance.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendanceAdapter(studentList, statusMap, this);
        rvAttendance.setAdapter(adapter);

        selectedDateMillis = MaterialDatePicker.todayInUtcMilliseconds();
        tvDate.setText(displayDate());

        findViewById(R.id.btnChangeDate).setOnClickListener(v -> showDatePicker());
        findViewById(R.id.btnAllPresent).setOnClickListener(v -> setAll(true));
        findViewById(R.id.btnAllAbsent).setOnClickListener(v -> setAll(false));
        btnSave.setOnClickListener(v -> saveAttendance());

        loadStudents();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // ---------- Load data ----------

    private void loadStudents() {
        setLoading(true);
        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                studentList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Student s = child.getValue(Student.class);
                    if (s != null) {
                        s.setId(child.getKey());
                        studentList.add(s);
                    }
                }
                studentList.sort((a, b) -> CourseDetailActivity.compareRoll(a.getRoll(), b.getRoll()));

                if (studentList.isEmpty()) {
                    setLoading(false);
                    tvSummary.setText("No students in this course");
                    return;
                }
                loadAttendanceForDate();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                showSnackbar("Error: " + error.getMessage());
            }
        });
    }

    // If attendance for this date already exists, load it so the teacher can edit it
    private void loadAttendanceForDate() {
        setLoading(true);
        attendanceRef.child(dateKey()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean alreadyTaken = snapshot.exists();

                statusMap.clear();
                for (Student s : studentList) {
                    Boolean saved = snapshot.child(s.getId()).getValue(Boolean.class);
                    statusMap.put(s.getId(), saved == null || saved); // default: present
                }

                tvExisting.setVisibility(alreadyTaken ? View.VISIBLE : View.GONE);
                btnSave.setText(alreadyTaken ? "Update attendance" : "Save attendance");

                adapter.notifyDataSetChanged();
                updateSummary();
                setLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                showSnackbar("Error: " + error.getMessage());
            }
        });
    }

    // ---------- Date picker ----------

    private void showDatePicker() {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now()) // no future dates
                .build();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select class date")
                .setSelection(selectedDateMillis)
                .setCalendarConstraints(constraints)
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            selectedDateMillis = selection;
            tvDate.setText(displayDate());
            if (!studentList.isEmpty()) loadAttendanceForDate();
        });

        picker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    // ---------- Marking ----------

    private void setAll(boolean present) {
        for (Student s : studentList) {
            statusMap.put(s.getId(), present);
        }
        adapter.notifyDataSetChanged();
        updateSummary();
    }

    @Override
    public void onStatusChanged() {
        updateSummary();
    }

    private void updateSummary() {
        int present = 0;
        for (Boolean b : statusMap.values()) {
            if (Boolean.TRUE.equals(b)) present++;
        }
        int absent = statusMap.size() - present;
        tvSummary.setText("Present: " + present + "   •   Absent: " + absent);
    }

    // ---------- Save (one write for the whole class) ----------

    private void saveAttendance() {
        if (studentList.isEmpty()) return;

        Map<String, Object> data = new HashMap<>(statusMap);
        setLoading(true);
        attendanceRef.child(dateKey()).setValue(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Attendance saved for " + displayDate(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showSnackbar("Failed: " + e.getMessage());
                });
    }

    // ---------- Date helpers (MaterialDatePicker works in UTC) ----------

    private String dateKey() {                 // used as the Firebase key, e.g. 2026-11-06
        return formatUtc("yyyy-MM-dd");
    }

    private String displayDate() {             // shown to the user, e.g. Fri, 06 Nov 2026
        return formatUtc("EEE, dd MMM yyyy");
    }

    private String formatUtc(String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(selectedDateMillis));
    }

    // ---------- UI helpers ----------

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading && !studentList.isEmpty());
    }

    private void showSnackbar(String message) {
        Snackbar.make(rvAttendance, message, Snackbar.LENGTH_SHORT).show();
    }
}