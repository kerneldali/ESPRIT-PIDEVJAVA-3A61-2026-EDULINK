package com.edulink.gui.services.challenge;

import com.edulink.gui.models.challenge.ChallengeParticipation;
import com.edulink.gui.services.UserService;
import com.edulink.gui.util.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChallengeParticipationService {

    private Connection cnx;
    private final UserService userService;

    public ChallengeParticipationService() {
        cnx         = MyConnection.getInstance().getCnx();
        userService = new UserService();
        ensureTable();
    }

    /**
     * Crée la table si elle n'existe pas encore.
     * Évite les crashs silencieux si l'utilisateur n'a pas lancé le SQL manuellement.
     */
    private void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS challenge_participation (" +
                     "  id                  INT AUTO_INCREMENT PRIMARY KEY," +
                     "  challenge_id        INT NOT NULL," +
                     "  user_id             INT NOT NULL," +
                     "  status              VARCHAR(20) DEFAULT 'JOINED'," +
                     "  joined_at           DATETIME DEFAULT CURRENT_TIMESTAMP," +
                     "  completed_at        DATETIME," +
                     "  submission_text     TEXT," +
                     "  submission_file_path VARCHAR(500)," +
                     "  UNIQUE KEY uq_participation (challenge_id, user_id)" +
                     ")";
        try (Statement st = cnx.createStatement()) {
            st.execute(sql);
            System.out.println("✅ Table challenge_participation OK");
        } catch (SQLException e) {
            System.err.println("❌ ensureTable failed: " + e.getMessage());
        }
    }

    // ── Rejoindre ─────────────────────────────────────────────────────────────

    /**
     * @return true si l'inscription a réussi, false sinon (déjà inscrit ou erreur SQL).
     */
    public boolean join(int userId, int challengeId) {
        if (hasJoined(userId, challengeId)) {
            System.out.println("⚠ User " + userId + " already joined challenge " + challengeId);
            return false;
        }
        String qry = "INSERT INTO challenge_participation (challenge_id, user_id, status, joined_at) VALUES (?,?,'JOINED',?)";
        try (PreparedStatement p = cnx.prepareStatement(qry)) {
            p.setInt(1, challengeId);
            p.setInt(2, userId);
            p.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            int rows = p.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ User " + userId + " joined challenge " + challengeId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ join failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ── Soumettre le travail ──────────────────────────────────────────────────

    /**
     * L'étudiant dépose son travail (texte + chemin de fichier optionnel).
     * Passe le statut de JOINED (ou REJECTED) → SUBMITTED.
     */
    public void submit(int userId, int challengeId, String text, String filePath) {
        String qry = "UPDATE challenge_participation " +
                     "SET status='SUBMITTED', submission_text=?, submission_file_path=? " +
                     "WHERE user_id=? AND challenge_id=? AND status IN ('JOINED','REJECTED')";
        try (PreparedStatement p = cnx.prepareStatement(qry)) {
            p.setString(1, text);
            p.setString(2, filePath);
            p.setInt(3, userId);
            p.setInt(4, challengeId);
            p.executeUpdate();
            System.out.println("✅ Submission sent – user=" + userId + " challenge=" + challengeId);
        } catch (SQLException e) {
            System.err.println("❌ submit: " + e.getMessage());
        }
    }

    // ── Validation / Rejet par l'admin ────────────────────────────────────────

    /**
     * L'admin valide la soumission → COMPLETED.
     *
     * <p>Récompense duale (volontaire) :
     * <ul>
     *   <li><b>XP de progression</b> ({@code user.xp}) — non-dépensable, sert au level / leaderboard.</li>
     *   <li><b>Solde du wallet</b> ({@code user.wallet_balance}) — monnaie liquide, échangeable
     *       avec d'autres étudiants via le module "Send to a Friend".</li>
     * </ul>
     * Compléter un challenge récompense les deux dimensions : tu progresses ET tu reçois
     * de la monnaie utilisable. Cela explique pourquoi la "EduLink Card" se met à jour
     * en même temps que le compteur d'XP du profil.
     *
     * <p>Note : chaque méthode update logue elle-même dans {@code transaction_log},
     * donc l'utilisateur verra deux entrées d'activité (une pour l'XP, une pour le wallet).
     */
    public void validate(int participationId, int userId, int xpReward) {
        String qry = "UPDATE challenge_participation SET status='COMPLETED', completed_at=? WHERE id=?";
        try (PreparedStatement p = cnx.prepareStatement(qry)) {
            p.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            p.setInt(2, participationId);
            int rows = p.executeUpdate();
            if (rows > 0) {
                // 1) Progression permanente (level)
                userService.updateXp(userId, xpReward);
                // 2) Monnaie liquide affichée sur l'EduLink Card (échangeable entre users)
                userService.updateWallet(userId, (double) xpReward);
                System.out.println("✅ Validated id=" + participationId
                        + " → +" + xpReward + " XP (progression) & +"
                        + xpReward + " coins (wallet)");
            }
        } catch (SQLException e) {
            System.err.println("❌ validate: " + e.getMessage());
        }
    }

    /**
     * L'admin rejette la soumission → REJECTED (l'étudiant peut resoumettre).
     */
    public void reject(int participationId) {
        String qry = "UPDATE challenge_participation SET status='REJECTED' WHERE id=?";
        try (PreparedStatement p = cnx.prepareStatement(qry)) {
            p.setInt(1, participationId);
            p.executeUpdate();
            System.out.println("✅ Rejected id=" + participationId);
        } catch (SQLException e) {
            System.err.println("❌ reject: " + e.getMessage());
        }
    }

    // ── Requêtes ──────────────────────────────────────────────────────────────

    /** Toutes les soumissions en attente de validation (statut SUBMITTED). */
    public List<ChallengeParticipation> getPendingSubmissions() {
        return getByStatus("SUBMITTED");
    }

    /** Toutes les participations d'un utilisateur. */
    public List<ChallengeParticipation> getByUser(int userId) {
        List<ChallengeParticipation> list = new ArrayList<>();
        String qry = "SELECT * FROM challenge_participation WHERE user_id=? ORDER BY joined_at DESC";
        try (PreparedStatement p = cnx.prepareStatement(qry)) {
            p.setInt(1, userId);
            ResultSet rs = p.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Participation d'un user pour un challenge donné, ou null. */
    public ChallengeParticipation getParticipation(int userId, int challengeId) {
        String qry = "SELECT * FROM challenge_participation WHERE user_id=? AND challenge_id=?";
        try (PreparedStatement p = cnx.prepareStatement(qry)) {
            p.setInt(1, userId);
            p.setInt(2, challengeId);
            ResultSet rs = p.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean hasJoined(int userId, int challengeId) {
        String qry = "SELECT id FROM challenge_participation WHERE user_id=? AND challenge_id=?";
        try (PreparedStatement p = cnx.prepareStatement(qry)) {
            p.setInt(1, userId); p.setInt(2, challengeId);
            return p.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    public boolean hasCompleted(int userId, int challengeId) {
        String qry = "SELECT id FROM challenge_participation WHERE user_id=? AND challenge_id=? AND status='COMPLETED'";
        try (PreparedStatement p = cnx.prepareStatement(qry)) {
            p.setInt(1, userId); p.setInt(2, challengeId);
            return p.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    // ── Privé ─────────────────────────────────────────────────────────────────

    private List<ChallengeParticipation> getByStatus(String status) {
        List<ChallengeParticipation> list = new ArrayList<>();
        String qry = "SELECT * FROM challenge_participation WHERE status=? ORDER BY joined_at ASC";
        try (PreparedStatement p = cnx.prepareStatement(qry)) {
            p.setString(1, status);
            ResultSet rs = p.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private ChallengeParticipation map(ResultSet rs) throws SQLException {
        ChallengeParticipation cp = new ChallengeParticipation();
        cp.setId(rs.getInt("id"));
        cp.setChallengeId(rs.getInt("challenge_id"));
        cp.setUserId(rs.getInt("user_id"));
        cp.setStatus(ChallengeParticipation.Status.valueOf(rs.getString("status")));

        Timestamp joined = rs.getTimestamp("joined_at");
        cp.setJoinedAt(joined != null ? joined.toLocalDateTime() : LocalDateTime.now());

        Timestamp completed = rs.getTimestamp("completed_at");
        cp.setCompletedAt(completed != null ? completed.toLocalDateTime() : null);

        cp.setSubmissionText(rs.getString("submission_text"));
        cp.setSubmissionFilePath(rs.getString("submission_file_path"));
        return cp;
    }
}
