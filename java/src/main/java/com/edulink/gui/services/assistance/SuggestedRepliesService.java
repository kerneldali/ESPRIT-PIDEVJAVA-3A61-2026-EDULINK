package com.edulink.gui.services.assistance;

import com.edulink.gui.models.assistance.ChatMessage;
import com.edulink.gui.services.GroqService;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates 3 context-aware suggested replies based on recent chat history.
 * Used to speed up the student-tutor conversation flow.
 */
public class SuggestedRepliesService {

    private final GroqService groq = new GroqService();

    private static final String LOCAL_SUGGEST_URL = "http://localhost:5004/predict";

    /**
     * Returns up to 3 short suggested replies.
     * Uses local Educational Challenge API first (ali), falls back to Groq.
     */
    public List<String> suggest(List<ChatMessage> recentMessages) {
        if (recentMessages == null || recentMessages.isEmpty()) {
            return defaultSuggestions();
        }

        // 1. Try Local Educational Model (Predict learning tasks)
        try {
            String lastMsg = recentMessages.get(recentMessages.size() - 1).getContent();
            String jsonBody = "{\"challenge_title\": \"Study Session\", \"challenge_goal\": \"" + lastMsg.replace("\"", "'") + "\"}";
            String localResp = post(LOCAL_SUGGEST_URL, jsonBody);
            
            if (localResp != null && localResp.contains("\"generated_tasks\"")) {
                // Crude parsing of JSON array
                String tasksPart = localResp.substring(localResp.indexOf("[") + 1, localResp.indexOf("]"));
                List<String> tasks = Arrays.stream(tasksPart.split(","))
                    .map(s -> s.replace("\"", "").trim())
                    .filter(s -> !s.isBlank())
                    .limit(3)
                    .collect(Collectors.toList());
                
                if (!tasks.isEmpty()) return tasks;
            }
        } catch (Exception e) {
            System.err.println("[SuggestedRepliesService] Local API failed: " + e.getMessage());
        }

        // 2. Fallback to Groq
        try {
            // Take the last 6 messages as context
            int from = Math.max(0, recentMessages.size() - 6);
            String context = recentMessages.subList(from, recentMessages.size()).stream()
                .map(m -> (m.getSenderName() != null ? m.getSenderName() : "User") + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

            String prompt = """
                You are helping a student in a tutoring session. Based on the recent conversation:
                
                %s
                
                Suggest exactly 3 short, natural follow-up replies the student could send.
                Format your response as exactly 3 lines:
                1. [suggestion]
                2. [suggestion]
                3. [suggestion]
                No extra text, no numbering other than 1/2/3.
                """.formatted(context);

            String response = groq.ask(prompt);
            if (response == null || response.isBlank()) return defaultSuggestions();

            List<String> suggestions = Arrays.stream(response.split("\n"))
                .map(l -> l.replaceAll("^[0-9]+\\.\\s*", "").trim())
                .filter(l -> !l.isBlank())
                .limit(3)
                .collect(Collectors.toList());

            return suggestions.isEmpty() ? defaultSuggestions() : suggestions;

        } catch (Exception e) {
            System.err.println("[SuggestedRepliesService] Groq Error: " + e.getMessage());
            return defaultSuggestions();
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

    private List<String> defaultSuggestions() {
        return List.of(
            "Can you explain that a bit more?",
            "I think I understand, let me try.",
            "Could you give me an example?"
        );
    }
}
