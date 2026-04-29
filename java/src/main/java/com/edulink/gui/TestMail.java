package com.edulink.gui;

import java.time.LocalDateTime;

import com.edulink.gui.models.event.Event;
import com.edulink.gui.models.reservation.Reservation;
import com.edulink.gui.services.event.GoogleCalendarService;
import com.edulink.gui.services.mail.MailService;
import com.edulink.gui.services.ml.MLService;

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

        // Tester Google Calendar
        Event eventOnline = new Event();
        eventOnline.setId(1);
        eventOnline.setTitle("Hackathon IA 2026");
        eventOnline.setDescription("Un hackathon sur l'intelligence artificielle");
        eventOnline.setDateStart(LocalDateTime.of(2026, 5, 15, 9, 0));
        eventOnline.setDateEnd(LocalDateTime.of(2026, 5, 15, 18, 0));
        eventOnline.setOnline(true);

        String meetLink = GoogleCalendarService.createMeetLink(eventOnline);
        if (meetLink != null) {
            System.out.println("✅ Lien Meet : " + meetLink);
        } else {
            System.out.println("❌ Echec génération Meet");
        }

        // Tester le ML
        Event eventML = new Event();
        eventML.setTitle("Hackathon IA 2026");
        eventML.setDescription("Un hackathon sur l'intelligence artificielle");
        eventML.setDateStart(LocalDateTime.of(2026, 5, 15, 9, 0));
        eventML.setMaxCapacity(50);

        int score = MLService.predictEventSuccess(eventML);
        System.out.println(MLService.getPredictionMessage(score));
    }
}