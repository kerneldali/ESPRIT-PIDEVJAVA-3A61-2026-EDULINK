package com.edulink.gui.services;

import com.edulink.gui.models.journal.Note;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class GeminiService {
    // Pollinations API — free, no key needed, just send a plain text prompt
    private static final String FREE_API_URL = "https://text.pollinations.ai/";
    private final HttpClient client;

    public GeminiService() {
        this.client = HttpClient.newHttpClient();
    }

    public String generateWeeklySummary(List<Note> notes) {
        if (notes == null || notes.isEmpty())
            return "No notes found for this week.";

        String notesText = notes.stream()
                .map(n -> n.getTitle() + ": " + n.getContent())
                .collect(Collectors.joining("\n"));

        String prompt = "You are an educational assistant. Provide a structured summary of my journal notes. Keep it encouraging.\n\nNOTES:\n"
                + notesText;

        try {
            // encode the prompt so it's safe to embed in a URL path
            String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(FREE_API_URL + encodedPrompt))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body().trim();
            } else {
                return "Summary API error (" + response.statusCode() + "): " + response.body();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Could not reach summary service: " + e.getMessage();
        }
    }
}
