package com.edulink.gui.models.courses;

import java.time.LocalDateTime;

public class Enrollment {
    private int id;
    private int studentId;
    private int coursId;
    private double progress;
    private LocalDateTime enrolledAt;
    private LocalDateTime lastAccessed;

    public Enrollment() {}

    public Enrollment(int id, int studentId, int coursId, double progress, LocalDateTime enrolledAt, LocalDateTime lastAccessed) {
        this.id = id;
        this.studentId = studentId;
        this.coursId = coursId;
        this.progress = progress;
        this.enrolledAt = enrolledAt;
        this.lastAccessed = lastAccessed;
    }

    public Enrollment(int studentId, int coursId, double progress, LocalDateTime enrolledAt, LocalDateTime lastAccessed) {
        this.studentId = studentId;
        this.coursId = coursId;
        this.progress = progress;
        this.enrolledAt = enrolledAt;
        this.lastAccessed = lastAccessed;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public int getCoursId() { return coursId; }
    public void setCoursId(int coursId) { this.coursId = coursId; }
    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = progress; }
    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }
    public LocalDateTime getLastAccessed() { return lastAccessed; }
    public void setLastAccessed(LocalDateTime lastAccessed) { this.lastAccessed = lastAccessed; }
}
