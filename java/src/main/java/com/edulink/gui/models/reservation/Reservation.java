package com.edulink.gui.models.reservation;

import java.time.LocalDateTime;

public class Reservation {

    private int id;
    private int userId;
    private int eventId;
    private LocalDateTime reservedAt;

    // Constructeur vide
    public Reservation() {
    }

    // Constructeur complet
    public Reservation(int userId, int eventId, LocalDateTime reservedAt) {
        this.userId = userId;
        this.eventId = eventId;
        this.reservedAt = reservedAt;
    }

    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public LocalDateTime getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(LocalDateTime reservedAt) {
        this.reservedAt = reservedAt;
    }

    @Override
    public String toString() {
        return "Reservation{id=" + id + ", userId=" + userId + ", eventId=" + eventId + "}";
    }
}