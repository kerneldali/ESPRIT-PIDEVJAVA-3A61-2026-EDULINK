package com.edulink.gui.models.challenge;

import java.time.LocalDateTime;

public class Challenge {
    private int id;
    private String title;
    private String description;
    private String difficulty;   // EASY | MEDIUM | HARD
    private int xpReward;
    private String status;       // OPEN | CLOSED
    private LocalDateTime deadline;
    private LocalDateTime createdAt;

    public Challenge() {}

    public Challenge(int id, String title, String description, String difficulty,
                     int xpReward, String status, LocalDateTime deadline, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.xpReward = xpReward;
        this.status = status;
        this.deadline = deadline;
        this.createdAt = createdAt;
    }

    public Challenge(String title, String description, String difficulty,
                     int xpReward, String status, LocalDateTime deadline, LocalDateTime createdAt) {
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.xpReward = xpReward;
        this.status = status;
        this.deadline = deadline;
        this.createdAt = createdAt;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getXpReward() { return xpReward; }
    public void setXpReward(int xpReward) { this.xpReward = xpReward; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return title;
    }
}
