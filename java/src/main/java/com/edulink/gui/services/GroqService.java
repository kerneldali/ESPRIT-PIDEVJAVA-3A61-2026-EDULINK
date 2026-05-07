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
            return "Error: Groq API key is not configured. Please add your key in GroqConfig.java.";
        }
        
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(60))
                    .build();

            String escapedSystem = sanitizeJsonString(systemPrompt);
            String escapedUser = sanitizeJsonString(userMessage);

            String requestBody = "{"
                    + "\"model\": \"llama-3.1-8b-instant\"," // Using the modern Llama 3.1 8B model
                    + "\"messages\": ["
                    + "  {\"role\": \"system\", \"content\": \"" + escapedSystem + "\"},"
                    + "  {\"role\": \"user\", \"content\": \"" + escapedUser + "\"}"
                    + "],"
                    + "\"temperature\": 0.4,"
                    + "\"max_tokens\": 1024"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Authorization", "Bearer " + GroqConfig.mahdi_api_key.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                String content = extractContent(response.body());
                return (content != null && !content.isEmpty()) ? content : "AI returned an empty response.";
            } else {
                String errorBody = response.body();
                System.err.println("Groq API Error: " + response.statusCode() + " - " + errorBody);
                if (response.statusCode() == 401) return "Error: Invalid/Expired Groq API Key.";
                if (response.statusCode() == 404) return "Error: Model not found or not available.";
                if (response.statusCode() == 429) return "Error: Rate limit exceeded. Try again in a few seconds.";
                return "AI Error (" + response.statusCode() + "): " + parseErrorMessage(errorBody);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Connection Error: " + e.getMessage();
        }
    }

    private String parseErrorMessage(String json) {
        try {
            String target = "\"message\":\"";
            int start = json.indexOf(target);
            if (start != -1) {
                start += target.length();
                int end = json.indexOf("\"", start);
                if (end != -1) return json.substring(start, end);
            }
        } catch (Exception e) {}
        return "Internal API failure.";
    }

    private String extractContent(String json) {
        try {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Parsing Error: Could not read AI response.";
    }

    private String sanitizeJsonString(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32) {
                        // Ignore control characters
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
