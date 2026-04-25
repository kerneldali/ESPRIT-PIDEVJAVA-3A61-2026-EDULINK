package com.edulink.gui.models.journal;

import java.sql.Timestamp;

public class Note {
    private int id;
    private int notebookId;
    private int categoryId;
    private String title;
    private String content;
    private String tags;
    private Timestamp createdAt;
    private Timestamp reminderAt;
    private String sentiment;
    private boolean shared;

    public Note() {
    }

    public Note(int id, int notebookId, String title, String content, String tags, Timestamp createdAt,
            boolean shared) {
        this.id = id;
        this.notebookId = notebookId;
        this.title = title;
        this.content = content;
        this.tags = tags;
        this.createdAt = createdAt;
        this.shared = shared;
    }

    public Note(int id, int notebookId, String title, String content, String tags, Timestamp createdAt,
            Timestamp reminderAt, String sentiment, boolean shared) {
        this.id = id;
        this.notebookId = notebookId;
        this.title = title;
        this.content = content;
        this.tags = tags;
        this.createdAt = createdAt;
        this.reminderAt = reminderAt;
        this.sentiment = sentiment;
        this.shared = shared;
    }

    public Note(int id, int notebookId, int categoryId, String title, String content, String tags,
            Timestamp createdAt, boolean shared) {
        this.id = id;
        this.notebookId = notebookId;
        this.categoryId = categoryId;
        this.title = title;
        this.content = content;
        this.tags = tags;
        this.createdAt = createdAt;
        this.shared = shared;
    }

    public Note(int id, int notebookId, int categoryId, String title, String content, String tags,
            Timestamp createdAt, Timestamp reminderAt, String sentiment, boolean shared) {
        this.id = id;
        this.notebookId = notebookId;
        this.categoryId = categoryId;
        this.title = title;
        this.content = content;
        this.tags = tags;
        this.createdAt = createdAt;
        this.reminderAt = reminderAt;
        this.sentiment = sentiment;
        this.shared = shared;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(int notebookId) {
        this.notebookId = notebookId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getReminderAt() {
        return reminderAt;
    }

    public void setReminderAt(Timestamp reminderAt) {
        this.reminderAt = reminderAt;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }
}
