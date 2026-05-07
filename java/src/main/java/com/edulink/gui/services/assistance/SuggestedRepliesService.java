package com.edulink.gui.services.assistance;

import com.edulink.gui.models.assistance.ChatMessage;
import com.edulink.gui.services.GroqService;
import com.edulink.gui.util.SessionManager;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates 3 context-aware, role-specific suggested replies based on recent chat history.
 * - If current user is a TUTOR → suggests explanations, examples, guiding questions
 * - If current user is a STUDENT → suggests follow-up questions, clarifications, confirmations
 * Uses local educational model first, falls back to Groq with topic + role context.
 */
public class SuggestedRepliesService {

    private final GroqService groq = new GroqService();
    private static final String LOCAL_SUGGEST_URL = "http://localhost:5000/suggest-replies";

    /**
     * Returns up to 3 short role-aware suggested replies.
     *
     * @param recentMessages recent chat messages (used as context)
     * @param sessionTopic   the help request category (e.g. "Mathematics")
     */
    public List<String> suggest(List<ChatMessage> recentMessages, String sessionTopic) {
        boolean isTutor = isTutor();
        if (recentMessages == null || recentMessages.isEmpty()) {
            return defaultSuggestions(isTutor);
        }

        // 1. Try Local Neural Suggestion Model (port 5000 /suggest-replies)
        try {
            // Build JSON array of recent messages (last 8)
            int from = Math.max(0, recentMessages.size() - 8);
            StringBuilder msgsJson = new StringBuilder("[");
            for (int i = from; i < recentMessages.size(); i++) {
                ChatMessage m = recentMessages.get(i);
                String sender = m.getSenderName() != null ? m.getSenderName() : "User";
                String content = safeStr(m.getContent());
                msgsJson.append("\"").append(sender).append(": ").append(content).append("\"");
                if (i < recentMessages.size() - 1) msgsJson.append(",");
            }
            msgsJson.append("]");
            String body = "{\"messages\": " + msgsJson + "}";

            String resp = post(LOCAL_SUGGEST_URL, body, 2000);
            if (resp != null && resp.contains("\"suggestions\"")) {
                // Parse suggestions array from JSON
                int arrStart = resp.indexOf('[', resp.indexOf("suggestions"));
                int arrEnd   = resp.indexOf(']', arrStart);
                if (arrStart >= 0 && arrEnd > arrStart) {
                    String inner = resp.substring(arrStart + 1, arrEnd);
                    List<String> suggestions = Arrays.stream(inner.split(",(?=\\s*\")"))
                        .map(s -> s.replaceAll("^\"|\"$|^\\s+\"", "").replace("\\\"", "\"").trim())
                        .filter(s -> !s.isBlank() && s.length() > 5)
                        .limit(3)
                        .collect(Collectors.toList());
                    if (!suggestions.isEmpty()) return suggestions;
                }
            }
        } catch (Exception e) {
            System.err.println("[SuggestedReplies] Local API failed: " + e.getMessage());
        }

        // 2. Groq fallback with role + topic context
        try {
            int from = Math.max(0, recentMessages.size() - 8);
            String context = recentMessages.subList(from, recentMessages.size()).stream()
                .map(m -> (m.getSenderName() != null ? m.getSenderName() : "User") + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

            String roleInstruction = isTutor
                ? """
                  You are helping a TUTOR in a tutoring session.
                  Suggest 3 short replies the TUTOR could send — focus on:
                  • Clear explanations or analogies
                  • Guiding questions to check understanding
                  • Encouraging the student to attempt the problem
                  """
                : """
                  You are helping a STUDENT in a tutoring session.
                  Suggest 3 short replies the STUDENT could send — focus on:
                  • Questions to deepen understanding
                  • Confirming they understood the explanation
                  • Asking for an example or different approach
                  """;

            String prompt = """
                %s
                
                Subject: %s
                
                Recent conversation:
                %s
                
                Give EXACTLY 3 natural, short replies (max 12 words each).
                Format as exactly 3 numbered lines:
                1. [reply]
                2. [reply]
                3. [reply]
                No extra text or explanation.
                """.formatted(
                    roleInstruction.trim(),
                    sessionTopic != null ? sessionTopic : "General",
                    context
                );

            String response = groq.ask(prompt);
            if (response == null || response.isBlank()) return defaultSuggestions(isTutor);

            List<String> suggestions = Arrays.stream(response.split("\n"))
                .map(l -> l.replaceAll("^[0-9]+[.)\\s]+", "").trim())
                .filter(l -> !l.isBlank() && l.length() > 3 && l.length() < 120)
                .limit(3)
                .collect(Collectors.toList());

            return suggestions.isEmpty() ? defaultSuggestions(isTutor) : suggestions;

        } catch (Exception e) {
            System.err.println("[SuggestedReplies] Groq error: " + e.getMessage());
            return defaultSuggestions(isTutor);
        }
    }

    /** Backward-compat overload (no topic) */
    public List<String> suggest(List<ChatMessage> recentMessages) {
        return suggest(recentMessages, null);
    }

    // ─── Defaults ─────────────────────────────────────────────────────────────

    private List<String> defaultSuggestions(boolean isTutor) {
        if (isTutor) {
            return List.of(
                "Let me explain this step by step.",
                "Does that make sense so far?",
                "Try it yourself — I'll check your work."
            );
        } else {
            return List.of(
                "Can you explain that a bit more?",
                "I think I understand, let me try.",
                "Could you give me an example?"
            );
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private boolean isTutor() {
        var user = SessionManager.getCurrentUser();
        if (user == null) return false;
        String roles = user.getRoles();
        return roles != null && (roles.contains("ROLE_FACULTY") || roles.contains("ROLE_ADMIN"));
    }

    private String safeStr(String s) {
        if (s == null) return "";
        return s.replace("\"", "'").replace("\n", " ");
    }

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
}

