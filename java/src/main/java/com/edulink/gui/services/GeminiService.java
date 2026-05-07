package com.edulink.gui.services;

import com.edulink.gui.models.journal.Note;
import com.edulink.gui.util.GeminiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;

public class GeminiService {
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GeminiConfig.GEMINI_API_KEY;
    private final HttpClient client;
    private final ObjectMapper mapper;

    public GeminiService() {
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    public String generateWeeklySummary(List<Note> notes) {
        if (notes == null || notes.isEmpty()) return "No notes found for this week.";

        String notesText = notes.stream()
                .map(n -> "- " + n.getTitle() + ": " + n.getContent())
                .collect(Collectors.joining("\n"));

        String prompt = "You are a helpful assistant. Below are my journal notes from the past week. " +
                "Please provide a structured executive summary highlighting key accomplishments, " +
                "recurring themes, and emotional mood. Keep it professional and encouraging.\n\n" +
                "NOTES:\n" + notesText;

        return callGemini(prompt);
    }

    public String analyzeSentiment(String content) {
        String prompt = "Analyze the sentiment of the following text. Respond with only ONE word: POSITIVE, NEUTRAL, or NEGATIVE.\n\n" +
                "TEXT: " + content;
        return callGemini(prompt).trim().toUpperCase();
    }

    public String suggestCategory(String content) {
        String prompt = "Suggest a single-word category for the following text (e.g., STUDY, PERSONAL, WORK, HEALTH, FINANCE).\n\n" +
                "TEXT: " + content;
        return callGemini(prompt).trim().toUpperCase();
    }

    private String callGemini(String prompt) {
        try {
            ObjectNode rootNode = mapper.createObjectNode();
            ArrayNode contentsArray = rootNode.putArray("contents");
            ObjectNode contentNode = contentsArray.addObject();
            ArrayNode partsArray = contentNode.putArray("parts");
            partsArray.addObject().put("text", prompt);

            String requestBody = mapper.writeValueAsString(rootNode);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode responseJson = mapper.readTree(response.body());
                return responseJson.path("candidates")
                        .get(0)
                        .path("content")
                        .path("parts")
                        .get(0)
                        .path("text")
                        .asText();
            } else {
                System.err.println("Gemini API Error: " + response.body());
                return "Error calling Gemini API (Status: " + response.statusCode() + ")";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Exception occurred: " + e.getMessage();
        }
    }
}
