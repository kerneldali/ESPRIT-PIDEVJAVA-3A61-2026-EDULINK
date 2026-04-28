package com.edulink.gui.services.assistance;

import com.edulink.gui.services.GroqService;

/**
 * Advanced content moderation service.
 * Detects toxicity with category (HATE_SPEECH, HARASSMENT, EXPLICIT, SPAM, CLEAN)
 * and severity (LOW, MEDIUM, HIGH) using Groq + local API fallback.
 * Supports multi-language input.
 */
public class ToxicityService {

    private final GroqService groq = new GroqService();
    private static final String LOCAL_API_URL = "http://localhost:5001/analyze";

    // ─── Category enum ────────────────────────────────────────────────────────
    public enum ToxicCategory {
        HATE_SPEECH, HARASSMENT, EXPLICIT, SPAM, THREAT, CLEAN, UNKNOWN;

        public String emoji() {
            return switch (this) {
                case HATE_SPEECH  -> "🚫";
                case HARASSMENT   -> "⚠️";
                case EXPLICIT     -> "🔞";
                case SPAM         -> "📢";
                case THREAT       -> "⚡";
                case CLEAN        -> "✅";
                default           -> "❓";
            };
        }

        public String label() {
            return switch (this) {
                case HATE_SPEECH  -> "Hate Speech";
                case HARASSMENT   -> "Harassment";
                case EXPLICIT     -> "Explicit Content";
                case SPAM         -> "Spam";
                case THREAT       -> "Threat / Violence";
                case CLEAN        -> "Clean";
                default           -> "Unknown";
            };
        }
    }

    // ─── Severity enum ────────────────────────────────────────────────────────
    public enum Severity { LOW, MEDIUM, HIGH, NONE }

    // ─── Result ───────────────────────────────────────────────────────────────
    public static class ToxicityResult {
        public final boolean isToxic;
        public final String reason;
        public final ToxicCategory category;
        public final Severity severity;

        public ToxicityResult(boolean isToxic, String reason,
                              ToxicCategory category, Severity severity) {
            this.isToxic  = isToxic;
            this.reason   = reason;
            this.category = category;
            this.severity = severity;
        }

        /** Convenience constructor for backward-compat calls */
        public ToxicityResult(boolean isToxic, String reason) {
            this(isToxic, reason,
                 isToxic ? ToxicCategory.UNKNOWN : ToxicCategory.CLEAN,
                 isToxic ? Severity.MEDIUM : Severity.NONE);
        }

        /** User-facing badge text */
        public String badge() {
            return isToxic ? category.emoji() + " " + category.label()
                           + " [" + severity.name() + "]" : "✅ Clean";
        }
    }

    // ─── Main analysis ────────────────────────────────────────────────────────

    /**
     * Analyzes text for toxicity. Uses local VADER API first, then Groq.
     * Never throws — returns CLEAN result on any failure.
     */
    public ToxicityResult analyze(String text) {
        if (text == null || text.isBlank()) return clean();

        // 1. Quick keyword pre-screen (instant, no API call)
        ToxicityResult quickResult = quickscreen(text);
        if (quickResult.isToxic) return quickResult;

        // 2. Local VADER sentiment API
        try {
            String body = "{\"text\": \"" + escape(text) + "\"}";
            String resp = post(LOCAL_API_URL, body, 2000);
            if (resp != null
                && (resp.contains("\"sentiment\":\"negative\"")
                    || resp.contains("\"sentiment\": \"negative\""))) {
                // Escalate to Groq for category + severity classification
                return classifyWithGroq(text);
            }
        } catch (Exception e) {
            System.err.println("[ToxicityService] Local API failed: " + e.getMessage());
        }

        // 3. Groq full classification
        return classifyWithGroq(text);
    }

    // ─── Groq full classification ─────────────────────────────────────────────

