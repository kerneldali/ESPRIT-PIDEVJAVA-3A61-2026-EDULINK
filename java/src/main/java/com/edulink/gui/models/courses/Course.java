package com.edulink.gui.models.courses;

import java.time.LocalDateTime;

public class Course {
    private int id;
    private int matiereId;
    private int authorId;
    private String title;
    private String description;
    private String level;
    private int pricePoints;
    private int xp;
    private String status;
    private LocalDateTime createdAt;
    private int quizCount;
    private int summaryCount;

    public Course() {}

    public Course(int id, int matiereId, int authorId, String title, String description, String level, int pricePoints, int xp, String status, LocalDateTime createdAt) {
        this.id = id;
        this.matiereId = matiereId;
        this.authorId = authorId;
        this.title = title;
        this.description = description;
        this.level = level;
        this.pricePoints = pricePoints;
        this.xp = xp;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Course(int matiereId, int authorId, String title, String description, String level, int pricePoints, int xp, String status, LocalDateTime createdAt) {
        this.matiereId = matiereId;
        this.authorId = authorId;
        this.title = title;
        this.description = description;
        this.level = level;
        this.pricePoints = pricePoints;
        this.xp = xp;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getMatiereId() { return matiereId; }
    public void setMatiereId(int matiereId) { this.matiereId = matiereId; }
    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public int getPricePoints() { return pricePoints; }
    public void setPricePoints(int pricePoints) { this.pricePoints = pricePoints; }
    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public int getQuizCount() { return quizCount; }
    public void setQuizCount(int quizCount) { this.quizCount = quizCount; }
    public int getSummaryCount() { return summaryCount; }
    public void setSummaryCount(int summaryCount) { this.summaryCount = summaryCount; }

    @Override
    public String toString() {
        return title;
    }
}
