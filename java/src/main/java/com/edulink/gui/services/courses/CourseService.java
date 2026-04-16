package com.edulink.gui.services.courses;

import com.edulink.gui.interfaces.IService;
import com.edulink.gui.models.courses.Course;
import com.edulink.gui.util.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CourseService implements IService<Course> {
    private Connection cnx;

    public CourseService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    @Override
    public void add(Course course) {
        try {
            PreparedStatement pst = cnx.prepareStatement(
                    "INSERT INTO cours (matiere_id, author_id, title, description, level, price_points, xp, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            pst.setInt(1, course.getMatiereId());
            pst.setInt(2, course.getAuthorId());
            pst.setString(3, course.getTitle());
            pst.setString(4, course.getDescription());
            pst.setString(5, course.getLevel());
            pst.setInt(6, course.getPricePoints());
            pst.setInt(7, course.getXp());
            pst.setString(8, course.getStatus());
            pst.setTimestamp(9, Timestamp.valueOf(course.getCreatedAt()));
            pst.executeUpdate();
            System.out.println("✅ Course added!");
        } catch (SQLException e) {
            System.err.println("⚠ Course add with price_points failed, trying without: " + e.getMessage());
            addWithoutPrice(course);
        }
    }

    public void add2(Course course) {
        try {
            PreparedStatement pst = cnx.prepareStatement(
                    "INSERT INTO cours (matiere_id, author_id, title, description, level, price_points, xp, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            pst.setInt(1, course.getMatiereId());
            pst.setInt(2, course.getAuthorId());
            pst.setString(3, course.getTitle());
            pst.setString(4, course.getDescription());
            pst.setString(5, course.getLevel());
            pst.setInt(6, course.getPricePoints());
            pst.setInt(7, course.getXp());
            pst.setString(8, course.getStatus());
            pst.setTimestamp(9, Timestamp.valueOf(course.getCreatedAt()));
            int rows = pst.executeUpdate();
            System.out.println("✅ Course added! Rows: " + rows);
        } catch (SQLException e) {
            System.err.println("⚠ Course add2 with price_points failed, trying without: " + e.getMessage());
            addWithoutPrice(course);
        }
    }

    private void addWithoutPrice(Course course) {
        try {
            // Try without price_points column
            PreparedStatement pst = cnx.prepareStatement(
                    "INSERT INTO cours (matiere_id, author_id, title, description, level, xp, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            pst.setInt(1, course.getMatiereId());
            pst.setInt(2, course.getAuthorId());
            pst.setString(3, course.getTitle());
            pst.setString(4, course.getDescription());
            pst.setString(5, course.getLevel());
            pst.setInt(6, course.getXp());
            pst.setString(7, course.getStatus());
            pst.setTimestamp(8, Timestamp.valueOf(course.getCreatedAt()));
            int rows = pst.executeUpdate();
            System.out.println("✅ Course added (without price_points)! Rows: " + rows);
        } catch (SQLException e2) {
            System.err.println("⚠ Still failed, trying minimal columns: " + e2.getMessage());
            try {
                // Ultra minimal - only required columns
                PreparedStatement pst = cnx.prepareStatement(
                        "INSERT INTO cours (matiere_id, title, description, level, xp, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)");
                pst.setInt(1, course.getMatiereId());
                pst.setString(2, course.getTitle());
                pst.setString(3, course.getDescription());
                pst.setString(4, course.getLevel());
                pst.setInt(5, course.getXp());
                pst.setString(6, course.getStatus());
                pst.setTimestamp(7, Timestamp.valueOf(course.getCreatedAt()));
                int rows = pst.executeUpdate();
                System.out.println("✅ Course added (minimal)! Rows: " + rows);
            } catch (SQLException e3) {
                System.err.println("❌ Course add completely failed: " + e3.getMessage());
                e3.printStackTrace();
            }
        }
    }

    @Override
    public void edit(Course course) {
        try {
            PreparedStatement pst = cnx.prepareStatement(
                    "UPDATE cours SET title=?, description=?, level=?, xp=?, status=? WHERE id=?");
            pst.setString(1, course.getTitle());
            pst.setString(2, course.getDescription());
            pst.setString(3, course.getLevel());
            pst.setInt(4, course.getXp());
            pst.setString(5, course.getStatus());
            pst.setInt(6, course.getId());
            pst.executeUpdate();
            System.out.println("✅ Course updated!");
        } catch (SQLException e) {
            System.err.println("❌ Course edit failed: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        try {
            PreparedStatement pst = cnx.prepareStatement("DELETE FROM cours WHERE id=?");
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("✅ Course deleted!");
        } catch (SQLException e) {
            System.err.println("❌ Course delete failed: " + e.getMessage());
        }
    }

    @Override
    public List<Course> getAll() {
        List<Course> courses = new ArrayList<>();
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM cours");
            while (rs.next()) {
                courses.add(mapResultSetToCourse(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Course getAll failed: " + e.getMessage());
        }
        return courses;
    }

    public List<Course> findByMatiere(int matiereId) {
        List<Course> courses = new ArrayList<>();
        try {
            PreparedStatement pst = cnx.prepareStatement("SELECT * FROM cours WHERE matiere_id=?");
            pst.setInt(1, matiereId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                courses.add(mapResultSetToCourse(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Course findByMatiere failed: " + e.getMessage());
        }
        return courses;
    }

    private Course mapResultSetToCourse(ResultSet rs) throws SQLException {
        Course c = new Course();
        c.setId(rs.getInt("id"));
        c.setMatiereId(rs.getInt("matiere_id"));
        // author_id might not exist
        try { c.setAuthorId(rs.getInt("author_id")); } catch (SQLException e) { c.setAuthorId(1); }
        c.setTitle(rs.getString("title"));
        c.setDescription(rs.getString("description"));
        c.setLevel(rs.getString("level"));
        // price_points might not exist
        try { c.setPricePoints(rs.getInt("price_points")); } catch (SQLException e) { c.setPricePoints(0); }
        try { c.setXp(rs.getInt("xp")); } catch (SQLException e) { c.setXp(0); }
        c.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) c.setCreatedAt(ts.toLocalDateTime());
        else c.setCreatedAt(java.time.LocalDateTime.now());
        return c;
    }
}
