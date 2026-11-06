package com.example.attendease;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.List;
import java.util.Map;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder> {

    public interface OnStatusChangeListener {
        void onStatusChanged();
    }

    private static final int COLOR_PRESENT = Color.parseColor("#43A047");
    private static final int COLOR_ABSENT = Color.parseColor("#E53935");

    private final List<Student> students;
    private final Map<String, Boolean> statusMap; // studentId -> present?
    private final OnStatusChangeListener listener;

    public AttendanceAdapter(List<Student> students, Map<String, Boolean> statusMap,
                             OnStatusChangeListener listener) {
        this.students = students;
        this.statusMap = statusMap;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance, parent, false);
        return new AttendanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        Student student = students.get(position);
        boolean present = Boolean.TRUE.equals(statusMap.get(student.getId()));

        holder.tvName.setText(student.getName());
        holder.tvRoll.setText("Roll: " + student.getRoll());

        Bitmap photo = ImageUtils.base64ToBitmap(student.getPhoto());
        if (photo != null) {
            holder.ivPhoto.setImageBitmap(photo);
        } else {
            holder.ivPhoto.setImageResource(R.drawable.ic_person);
        }

        // Remove the old listener first: this row may be recycled from another student
        holder.switchPresent.setOnCheckedChangeListener(null);
        holder.switchPresent.setChecked(present);
        showStatus(holder, present);

        holder.switchPresent.setOnCheckedChangeListener((button, isChecked) -> {
            statusMap.put(student.getId(), isChecked);
            showStatus(holder, isChecked);
            listener.onStatusChanged();
        });

        // Tapping anywhere on the card also toggles
        holder.itemView.setOnClickListener(v -> holder.switchPresent.toggle());
    }

    private void showStatus(AttendanceViewHolder holder, boolean present) {
        holder.tvStatus.setText(present ? "Present" : "Absent");
        holder.tvStatus.setTextColor(present ? COLOR_PRESENT : COLOR_ABSENT);
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    static class AttendanceViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivPhoto;
        TextView tvName, tvRoll, tvStatus;
        MaterialSwitch switchPresent;

        AttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivPhoto);
            tvName = itemView.findViewById(R.id.tvName);
            tvRoll = itemView.findViewById(R.id.tvRoll);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            switchPresent = itemView.findViewById(R.id.switchPresent);
        }
    }
}