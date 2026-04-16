package com.edulink.gui.services.courses;

import com.edulink.gui.interfaces.IService;
import com.edulink.gui.models.courses.ContentProposal;
import com.edulink.gui.util.MyConnection;

import java.time.LocalDateTime;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ContentProposalService implements IService<ContentProposal> {
    private Connection cnx;

    public ContentProposalService() {
        cnx = MyConnection.getInstance().getCnx();
        ensureTableExists();
    }

    private void ensureTableExists() {
        try {
            Statement st = cnx.createStatement();
            st.executeUpdate("CREATE TABLE IF NOT EXISTS content_proposal ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "suggested_by INT NOT NULL, "
                    + "type VARCHAR(50) NOT NULL, "
                    + "title VARCHAR(255) NOT NULL, "
                    + "description TEXT, "
                    + "status VARCHAR(50) DEFAULT 'PENDING', "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ")");
        } catch (SQLException e) {
            System.err.println("Could not ensure content_proposal table: " + e.getMessage());
        }
    }

    @Override
    public void add(ContentProposal p) {
        String query = "INSERT INTO content_proposal (suggested_by, type, title, description, status, created_at) VALUES ("
                + p.getSuggestedBy() + ", '"
                + p.getType() + "', '"
                + p.getTitle() + "', '"
                + p.getDescription() + "', '"
                + p.getStatus() + "', '"
                + Timestamp.valueOf(p.getCreatedAt()) + "')";
        try {
            Statement st = cnx.createStatement();
            st.executeUpdate(query);
            System.out.println("✅ ContentProposal added (Statement)!");
        } catch (SQLException e) {
            System.err.println("❌ ContentProposal add failed: " + e.getMessage());
        }
    }

    @Override
    public void add2(ContentProposal p) {
        String query = "INSERT INTO content_proposal (suggested_by, type, title, description, status, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setInt(1, p.getSuggestedBy());
            pst.setString(2, p.getType());
            pst.setString(3, p.getTitle());
            pst.setString(4, p.getDescription());
            pst.setString(5, p.getStatus());
            pst.setTimestamp(6, Timestamp.valueOf(p.getCreatedAt()));
            int rows = pst.executeUpdate();
            System.out.println("✅ ContentProposal added (PreparedStatement)! Rows: " + rows);
        } catch (SQLException e) {
            System.err.println("❌ ContentProposal add2 failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void edit(ContentProposal p) {
        String query = "UPDATE content_proposal SET status=? WHERE id=?";
        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setString(1, p.getStatus());
            pst.setInt(2, p.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ ContentProposal edit failed: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String query = "DELETE FROM content_proposal WHERE id=?";
        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ ContentProposal delete failed: " + e.getMessage());
        }
    }

    @Override
    public List<ContentProposal> getAll() {
        List<ContentProposal> list = new ArrayList<>();
        String query = "SELECT * FROM content_proposal ORDER BY created_at DESC";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ ContentProposal getAll failed: " + e.getMessage());
        }
        return list;
    }

    public List<ContentProposal> getByStudent(int studentId) {
        List<ContentProposal> list = new ArrayList<>();
        String query = "SELECT * FROM content_proposal WHERE suggested_by=? ORDER BY created_at DESC";
        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setInt(1, studentId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ ContentProposal getByStudent failed: " + e.getMessage());
        }
        return list;
    }

    private ContentProposal mapResultSet(ResultSet rs) throws SQLException {
        ContentProposal p = new ContentProposal();
        p.setId(rs.getInt("id"));
        p.setSuggestedBy(rs.getInt("suggested_by"));
        p.setType(rs.getString("type"));
        p.setTitle(rs.getString("title"));
        p.setDescription(rs.getString("description"));
        p.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) p.setCreatedAt(ts.toLocalDateTime());
        else p.setCreatedAt(java.time.LocalDateTime.now());
        return p;
    }
}
