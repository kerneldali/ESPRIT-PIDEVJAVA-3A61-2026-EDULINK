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

    // ── Auto-decision fields (populated by ChallengeService.runAutoDecisions) ──
    private boolean featured;        // true = currently the most popular challenge
    private int xpBoostPct;          // 0 = no boost; 50 = +50% reward while active
    private LocalDateTime boostUntil;// when the active boost expires

    // ── AI-generated cover image (Pollinations.ai) ──
    private String imageUrl;         // remote URL of the cover image, may be null

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

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public int getXpBoostPct() { return xpBoostPct; }
    public void setXpBoostPct(int xpBoostPct) { this.xpBoostPct = xpBoostPct; }

    public LocalDateTime getBoostUntil() { return boostUntil; }
    public void setBoostUntil(LocalDateTime boostUntil) { this.boostUntil = boostUntil; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    /** True if the boost is configured and still active right now. */
    public boolean isBoostActive() {
        return xpBoostPct > 0 && boostUntil != null && boostUntil.isAfter(LocalDateTime.now());
    }

    /**
     * Returns the XP reward to award now (base reward + active boost multiplier).
     * If no boost is active, returns the base xpReward.
     */
    public int getEffectiveXpReward() {
        if (!isBoostActive()) return xpReward;
        return xpReward + (xpReward * xpBoostPct / 100);
    }

    @Override
    public String toString() {
        return title;
    }
}
