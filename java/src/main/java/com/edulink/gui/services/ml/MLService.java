package com.edulink.gui.services.ml;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.edulink.gui.models.event.Event;

public class MLService {

    private static final String FLASK_URL = "http://localhost:5000/predict";

    public static int predictEventSuccess(Event event) {
        try {
            // Construire le JSON à envoyer
            String json = String.format(
                "{\"title\": \"%s\", \"description\": \"%s\", \"dateStart\": \"%s\", \"maxCapacity\": %d}",
                event.getTitle().replace("\"", "\\\""),
                event.getDescription().replace("\"", "\\\""),
                event.getDateStart().toString(),
                event.getMaxCapacity()
            );

            // Créer la requête HTTP
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(FLASK_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            // Envoyer et récupérer la réponse
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                // Parser le résultat : {"predicted_target": 1}
                int score = Integer.parseInt(
                    body.replace("{", "")
                        .replace("}", "")
                        .replace("\"predicted_target\":", "")
                        .trim()
                );
                System.out.println("🤖 Prédiction ML : " + (score == 1 ? "✅ Succès" : "❌ Echec"));
                return score;
            } else {
                System.out.println("❌ Erreur Flask : " + response.statusCode());
                return -1;
            }

        } catch (Exception e) {
            System.out.println("❌ Erreur ML : " + e.getMessage());
            return -1;
        }
    }

    // Retourne un message lisible pour l'utilisateur
    public static String getPredictionMessage(int score) {
        return switch (score) {
            case 1 -> "🚀 Cet événement a de grandes chances de cartonner !";
            case 0 -> "⚠️ Cet événement risque de ne pas attirer beaucoup de participants.";
            default -> "🤖 Prédiction indisponible — vérifie que le serveur ML est lancé.";
        };
    }
}