    private ToxicityResult classifyWithGroq(String text) {
        try {
            String prompt = """
                You are an expert content moderation AI. Analyze the following text for safety.
                The text may be in any language (English, French, Arabic, etc.) — detect and consider it.
                
                Text: "%s"
                
                Classify it strictly using ONLY these categories:
                - HATE_SPEECH: targets race, religion, gender, ethnicity
                - HARASSMENT: personal insults, bullying, threats against individuals
                - EXPLICIT: sexual or graphic violent content
                - SPAM: irrelevant repeated content, scam links
                - THREAT: direct threats of violence or harm
                - CLEAN: no issues found
                
                Reply ONLY in this exact JSON (no markdown, no extra text):
                {"toxic": true/false, "category": "HATE_SPEECH|HARASSMENT|EXPLICIT|SPAM|THREAT|CLEAN", "severity": "LOW|MEDIUM|HIGH|NONE", "reason": "one concise sentence", "language": "en|fr|ar|es|..."}
                """.formatted(text.replace("\"", "'").replace("\n", " "));

            String resp = groq.ask(prompt);
            if (resp == null || resp.isBlank()) return clean();

            // Parse JSON fields
            boolean toxic       = parseBoolean(resp, "toxic", false);
            String catStr       = parseField(resp, "category", "CLEAN").toUpperCase().trim();
            String sevStr       = parseField(resp, "severity", "NONE").toUpperCase().trim();
            String reason       = parseField(resp, "reason", "");
            // language is informational — logged but not stored in result

            ToxicCategory cat = safeCategory(catStr);
            Severity sev      = safeSeverity(sevStr);

            // If AI says clean but UNKNOWN category, treat as clean
            if (!toxic) cat = ToxicCategory.CLEAN;

            return new ToxicityResult(toxic, reason, cat, sev);

        } catch (Exception e) {
            System.err.println("[ToxicityService] Groq error: " + e.getMessage());
            return clean();
        }
    }

    // ─── Quick keyword pre-screener ───────────────────────────────────────────
    // Catches obvious cases without any API call for sub-millisecond response

    private static final String[] OBVIOUS_KEYWORDS = {
        // English
        "kill yourself", "kys", "die you", "i will kill", "you're a piece of",
        // French
        "va te faire", "je vais te tuer", "ferme ta gueule",
        // Arabic (transliterated common insults)
        "ibn el", "bint el", "sharmouta", "kess",
        // Generic
        "n****", "f****", "c***"
    };

    private ToxicityResult quickscreen(String text) {
        String lower = text.toLowerCase();
        for (String kw : OBVIOUS_KEYWORDS) {
            if (lower.contains(kw)) {
                return new ToxicityResult(true,
                    "Contains prohibited language: '" + kw + "'",
                    ToxicCategory.HARASSMENT, Severity.HIGH);
            }
        }
        return clean();
    }

    // ─── Parsers ──────────────────────────────────────────────────────────────

    private boolean parseBoolean(String json, String key, boolean def) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return def;
        int colon = json.indexOf(':', idx);
        if (colon < 0) return def;
        String rest = json.substring(colon + 1).trim();
        return rest.startsWith("true");
    }

    private String parseField(String json, String key, String def) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return def;
        int colon = json.indexOf(':', idx);
        if (colon < 0) return def;
        int q1 = json.indexOf('"', colon + 1);
        if (q1 < 0) return def;
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 < 0) return def;
        return json.substring(q1 + 1, q2);
    }

    private ToxicCategory safeCategory(String s) {
        try { return ToxicCategory.valueOf(s); }
        catch (Exception e) { return ToxicCategory.UNKNOWN; }
    }

    private Severity safeSeverity(String s) {
        try { return Severity.valueOf(s); }
        catch (Exception e) { return Severity.NONE; }
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

    private ToxicityResult clean() {
        return new ToxicityResult(false, "", ToxicCategory.CLEAN, Severity.NONE);
    }
}
