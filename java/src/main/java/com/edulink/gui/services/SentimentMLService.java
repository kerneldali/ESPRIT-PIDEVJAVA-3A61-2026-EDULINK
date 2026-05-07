package com.edulink.gui.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SentimentMLService {
    // points to the NLP sentiment endpoint running locally
    private static final String API_URL = "http://localhost:5001/analyze";

    private final HttpClient client;
    private final ObjectMapper mapper;

    public SentimentMLService() {
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    public String predictSentiment(String text) {
        try {
            ObjectNode rootNode = mapper.createObjectNode();
            rootNode.put("text", text);

            String requestBody = mapper.writeValueAsString(rootNode);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode responseJson = mapper.readTree(response.body());
                return responseJson.path("sentiment").asText();
            } else {
                return "neutral";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "neutral";
        }
    }
}
