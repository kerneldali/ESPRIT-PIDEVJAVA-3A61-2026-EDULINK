package com.edulink.gui.services.assistance;

import com.edulink.gui.services.GroqService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Classifies a help request's category and difficulty using Groq (LLaMA).
 * Falls back gracefully if the AI call fails.
 */
public class AiClassificationService {

    private final GroqService groq = new GroqService();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String[] CATEGORIES = {
        "Mathematics", "Programming", "Physics", "Chemistry",
        "Language", "History", "Biology", "Data Science", "General"
    };

    private static final String[] DIFFICULTIES = {"EASY", "MEDIUM", "HARD"};

    private static final String LOCAL_CLASS_URL = "http://localhost:8000/predict";

    /**
     * Returns a two-element array: [category, difficulty].
     * Uses local Educational Model first, falls back to Groq.
     */
    public String[] classify(String title, String description) {
        // 1. Try Local Classification API
        try {
            String jsonBody = String.format("{\"challenge_title\": \"%s\", \"challenge_goal\": \"%s\"}", 
                title.replace("\"", "'"), description.replace("\"", "'"));
            String localResp = post(LOCAL_CLASS_URL, jsonBody);
            
            if (localResp != null && localResp.contains("\"predicted_category\"")) {
                JsonNode node = mapper.readTree(localResp);
                String cat = node.path("predicted_category").asText("");
                
                if (!cat.isEmpty()) {
                    // Try to match with our list to normalize capitalization
                    for (String c : CATEGORIES) {
                        if (c.equalsIgnoreCase(cat)) {
                            return new String[]{c, "MEDIUM"};
                        }
                    }
                    // If no match, return the raw category from the model
                    return new String[]{cat, "MEDIUM"};
                }
            }
        } catch (Exception e) {
            System.err.println("[AiClassification] Local API failed: " + e.getMessage());
        }

        // 2. Fallback to Groq
        try {
            String prompt = """
                You are an academic assistant. Classify the following student help request.
                
                Title: "%s"
                Description: "%s"
                
                Respond ONLY in this exact JSON format (no markdown, no explanation):
                {"category": "ONE_OF[Mathematics,Programming,Physics,Chemistry,Language,History,Biology,Data Science,General]",
                 "difficulty": "ONE_OF[EASY,MEDIUM,HARD]"}
                """.formatted(title, description);

            String response = groq.ask(prompt);
            if (response == null || response.isBlank()) return fallback();

            int start = response.indexOf('{');
            int end   = response.lastIndexOf('}');
            if (start < 0 || end < 0) return fallback();

            JsonNode node = mapper.readTree(response.substring(start, end + 1));
            String cat  = node.path("category").asText("General");
            String diff = node.path("difficulty").asText("MEDIUM");

            cat  = isValidCategory(cat)   ? cat  : "General";
            diff = isValidDifficulty(diff) ? diff : "MEDIUM";

            return new String[]{cat, diff};

        } catch (Exception e) {
            System.err.println("[AiClassification] Groq Error: " + e.getMessage());
            return fallback();
        }
    }

    private String post(String urlStr, String jsonBody) throws Exception {
        java.net.URL url = new java.net.URL(urlStr);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(1500);
        conn.setReadTimeout(1500);
        conn.setDoOutput(true);
        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes("utf-8"));
        }
        if (conn.getResponseCode() != 200) return null;
        try (java.util.Scanner s = new java.util.Scanner(conn.getInputStream(), "utf-8")) {
            s.useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

    private String[] fallback() {
        return new String[]{"General", "MEDIUM"};
    }

    private boolean isValidCategory(String c) {
        for (String v : CATEGORIES) if (v.equalsIgnoreCase(c)) return true;
        return false;
    }

    private boolean isValidDifficulty(String d) {
        for (String v : DIFFICULTIES) if (v.equalsIgnoreCase(d)) return true;
        return false;
    }
}
