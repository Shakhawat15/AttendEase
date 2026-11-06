package com.example.attendease;

public class AttendanceRecord {
    private final String date;     // key format: yyyy-MM-dd
    private final boolean present;

    public AttendanceRecord(String date, boolean present) {
        this.date = date;
        this.present = present;
    }

    public String getDate() { return date; }
    public boolean isPresent() { return present; }
}