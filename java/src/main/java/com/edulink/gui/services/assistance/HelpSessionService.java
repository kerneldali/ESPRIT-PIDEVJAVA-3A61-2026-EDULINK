package com.edulink.gui.services.assistance;

import com.edulink.gui.models.assistance.ChatMessage;
import com.edulink.gui.models.assistance.HelpSession;
import com.edulink.gui.services.mail.MailService;
import com.edulink.gui.util.MyConnection;
import com.edulink.gui.util.SessionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages tutoring sessions end-to-end:
 * - escrow bounty on session start
 * - anti-farming validation on close
 * - on-chain credit transfer via EduTokenService
 * - AI summary generation + email
 */
public class HelpSessionService {

    private final ToxicityService toxicityService       = new ToxicityService();
    private final SessionSummaryService summaryService  = new SessionSummaryService();
    private Connection cnx;

    // Anti-farming thresholds
    private static final int    MIN_MESSAGES     = 5;   // at least 5 messages to pay bounty
    private static final long   MIN_DURATION_MIN = 3;   // at least 3 minutes
    private static final int    QUALITY_THRESHOLD = 60;  // AI quality score >= 60
    private static final int    MAX_SESSIONS_PER_PAIR_PER_DAY = 2; // rate-limit same pair

    public HelpSessionService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    // ─────────────────────────────────────────────────────
    // CREATE SESSION (escrow the bounty)
    // ─────────────────────────────────────────────────────
    /**
     * Opens a tutoring session and escrows the bounty from the student's wallet.
     * @return the created HelpSession, or null on failure
     */
    public HelpSession createSession(int helpRequestId, int tutorId, int studentId, int bounty) {
        if (cnx == null) return null;

        // Rate-limit: same pair can't have more than MAX sessions today
        if (isDailyLimitReached(tutorId, studentId)) {
            System.out.println("[HelpSession] Daily session limit reached for this pair.");
            return null;
        }

        // Check student has enough balance (DB-side wallet)
        if (!hasEnoughBalance(studentId, bounty)) {
            System.out.println("[HelpSession] Student does not have enough credits.");
            return null;
        }

        HelpSession session = new HelpSession();
        session.setHelpRequestId(helpRequestId);
        session.setTutorId(tutorId);
        session.setStudentId(studentId);
        session.setBountyEscrowed(bounty);
        session.setJitsiRoomId("edulink-" + UUID.randomUUID().toString().substring(0, 8));

        String sql = """
            INSERT INTO help_session
              (help_request_id, tutor_id, student_id, jitsi_room_id, bounty_escrowed, is_active)
            VALUES (?, ?, ?, ?, ?, 1)
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, helpRequestId);
            ps.setInt(2, tutorId);
            ps.setInt(3, studentId);
            ps.setString(4, session.getJitsiRoomId());
            ps.setInt(5, bounty);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) session.setId(rs.getInt(1));
        } catch (SQLException e) {
            System.err.println("[HelpSession] createSession error: " + e.getMessage());
            return null;
        }

        // Deduct bounty from student's DB wallet (escrow)
        adjustWallet(studentId, -bounty, "ESCROW", session.getId());

        // Mark help_request as IN_PROGRESS
        updateRequestStatus(helpRequestId, "IN_PROGRESS");
        System.out.println("[HelpSession] Session created #" + session.getId()
            + " | Jitsi: " + session.getJitsiRoomId());
        return session;
    }

    // ─────────────────────────────────────────────────────
    // CLOSE SESSION (anti-farming gate + bounty transfer)
    // ─────────────────────────────────────────────────────
    /**
     * Closes a session. Runs the anti-farming checks before paying the tutor.
     * @return the generated AI summary
     */
    public String closeSession(int sessionId) {
        if (cnx == null) return "Error: No DB connection.";

        HelpSession session = getSessionById(sessionId);
        if (session == null) return "Session not found.";

        List<ChatMessage> messages = getMessages(sessionId);
        long duration = session.getDurationMinutes();

        // ── Anti-Farming Gate ──────────────────────────────
        boolean passedEngagement = messages.size() >= MIN_MESSAGES && duration >= MIN_DURATION_MIN;

        SessionSummaryService.SummaryResult result = summaryService.summarize(messages);
        boolean passedQuality = result.qualityScore >= QUALITY_THRESHOLD;

        System.out.printf("[HelpSession] Close check — msgs:%d dur:%dmin quality:%d%n",
            messages.size(), duration, result.qualityScore);

        if (passedEngagement && passedQuality) {
            // Pay the tutor from escrow
            adjustWallet(session.getTutorId(), session.getBountyEscrowed(), "BOUNTY_EARNED", sessionId);
            markBountyPaid(sessionId, result.qualityScore);
            System.out.println("[HelpSession] Bounty paid to tutor #" + session.getTutorId());
        } else {
            // Refund the student
            adjustWallet(session.getStudentId(), session.getBountyEscrowed(), "REFUND", sessionId);
            System.out.println("[HelpSession] Refund issued — engagement or quality below threshold.");
        }

        // Update session in DB (mark closed, save summary)
        finalizeSession(sessionId, result.summary, result.qualityScore);

        // Update help_request status
        updateRequestStatus(session.getHelpRequestId(), "CLOSED");

        // Send summary email to both parties
        sendSummaryEmails(session, result.summary);

        return result.summary;
    }

    // ─────────────────────────────────────────────────────
    // MESSAGES
    // ─────────────────────────────────────────────────────
    /**
     * Sends a message through toxicity check before saving.
     * Returns the saved ChatMessage, or null if rejected.
     */
    public ChatMessage sendMessage(int sessionId, int senderId, String content) {
        if (content == null || content.isBlank()) return null;

        ToxicityService.ToxicityResult tr = toxicityService.analyze(content);

        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setSenderId(senderId);
        msg.setContent(content.trim());
        msg.setToxic(tr.isToxic);
        msg.setSentiment("NEUTRAL");
        msg.setDetectedLanguage("en");

        if (cnx == null) return null;
        String sql = """
            INSERT INTO chat_message (session_id, sender_id, content, is_toxic, sentiment, detected_language)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, sessionId);
            ps.setInt(2, senderId);
            ps.setString(3, msg.getContent());
            ps.setBoolean(4, msg.isToxic());
            ps.setString(5, msg.getSentiment());
            ps.setString(6, msg.getDetectedLanguage());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) msg.setId(rs.getInt(1));

            // Increment message counter in session
            cnx.createStatement().execute(
                "UPDATE help_session SET message_count = message_count + 1 WHERE id = " + sessionId);
        } catch (SQLException e) {
            System.err.println("[HelpSession] sendMessage error: " + e.getMessage());
            return null;
        }

        if (tr.isToxic) {
            System.out.println("[HelpSession] ⚠ Toxic message detected: " + tr.reason);
        }
        return msg;
    }

    public List<ChatMessage> getMessages(int sessionId) {
        List<ChatMessage> out = new ArrayList<>();
        if (cnx == null) return out;
        String sql = """
            SELECT cm.*, u.full_name as sender_name
            FROM chat_message cm
            LEFT JOIN user u ON u.id = cm.sender_id
            WHERE cm.session_id = ?
            ORDER BY cm.timestamp ASC
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ResultSet rs = ps.executeQuery();
            int currentUserId = (SessionManager.getCurrentUser() != null)
                ? SessionManager.getCurrentUser().getId() : -1;
            while (rs.next()) {
                ChatMessage m = new ChatMessage();
                m.setId(rs.getInt("id"));
                m.setSessionId(sessionId);
                m.setSenderId(rs.getInt("sender_id"));
                m.setContent(rs.getString("content"));
                m.setToxic(rs.getBoolean("is_toxic"));
                m.setSentiment(rs.getString("sentiment"));
                m.setDetectedLanguage(rs.getString("detected_language"));
                m.setTimestamp(rs.getTimestamp("timestamp"));
                m.setSenderName(rs.getString("sender_name"));
                m.setMine(m.getSenderId() == currentUserId);
                out.add(m);
            }
        } catch (SQLException e) {
            System.err.println("[HelpSession] getMessages error: " + e.getMessage());
        }
        return out;
    }

    public HelpSession getActiveSessionForRequest(int helpRequestId) {
        if (cnx == null) return null;
        String sql = """
            SELECT hs.*, u1.full_name as tutor_name, u2.full_name as student_name
            FROM help_session hs
            LEFT JOIN user u1 ON u1.id = hs.tutor_id
            LEFT JOIN user u2 ON u2.id = hs.student_id
            WHERE hs.help_request_id = ? AND hs.is_active = 1
            LIMIT 1
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, helpRequestId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapSession(rs);
        } catch (SQLException e) {
            System.err.println("[HelpSession] getActiveSession error: " + e.getMessage());
        }
        return null;
    }

    public HelpSession getSessionById(int sessionId) {
        if (cnx == null) return null;
        String sql = """
            SELECT hs.*, u1.full_name as tutor_name, u2.full_name as student_name
            FROM help_session hs
            LEFT JOIN user u1 ON u1.id = hs.tutor_id
            LEFT JOIN user u2 ON u2.id = hs.student_id
            WHERE hs.id = ?
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapSession(rs);
        } catch (SQLException e) {
            System.err.println("[HelpSession] getSessionById error: " + e.getMessage());
        }
        return null;
    }

    // ─────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────
    private boolean isDailyLimitReached(int tutorId, int studentId) {
        if (cnx == null) return false;
        String sql = """
            SELECT COUNT(*) FROM help_session
            WHERE tutor_id = ? AND student_id = ?
              AND DATE(started_at) = CURDATE()
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, tutorId);
            ps.setInt(2, studentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) >= MAX_SESSIONS_PER_PAIR_PER_DAY;
        } catch (SQLException e) {
            System.err.println("[HelpSession] dailyLimit check error: " + e.getMessage());
        }
        return false;
    }

    private boolean hasEnoughBalance(int userId, int amount) {
        if (amount <= 0) return true;
        if (cnx == null) return false;
        String sql = "SELECT wallet_balance FROM user WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("wallet_balance") >= amount;
        } catch (SQLException e) {
            System.err.println("[HelpSession] balance check error: " + e.getMessage());
        }
        return false;
    }

    /** Adjusts wallet_balance and logs the transaction */
    private void adjustWallet(int userId, int delta, String type, int sessionId) {
        if (cnx == null) return;
        try {
            // Update balance
            PreparedStatement ps = cnx.prepareStatement(
                "UPDATE user SET wallet_balance = GREATEST(0, wallet_balance + ?) WHERE id = ?");
            ps.setInt(1, delta);
            ps.setInt(2, userId);
            ps.executeUpdate();

            // Log transaction
            PreparedStatement log = cnx.prepareStatement(
                "INSERT INTO token_transaction (from_user_id, to_user_id, amount, tx_type, note) VALUES (?,?,?,?,?)");
            log.setObject(1, delta < 0 ? userId  : null);
            log.setObject(2, delta > 0 ? userId  : null);
            log.setInt(3, Math.abs(delta));
            log.setString(4, type);
            log.setString(5, "session #" + sessionId);
            log.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[HelpSession] adjustWallet error: " + e.getMessage());
        }
    }

    private void markBountyPaid(int sessionId, int qualityScore) {
        if (cnx == null) return;
        try {
            PreparedStatement ps = cnx.prepareStatement(
                "UPDATE help_session SET bounty_paid=1, quality_score=? WHERE id=?");
            ps.setInt(1, qualityScore);
            ps.setInt(2, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[HelpSession] markBountyPaid error: " + e.getMessage());
        }
    }

    private void finalizeSession(int sessionId, String summary, int qualityScore) {
        if (cnx == null) return;
        try {
            PreparedStatement ps = cnx.prepareStatement(
                "UPDATE help_session SET is_active=0, summary=?, quality_score=?, ended_at=NOW() WHERE id=?");
            ps.setString(1, summary);
            ps.setInt(2, qualityScore);
            ps.setInt(3, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[HelpSession] finalizeSession error: " + e.getMessage());
        }
    }

    private void updateRequestStatus(int requestId, String status) {
        if (cnx == null) return;
        try {
            PreparedStatement ps = cnx.prepareStatement(
                "UPDATE help_request SET status=? WHERE id=?");
            ps.setString(1, status);
            ps.setInt(2, requestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[HelpSession] updateRequestStatus error: " + e.getMessage());
        }
    }

    private void sendSummaryEmails(HelpSession session, String summary) {
        try {
            MailService.sendSessionSummaryEmail(session, summary);
        } catch (Exception e) {
            System.err.println("[HelpSession] Email send failed: " + e.getMessage());
        }
    }

    private HelpSession mapSession(ResultSet rs) throws SQLException {
        HelpSession s = new HelpSession();
        s.setId(rs.getInt("id"));
        s.setHelpRequestId(rs.getInt("help_request_id"));
        s.setTutorId(rs.getInt("tutor_id"));
        s.setStudentId(rs.getInt("student_id"));
        s.setJitsiRoomId(rs.getString("jitsi_room_id"));
        s.setSummary(rs.getString("summary"));
        s.setActive(rs.getBoolean("is_active"));
        s.setBountyEscrowed(rs.getInt("bounty_escrowed"));
        s.setBountyPaid(rs.getBoolean("bounty_paid"));
        s.setQualityScore(rs.getInt("quality_score"));
        s.setMessageCount(rs.getInt("message_count"));
        s.setStartedAt(rs.getTimestamp("started_at"));
        s.setEndedAt(rs.getTimestamp("ended_at"));
        try { s.setTutorName(rs.getString("tutor_name")); } catch (Exception ignored) {}
        try { s.setStudentName(rs.getString("student_name")); } catch (Exception ignored) {}
        return s;
    }
}
