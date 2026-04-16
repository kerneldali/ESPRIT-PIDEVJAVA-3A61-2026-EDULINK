package com.edulink.gui.services.courses;

import com.edulink.gui.interfaces.IService;
import com.edulink.gui.models.courses.Resource;
import com.edulink.gui.util.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResourceService implements IService<Resource> {
    private Connection cnx;

    public ResourceService() {
        cnx = MyConnection.getInstance().getCnx();
        ensureTableExists();
    }

    private void ensureTableExists() {
        try {
            Statement st = cnx.createStatement();
            // Table for resources
            st.executeUpdate("CREATE TABLE IF NOT EXISTS resource ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "cours_id INT NOT NULL, "
                    + "author_id INT DEFAULT 1, "
                    + "title VARCHAR(255) NOT NULL, "
                    + "url TEXT, "
                    + "type VARCHAR(50), "
                    + "status VARCHAR(50) DEFAULT 'ACTIVE'"
                    + ")");
            
            // Table to track which student completed which resource
            st.executeUpdate("CREATE TABLE IF NOT EXISTS resource_completion ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "student_id INT NOT NULL, "
                    + "resource_id INT NOT NULL, "
                    + "completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "UNIQUE KEY unique_completion (student_id, resource_id)"
                    + ")");
            System.out.println("✅ Resource tables ensured.");
        } catch (SQLException e) {
            System.err.println("Could not ensure resource table: " + e.getMessage());
        }
    }

    @Override
    public void add(Resource resource) {
        // Try with author_id first, fallback without it
        try {
            PreparedStatement pst = cnx.prepareStatement(
                    "INSERT INTO resource (cours_id, author_id, title, url, type, status) VALUES (?, ?, ?, ?, ?, ?)");
            pst.setInt(1, resource.getCoursId());
            pst.setInt(2, resource.getAuthorId());
            pst.setString(3, resource.getTitle());
            pst.setString(4, resource.getUrl());
            pst.setString(5, resource.getType());
            pst.setString(6, resource.getStatus());
            pst.executeUpdate();
            System.out.println("✅ Resource added!");
        } catch (SQLException e) {
            System.err.println("⚠ Resource add with author_id failed, trying without: " + e.getMessage());
            addWithoutAuthor(resource);
        }
    }

    public void add2(Resource resource) {
        // Try with author_id first, fallback without it
        try {
            PreparedStatement pst = cnx.prepareStatement(
                    "INSERT INTO resource (cours_id, author_id, title, url, type, status) VALUES (?, ?, ?, ?, ?, ?)");
            pst.setInt(1, resource.getCoursId());
            pst.setInt(2, resource.getAuthorId());
            pst.setString(3, resource.getTitle());
            pst.setString(4, resource.getUrl());
            pst.setString(5, resource.getType());
            pst.setString(6, resource.getStatus());
            int rows = pst.executeUpdate();
            System.out.println("✅ Resource added (PreparedStatement)! Rows: " + rows);
        } catch (SQLException e) {
            System.err.println("⚠ Resource add2 with author_id failed, trying without: " + e.getMessage());
            addWithoutAuthor(resource);
        }
    }

    private void addWithoutAuthor(Resource resource) {
        try {
            PreparedStatement pst = cnx.prepareStatement(
                    "INSERT INTO resource (cours_id, title, url, type, status) VALUES (?, ?, ?, ?, ?)");
            pst.setInt(1, resource.getCoursId());
            pst.setString(2, resource.getTitle());
            pst.setString(3, resource.getUrl());
            pst.setString(4, resource.getType());
            pst.setString(5, resource.getStatus());
            int rows = pst.executeUpdate();
            System.out.println("✅ Resource added (without author_id)! Rows: " + rows);
        } catch (SQLException e2) {
            System.err.println("❌ Resource add completely failed: " + e2.getMessage());
            e2.printStackTrace();
        }
    }

    @Override
    public void edit(Resource resource) {
        try {
            PreparedStatement pst = cnx.prepareStatement(
                    "UPDATE resource SET title=?, url=?, type=?, status=? WHERE id=?");
            pst.setString(1, resource.getTitle());
            pst.setString(2, resource.getUrl());
            pst.setString(3, resource.getType());
            pst.setString(4, resource.getStatus());
            pst.setInt(5, resource.getId());
            pst.executeUpdate();
            System.out.println("✅ Resource updated!");
        } catch (SQLException e) {
            System.err.println("❌ Resource edit failed: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        try {
            PreparedStatement pst = cnx.prepareStatement("DELETE FROM resource WHERE id=?");
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("✅ Resource deleted!");
        } catch (SQLException e) {
            System.err.println("❌ Resource delete failed: " + e.getMessage());
        }
    }

    @Override
    public List<Resource> getAll() {
        List<Resource> resources = new ArrayList<>();
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM resource");
            while (rs.next()) {
                resources.add(mapResultSetToResource(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Resource getAll failed: " + e.getMessage());
        }
        return resources;
    }

    public List<Resource> findByCourse(int courseId) {
        List<Resource> resources = new ArrayList<>();
        try {
            PreparedStatement pst = cnx.prepareStatement("SELECT * FROM resource WHERE cours_id=?");
            pst.setInt(1, courseId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                resources.add(mapResultSetToResource(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Resource findByCourse failed: " + e.getMessage());
        }
        return resources;
    }

    public boolean isResourceCompleted(int studentId, int resourceId) {
        try {
            PreparedStatement pst = cnx.prepareStatement("SELECT 1 FROM resource_completion WHERE student_id=? AND resource_id=?");
            pst.setInt(1, studentId);
            pst.setInt(2, resourceId);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("❌ Error checking resource completion: " + e.getMessage());
            return false;
        }
    }

    public void markAsCompleted(int studentId, int resourceId) {
        try {
            PreparedStatement pst = cnx.prepareStatement(
                "INSERT IGNORE INTO resource_completion (student_id, resource_id) VALUES (?, ?)");
            pst.setInt(1, studentId);
            pst.setInt(2, resourceId);
            pst.executeUpdate();
            System.out.println("✅ Resource #" + resourceId + " marked as completed by Student #" + studentId);
        } catch (SQLException e) {
            System.err.println("❌ Error marking resource as completed: " + e.getMessage());
        }
    }

    public int getCompletedCount(int studentId, int courseId) {
        try {
            PreparedStatement pst = cnx.prepareStatement(
                "SELECT COUNT(*) FROM resource_completion rc " +
                "JOIN resource r ON rc.resource_id = r.id " +
                "WHERE rc.student_id = ? AND r.cours_id = ?");
            pst.setInt(1, studentId);
            pst.setInt(2, courseId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("❌ Error getting completed count: " + e.getMessage());
        }
        return 0;
    }

    private Resource mapResultSetToResource(ResultSet rs) throws SQLException {
        Resource r = new Resource();
        r.setId(rs.getInt("id"));
        r.setCoursId(rs.getInt("cours_id"));
        // author_id might not exist in older schemas
        try { r.setAuthorId(rs.getInt("author_id")); } catch (SQLException e) { r.setAuthorId(1); }
        r.setTitle(rs.getString("title"));
        r.setUrl(rs.getString("url"));
        r.setType(rs.getString("type"));
        r.setStatus(rs.getString("status"));
        return r;
    }
}
