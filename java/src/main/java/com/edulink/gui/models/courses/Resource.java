package com.edulink.gui.models.courses;

public class Resource {
    private int id;
    private int coursId;
    private int authorId;
    private String title;
    private String url;
    private String type;
    private String status;

    public Resource() {}

    public Resource(int id, int coursId, int authorId, String title, String url, String type, String status) {
        this.id = id;
        this.coursId = coursId;
        this.authorId = authorId;
        this.title = title;
        this.url = url;
        this.type = type;
        this.status = status;
    }

    public Resource(int coursId, int authorId, String title, String url, String type, String status) {
        this.coursId = coursId;
        this.authorId = authorId;
        this.title = title;
        this.url = url;
        this.type = type;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCoursId() { return coursId; }
    public void setCoursId(int coursId) { this.coursId = coursId; }
    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return title + " (" + type + ")";
    }
}
