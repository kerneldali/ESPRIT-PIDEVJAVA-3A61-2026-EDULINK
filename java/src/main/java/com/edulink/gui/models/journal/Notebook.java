package com.edulink.gui.models.journal;

import java.sql.Timestamp;

public class Notebook {
    private int id;
    private int userId;
    private String title;
    private String coverColor;
    private Timestamp createdAt;

    public Notebook() {
    }

    public Notebook(int id, int userId, String title, String coverColor, Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.coverColor = coverColor;
        this.createdAt = createdAt;
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

    public String getCoverColor() {
        return coverColor;
    }

    public void setCoverColor(String coverColor) {
        this.coverColor = coverColor;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return title;
    }
}
