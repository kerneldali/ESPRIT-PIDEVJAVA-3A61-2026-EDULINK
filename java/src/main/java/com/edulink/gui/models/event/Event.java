package com.edulink.gui.models.event;

import java.time.LocalDateTime;

public class Event {

    private int id;
    private int organizerId;
    private String title;
    private String description;
    private LocalDateTime dateStart;
    private LocalDateTime dateEnd;
    private boolean isOnline;
    private String meetLink;
    private String location;
    private int maxCapacity;
    private String image;
    private Integer predictedScore;

    // Constructeur vide (obligatoire)
    public Event() {
    }

    // Constructeur complet
    public Event(int organizerId, String title, String description,
            LocalDateTime dateStart, LocalDateTime dateEnd,
            boolean isOnline, String meetLink, String location,
            int maxCapacity, String image) {
        this.organizerId = organizerId;
        this.title = title;
        this.description = description;
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        this.isOnline = isOnline;
        this.meetLink = meetLink;
        this.location = location;
        this.maxCapacity = maxCapacity;
        this.image = image;
    }

    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(int organizerId) {
        this.organizerId = organizerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateStart() {
        return dateStart;
    }

    public void setDateStart(LocalDateTime dateStart) {
        this.dateStart = dateStart;
    }

    public LocalDateTime getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(LocalDateTime dateEnd) {
        this.dateEnd = dateEnd;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public String getMeetLink() {
        return meetLink;
    }

    public void setMeetLink(String meetLink) {
        this.meetLink = meetLink;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Integer getPredictedScore() {
        return predictedScore;
    }

    public void setPredictedScore(Integer predictedScore) {
        this.predictedScore = predictedScore;
    }

    @Override
    public String toString() {
        return "Event{id=" + id + ", title='" + title + "', isOnline=" + isOnline + "}";
    }
}