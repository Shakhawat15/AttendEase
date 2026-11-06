package com.example.attendease;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final List<AttendanceRecord> records;

    public HistoryAdapter(List<AttendanceRecord> records) {
        this.records = records;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        AttendanceRecord record = records.get(position);
        holder.tvDate.setText(DateUtils.keyToDisplay(record.getDate()));
        holder.tvStatus.setText(record.isPresent() ? "Present" : "Absent");
        holder.tvStatus.setTextColor(Color.parseColor(record.isPresent() ? "#43A047" : "#E53935"));
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvStatus;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}