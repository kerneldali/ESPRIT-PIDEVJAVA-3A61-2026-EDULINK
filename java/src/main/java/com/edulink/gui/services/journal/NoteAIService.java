package com.edulink.gui.services.journal;

import com.edulink.gui.models.journal.Note;
import java.util.List;

/**
 * Service for AI-powered note summarization and categorization.
 * In a real application, this would call the Gemini API.
 */
public class NoteAIService {

    /**
     * Generates a weekly summary of notes.
     */
    public String generateWeeklySummary(List<Note> notes) {
        if (notes.isEmpty())
            return "No notes found for this week.";
        try {
            java.net.URL url = new java.net.URL("http://localhost:5001/summarize");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            StringBuilder notesJson = new StringBuilder("[");
            for (int i = 0; i < notes.size(); i++) {
                Note n = notes.get(i);
                notesJson.append("{\"title\": \"").append(n.getTitle().replace("\"", "\\\"")).append("\", ")
                        .append("\"content\": \"").append(n.getContent().replace("\"", "\\\"")).append("\"}");
                if (i < notes.size() - 1)
                    notesJson.append(", ");
            }
            notesJson.append("]");

            String jsonInput = "{\"notes\": " + notesJson.toString() + "}";
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(jsonInput.getBytes("utf-8"));
            }

            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream(), "utf-8"))) {
                return br.lines().collect(java.util.stream.Collectors.joining("\n"));
            }
        } catch (Exception e) {
            return "Error calling Summary API: " + e.getMessage();
        }
    }

    /**
     * Suggests categories based on the summary.
     */
    public String suggestCategory(String summary) {
        if (summary.contains("study") || summary.contains("exam"))
            return "Education";
        if (summary.contains("work") || summary.contains("project"))
            return "Work";
        if (summary.contains("personal") || summary.contains("feel"))
            return "Personal";
        return "General";
    }
}
