package com.edulink.gui.services.assistance;

import com.edulink.gui.services.GroqService;
import com.edulink.gui.util.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Suggests the best tutors for a given help request.
 * Uses DB-based stats (sessions, quality, category match) + AI explanation.
 * Returns a scored, ranked list with per-tutor AI rationale.
 */
public class SmartMatchingService {

    private final GroqService groq = new GroqService();
    private Connection cnx;

    public SmartMatchingService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    // ─── TutorMatch DTO ───────────────────────────────────────────────────────
    public static class TutorMatch {
        public final int userId;
        public final String fullName;
        public final String email;
        public final int sessionsCompleted;
        public final double avgQuality;
        public final int categoryMatchCount;  // # sessions in the requested category
        public String aiReason;
        public int aiScore; // 0-100 composite score from AI

        public TutorMatch(int userId, String fullName, String email,
                          int sessionsCompleted, double avgQuality, int categoryMatchCount) {
            this.userId             = userId;
            this.fullName           = fullName;
            this.email              = email;
            this.sessionsCompleted  = sessionsCompleted;
            this.avgQuality         = avgQuality;
            this.categoryMatchCount = categoryMatchCount;
            this.aiReason           = "Experienced tutor";
            this.aiScore            = computeBaseScore(sessionsCompleted, avgQuality, categoryMatchCount);
        }

        /** Composite score used for initial ranking before AI enrichment */
        private static int computeBaseScore(int sessions, double quality, int catMatch) {
            double qualityPart = quality * 0.50;            // 50 pts max
            double sessionPart = Math.min(sessions, 20) * 1.5; // 30 pts max
            double matchPart   = Math.min(catMatch, 10)  * 2.0; // 20 pts max
            return (int) Math.min(100, qualityPart + sessionPart + matchPart);
        }

        /** User-facing score bar (█ blocks out of 10) */
        public String scoreBar() {
            int filled = (int) Math.round(aiScore / 10.0);
            return "█".repeat(filled) + "░".repeat(10 - filled) + " " + aiScore + "/100";
        }
    }

    // ─── Main entry ───────────────────────────────────────────────────────────

    /**
     * Returns a ranked list of up to 5 suitable tutors.
     * Always returns results even if AI is unavailable (falls back to DB scores).
     */
    public List<TutorMatch> findMatches(String category, String difficulty, String description) {
        List<TutorMatch> candidates = loadCandidatesFromDb(category);
        if (candidates.isEmpty()) return candidates;

        // Enrich with AI explanations
        try {
            enrichWithAi(candidates, category, difficulty, description);
        } catch (Exception e) {
            System.err.println("[SmartMatching] AI enrichment failed: " + e.getMessage());
        }

        // Sort: AI score desc, then quality desc, then sessions desc
        candidates.sort((a, b) -> {
            int s = Integer.compare(b.aiScore, a.aiScore);
            if (s != 0) return s;
            int q = Double.compare(b.avgQuality, a.avgQuality);
            return (q != 0) ? q : Integer.compare(b.sessionsCompleted, a.sessionsCompleted);
        });

        return candidates.subList(0, Math.min(5, candidates.size()));
    }

    // ─── DB Query ─────────────────────────────────────────────────────────────

