package com.example.attendease;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    public interface OnStudentActionListener {
        void onStudentClick(Student student);
        void onCallClick(Student student);
        void onDeleteClick(Student student);
    }

    private final List<Student> students;
    private final OnStudentActionListener listener;

    // studentId -> {presentCount, totalClasses}
    private Map<String, int[]> stats = new HashMap<>();

    public StudentAdapter(List<Student> students, OnStudentActionListener listener) {
        this.students = students;
        this.listener = listener;
    }

    public void setStats(Map<String, int[]> stats) {
        this.stats = stats;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = students.get(position);

        holder.tvName.setText(student.getName());
        holder.tvRoll.setText("Roll: " + student.getRoll());

        Bitmap photo = ImageUtils.base64ToBitmap(student.getPhoto());
        if (photo != null) {
            holder.ivPhoto.setImageBitmap(photo);
        } else {
            holder.ivPhoto.setImageResource(R.drawable.ic_person);
        }

        // Attendance percentage
        int[] s = stats.get(student.getId());
        if (s != null && s[1] > 0) {
            int percent = s[0] * 100 / s[1];
            holder.tvAttendance.setText("Attendance: " + percent + "%  (" + s[0] + "/" + s[1] + ")");
            holder.tvAttendance.setTextColor(colorFor(percent));
        } else {
            holder.tvAttendance.setText("No classes yet");
            holder.tvAttendance.setTextColor(Color.GRAY);
        }

        holder.itemView.setOnClickListener(v -> listener.onStudentClick(student));
        holder.btnCall.setOnClickListener(v -> listener.onCallClick(student));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(student));
    }

    // Green >= 75%, orange 50-74%, red < 50%
    static int colorFor(int percent) {
        if (percent >= 75) return Color.parseColor("#43A047");
        if (percent >= 50) return Color.parseColor("#FB8C00");
        return Color.parseColor("#E53935");
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivPhoto;
        TextView tvName, tvRoll, tvAttendance;
        ImageButton btnCall, btnDelete;

        StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivPhoto);
            tvName = itemView.findViewById(R.id.tvStudentName);
            tvRoll = itemView.findViewById(R.id.tvStudentRoll);
            tvAttendance = itemView.findViewById(R.id.tvAttendance);
            btnCall = itemView.findViewById(R.id.btnCall);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}