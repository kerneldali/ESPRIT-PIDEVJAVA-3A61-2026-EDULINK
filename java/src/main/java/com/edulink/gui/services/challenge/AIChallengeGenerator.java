package com.edulink.gui.services.challenge;

import com.edulink.gui.models.challenge.Challenge;
import com.edulink.gui.models.challenge.ChallengeTask;
import com.edulink.gui.services.GroqService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates a complete Challenge (title + description + difficulty + xp + tasks)
 * from a free-text topic, using the existing GroqService HTTP layer.
 *
 * Why a wrapper instead of calling GroqService directly:
 *   - Centralises the strict JSON schema we want from the LLM.
 *   - Validates and clamps values (xp 10..500, difficulty whitelist) so a hallucinated
 *     response can never break the form or the DB.
 *   - Lets the caller treat the result as a domain object rather than raw text.
 */
public class AIChallengeGenerator {

    private final GroqService groq = new GroqService();
    private final ObjectMapper mapper = new ObjectMapper();

    /** Carries both the Challenge and its tasks so the UI can pre-fill the form. */
    public static class GeneratedChallenge {
        public Challenge challenge;
        public List<ChallengeTask> tasks = new ArrayList<>();
        public String rawResponse; // useful for debugging if parsing fails
        public String error;       // null if success
    }

    /**
     * Asks the LLM to design a challenge around the given topic and returns a
     * GeneratedChallenge. Never throws — always returns a result object whose
     * {@code error} field is set on failure so the caller can show a toast.
     */
    public GeneratedChallenge generate(String topic) {
        GeneratedChallenge out = new GeneratedChallenge();

        if (topic == null || topic.isBlank()) {
            out.error = "Le sujet ne peut pas être vide.";
            return out;
        }

        String systemPrompt =
            "You are an instructional designer for a gamified learning platform called EduLink. " +
            "You design challenges (small structured assignments) for university students. " +
            "ALWAYS respond with a SINGLE JSON object — no prose, no markdown, no code fences. " +
            "The JSON MUST follow this exact schema:\n" +
            "{\n" +
            "  \"title\": string (max 80 chars, in French),\n" +
            "  \"description\": string (3-5 sentences, in French, motivational),\n" +
            "  \"difficulty\": one of \"EASY\", \"MEDIUM\", \"HARD\",\n" +
            "  \"xpReward\": integer between 50 and 300 (consistent with the difficulty),\n" +
            "  \"deadlineDaysFromNow\": integer between 3 and 30,\n" +
            "  \"tasks\": array of 3 to 5 objects, each: { \"title\": string, " +
            "\"description\": string, \"required\": boolean }\n" +
            "}\n" +
            "Difficulty heuristic: EASY=50-100 XP, MEDIUM=100-200 XP, HARD=200-300 XP. " +
            "At least 2 tasks must have \"required\": true. " +
            "Do not include any field outside this schema.";

        String userMessage = "Topic: " + topic + "\nGenerate the challenge JSON now.";

        String raw = groq.generateResponse(systemPrompt, userMessage);
        out.rawResponse = raw;

        if (raw == null || raw.isBlank()) {
            out.error = "Réponse vide de l'IA.";
            return out;
        }
        if (raw.startsWith("Please configure") || raw.startsWith("API Error") || raw.startsWith("Error:")) {
            out.error = raw;
            return out;
        }

        // The model sometimes wraps JSON in code fences despite instructions —
        // be defensive about it.
        String cleaned = stripCodeFences(raw).trim();

        try {
            JsonNode node = mapper.readTree(cleaned);

            String title       = textOrEmpty(node, "title");
            String description = textOrEmpty(node, "description");
            String difficulty  = textOrEmpty(node, "difficulty").toUpperCase();
            int    xp          = intOrDefault(node, "xpReward", 100);
            int    days        = intOrDefault(node, "deadlineDaysFromNow", 7);

            // Sanitise. If the model misbehaves, we still produce a usable object.
            if (title.length() > 80) title = title.substring(0, 80);
            if (!"EASY".equals(difficulty) && !"MEDIUM".equals(difficulty) && !"HARD".equals(difficulty)) {
                difficulty = "MEDIUM";
            }
            if (xp < 10)   xp = 10;
            if (xp > 500)  xp = 500;
            if (days < 1)  days = 1;
            if (days > 60) days = 60;

            Challenge c = new Challenge();
            c.setTitle(title.isBlank() ? ("Challenge: " + topic) : title);
            c.setDescription(description);
            c.setDifficulty(difficulty);
            c.setXpReward(xp);
            c.setStatus("OPEN");
            c.setDeadline(LocalDateTime.now().plusDays(days));
            c.setCreatedAt(LocalDateTime.now());
            out.challenge = c;

            JsonNode tasksNode = node.get("tasks");
            if (tasksNode != null && tasksNode.isArray()) {
                int order = 1;
                for (JsonNode t : tasksNode) {
                    ChallengeTask task = new ChallengeTask();
                    task.setTitle(textOrEmpty(t, "title"));
                    task.setDescription(textOrEmpty(t, "description"));
                    task.setRequired(t.has("required") && t.get("required").asBoolean(true));
                    task.setOrderIndex(order++);
                    if (!task.getTitle().isBlank()) {
                        out.tasks.add(task);
                    }
                }
            }

            // Final guarantee: at least one required task, otherwise the user
            // would be unable to complete it.
            if (!out.tasks.isEmpty() && out.tasks.stream().noneMatch(ChallengeTask::isRequired)) {
                out.tasks.get(0).setRequired(true);
            }

            return out;
        } catch (Exception e) {
            out.error = "Impossible de parser la réponse IA : " + e.getMessage();
            System.err.println("[AIChallengeGenerator] parse failed. Raw: " + cleaned);
            return out;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String stripCodeFences(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) t = t.substring(firstNl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t;
    }

    private String textOrEmpty(JsonNode parent, String field) {
        if (parent == null) return "";
        JsonNode n = parent.get(field);
        return n == null || n.isNull() ? "" : n.asText("");
    }

    private int intOrDefault(JsonNode parent, String field, int def) {
        if (parent == null) return def;
        JsonNode n = parent.get(field);
        return n == null || n.isNull() ? def : n.asInt(def);
    }
}
