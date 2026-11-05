package com.example.attendease;

public class Course {
    private String id;
    private String name;
    private String code;
    private String batch;

    public Course() { } // required by Firebase

    public Course(String id, String name, String code, String batch) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.batch = batch;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }
}