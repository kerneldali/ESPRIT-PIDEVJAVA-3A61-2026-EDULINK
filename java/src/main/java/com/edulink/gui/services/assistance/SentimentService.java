package com.edulink.gui.services.assistance;

import com.edulink.gui.services.GroqService;

/**
 * Analyzes message sentiment (POSITIVE, NEGATIVE, NEUTRAL, CONFUSED)
 * and detects the language (EN, FR, AR, ES, ...).
 * Non-blocking — used for analytics and session quality boost.
 */
public class SentimentService {

    private final GroqService groq = new GroqService();
    private static final String LOCAL_SENTIMENT_URL = "http://localhost:5001/analyze";

    public enum Sentiment {
        POSITIVE, NEGATIVE, NEUTRAL, CONFUSED, UNKNOWN
    }

    public static class SentimentResult {
        public final Sentiment sentiment;
        public final String language;       // ISO 639-1 code: "en", "fr", "ar", ...
        public final double confidence;     // 0.0 – 1.0
        public final String raw;            // raw Groq / API response

        public SentimentResult(Sentiment sentiment, String language, double confidence, String raw) {
            this.sentiment  = sentiment;
            this.language   = language;
            this.confidence = confidence;
            this.raw        = raw;
        }

        /** Human-readable label with emoji */
        public String label() {
            return switch (sentiment) {
                case POSITIVE  -> "😊 Positive";
                case NEGATIVE  -> "😞 Negative";
                case NEUTRAL   -> "😐 Neutral";
                case CONFUSED  -> "🤔 Confused";
                default        -> "❓ Unknown";
            };
        }
    }

    /**
     * Analyzes text sentiment and language.
     * Tries local VADER API first, then falls back to Groq.
     * Returns UNKNOWN on any failure (never throws).
     */
    public SentimentResult analyze(String text) {
        if (text == null || text.isBlank()) {
            return new SentimentResult(Sentiment.UNKNOWN, "en", 0.0, "");
        }

        // 1 — Local VADER / sentiment microservice
        try {
            String json = "{\"text\": \"" + escape(text) + "\"}";
            String resp = post(LOCAL_SENTIMENT_URL, json, 2000);
            if (resp != null) {
                Sentiment s = parseSentimentFromVader(resp);
                String lang = parseLang(resp, "en");
                if (s != Sentiment.UNKNOWN) {
                    return new SentimentResult(s, lang, 0.85, resp);
                }
            }
        } catch (Exception e) {
            System.err.println("[SentimentService] Local API unavailable: " + e.getMessage());
        }

        // 2 — Groq LLaMA fallback
        try {
            String prompt = """
                Analyze the sentiment and language of this text.
                Text: "%s"
                
                Reply ONLY in this exact JSON format (no markdown):
                {"sentiment": "POSITIVE|NEGATIVE|NEUTRAL|CONFUSED", "language": "en|fr|ar|es|de|...", "confidence": 0.0-1.0}
                """.formatted(text.replace("\"", "'").replace("\n", " "));

            String resp = groq.ask(prompt);
            if (resp == null || resp.isBlank()) return unknown();

            Sentiment s      = parseSentimentFromJson(resp);
            String lang      = parseJsonField(resp, "language", "en");
            double conf      = parseJsonDouble(resp, "confidence", 0.7);
            return new SentimentResult(s, lang, conf, resp);

        } catch (Exception e) {
            System.err.println("[SentimentService] Groq error: " + e.getMessage());
            return unknown();
        }
    }

    // ─── Parsers ──────────────────────────────────────────────────────────────

    private Sentiment parseSentimentFromVader(String json) {
        String lower = json.toLowerCase();
        if (lower.contains("\"sentiment\":\"positive\"") || lower.contains("\"sentiment\": \"positive\""))
            return Sentiment.POSITIVE;
        if (lower.contains("\"sentiment\":\"negative\"") || lower.contains("\"sentiment\": \"negative\""))
            return Sentiment.NEGATIVE;
        if (lower.contains("\"sentiment\":\"neutral\"") || lower.contains("\"sentiment\": \"neutral\""))
            return Sentiment.NEUTRAL;
        return Sentiment.UNKNOWN;
    }

    private Sentiment parseSentimentFromJson(String json) {
        String v = parseJsonField(json, "sentiment", "NEUTRAL").toUpperCase().trim();
        try { return Sentiment.valueOf(v); } catch (Exception e) { return Sentiment.NEUTRAL; }
    }

    private String parseLang(String json, String defaultLang) {
        return parseJsonField(json, "language", defaultLang);
    }

    private String parseJsonField(String json, String key, String defaultVal) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return defaultVal;
        int colon = json.indexOf(':', idx);
        if (colon < 0) return defaultVal;
        int q1 = json.indexOf('"', colon + 1);
        if (q1 < 0) return defaultVal;
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 < 0) return defaultVal;
        return json.substring(q1 + 1, q2);
    }

    private double parseJsonDouble(String json, String key, double defaultVal) {
        try {
            int idx = json.indexOf("\"" + key + "\"");
            if (idx < 0) return defaultVal;
            int colon = json.indexOf(':', idx);
            if (colon < 0) return defaultVal;
            int end = colon + 1;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.'))
                end++;
            return Double.parseDouble(json.substring(colon + 1, end).trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    // ─── HTTP helper ──────────────────────────────────────────────────────────

    private String post(String urlStr, String body, int timeoutMs) throws Exception {
        java.net.URL url = new java.net.URL(urlStr);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        conn.setDoOutput(true);
        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes("utf-8"));
        }
        if (conn.getResponseCode() != 200) return null;
        try (java.util.Scanner s = new java.util.Scanner(conn.getInputStream(), "utf-8")) {
            s.useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }

    private SentimentResult unknown() {
        return new SentimentResult(Sentiment.UNKNOWN, "en", 0.0, "");
    }
}