    private List<TutorMatch> loadCandidatesFromDb(String category) {
        List<TutorMatch> out = new ArrayList<>();
        if (cnx == null) return out;

        // Main query: faculty + admin users with session stats
        String sql = """
            SELECT u.id, u.full_name, u.email,
                   COUNT(hs.id)                       AS sessions,
                   COALESCE(AVG(hs.quality_score), 0) AS avg_quality
            FROM user u
            LEFT JOIN help_session hs ON hs.tutor_id = u.id AND hs.bounty_paid = 1
            WHERE u.roles LIKE '%ROLE_FACULTY%' OR u.roles LIKE '%ROLE_ADMIN%'
            GROUP BY u.id
            ORDER BY avg_quality DESC, sessions DESC
            LIMIT 10
            """;

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int uid = rs.getInt("id");
                out.add(new TutorMatch(
                    uid,
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getInt("sessions"),
                    rs.getDouble("avg_quality"),
                    countCategoryMatch(uid, category)
                ));
            }
        } catch (SQLException e) {
            System.err.println("[SmartMatching] DB error: " + e.getMessage());
        }
        return out;
    }

    /** Counts how many sessions this tutor has in the requested category */
    private int countCategoryMatch(int tutorId, String category) {
        if (cnx == null || category == null || category.isBlank()) return 0;
        String sql = """
            SELECT COUNT(*) FROM help_session hs
            JOIN help_request hr ON hr.id = hs.help_request_id
            WHERE hs.tutor_id = ? AND hr.category = ?
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, tutorId);
            ps.setString(2, category);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[SmartMatching] categoryMatch error: " + e.getMessage());
        }
        return 0;
    }

    // ─── AI Enrichment ────────────────────────────────────────────────────────

    private void enrichWithAi(List<TutorMatch> tutors, String category,
                               String difficulty, String description) {
        if (tutors.isEmpty()) return;

        // Build tutor list for the prompt
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tutors.size(); i++) {
            TutorMatch t = tutors.get(i);
            sb.append(i + 1).append(". ").append(t.fullName)
              .append(" | sessions=").append(t.sessionsCompleted)
              .append(", avgQuality=").append(String.format("%.0f", t.avgQuality))
              .append(", categoryMatch=").append(t.categoryMatchCount).append("\n");
        }

        String prompt = """
            You are an intelligent tutor-matching AI for an educational platform.
            
            STUDENT REQUEST:
            Subject: %s | Difficulty: %s
            Description: "%s"
            
            AVAILABLE TUTORS:
            %s
            
            For each tutor, provide:
            1. A composite score (0-100) based on: expertise in subject (40%%), session quality (40%%), availability/experience (20%%)
            2. One specific sentence explaining why they are the best fit for THIS request
            
            Reply as EXACTLY this JSON array (no markdown):
            [{"n": 1, "score": 85, "reason": "..."}, {"n": 2, "score": 72, "reason": "..."}, ...]
            """.formatted(
                category == null ? "General" : category,
                difficulty == null ? "MEDIUM" : difficulty,
                description == null ? "" : description.replace("\"", "'"),
                sb.toString()
            );

        String resp = groq.ask(prompt);
        if (resp == null || resp.isBlank()) return;

        // Parse JSON array: [{"n":1,"score":85,"reason":"..."}, ...]
        try {
            // Find array bounds
            int arrStart = resp.indexOf('[');
            int arrEnd   = resp.lastIndexOf(']');
            if (arrStart < 0 || arrEnd < 0) return;
            String arr = resp.substring(arrStart + 1, arrEnd);

            // Split by object boundaries
            String[] objects = arr.split("\\},\\s*\\{");
            for (String obj : objects) {
                obj = obj.replace("{", "").replace("}", "").trim();
                int n      = parseInt(obj, "n", -1);
                int score  = parseInt(obj, "score", -1);
                String reason = parseStr(obj, "reason", "");
                if (n >= 1 && n <= tutors.size() && score >= 0) {
                    TutorMatch t = tutors.get(n - 1);
                    t.aiScore  = Math.min(100, score);
                    if (!reason.isBlank()) t.aiReason = reason;
                }
            }
        } catch (Exception e) {
            System.err.println("[SmartMatching] JSON parse failed: " + e.getMessage());
            // Fallback: just give each tutor a simple reason
            simpleFallbackReasons(tutors, category);
        }
    }

    private void simpleFallbackReasons(List<TutorMatch> tutors, String category) {
        for (TutorMatch t : tutors) {
            if (t.categoryMatchCount > 0) {
                t.aiReason = "Has " + t.categoryMatchCount + " previous sessions in " + category + ".";
            } else if (t.sessionsCompleted > 0) {
                t.aiReason = "Experienced tutor with " + t.sessionsCompleted + " completed sessions.";
            }
        }
    }

    // ─── Mini JSON parsers ────────────────────────────────────────────────────

    private int parseInt(String obj, String key, int def) {
        try {
            int idx = obj.indexOf("\"" + key + "\"");
            if (idx < 0) return def;
            int colon = obj.indexOf(':', idx);
            if (colon < 0) return def;
            StringBuilder num = new StringBuilder();
            for (int i = colon + 1; i < obj.length(); i++) {
                char c = obj.charAt(i);
                if (Character.isDigit(c)) num.append(c);
                else if (!Character.isWhitespace(c) && num.length() > 0) break;
            }
            return num.length() > 0 ? Integer.parseInt(num.toString()) : def;
        } catch (Exception e) { return def; }
    }

    private String parseStr(String obj, String key, String def) {
        try {
            int idx = obj.indexOf("\"" + key + "\"");
            if (idx < 0) return def;
            int colon = obj.indexOf(':', idx);
            if (colon < 0) return def;
            int q1 = obj.indexOf('"', colon + 1);
            if (q1 < 0) return def;
            int q2 = obj.indexOf('"', q1 + 1);
            if (q2 < 0) return def;
            return obj.substring(q1 + 1, q2);
        } catch (Exception e) { return def; }
    }
}
