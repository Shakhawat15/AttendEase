package com.example.attendease;

public class Student {
    private String id;
    private String name;
    private String roll;
    private String phone;
    private String guardianPhone;
    private String photo; // small JPEG stored as a Base64 text string

    public Student() { } // required by Firebase

    public Student(String id, String name, String roll, String phone,
                   String guardianPhone, String photo) {
        this.id = id;
        this.name = name;
        this.roll = roll;
        this.phone = phone;
        this.guardianPhone = guardianPhone;
        this.photo = photo;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRoll() { return roll; }
    public void setRoll(String roll) { this.roll = roll; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGuardianPhone() { return guardianPhone; }
    public void setGuardianPhone(String guardianPhone) { this.guardianPhone = guardianPhone; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }
}