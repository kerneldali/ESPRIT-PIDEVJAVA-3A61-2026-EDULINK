package com.edulink.gui.models.assistance;

import java.time.LocalDateTime;

public class ForumReply {
    private int id;
    private int threadId;
    private int authorId;
    private String authorName; // Joined from User
    private String content;
    private LocalDateTime createdAt;
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getThreadId() { return threadId; }
    public void setThreadId(int threadId) { this.threadId = threadId; }
    
    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }
    
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}