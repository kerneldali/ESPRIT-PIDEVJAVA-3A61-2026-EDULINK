package com.edulink.gui.services.reservation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.edulink.gui.models.event.Event;
import com.edulink.gui.models.reservation.Reservation;
import com.edulink.gui.services.event.EventService;
import com.edulink.gui.services.mail.MailService;
import com.edulink.gui.util.MyConnection;

public class ReservationService {

    private Connection cnx;

    public ReservationService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    // =====================
    // CREATE
    // =====================
    public boolean addReservation(Reservation reservation, String userEmail) {
        // Business logic: check if already reserved
        if (isAlreadyReserved(reservation.getUserId(), reservation.getEventId())) {
            System.out.println("❌ Erreur : L'utilisateur a déjà réservé cet événement.");
            return false;
        }

        String sql = "INSERT INTO reservation (user_id, event_id, reserved_at) VALUES (?, ?, ?)";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, reservation.getUserId());
            ps.setInt(2, reservation.getEventId());
            ps.setTimestamp(3, Timestamp.valueOf(reservation.getReservedAt()));
            ps.executeUpdate();

            // Récupérer l'id généré automatiquement
            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                reservation.setId(generatedKeys.getInt(1));
            }

            System.out.println("✅ Réservation effectuée !");

            // Envoyer le mail de confirmation
            EventService eventService = new EventService();
            Event event = eventService.getEventById(reservation.getEventId());

            if (event != null && userEmail != null) {
                MailService.sendReservationConfirmation(userEmail, event, reservation);
            }

            return true;
        } catch (SQLException e) {
            System.out.println("❌ Erreur ajout réservation : " + e.getMessage());
            return false;
        }
    }

    public boolean isAlreadyReserved(int userId, int eventId) {
        String sql = "SELECT COUNT(*) FROM reservation WHERE user_id = ? AND event_id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, eventId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur validation double réservation : " + e.getMessage());
        }
        return false;
    }

    // =====================
    // READ ALL
    // =====================
    public List<Reservation> getAllReservations() {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservation";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Reservation r = new Reservation();
                r.setId(rs.getInt("id"));
                r.setUserId(rs.getInt("user_id"));
                r.setEventId(rs.getInt("event_id"));
                r.setReservedAt(rs.getTimestamp("reserved_at").toLocalDateTime());
                reservations.add(r);
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur récupération réservations : " + e.getMessage());
        }
        return reservations;
    }

    // =====================
    // READ BY USER
    // =====================
    public List<Reservation> getReservationsByUserId(int userId) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservation WHERE user_id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Reservation r = new Reservation();
                r.setId(rs.getInt("id"));
                r.setUserId(rs.getInt("user_id"));
                r.setEventId(rs.getInt("event_id"));
                r.setReservedAt(rs.getTimestamp("reserved_at").toLocalDateTime());
                reservations.add(r);
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur récupération par utilisateur : " + e.getMessage());
        }
        return reservations;
    }

    // =====================
    // DELETE
    // =====================
    public boolean deleteReservation(int id) {
        String sql = "DELETE FROM reservation WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ Réservation annulée !");
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Erreur annulation : " + e.getMessage());
            return false;
        }
    }
}
