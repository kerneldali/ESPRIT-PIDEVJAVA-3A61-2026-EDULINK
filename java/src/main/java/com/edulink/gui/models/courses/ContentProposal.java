package com.edulink.gui.models.courses;

import java.time.LocalDateTime;

public class ContentProposal {
    private int id;
    private int suggestedBy;
    private String type; // MATIERE, COURSE, RESSOURCE
    private String title;
    private String description;
    private String status; // PENDING, ACCEPTED, REJECTED
    private LocalDateTime createdAt;

    public ContentProposal() {}

    public ContentProposal(int id, int suggestedBy, String type, String title, String description, String status, LocalDateTime createdAt) {
        this.id = id;
        this.suggestedBy = suggestedBy;
        this.type = type;
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getSuggestedBy() { return suggestedBy; }
    public void setSuggestedBy(int suggestedBy) { this.suggestedBy = suggestedBy; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
