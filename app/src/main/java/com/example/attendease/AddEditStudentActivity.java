package com.example.attendease;

import android.content.ActivityNotFoundException;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class AddEditStudentActivity extends AppCompatActivity {

    public static final String EXTRA_COURSE_ID = "course_id";
    public static final String EXTRA_STUDENT_ID = "student_id";

    // Bangladesh mobile number: 01 + operator digit (3-9) + 8 digits = 11 digits
    private static final String PHONE_PATTERN = "^01[3-9]\\d{8}$";

    private String studentId;       // null = add mode
    private boolean isEdit;
    private String photoBase64;     // current photo (null = no photo)

    private DatabaseReference studentsRef;

    private ShapeableImageView ivPhoto;
    private TextInputLayout tilName, tilRoll, tilPhone, tilGuardianPhone;
    private TextInputEditText etName, etRoll, etPhone, etGuardianPhone;
    private MaterialButton btnSave;
    private ProgressBar progressBar;

    // Camera: returns a small Bitmap directly (opens the camera app)
    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) setPhoto(bitmap);
            });

    // Gallery: returns the Uri of the picked image (opens the gallery/file picker)
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                Bitmap bitmap = ImageUtils.uriToBitmap(this, uri);
                if (bitmap != null) {
                    setPhoto(bitmap);
                } else {
                    showSnackbar("Could not load image");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_student);

        String courseId = getIntent().getStringExtra(EXTRA_COURSE_ID);
        studentId = getIntent().getStringExtra(EXTRA_STUDENT_ID);
        isEdit = studentId != null;
        if (courseId == null) {
            finish();
            return;
        }
        studentsRef = FirebaseHelper.getDatabase().getReference("students").child(courseId);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEdit ? "Edit student" : "Add student");
        }

        ivPhoto = findViewById(R.id.ivPhoto);
        tilName = findViewById(R.id.tilName);
        tilRoll = findViewById(R.id.tilRoll);
        tilPhone = findViewById(R.id.tilPhone);
        tilGuardianPhone = findViewById(R.id.tilGuardianPhone);
        etName = findViewById(R.id.etName);
        etRoll = findViewById(R.id.etRoll);
        etPhone = findViewById(R.id.etPhone);
        etGuardianPhone = findViewById(R.id.etGuardianPhone);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnCamera).setOnClickListener(v -> {
            try {
                cameraLauncher.launch(null);
            } catch (ActivityNotFoundException e) {
                showSnackbar("No camera app found");
            }
        });
        findViewById(R.id.btnGallery).setOnClickListener(v -> galleryLauncher.launch("image/*"));
        btnSave.setOnClickListener(v -> attemptSave());

        if (isEdit) {
            btnSave.setText("Update student");
            loadStudent();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // ---------- Edit mode: load existing data ----------

    private void loadStudent() {
        setLoading(true);
        studentsRef.child(studentId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                setLoading(false);
                Student s = snapshot.getValue(Student.class);
                if (s == null) {
                    Toast.makeText(AddEditStudentActivity.this, "Student not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                etName.setText(s.getName());
                etRoll.setText(s.getRoll());
                etPhone.setText(s.getPhone());
                etGuardianPhone.setText(s.getGuardianPhone());

                photoBase64 = s.getPhoto();
                Bitmap bitmap = ImageUtils.base64ToBitmap(photoBase64);
                if (bitmap != null) ivPhoto.setImageBitmap(bitmap);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                showSnackbar("Error: " + error.getMessage());
            }
        });
    }

    private void setPhoto(Bitmap bitmap) {
        photoBase64 = ImageUtils.bitmapToBase64(bitmap);
        ivPhoto.setImageBitmap(bitmap);
    }

    // ---------- Validation + Save ----------

    private void attemptSave() {
        String name = textOf(etName).trim();
        String roll = textOf(etRoll).trim();
        String phone = textOf(etPhone).trim();
        String guardianPhone = textOf(etGuardianPhone).trim();

        tilName.setError(null);
        tilRoll.setError(null);
        tilPhone.setError(null);
        tilGuardianPhone.setError(null);

        if (TextUtils.isEmpty(name)) {
            tilName.setError("Name is required");
            etName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(roll)) {
            tilRoll.setError("Roll is required");
            etRoll.requestFocus();
            return;
        }
        if (!TextUtils.isEmpty(phone) && !phone.matches(PHONE_PATTERN)) {
            tilPhone.setError("Enter a valid 11-digit number (01XXXXXXXXX)");
            etPhone.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(guardianPhone)) {
            tilGuardianPhone.setError("Guardian phone is required");
            etGuardianPhone.requestFocus();
            return;
        }
        if (!guardianPhone.matches(PHONE_PATTERN)) {
            tilGuardianPhone.setError("Enter a valid 11-digit number (01XXXXXXXXX)");
            etGuardianPhone.requestFocus();
            return;
        }

        String id = isEdit ? studentId : studentsRef.push().getKey();
        Student student = new Student(id, name, roll, phone, guardianPhone, photoBase64);

        setLoading(true);
        checkRollAndSave(student);
    }

    // Roll must be unique within this course
    private void checkRollAndSave(Student student) {
        studentsRef.orderByChild("roll").equalTo(student.getRoll())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            // Same roll on a DIFFERENT student = duplicate
                            if (!child.getKey().equals(student.getId())) {
                                setLoading(false);
                                tilRoll.setError("This roll already exists in this course");
                                etRoll.requestFocus();
                                return;
                            }
                        }
                        writeStudent(student);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        setLoading(false);
                        showSnackbar("Error: " + error.getMessage());
                    }
                });
    }

    private void writeStudent(Student student) {
        studentsRef.child(student.getId()).setValue(student)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, isEdit ? "Student updated" : "Student added",
                            Toast.LENGTH_SHORT).show();
                    finish(); // back to the list, which updates in real time
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showSnackbar("Failed: " + e.getMessage());
                });
    }

    // ---------- Helpers ----------

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }

    private String textOf(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }
}