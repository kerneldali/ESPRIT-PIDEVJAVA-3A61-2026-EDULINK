package com.edulink.gui.services.event;

import com.edulink.gui.util.MyConnection;
import com.edulink.gui.models.event.Event;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventService {

    private Connection cnx;

    public EventService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    // =====================
    // CREATE
    // =====================
    public boolean addEvent(Event event) {
        String sql = "INSERT INTO event (organizer_id, title, description, date_start, date_end, is_online, meet_link, location, max_capacity, image) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, event.getOrganizerId());
            ps.setString(2, event.getTitle());
            ps.setString(3, event.getDescription());
            ps.setTimestamp(4, Timestamp.valueOf(event.getDateStart()));
            ps.setTimestamp(5, Timestamp.valueOf(event.getDateEnd()));
            ps.setBoolean(6, event.isOnline());
            ps.setString(7, event.getMeetLink());
            ps.setString(8, event.getLocation());
            ps.setInt(9, event.getMaxCapacity());
            ps.setString(10, event.getImage());
            ps.executeUpdate();
            System.out.println("✅ Événement ajouté !");

            // Si l'événement est en ligne → générer un lien Meet
            if (event.isOnline()) {
                String meetLink = GoogleCalendarService.createMeetLink(event);
                if (meetLink != null) {
                    event.setMeetLink(meetLink);
                    // Mettre à jour le meet_link en BDD
                    String updateSql = "UPDATE event SET meet_link = ? WHERE id = ?";
                    PreparedStatement updatePs = cnx.prepareStatement(updateSql);
                    updatePs.setString(1, meetLink);
                    updatePs.setInt(2, event.getId());
                    updatePs.executeUpdate();
                }
            }
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Erreur ajout : " + e.getMessage());
            return false;
        }
    }

    // =====================
    // READ ALL
    // =====================
    public List<Event> getAllEvents() {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM event";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Event e = new Event();
                e.setId(rs.getInt("id"));
                e.setOrganizerId(rs.getInt("organizer_id"));
                e.setTitle(rs.getString("title"));
                e.setDescription(rs.getString("description"));
                e.setDateStart(rs.getTimestamp("date_start").toLocalDateTime());
                e.setDateEnd(rs.getTimestamp("date_end").toLocalDateTime());
                e.setOnline(rs.getBoolean("is_online"));
                e.setMeetLink(rs.getString("meet_link"));
                e.setLocation(rs.getString("location"));
                e.setMaxCapacity(rs.getInt("max_capacity"));
                e.setImage(rs.getString("image"));
                e.setPredictedScore(rs.getInt("predicted_score"));
                events.add(e);
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur récupération : " + e.getMessage());
        }
        return events;
    }

    // =====================
    // READ ONE
    // =====================
    public Event getEventById(int id) {
        String sql = "SELECT * FROM event WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Event e = new Event();
                e.setId(rs.getInt("id"));
                e.setOrganizerId(rs.getInt("organizer_id"));
                e.setTitle(rs.getString("title"));
                e.setDescription(rs.getString("description"));
                e.setDateStart(rs.getTimestamp("date_start").toLocalDateTime());
                e.setDateEnd(rs.getTimestamp("date_end").toLocalDateTime());
                e.setOnline(rs.getBoolean("is_online"));
                e.setMeetLink(rs.getString("meet_link"));
                e.setLocation(rs.getString("location"));
                e.setMaxCapacity(rs.getInt("max_capacity"));
                e.setImage(rs.getString("image"));
                e.setPredictedScore(rs.getInt("predicted_score"));
                return e;
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur récupération : " + e.getMessage());
        }
        return null;
    }

    // =====================
    // UPDATE
    // =====================
    public boolean updateEvent(Event event) {
        String sql = "UPDATE event SET organizer_id=?, title=?, description=?, date_start=?, date_end=?, is_online=?, meet_link=?, location=?, max_capacity=?, image=? WHERE id=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, event.getOrganizerId());
            ps.setString(2, event.getTitle());
            ps.setString(3, event.getDescription());
            ps.setTimestamp(4, Timestamp.valueOf(event.getDateStart()));
            ps.setTimestamp(5, Timestamp.valueOf(event.getDateEnd()));
            ps.setBoolean(6, event.isOnline());
            ps.setString(7, event.getMeetLink());
            ps.setString(8, event.getLocation());
            ps.setInt(9, event.getMaxCapacity());
            ps.setString(10, event.getImage());
            ps.setInt(11, event.getId());
            ps.executeUpdate();
            System.out.println("✅ Événement modifié !");
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Erreur modification : " + e.getMessage());
            return false;
        }
    }

    // =====================
    // DELETE
    // =====================
    public boolean deleteEvent(int id) {
        String sql = "DELETE FROM event WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ Événement supprimé !");
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Erreur suppression : " + e.getMessage());
            return false;
        }
    }
}