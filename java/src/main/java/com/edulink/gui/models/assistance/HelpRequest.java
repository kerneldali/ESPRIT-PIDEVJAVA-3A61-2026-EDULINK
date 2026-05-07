package com.edulink.gui.models.assistance;

import java.sql.Timestamp;

public class HelpRequest {
    private int id;
    private String title;
    private String description;
    private String status;
    private int bounty;
    private boolean isTicket;
    private Timestamp createdAt;
    private Integer studentId;
    private Integer sessionId;
    private String category;
    private String difficulty;
    private String closeReason;
    private Integer tutorId;          // tutor who accepted this request
    private String jitsiRoomId;       // live video room tied to the session
    private String sessionSummary;    // AI-generated summary after session closes

    public HelpRequest() {
        this.status = "OPEN";
        this.bounty = 0;
        this.isTicket = false;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public int getBounty() { return bounty; }
    public void setBounty(int bounty) { this.bounty = bounty; }
    
    public boolean isTicket() { return isTicket; }
    public void setTicket(boolean ticket) { isTicket = ticket; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    
    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }
    
    public Integer getSessionId() { return sessionId; }
    public void setSessionId(Integer sessionId) { this.sessionId = sessionId; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    
    public String getCloseReason() { return closeReason; }
    public void setCloseReason(String closeReason) { this.closeReason = closeReason; }

    public Integer getTutorId() { return tutorId; }
    public void setTutorId(Integer tutorId) { this.tutorId = tutorId; }

    public String getJitsiRoomId() { return jitsiRoomId; }
    public void setJitsiRoomId(String jitsiRoomId) { this.jitsiRoomId = jitsiRoomId; }

    public String getSessionSummary() { return sessionSummary; }
    public void setSessionSummary(String sessionSummary) { this.sessionSummary = sessionSummary; }
}
