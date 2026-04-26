package com.edulink.gui.services.assistance;

import com.edulink.gui.services.GroqService;

/**
 * Checks a piece of text for toxicity / hate speech using Groq.
 * Used in both the chat and community post submission flows.
 */
public class ToxicityService {

    private final GroqService groq = new GroqService();

    public static class ToxicityResult {
        public final boolean isToxic;
        public final String reason;

        public ToxicityResult(boolean isToxic, String reason) {
            this.isToxic = isToxic;
            this.reason  = reason;
        }
    }

    /**
     * Analyzes the given text and returns whether it is toxic.
     * Returns isToxic=false on API failure to avoid false positives.
     */
    public ToxicityResult analyze(String text) {
        if (text == null || text.isBlank()) return new ToxicityResult(false, "");

        try {
            String prompt = """
                You are a content moderation AI. Analyze the following text for toxicity,
                hate speech, harassment, or explicit content.
                
                Text: "%s"
                
                Reply ONLY in this exact format (no markdown):
                TOXIC: YES or NO
                REASON: one short sentence
                """.formatted(text.replace("\"", "'"));

            String response = groq.ask(prompt);
            if (response == null || response.isBlank()) return new ToxicityResult(false, "");

            boolean toxic = response.contains("TOXIC: YES");
            String reason = "";
            int idx = response.indexOf("REASON:");
            if (idx >= 0) reason = response.substring(idx + 7).trim();

            return new ToxicityResult(toxic, reason);

        } catch (Exception e) {
            System.err.println("[ToxicityService] Error: " + e.getMessage());
            return new ToxicityResult(false, "");
        }
    }
}
