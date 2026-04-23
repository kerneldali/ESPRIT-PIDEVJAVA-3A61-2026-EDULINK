package com.edulink.gui.services.challenge;

import com.edulink.gui.models.challenge.ChallengeTask;
import com.edulink.gui.util.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChallengeTaskService {

    private final Connection cnx;

    public ChallengeTaskService() {
        cnx = MyConnection.getInstance().getCnx();
        ensureTables();
    }

    private void ensureTables() {
        if (cnx == null) return;
        // Pas de FK pour éviter les erreurs si le moteur ne les supporte pas
        String tasks = "CREATE TABLE IF NOT EXISTS challenge_task (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  challenge_id INT NOT NULL," +
                "  title VARCHAR(255) NOT NULL," +
                "  description TEXT," +
                "  order_index INT DEFAULT 0," +
                "  is_required TINYINT(1) DEFAULT 1" +
                ")";
        String completions = "CREATE TABLE IF NOT EXISTS task_completion (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  participation_id INT NOT NULL," +
                "  task_id INT NOT NULL," +
                "  completed_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "  UNIQUE KEY uq_task_completion (participation_id, task_id)" +
                ")";
        try (Statement st = cnx.createStatement()) {
            st.execute(tasks);
            System.out.println("✅ Table challenge_task OK");
            st.execute(completions);
            System.out.println("✅ Table task_completion OK");
        } catch (SQLException e) {
            System.out.println("❌ ensureTables: " + e.getMessage());
        }
    }

    // ── CRUD Tasks ────────────────────────────────────────────────────────────

    public boolean add(ChallengeTask task) {
        if (cnx == null) {
            System.err.println("❌ add task: no DB connection");
            return false;
        }
        String sql = "INSERT INTO challenge_task (challenge_id, title, description, order_index, is_required) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, task.getChallengeId());
            ps.setString(2, task.getTitle());
            ps.setString(3, task.getDescription());
            ps.setInt(4, task.getOrderIndex());
            ps.setBoolean(5, task.isRequired());
            int rows = ps.executeUpdate();
            System.out.println("✅ task added, rows=" + rows + ", challengeId=" + task.getChallengeId());
            return rows > 0;
        } catch (Exception e) {
            System.err.println("❌ add task: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void edit(ChallengeTask task) {
        String sql = "UPDATE challenge_task SET title=?, description=?, order_index=?, is_required=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setInt(3, task.getOrderIndex());
            ps.setBoolean(4, task.isRequired());
            ps.setInt(5, task.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.out.println("❌ edit task: " + e.getMessage()); }
    }

    public void delete(int taskId) {
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM challenge_task WHERE id=?")) {
            ps.setInt(1, taskId);
            ps.executeUpdate();
        } catch (SQLException e) { System.out.println("❌ delete task: " + e.getMessage()); }
    }

    public List<ChallengeTask> getByChallenge(int challengeId) {
        List<ChallengeTask> list = new ArrayList<>();
        String sql = "SELECT * FROM challenge_task WHERE challenge_id=? ORDER BY order_index ASC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, challengeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ChallengeTask t = new ChallengeTask();
                t.setId(rs.getInt("id"));
                t.setChallengeId(rs.getInt("challenge_id"));
                t.setTitle(rs.getString("title"));
                t.setDescription(rs.getString("description"));
                t.setOrderIndex(rs.getInt("order_index"));
                t.setRequired(rs.getBoolean("is_required"));
                list.add(t);
            }
        } catch (SQLException e) { System.out.println("❌ getByChallenge: " + e.getMessage()); }
        return list;
    }

    // ── Task completion ───────────────────────────────────────────────────────

    public void markCompleted(int participationId, int taskId) {
        String sql = "INSERT IGNORE INTO task_completion (participation_id, task_id) VALUES (?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, participationId);
            ps.setInt(2, taskId);
            ps.executeUpdate();
        } catch (SQLException e) { System.out.println("❌ markCompleted: " + e.getMessage()); }
    }

    public void markUncompleted(int participationId, int taskId) {
        String sql = "DELETE FROM task_completion WHERE participation_id=? AND task_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, participationId);
            ps.setInt(2, taskId);
            ps.executeUpdate();
        } catch (SQLException e) { System.out.println("❌ markUncompleted: " + e.getMessage()); }
    }

    public List<Integer> getCompletedTaskIds(int participationId) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT task_id FROM task_completion WHERE participation_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, participationId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("task_id"));
        } catch (SQLException e) { System.out.println("❌ getCompletedTaskIds: " + e.getMessage()); }
        return ids;
    }

    /** Vérifie que toutes les tâches obligatoires sont cochées */
    public boolean allRequiredCompleted(int participationId, int challengeId) {
        String sql = "SELECT COUNT(*) FROM challenge_task ct " +
                     "LEFT JOIN task_completion tc ON ct.id = tc.task_id AND tc.participation_id = ? " +
                     "WHERE ct.challenge_id = ? AND ct.is_required = 1 AND tc.id IS NULL";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, participationId);
            ps.setInt(2, challengeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) == 0;
        } catch (SQLException e) { System.out.println("❌ allRequiredCompleted: " + e.getMessage()); }
        return false;
    }
}
