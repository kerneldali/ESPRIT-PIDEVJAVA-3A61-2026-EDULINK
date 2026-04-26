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

    private static final String LOCAL_API_URL = "http://localhost:5001/analyze";

    /**
     * Analyzes the given text and returns whether it is toxic.
     * Uses local VADER-based Sentiment API first, falls back to Groq.
     */
    public ToxicityResult analyze(String text) {
        if (text == null || text.isBlank()) return new ToxicityResult(false, "");

        // 1. Try Local Sentiment API (Academic VADER)
        try {
            String jsonBody = "{\"text\": \"" + text.replace("\"", "\\\"") + "\"}";
            String localResp = post(LOCAL_API_URL, jsonBody);
            if (localResp != null && (localResp.contains("\"sentiment\":\"negative\"") || localResp.contains("\"sentiment\": \"negative\""))) {
                return new ToxicityResult(true, "Local AI detected negative/toxic sentiment.");
            }
        } catch (Exception e) {
            System.err.println("[ToxicityService] Local API failed: " + e.getMessage());
        }

        // 2. Fallback to Groq
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
            System.err.println("[ToxicityService] Groq Error: " + e.getMessage());
            return new ToxicityResult(false, "");
        }
    }

    private String post(String urlStr, String jsonBody) throws Exception {
        java.net.URL url = new java.net.URL(urlStr);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);
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
}
