package com.edulink.gui.services.challenge;

import com.edulink.gui.interfaces.IService;
import com.edulink.gui.models.challenge.Challenge;
import com.edulink.gui.util.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChallengeService implements IService<Challenge> {

    private Connection cnx;

    /** Default boost magnitude applied to under-performing challenges. */
    public static final int DEFAULT_BOOST_PCT  = 50;
    /** Default duration of an XP boost, in days. */
    public static final int DEFAULT_BOOST_DAYS = 7;

    public ChallengeService() {
        cnx = MyConnection.getInstance().getCnx();
        ensureAutoDecisionColumns();
    }

    /**
     * Idempotent migration: adds the auto-decision columns to the challenge table
     * if they don't already exist. Allows the feature to ship without a manual
     * SQL script for teammates.
     */
    private void ensureAutoDecisionColumns() {
        if (cnx == null) return;
        String[] alters = {
            "ALTER TABLE challenge ADD COLUMN IF NOT EXISTS featured TINYINT(1) NOT NULL DEFAULT 0",
            "ALTER TABLE challenge ADD COLUMN IF NOT EXISTS xp_boost_pct INT NOT NULL DEFAULT 0",
            "ALTER TABLE challenge ADD COLUMN IF NOT EXISTS boost_until DATETIME NULL",
            "ALTER TABLE challenge ADD COLUMN IF NOT EXISTS image_url VARCHAR(1024) NULL"
        };
        try (Statement st = cnx.createStatement()) {
            for (String sql : alters) {
                try { st.execute(sql); } catch (SQLException ignore) { /* older MySQL may lack IF NOT EXISTS */ }
            }
        } catch (SQLException e) {
            System.err.println("[ChallengeService] Could not ensure auto-decision columns: " + e.getMessage());
        }
    }

    @Override
    public void add(Challenge challenge) {
        String qry = "INSERT INTO challenge (title, description, difficulty, xp_reward, status, deadline, created_at, image_url) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, challenge.getTitle());
            pstm.setString(2, challenge.getDescription());
            pstm.setString(3, challenge.getDifficulty());
            pstm.setInt(4, challenge.getXpReward());
            pstm.setString(5, challenge.getStatus());
            pstm.setTimestamp(6, challenge.getDeadline() != null ? Timestamp.valueOf(challenge.getDeadline()) : null);
            pstm.setTimestamp(7, Timestamp.valueOf(challenge.getCreatedAt()));
            pstm.setString(8, challenge.getImageUrl());
            pstm.executeUpdate();
            System.out.println("✅ Challenge added: " + challenge.getTitle());
        } catch (SQLException e) {
            System.err.println("❌ Challenge add failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void add2(Challenge challenge) {
        add(challenge);
    }

    /**
     * Inserts a challenge and returns its generated id (or -1 on error).
     * Useful when the caller needs to attach related rows (e.g. tasks generated
     * by the AI assistant) right after creation.
     */
    public int addReturningId(Challenge challenge) {
        String qry = "INSERT INTO challenge (title, description, difficulty, xp_reward, status, deadline, created_at, image_url) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry, Statement.RETURN_GENERATED_KEYS)) {
            pstm.setString(1, challenge.getTitle());
            pstm.setString(2, challenge.getDescription());
            pstm.setString(3, challenge.getDifficulty());
            pstm.setInt(4, challenge.getXpReward());
            pstm.setString(5, challenge.getStatus());
            pstm.setTimestamp(6, challenge.getDeadline() != null ? Timestamp.valueOf(challenge.getDeadline()) : null);
            pstm.setTimestamp(7, Timestamp.valueOf(challenge.getCreatedAt()));
            pstm.setString(8, challenge.getImageUrl());
            pstm.executeUpdate();
            try (ResultSet keys = pstm.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    challenge.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Challenge addReturningId failed: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public void edit(Challenge challenge) {
        String qry = "UPDATE challenge SET title=?, description=?, difficulty=?, xp_reward=?, status=?, deadline=?, image_url=? " +
                     "WHERE id=?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, challenge.getTitle());
            pstm.setString(2, challenge.getDescription());
            pstm.setString(3, challenge.getDifficulty());
            pstm.setInt(4, challenge.getXpReward());
            pstm.setString(5, challenge.getStatus());
            pstm.setTimestamp(6, challenge.getDeadline() != null ? Timestamp.valueOf(challenge.getDeadline()) : null);
            pstm.setString(7, challenge.getImageUrl());
            pstm.setInt(8, challenge.getId());
            pstm.executeUpdate();
            System.out.println("✅ Challenge updated: " + challenge.getTitle());
        } catch (SQLException e) {
            System.err.println("❌ Challenge edit failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        String qry = "DELETE FROM challenge WHERE id=?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
            System.out.println("✅ Challenge deleted (id=" + id + ")");
        } catch (SQLException e) {
            System.err.println("❌ Challenge delete failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<Challenge> getAll() {
        List<Challenge> list = new ArrayList<>();
        String qry = "SELECT * FROM challenge ORDER BY created_at DESC";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Challenge getAll failed: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Returns OPEN challenges, with the featured one floated to the top so the
     * student-facing list naturally promotes it.
     */
    public List<Challenge> getOpen() {
        List<Challenge> list = new ArrayList<>();
        String qry = "SELECT * FROM challenge WHERE status = 'OPEN' " +
                     "ORDER BY featured DESC, deadline ASC";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Challenge getOpen failed: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public Challenge getById(int id) {
        String qry = "SELECT * FROM challenge WHERE id=?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) return mapResultSet(rs);
        } catch (SQLException e) {
            System.err.println("❌ Challenge getById failed: " + e.getMessage());
        }
        return null;
    }

    public List<Challenge> getByDifficulty(String difficulty) {
        List<Challenge> list = new ArrayList<>();
        String qry = "SELECT * FROM challenge WHERE difficulty=? ORDER BY created_at DESC";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, difficulty);
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Challenge getByDifficulty failed: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Fills the image_url column for every challenge that currently has none,
     * using the provided ChallengeImageService. Idempotent: running it twice
     * leaves already-populated rows untouched.
     *
     * @return the number of rows updated
     */
    public int backfillImageUrls(ChallengeImageService imageService) {
        if (cnx == null || imageService == null) return 0;
        int updated = 0;
        String select = "SELECT id, title, difficulty FROM challenge " +
                        "WHERE image_url IS NULL OR image_url = ''";
        String update = "UPDATE challenge SET image_url = ? WHERE id = ?";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(select);
             PreparedStatement upd = cnx.prepareStatement(update)) {
            while (rs.next()) {
                int id        = rs.getInt("id");
                String title  = rs.getString("title");
                String diff   = rs.getString("difficulty");
                String url    = imageService.buildImageUrl(title, diff);
                upd.setString(1, url);
                upd.setInt(2, id);
                upd.executeUpdate();
                updated++;
            }
        } catch (SQLException e) {
            System.err.println("[ChallengeService] backfillImageUrls failed: " + e.getMessage());
        }
        return updated;
    }

    /** Returns the currently featured challenge, or null if none. */
    public Challenge getFeatured() {
        String qry = "SELECT * FROM challenge WHERE featured = 1 LIMIT 1";
        try (Statement st = cnx.createStatement()) {
            ResultSet rs = st.executeQuery(qry);
            if (rs.next()) return mapResultSet(rs);
        } catch (SQLException e) {
            System.err.println("❌ getFeatured failed: " + e.getMessage());
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AUTO-DECISION ENGINE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Carries the outcome of a runAutoDecisions() call so the UI can show
     * exactly what was changed and why. Empty fields = no change.
     */
    public static class AutoDecisionResult {
        public String featuredTitle;          // newly featured challenge (null if none)
        public List<String> boostedTitles;    // challenges that received the +X% boost
        public int boostPct;                  // boost magnitude applied
        public LocalDateTime boostUntil;      // boost expiration
        public String summary;                // human-readable summary line
        public AutoDecisionResult() {
            this.boostedTitles = new ArrayList<>();
            this.boostPct = DEFAULT_BOOST_PCT;
        }
    }

    /**
     * Core "stat → decision" logic.
     *
     * Rules (kept intentionally simple and explainable to a jury):
     *   1. Score each OPEN challenge as: participations × (completion_rate + 1).
     *      The +1 prevents brand-new challenges with 0 completions from being
     *      penalised to 0.
     *   2. The challenge with the highest score is flagged as the unique FEATURED
     *      challenge. All others are un-featured.
     *   3. Challenges whose participation count is strictly below the average
     *      receive a +DEFAULT_BOOST_PCT boost for DEFAULT_BOOST_DAYS days.
     *      Boosts on other challenges are cleared.
     *
     * If there are no OPEN challenges, the call is a no-op.
     */
    public AutoDecisionResult runAutoDecisions() {
        AutoDecisionResult result = new AutoDecisionResult();

        if (cnx == null) {
            result.summary = "Pas de connexion DB — aucune décision appliquée.";
            return result;
        }

        // 1) Pull stats for every OPEN challenge.
        String statsSql =
            "SELECT c.id, c.title, " +
            "       COUNT(cp.id) AS total, " +
            "       COUNT(CASE WHEN cp.status='COMPLETED' THEN 1 END) AS completed " +
            "FROM challenge c " +
            "LEFT JOIN challenge_participation cp ON c.id = cp.challenge_id " +
            "WHERE c.status = 'OPEN' " +
            "GROUP BY c.id, c.title";

        List<int[]>    ids = new ArrayList<>(); // [id, total, completed]
        List<String>   titles = new ArrayList<>();
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(statsSql)) {
            while (rs.next()) {
                ids.add(new int[]{rs.getInt("id"), rs.getInt("total"), rs.getInt("completed")});
                titles.add(rs.getString("title"));
            }
        } catch (SQLException e) {
            System.err.println("[AutoDecision] stats query failed: " + e.getMessage());
            result.summary = "Erreur lors de la lecture des statistiques.";
            return result;
        }

        if (ids.isEmpty()) {
            result.summary = "Aucun challenge ouvert — aucune décision applicable.";
            return result;
        }

        // 2) Compute scores and pick the featured challenge.
        int    bestIdx   = -1;
        double bestScore = -1;
        int    totalSum  = 0;
        for (int i = 0; i < ids.size(); i++) {
            int total     = ids.get(i)[1];
            int completed = ids.get(i)[2];
            double rate   = total > 0 ? (completed / (double) total) : 0;
            double score  = total * (rate + 1);
            if (score > bestScore) { bestScore = score; bestIdx = i; }
            totalSum += total;
        }
        double avgParticipation = totalSum / (double) ids.size();

        // 3) Persist: clear the global featured flag, set the new one,
        //    and update boosts.
        try {
            cnx.setAutoCommit(false);

            // 3a) reset featured globally
            try (Statement st = cnx.createStatement()) {
                st.executeUpdate("UPDATE challenge SET featured = 0");
            }

            // 3b) set the new featured (only if it has at least 1 participant
            //     — otherwise "featuring" an empty challenge is meaningless)
            int featuredId = -1;
            if (bestIdx >= 0 && ids.get(bestIdx)[1] > 0) {
                featuredId = ids.get(bestIdx)[0];
                try (PreparedStatement ps = cnx.prepareStatement(
                        "UPDATE challenge SET featured = 1 WHERE id = ?")) {
                    ps.setInt(1, featuredId);
                    ps.executeUpdate();
                }
                result.featuredTitle = titles.get(bestIdx);
            }

            // 3c) clear boost on every challenge that's NOT under-performing,
            //     then set the boost on the under-performing ones
            LocalDateTime boostUntil = LocalDateTime.now().plusDays(DEFAULT_BOOST_DAYS);
            try (Statement st = cnx.createStatement()) {
                st.executeUpdate("UPDATE challenge SET xp_boost_pct = 0, boost_until = NULL");
            }
            try (PreparedStatement ps = cnx.prepareStatement(
                    "UPDATE challenge SET xp_boost_pct = ?, boost_until = ? WHERE id = ?")) {
                ps.setInt(1, DEFAULT_BOOST_PCT);
                ps.setTimestamp(2, Timestamp.valueOf(boostUntil));
                for (int i = 0; i < ids.size(); i++) {
                    int total = ids.get(i)[1];
                    // Apply boost only to *strictly* below-average challenges,
                    // and never to the freshly featured one.
                    if (total < avgParticipation && ids.get(i)[0] != featuredId) {
                        ps.setInt(3, ids.get(i)[0]);
                        ps.executeUpdate();
                        result.boostedTitles.add(titles.get(i));
                    }
                }
            }
            result.boostUntil = boostUntil;

            cnx.commit();
        } catch (SQLException e) {
            try { cnx.rollback(); } catch (SQLException ignore) {}
            System.err.println("[AutoDecision] persistence failed: " + e.getMessage());
            result.summary = "Erreur lors de l'application des décisions : " + e.getMessage();
            return result;
        } finally {
            try { cnx.setAutoCommit(true); } catch (SQLException ignore) {}
        }

        // 4) Build the human-readable summary line.
        StringBuilder sb = new StringBuilder();
        if (result.featuredTitle != null) {
            sb.append("⭐ Challenge mis en avant : « ").append(result.featuredTitle).append(" ».");
        } else {
            sb.append("Aucun challenge avec assez de participants pour être mis en avant.");
        }
        if (!result.boostedTitles.isEmpty()) {
            sb.append("  +").append(DEFAULT_BOOST_PCT).append("% XP pendant ")
              .append(DEFAULT_BOOST_DAYS).append("j sur ")
              .append(result.boostedTitles.size())
              .append(" challenge(s) sous-performant(s).");
        }
        result.summary = sb.toString();
        return result;
    }

    // --- Mapper privé ---
    private Challenge mapResultSet(ResultSet rs) throws SQLException {
        Challenge c = new Challenge();
        c.setId(rs.getInt("id"));
        c.setTitle(rs.getString("title"));
        c.setDescription(rs.getString("description"));
        c.setDifficulty(rs.getString("difficulty"));
        c.setXpReward(rs.getInt("xp_reward"));
        c.setStatus(rs.getString("status"));

        Timestamp deadline = rs.getTimestamp("deadline");
        c.setDeadline(deadline != null ? deadline.toLocalDateTime() : null);

        Timestamp createdAt = rs.getTimestamp("created_at");
        c.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now());

        // Auto-decision columns. Defensive reads: ignore absence so the rest of
        // the code keeps working if the migration hasn't run yet.
        try { c.setFeatured(rs.getInt("featured") == 1); } catch (SQLException ignore) {}
        try { c.setXpBoostPct(rs.getInt("xp_boost_pct")); } catch (SQLException ignore) {}
        try {
            Timestamp until = rs.getTimestamp("boost_until");
            c.setBoostUntil(until != null ? until.toLocalDateTime() : null);
        } catch (SQLException ignore) {}
        try { c.setImageUrl(rs.getString("image_url")); } catch (SQLException ignore) {}

        return c;
    }
}
