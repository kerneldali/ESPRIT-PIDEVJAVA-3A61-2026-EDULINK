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

    public void reportPost(int postId, int authorId, String reason) {
        // Logs a report back into your existing help_requests moderation screen!
        String qry = "INSERT INTO help_request (title, description, status, bounty, is_ticket, created_at, category, student_id) " +
                     "VALUES (?, ?, 'OPEN', 0, 1, NOW(), 'Moderator Report', ?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, "Forum Report on Topic #" + postId);
            pstm.setString(2, reason);
            pstm.setInt(3, authorId > 0 ? authorId : 1);
            pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}