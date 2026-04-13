package com.edulink.gui.models.assistance;

import java.time.LocalDateTime;

public class ForumThread {
    private int id;
    private int boardId;
    private int authorId;
    private String authorName; // Joined from User
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private int views;
    private boolean isPinned;
    private boolean isLocked;
    private int replyCount; // computed

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getBoardId() { return boardId; }
    public void setBoardId(int boardId) { this.boardId = boardId; }
    
    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }
    
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }
    
    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }
    
    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }

    public int getReplyCount() { return replyCount; }
    public void setReplyCount(int replyCount) { this.replyCount = replyCount; }
}