package com.edulink.gui.services.assistance;

import com.edulink.gui.models.assistance.ChatMessage;
import com.edulink.gui.services.GroqService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates an AI summary of a completed tutoring session
 * and evaluates a quality score (0-100) for anti-farming purposes.
 */
public class SessionSummaryService {

    private final GroqService groq = new GroqService();

    public static class SummaryResult {
        public final String summary;
        public final int qualityScore; // 0-100

        public SummaryResult(String summary, int qualityScore) {
            this.summary      = summary;
            this.qualityScore = qualityScore;
        }
    }

    /**
     * Summarizes the chat transcript and scores session quality.
     * Falls back to a generic message if the API is unavailable.
     */
    public SummaryResult summarize(List<ChatMessage> messages) {
        if (messages == null || messages.size() < 3) {
            return new SummaryResult("Session too short to summarize.", 30);
        }

        try {
            String transcript = messages.stream()
                .map(m -> (m.getSenderName() != null ? m.getSenderName() : "User") + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

            String prompt = """
                You are an academic assistant reviewing a tutoring session transcript.
                
                TRANSCRIPT:
                %s
                
                Please respond ONLY with this exact format (no markdown):
                SUMMARY: a 3-5 sentence plain text summary of what was learned
                QUALITY: a number from 0 to 100 rating how productive this session was
                (100 = excellent tutoring, 0 = no real academic content)
                """.formatted(transcript);

            String response = groq.ask(prompt);
            if (response == null || response.isBlank()) return fallback();

            String summary = "Session completed.";
            int quality    = 50;

            int si = response.indexOf("SUMMARY:");
            int qi = response.indexOf("QUALITY:");
            if (si >= 0) {
                int end = (qi > si) ? qi : response.length();
                summary = response.substring(si + 8, end).trim();
            }
            if (qi >= 0) {
                String qStr = response.substring(qi + 8).trim().replaceAll("[^0-9]", "");
                if (!qStr.isEmpty()) {
                    quality = Math.min(100, Math.max(0, Integer.parseInt(qStr)));
                }
            }

            return new SummaryResult(summary, quality);

        } catch (Exception e) {
            System.err.println("[SessionSummaryService] Error: " + e.getMessage());
            return fallback();
        }
    }

    private SummaryResult fallback() {
        return new SummaryResult("The tutoring session has been completed. No summary available.", 50);
    }
}
