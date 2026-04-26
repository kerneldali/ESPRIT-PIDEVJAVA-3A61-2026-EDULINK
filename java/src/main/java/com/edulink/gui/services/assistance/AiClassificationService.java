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

    /**
     * Returns a two-element array: [category, difficulty].
     * Falls back to ["General", "MEDIUM"] if AI is unavailable.
     */
    public String[] classify(String title, String description) {
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

            // Extract JSON from the response
            int start = response.indexOf('{');
            int end   = response.lastIndexOf('}');
            if (start < 0 || end < 0) return fallback();

            JsonNode node = mapper.readTree(response.substring(start, end + 1));
            String cat  = node.path("category").asText("General");
            String diff = node.path("difficulty").asText("MEDIUM");

            // Validate against known values
            cat  = isValidCategory(cat)   ? cat  : "General";
            diff = isValidDifficulty(diff) ? diff : "MEDIUM";

            return new String[]{cat, diff};

        } catch (Exception e) {
            System.err.println("[AiClassification] Error: " + e.getMessage());
            return fallback();
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
