package com.edulink.gui.services;

import com.edulink.gui.util.GroqConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GroqService {

    public String ask(String prompt) {
        return generateResponse("You are a helpful AI assistant.", prompt);
    }

    public String generateResponse(String systemPrompt, String userMessage) {
        if (GroqConfig.mahdi_api_key == null || GroqConfig.mahdi_api_key.isEmpty() || GroqConfig.mahdi_api_key.equals("dummy_key_to_be_replaced")) {
            return "Please configure your Groq API key in the code (mahdi_api_key).";
        }
        
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            String escapedSystem = sanitizeJsonString(systemPrompt);
            String escapedUser = sanitizeJsonString(userMessage);

            String requestBody = "{"
                    + "\"model\": \"llama-3.1-8b-instant\","
                    + "\"messages\": ["
                    + "  {\"role\": \"system\", \"content\": \"" + escapedSystem + "\"},"
                    + "  {\"role\": \"user\", \"content\": \"" + escapedUser + "\"}"
                    + "],"
                    + "\"temperature\": 0.7"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Authorization", "Bearer " + GroqConfig.mahdi_api_key)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return extractContent(response.body());
            } else {
                return "API Error: " + response.statusCode() + "\n" + response.body();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    private String extractContent(String json) {
        String target = "\"content\":\"";
        int start = json.indexOf(target);
        if (start != -1) {
            start += target.length();
            StringBuilder sb = new StringBuilder();
            boolean escape = false;
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (escape) {
                    if (c == 'n') sb.append('\n');
                    else if (c == 'r') sb.append('\r');
                    else if (c == 't') sb.append('\t');
                    else if (c == '"') sb.append('"');
                    else if (c == '\\') sb.append('\\');
                    else sb.append(c);
                    escape = false;
                } else {
                    if (c == '\\') {
                        escape = true;
                    } else if (c == '"') {
                        break;
                    } else {
                        sb.append(c);
                    }
                }
            }
            return sb.toString();
        }
        return "Could not parse response.";
    }

    private String sanitizeJsonString(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\') sb.append("\\\\");
            else if (c == '"') sb.append("\\\"");
            else if (c == '\n') sb.append("\\n");
            else if (c == '\r') { /* ignore */ }
            else if (c == '\t') sb.append("\\t");
            else if (c < 32) {
                // Ignore invalid control characters like \x13
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
