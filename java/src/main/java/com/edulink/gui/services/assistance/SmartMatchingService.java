package com.edulink.gui.services.assistance;

import com.edulink.gui.services.GroqService;
import com.edulink.gui.util.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Suggests the best tutors for a given help request
 * using DB-based stats and an optional AI scoring pass.
 */
public class SmartMatchingService {

    private final GroqService groq = new GroqService();
    private Connection cnx;

    public SmartMatchingService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    public static class TutorMatch {
        public final int userId;
        public final String fullName;
        public final String email;
        public final int sessionsCompleted;
        public final double avgQuality;
        public String aiReason;

        public TutorMatch(int userId, String fullName, String email,
                          int sessionsCompleted, double avgQuality) {
            this.userId            = userId;
            this.fullName          = fullName;
            this.email             = email;
            this.sessionsCompleted = sessionsCompleted;
            this.avgQuality        = avgQuality;
            this.aiReason          = "Experienced tutor";
        }
    }

    /**
     * Returns a ranked list of up to 5 suitable tutors for this help request.
     */
    public List<TutorMatch> findMatches(String category, String difficulty, String description) {
        List<TutorMatch> candidates = loadCandidatesFromDb();
        if (candidates.isEmpty()) return candidates;

        // Try AI ranking if Groq is available
        try {
            enrichWithAiReason(candidates, category, difficulty, description);
        } catch (Exception e) {
            System.err.println("[SmartMatching] AI ranking failed, using DB stats: " + e.getMessage());
        }

        // Sort: avg quality desc, then sessions desc
        candidates.sort((a, b) -> {
            int q = Double.compare(b.avgQuality, a.avgQuality);
            return (q != 0) ? q : Integer.compare(b.sessionsCompleted, a.sessionsCompleted);
        });

        return candidates.subList(0, Math.min(5, candidates.size()));
    }

    // Loads faculty/tutor users with their session stats
    private List<TutorMatch> loadCandidatesFromDb() {
        List<TutorMatch> out = new ArrayList<>();
        if (cnx == null) return out;
        String sql = """
            SELECT u.id, u.full_name, u.email,
                   COUNT(hs.id) AS sessions,
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
                out.add(new TutorMatch(
                    rs.getInt("id"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getInt("sessions"),
                    rs.getDouble("avg_quality")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[SmartMatching] DB error: " + e.getMessage());
        }
        return out;
    }

    private void enrichWithAiReason(List<TutorMatch> tutors, String category,
                                     String difficulty, String description) {
        if (tutors.isEmpty()) return;
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < tutors.size(); i++) {
            names.append((i + 1)).append(". ").append(tutors.get(i).fullName)
                 .append(" (").append(tutors.get(i).sessionsCompleted)
                 .append(" sessions, avg quality ").append(String.format("%.0f", tutors.get(i).avgQuality))
                 .append(")\n");
        }

        String prompt = """
            Given a help request in the subject "%s" at difficulty "%s":
            "%s"
            
            These are available tutors:
            %s
            For each tutor, write ONE short sentence explaining why they're a good fit.
            Format: [tutor number]. [reason]
            """.formatted(category, difficulty, description, names);

        String resp = groq.ask(prompt);
        if (resp == null || resp.isBlank()) return;

        String[] lines = resp.split("\n");
        Map<Integer, String> reasons = new HashMap<>();
        for (String line : lines) {
            line = line.trim();
            if (line.matches("^[0-9]+\\..*")) {
                int dot = line.indexOf('.');
                try {
                    int idx = Integer.parseInt(line.substring(0, dot).trim()) - 1;
                    reasons.put(idx, line.substring(dot + 1).trim());
                } catch (NumberFormatException ignored) {}
            }
        }
        for (Map.Entry<Integer, String> e : reasons.entrySet()) {
            if (e.getKey() < tutors.size()) {
                tutors.get(e.getKey()).aiReason = e.getValue();
            }
        }
    }
}
