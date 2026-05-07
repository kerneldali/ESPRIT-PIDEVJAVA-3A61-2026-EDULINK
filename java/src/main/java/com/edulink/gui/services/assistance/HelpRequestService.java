package com.edulink.gui.services.assistance;

import com.edulink.gui.interfaces.IService;
import com.edulink.gui.models.assistance.HelpRequest;
import com.edulink.gui.util.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service layer for HelpRequest CRUD operations.
 */
public class HelpRequestService implements IService<HelpRequest> {

    private Connection cnx;

    public HelpRequestService() {
        cnx = MyConnection.getInstance().getCnx();
        ensureStudentIdColumnExists();
    }

    private void ensureStudentIdColumnExists() {
        if (cnx == null) return;
        try (Statement st = cnx.createStatement()) {
            try {
                st.executeQuery("SELECT student_id FROM help_request LIMIT 1").close();
            } catch (SQLException e) {
                // Column missing, add it
                System.out.println("[HelpRequestService] Adding missing student_id column...");
                st.execute("ALTER TABLE help_request ADD COLUMN student_id INT DEFAULT 1");
                // Also update existing ones to a default user if possible
            }
        } catch (SQLException e) {
            System.err.println("[HelpRequestService] Schema fix failed: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return MyConnection.getInstance().isConnected();
    }

    @Override
    public void add(HelpRequest req) {
        if (cnx == null) return;
        String sql = "INSERT INTO help_request (title, description, status, bounty, is_ticket, created_at, category, difficulty, close_reason, student_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            if (req.getStudentId() == null || req.getStudentId() <= 0) {
                com.edulink.gui.models.User current = com.edulink.gui.util.SessionManager.getCurrentUser();
                if (current != null) req.setStudentId(current.getId());
            }
            
            PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, req.getTitle());
            ps.setString(2, req.getDescription());
            ps.setString(3, req.getStatus());
            ps.setInt(4, req.getBounty());
            ps.setBoolean(5, req.isTicket());
            ps.setTimestamp(6, req.getCreatedAt() != null ? req.getCreatedAt() : new Timestamp(System.currentTimeMillis()));
            ps.setString(7, req.getCategory());
            ps.setString(8, req.getDifficulty());
            ps.setString(9, req.getCloseReason());
            ps.setInt(10, req.getStudentId() != null ? req.getStudentId() : 1);
            
            ps.executeUpdate();
            
            // Sync the object with the ID from DB
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                req.setId(rs.getInt(1));
            }
            System.out.println("HelpRequest ajouté avec succès ! ID: " + req.getId());
        } catch (SQLException e) {
            System.err.println("Error adding HelpRequest: " + e.getMessage());
        }
    }

    @Override
    public void add2(HelpRequest req) {
        if (cnx == null) return;
        String sql = "INSERT INTO help_request (title, description, status, bounty, is_ticket, created_at, category, difficulty, close_reason, student_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            if (req.getStudentId() == null || req.getStudentId() <= 0) {
                com.edulink.gui.models.User current = com.edulink.gui.util.SessionManager.getCurrentUser();
                if (current != null) req.setStudentId(current.getId());
            }
            
            PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, req.getTitle());
            ps.setString(2, req.getDescription());
            ps.setString(3, req.getStatus());
            ps.setInt(4, req.getBounty());
            ps.setBoolean(5, req.isTicket());
            ps.setTimestamp(6, req.getCreatedAt() != null ? req.getCreatedAt() : new Timestamp(System.currentTimeMillis()));
            ps.setString(7, req.getCategory());
            ps.setString(8, req.getDifficulty());
            ps.setString(9, req.getCloseReason());
            ps.setInt(10, req.getStudentId() != null ? req.getStudentId() : 1);
            
            ps.executeUpdate();
            
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                req.setId(rs.getInt(1));
            }
            System.out.println("✅ HelpRequest added (add2)! ID: " + req.getId());
        } catch (SQLException e) {
            System.err.println("Error adding HelpRequest2: " + e.getMessage());
        }
    }

    @Override
    public void edit(HelpRequest req) {
        if (cnx == null) return;
        String sql = "UPDATE help_request SET title=?, description=?, status=?, bounty=?, is_ticket=?, category=?, difficulty=?, close_reason=? WHERE id=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, req.getTitle());
            ps.setString(2, req.getDescription());
            ps.setString(3, req.getStatus());
            ps.setInt(4, req.getBounty());
            ps.setBoolean(5, req.isTicket());
            ps.setString(6, req.getCategory());
            ps.setString(7, req.getDifficulty());
            ps.setString(8, req.getCloseReason());
            ps.setInt(9, req.getId());
            ps.executeUpdate();
            System.out.println("HelpRequest modifié avec succès ! ID: " + req.getId());
        } catch (SQLException e) {
            System.err.println("Error editing HelpRequest: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        if (cnx == null) return;
        String sql = "DELETE FROM help_request WHERE id=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("HelpRequest supprimé ! ID: " + id);
        } catch (SQLException e) {
            System.err.println("Error deleting HelpRequest: " + e.getMessage());
        }
    }

    @Override
    public List<HelpRequest> getAll() {
        List<HelpRequest> list = new ArrayList<>();
        if (cnx == null) return list;
        String sql = "SELECT * FROM help_request ORDER BY id DESC";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                list.add(mapResultSetToHelpRequest(rs));
            }
        } catch (SQLException e) { System.err.println("Error loading HelpRequests: " + e.getMessage()); }
        return list;
    }

    public List<HelpRequest> getReportedTickets() {
        List<HelpRequest> list = new ArrayList<>();
        if (cnx == null) return list;
        String sql = "SELECT * FROM help_request WHERE is_ticket = 1 ORDER BY created_at DESC";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                list.add(mapResultSetToHelpRequest(rs));
            }
        } catch (SQLException e) { System.err.println("Error loading Reported Tickets: " + e.getMessage()); }
        return list;
    }

    public void resolveTicket(int id) {
        if (cnx == null) return;
        String sql = "UPDATE help_request SET is_ticket = 0, status = 'CLOSED', close_reason = 'ADMIN_RESOLVED' WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Ticket #" + id + " resolved by admin.");
        } catch (SQLException e) { System.err.println("Error resolving ticket: " + e.getMessage()); }
    }

    public java.util.Map<String, Object> getStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        if (cnx == null) return stats;
        String sql = "SELECT " +
                     "COUNT(*) AS total, " +
                     "SUM(CASE WHEN status = 'OPEN' THEN 1 ELSE 0 END) AS open, " +
                     "SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS inProgress, " +
                     "SUM(CASE WHEN status = 'CLOSED' THEN 1 ELSE 0 END) AS closed, " +
                     "COALESCE(SUM(bounty), 0) AS totalBounty, " +
                     "SUM(CASE WHEN is_ticket = 1 THEN 1 ELSE 0 END) AS tickets " +
                     "FROM help_request";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) {
                int total = rs.getInt("total");
                int closed = rs.getInt("closed");
                stats.put("totalRequests", total);
                stats.put("openRequests", rs.getInt("open"));
                stats.put("inProgressRequests", rs.getInt("inProgress"));
                stats.put("closedRequests", closed);
                stats.put("totalBounty", rs.getInt("totalBounty"));
                stats.put("reportedTickets", rs.getInt("tickets"));
                
                double rate = (total > 0) ? (double) closed / total * 100 : 0;
                stats.put("resolutionRate", Math.round(rate));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return stats;
    }

    public HelpRequest getById(int id) {
        if (cnx == null) return null;
        String sql = "SELECT * FROM help_request WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSetToHelpRequest(rs);
        } catch (SQLException e) {
            System.err.println("Error fetching HelpRequest by ID: " + e.getMessage());
        }
        return null;
    }

    public List<HelpRequest> search(String keyword) {
        List<HelpRequest> list = new ArrayList<>();
        if (cnx == null) return list;
        String sql = "SELECT * FROM help_request WHERE title LIKE ? OR description LIKE ? OR category LIKE ? ORDER BY id DESC";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToHelpRequest(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error searching HelpRequests: " + e.getMessage());
        }
        return list;
    }

    private HelpRequest mapResultSetToHelpRequest(ResultSet rs) throws SQLException {
        HelpRequest req = new HelpRequest();
        req.setId(rs.getInt("id"));
        req.setTitle(rs.getString("title"));
        req.setDescription(rs.getString("description"));
        req.setStatus(rs.getString("status"));
        req.setBounty(rs.getInt("bounty"));
        req.setTicket(rs.getBoolean("is_ticket"));
        req.setCreatedAt(rs.getTimestamp("created_at"));
        req.setCategory(rs.getString("category"));
        req.setDifficulty(rs.getString("difficulty"));
        req.setCloseReason(rs.getString("close_reason"));
        
        // Map student_id safely
        int sId = rs.getInt("student_id");
        if (!rs.wasNull()) req.setStudentId(sId);
        
        return req;
    }
}
