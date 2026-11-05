package com.example.attendease;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    // The Activity implements this, so the adapter only reports clicks
    public interface OnCourseActionListener {
        void onCourseClick(Course course);
        void onEditClick(Course course);
        void onDeleteClick(Course course);
    }

    private final List<Course> courses;
    private final OnCourseActionListener listener;

    public CourseAdapter(List<Course> courses, OnCourseActionListener listener) {
        this.courses = courses;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courses.get(position);

        holder.tvCourseName.setText(course.getName());

        String info = course.getCode();
        if (course.getBatch() != null && !course.getBatch().isEmpty()) {
            info += "  •  Batch " + course.getBatch();
        }
        holder.tvCourseInfo.setText(info);

        holder.itemView.setOnClickListener(v -> listener.onCourseClick(course));
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(course));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(course));
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    // Holds references to the views of one row, so findViewById runs only once per row
    static class CourseViewHolder extends RecyclerView.ViewHolder {
        TextView tvCourseName, tvCourseInfo;
        ImageButton btnEdit, btnDelete;

        CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseName = itemView.findViewById(R.id.tvCourseName);
            tvCourseInfo = itemView.findViewById(R.id.tvCourseInfo);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}