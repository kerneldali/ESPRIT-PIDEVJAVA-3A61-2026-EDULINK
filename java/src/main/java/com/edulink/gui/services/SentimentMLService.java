package com.edulink.gui.services;

import com.edulink.gui.util.GroqConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SentimentMLService {
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final HttpClient client;
    private final ObjectMapper mapper;

    public SentimentMLService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
    }

    public String predictSentiment(String text) {
        try {
            ObjectNode rootNode = mapper.createObjectNode();
            rootNode.put("model", "llama3-8b-8192");

            ArrayNode messages = rootNode.putArray("messages");

            ObjectNode systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content",
                    "You are a sentiment classifier. Classify the sentiment of the given text as exactly one word: POSITIVE, NEGATIVE, or NEUTRAL. Reply with ONLY that single word, nothing else.");

            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", text);

            rootNode.put("temperature", 0);
            rootNode.put("max_tokens", 10);

            String requestBody = mapper.writeValueAsString(rootNode);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + GroqConfig.mahdi_api_key)
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("🧠 Sentiment API response status: " + response.statusCode());

            if (response.statusCode() == 200) {
                JsonNode responseJson = mapper.readTree(response.body());
                String result = responseJson.path("choices").get(0)
                        .path("message").path("content").asText().trim().toUpperCase();

                System.out.println("🧠 Sentiment result: " + result);

                if (result.contains("POSITIVE")) return "POSITIVE";
                if (result.contains("NEGATIVE")) return "NEGATIVE";
                return "NEUTRAL";
            } else {
                System.err.println("❌ Sentiment API error: " + response.body());
                return "NEUTRAL";
            }
        } catch (Exception e) {
            System.err.println("❌ Sentiment analysis exception: " + e.getMessage());
            e.printStackTrace();
            return "NEUTRAL";
        }
    }
}
