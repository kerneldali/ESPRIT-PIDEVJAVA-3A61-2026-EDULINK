package com.edulink.gui.services.assistance;

import com.edulink.gui.models.assistance.ForumThread;
import com.edulink.gui.models.assistance.ForumReply;
import com.edulink.gui.util.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ForumService {
    private Connection cnx;

    public ForumService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    public List<ForumThread> getAllThreads() {
        List<ForumThread> list = new ArrayList<>();
        String qry = "SELECT t.*, u.full_name as author_name, " +
                     "(SELECT COUNT(*) FROM forum_reply r WHERE r.thread_id = t.id) as reply_count " +
                     "FROM forum_thread t " +
                     "LEFT JOIN user u ON t.author_id = u.id " +
                     "ORDER BY t.is_pinned DESC, t.created_at DESC";

        try (Statement stm = cnx.createStatement();
             ResultSet rs = stm.executeQuery(qry)) {
            while (rs.next()) {
                ForumThread t = new ForumThread();
                t.setId(rs.getInt("id"));
                t.setBoardId(rs.getInt("board_id"));
                t.setAuthorId(rs.getInt("author_id"));
                t.setAuthorName(rs.getString("author_name"));
                t.setTitle(rs.getString("title"));
                t.setContent(rs.getString("content"));
                t.setViews(rs.getInt("views"));
                t.setPinned(rs.getBoolean("is_pinned"));
                t.setLocked(rs.getBoolean("is_locked"));
                t.setReplyCount(rs.getInt("reply_count"));
                if (rs.getTimestamp("created_at") != null) {
                    t.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                }
                list.add(t);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<ForumReply> getRepliesForThread(int threadId) {
        List<ForumReply> list = new ArrayList<>();
        String qry = "SELECT r.*, u.full_name as author_name FROM forum_reply r " +
                     "LEFT JOIN user u ON r.author_id = u.id " +
                     "WHERE r.thread_id = ? ORDER BY r.created_at ASC";

        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, threadId);
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    ForumReply r = new ForumReply();
                    r.setId(rs.getInt("id"));
                    r.setThreadId(rs.getInt("thread_id"));
                    r.setAuthorId(rs.getInt("author_id"));
                    r.setAuthorName(rs.getString("author_name"));
                    r.setContent(rs.getString("content"));
                    if (rs.getTimestamp("created_at") != null) {
                        r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    }
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void addThread(ForumThread t) {
        String qry = "INSERT INTO forum_thread (board_id, author_id, title, slug, content, created_at, updated_at, views, is_pinned, is_locked) " +
                     "VALUES (?, ?, ?, ?, ?, NOW(), NOW(), 0, 0, 0)";

        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, t.getBoardId() > 0 ? t.getBoardId() : 1);
            pstm.setInt(2, t.getAuthorId() > 0 ? t.getAuthorId() : 1);
            pstm.setString(3, t.getTitle());
            pstm.setString(4, t.getTitle().replaceAll("\\s+", "-").toLowerCase() + "-" + System.currentTimeMillis());
            pstm.setString(5, t.getContent());
            pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addReply(ForumReply r) {
        String qry = "INSERT INTO forum_reply (thread_id, author_id, content, created_at, updated_at) " +
                     "VALUES (?, ?, ?, NOW(), NOW())";

        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, r.getThreadId());
            pstm.setInt(2, r.getAuthorId() > 0 ? r.getAuthorId() : 1);
            pstm.setString(3, r.getContent());
            pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- Admin Moderation Logic ---

    public void deleteThread(int threadId) {
        String qry = "DELETE FROM forum_thread WHERE id = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, threadId);
            pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteReply(int replyId) {
        String qry = "DELETE FROM forum_reply WHERE id = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, replyId);
            pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setThreadStatus(int threadId, boolean pinned, boolean locked) {
        String qry = "UPDATE forum_thread SET is_pinned = ?, is_locked = ? WHERE id = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setBoolean(1, pinned);
            pstm.setBoolean(2, locked);
            pstm.setInt(3, threadId);
            pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void reportPost(int postId, int reporterId, String reason) {
        String sql = "INSERT INTO post_report (post_id, reporter_id, reason, status) VALUES (?, ?, ?, 'PENDING')";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.setInt(2, reporterId > 0 ? reporterId : 1);
            ps.setString(3, reason);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ─── Reactions ────────────────────────────────────────────
    /** Add or update a reaction. Allowed types: LIKE, LOVE, INSIGHTFUL, FUNNY, SUPPORT */
    public void addReaction(int postId, int userId, String reactionType) {
        String sql = "INSERT INTO post_reaction (post_id, user_id, reaction_type) VALUES (?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE reaction_type = VALUES(reaction_type)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.setInt(2, userId);
            ps.setString(3, reactionType);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeReaction(int postId, int userId) {
        String sql = "DELETE FROM post_reaction WHERE post_id=? AND user_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Returns a map of reactionType -> count for a given post */
    public java.util.Map<String, Integer> getReactions(int postId) {
        java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
        String sql = "SELECT reaction_type, COUNT(*) as cnt FROM post_reaction WHERE post_id=? GROUP BY reaction_type";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) map.put(rs.getString("reaction_type"), rs.getInt("cnt"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    /** Returns current user's reaction for a post, or null */
    public String getUserReaction(int postId, int userId) {
        String sql = "SELECT reaction_type FROM post_reaction WHERE post_id=? AND user_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("reaction_type");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ─── Admin: Reports ───────────────────────────────────────
    public List<java.util.Map<String, Object>> getPendingReports() {
        List<java.util.Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT pr.id, pr.post_id, pr.reason, pr.status, pr.created_at,
                   ft.title as post_title, u.full_name as reporter
            FROM post_report pr
            LEFT JOIN forum_thread ft ON ft.id = pr.post_id
            LEFT JOIN user u ON u.id = pr.reporter_id
            WHERE pr.status = 'PENDING'
            ORDER BY pr.created_at DESC
            """;
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("id",         rs.getInt("id"));
                row.put("postId",     rs.getInt("post_id"));
                row.put("postTitle",  rs.getString("post_title"));
                row.put("reason",     rs.getString("reason"));
                row.put("reporter",   rs.getString("reporter"));
                row.put("createdAt",  rs.getTimestamp("created_at"));
                list.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void dismissReport(int reportId) {
        updateReportStatus(reportId, "DISMISSED");
    }

    public void actionReport(int reportId, int postId) {
        updateReportStatus(reportId, "ACTIONED");
        // Cascade delete the post and its reactions
        try (PreparedStatement ps = cnx.prepareStatement(
                "DELETE FROM forum_thread WHERE id=?")) {
            ps.setInt(1, postId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateReportStatus(int reportId, String status) {
        String sql = "UPDATE post_report SET status=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, reportId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}