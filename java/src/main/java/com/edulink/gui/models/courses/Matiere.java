package com.edulink.gui.models.courses;

import java.time.LocalDateTime;

public class Matiere {
    private int id;
    private int creatorId;
    private String name;
    private String description;
    private String status;
    private String imageUrl;
    private LocalDateTime createdAt;

    public Matiere() {}

    public Matiere(int id, int creatorId, String name, String description, String status, String imageUrl, LocalDateTime createdAt) {
        this.id = id;
        this.creatorId = creatorId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }

    public Matiere(int creatorId, String name, String description, String status, String imageUrl, LocalDateTime createdAt) {
        this.creatorId = creatorId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCreatorId() { return creatorId; }
    public void setCreatorId(int creatorId) { this.creatorId = creatorId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return name;
    }
}
