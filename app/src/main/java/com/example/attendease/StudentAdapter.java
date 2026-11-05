package com.example.attendease;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    public interface OnStudentActionListener {
        void onStudentClick(Student student);
        void onCallClick(Student student);
        void onDeleteClick(Student student);
    }

    private final List<Student> students;
    private final OnStudentActionListener listener;

    public StudentAdapter(List<Student> students, OnStudentActionListener listener) {
        this.students = students;
        this.listener = listener;
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
            holder.ivPhoto.setImageResource(R.drawable.ic_person); // reset recycled rows
        }

        holder.itemView.setOnClickListener(v -> listener.onStudentClick(student));
        holder.btnCall.setOnClickListener(v -> listener.onCallClick(student));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(student));
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivPhoto;
        TextView tvName, tvRoll;
        ImageButton btnCall, btnDelete;

        StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivPhoto);
            tvName = itemView.findViewById(R.id.tvStudentName);
            tvRoll = itemView.findViewById(R.id.tvStudentRoll);
            btnCall = itemView.findViewById(R.id.btnCall);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}