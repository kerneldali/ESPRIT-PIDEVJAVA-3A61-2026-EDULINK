package com.edulink.gui.models.journal;

import java.sql.Timestamp;

public class PersonalTask {
    private int id;
    private int userId;
    private String title;
    private boolean isCompleted;
    private Timestamp createdAt;
    private Timestamp completedAt;
    private Timestamp reminderAt;

    public PersonalTask() {
    }

    public PersonalTask(int id, int userId, String title, boolean isCompleted, Timestamp createdAt,
            Timestamp completedAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.isCompleted = isCompleted;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public PersonalTask(int id, int userId, String title, boolean isCompleted, Timestamp createdAt,
            Timestamp completedAt, Timestamp reminderAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.isCompleted = isCompleted;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.reminderAt = reminderAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }

    public Timestamp getReminderAt() {
        return reminderAt;
    }

    public void setReminderAt(Timestamp reminderAt) {
        this.reminderAt = reminderAt;
    }
}
