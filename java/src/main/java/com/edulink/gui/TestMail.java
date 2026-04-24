package com.edulink.gui;

import java.time.LocalDateTime;

import com.edulink.gui.models.event.Event;
import com.edulink.gui.models.reservation.Reservation;
import com.edulink.gui.services.mail.MailService;

public class TestMail {
    public static void main(String[] args) {

        // Créer un événement fictif
        Event event = new Event();
        event.setId(1);
        event.setTitle("Hackathon IA 2026");
        event.setDescription("Un hackathon sur l'intelligence artificielle");
        event.setDateStart(LocalDateTime.of(2026, 5, 15, 9, 0));
        event.setDateEnd(LocalDateTime.of(2026, 5, 15, 18, 0));
        event.setOnline(false);
        event.setLocation("ESPRIT, Ariana");
        event.setMaxCapacity(50);

        // Créer une réservation fictive
        Reservation reservation = new Reservation();
        reservation.setId(42);
        reservation.setUserId(15);
        reservation.setEventId(1);
        reservation.setReservedAt(LocalDateTime.now());

        // Envoyer le mail
        // ⚠️ Remplace par ton vrai email pour recevoir le test
        boolean result = MailService.sendReservationConfirmation(
                "zariatyassine1@gmail.com",   // ← mets ton email ici
                event,
                reservation
        );

        if (result) {
            System.out.println("✅ Mail envoyé avec succès !");
        } else {
            System.out.println("❌ Echec de l'envoi !");
        }
    }
}