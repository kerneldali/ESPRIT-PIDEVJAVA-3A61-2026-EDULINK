package com.edulink.gui.services.event;

import java.io.InputStream;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

import com.edulink.gui.models.event.Event;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.ConferenceSolution;
import com.google.api.services.calendar.model.ConferenceSolutionKey;
import com.google.api.services.calendar.model.EntryPoint;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "EduLink";
    private static final String CREDENTIALS_FILE = "/edulink-486610-8edafaeff3a7.json";
    private static final String CALENDAR_ID = "primary";

    private static Calendar getCalendarService() throws Exception {
        InputStream credentialsStream = GoogleCalendarService.class
                .getResourceAsStream(CREDENTIALS_FILE);

        if (credentialsStream == null) {
            throw new Exception("❌ Fichier credentials introuvable !");
        }

        GoogleCredentials credentials = GoogleCredentials
                .fromStream(credentialsStream)
                .createScoped(Collections.singletonList(
                        "https://www.googleapis.com/auth/calendar"
                ));

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static String createMeetLink(Event event) {
        try {
            Calendar service = getCalendarService();

            com.google.api.services.calendar.model.Event googleEvent =
                    new com.google.api.services.calendar.model.Event();

            googleEvent.setSummary(event.getTitle());
            googleEvent.setDescription(event.getDescription());

            // Date de début
            DateTime startDateTime = new DateTime(
                    event.getDateStart()
                            .atZone(ZoneId.of("Africa/Tunis"))
                            .toInstant()
                            .toEpochMilli()
            );
            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime)
                    .setTimeZone("Africa/Tunis");
            googleEvent.setStart(start);

            // Date de fin
            DateTime endDateTime = new DateTime(
                    event.getDateEnd()
                            .atZone(ZoneId.of("Africa/Tunis"))
                            .toInstant()
                            .toEpochMilli()
            );
            EventDateTime end = new EventDateTime()
                    .setDateTime(endDateTime)
                    .setTimeZone("Africa/Tunis");
            googleEvent.setEnd(end);

            // Générer le lien Meet
            String meetCode = generateMeetCode();
            String meetLink = "https://meet.google.com/" + meetCode;

            // Configurer la conférence
            ConferenceData conferenceData = new ConferenceData();
            ConferenceSolution solution = new ConferenceSolution();
            ConferenceSolutionKey solutionKey = new ConferenceSolutionKey();
            solutionKey.setType("hangoutsMeet");
            solution.setKey(solutionKey);
            solution.setName("Google Meet");

            EntryPoint entryPoint = new EntryPoint();
            entryPoint.setEntryPointType("video");
            entryPoint.setUri(meetLink);
            entryPoint.setLabel(meetLink);

            conferenceData.set("solution", solution);
            conferenceData.setEntryPoints(Arrays.asList(entryPoint));
            conferenceData.setConferenceId(meetCode);
            googleEvent.setConferenceData(conferenceData);

            // Insérer l'événement dans Google Calendar
            com.google.api.services.calendar.model.Event createdEvent = service.events()
                    .insert(CALENDAR_ID, googleEvent)
                    .setConferenceDataVersion(1)
                    .execute();

            // Récupérer le lien Meet depuis la réponse
            String finalMeetLink = createdEvent.getConferenceData() != null
                    && createdEvent.getConferenceData().getEntryPoints() != null
                    && !createdEvent.getConferenceData().getEntryPoints().isEmpty()
                    ? createdEvent.getConferenceData().getEntryPoints().get(0).getUri()
                    : meetLink;

            System.out.println("✅ Lien Meet généré : " + finalMeetLink);
            return finalMeetLink;

        } catch (Exception e) {
            System.out.println("❌ Erreur Google Calendar : " + e.getMessage());
            return null;
        }
    }

    // Génère un code Meet aléatoire format : xxx-xxxx-xxx
    private static String generateMeetCode() {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        java.util.Random random = new java.util.Random();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 3; i++)
            code.append(chars.charAt(random.nextInt(chars.length())));
        code.append("-");
        for (int i = 0; i < 4; i++)
            code.append(chars.charAt(random.nextInt(chars.length())));
        code.append("-");
        for (int i = 0; i < 3; i++)
            code.append(chars.charAt(random.nextInt(chars.length())));

        return code.toString();
    }
}