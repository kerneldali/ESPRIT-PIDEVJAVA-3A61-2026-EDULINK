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

    /**
     * Returns up to 3 short suggested replies.
     * Falls back to common academic phrases on failure.
     */
    public List<String> suggest(List<ChatMessage> recentMessages) {
        if (recentMessages == null || recentMessages.isEmpty()) {
            return defaultSuggestions();
        }

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
            System.err.println("[SuggestedRepliesService] Error: " + e.getMessage());
            return defaultSuggestions();
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
