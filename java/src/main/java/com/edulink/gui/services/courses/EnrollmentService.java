package com.edulink.gui.services.courses;

import com.edulink.gui.interfaces.IService;
import com.edulink.gui.models.courses.Enrollment;
import com.edulink.gui.util.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EnrollmentService implements IService<Enrollment> {
    private Connection cnx;

    public EnrollmentService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    @Override
    public void add(Enrollment enrollment) {
        add2(enrollment);
    }

    public boolean add2(Enrollment enrollment) {
        if (cnx == null) return false;
        
        int sid = enrollment.getStudentId();
        int cid = enrollment.getCoursId();
        
        System.out.println("🚀 [ENROLL_SERVICE] Target Schema Alignment: Student=" + sid + ", Course=" + cid);

        try {
            // Check existence first
            String checkSql = "SELECT id FROM enrollment WHERE student_id=? AND cours_id=?";
            PreparedStatement cpst = cnx.prepareStatement(checkSql);
            cpst.setInt(1, sid);
            cpst.setInt(2, cid);
            if (cpst.executeQuery().next()) {
                System.out.println("ℹ️ [ENROLL_SERVICE] Already enrolled.");
                return true;
            }

            // INSERT matching user's exact schema: id, student_id, cours_id, enrolled_at, progress, completed_at, completed_resources
            // We only need to provide student_id, cours_id, and progress. The rest can be null or default.
            String sql = "INSERT INTO enrollment (student_id, cours_id, progress, enrolled_at, completed_resources) VALUES (?, ?, 0, NOW(), '[]')";
            PreparedStatement pst = cnx.prepareStatement(sql);
            pst.setInt(1, sid);
            pst.setInt(2, cid);
            int rows = pst.executeUpdate();
            
            System.out.println("✅ [ENROLL_SERVICE] Insertion Successful. Rows: " + rows);
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("❌ [ENROLL_SERVICE] Schema Mismatch/SQL Error: " + e.getMessage());
            // Fallback for different naming
            try {
                String fallbackSql = "INSERT INTO enrollment (student_id, course_id, progress) VALUES (?, ?, 0)";
                PreparedStatement pst = cnx.prepareStatement(fallbackSql);
                pst.setInt(1, sid);
                pst.setInt(2, cid);
                return pst.executeUpdate() > 0;
            } catch (SQLException e2) {
                System.err.println("❌ [ENROLL_SERVICE] Fallback failed: " + e2.getMessage());
                return false;
            }
        }
    }

    @Override
    public List<Enrollment> getAll() {
        return getEnrollmentByStudent(-1);
    }

    public List<Enrollment> getEnrollmentByStudent(int studentId) {
        List<Enrollment> list = new ArrayList<>();
        if (cnx == null) return list;
        try {
            String sql = (studentId == -1) ? "SELECT * FROM enrollment" : "SELECT * FROM enrollment WHERE student_id=?";
            PreparedStatement pst = cnx.prepareStatement(sql);
            if (studentId != -1) pst.setInt(1, studentId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Enrollment e = new Enrollment();
                e.setId(rs.getInt("id"));
                e.setStudentId(rs.getInt("student_id"));
                try {
                    e.setCoursId(rs.getInt("cours_id"));
                } catch (SQLException ex) {
                    e.setCoursId(rs.getInt("course_id"));
                }
                e.setProgress(rs.getDouble("progress"));
                list.add(e);
            }
            System.out.println("📊 [ENROLL_SERVICE] Fetched " + list.size() + " enrollments for student=" + studentId);
        } catch (SQLException ex) {
            System.err.println("❌ [ENROLL_SERVICE] Fetch failed: " + ex.getMessage());
        }
        return list;
    }

    @Override public void edit(Enrollment enrollment) {
        try {
            PreparedStatement pst = cnx.prepareStatement("UPDATE enrollment SET progress=? WHERE id=?");
            pst.setDouble(1, enrollment.getProgress());
            pst.setInt(2, enrollment.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override public void delete(int id) {
        try {
            PreparedStatement pst = cnx.prepareStatement("DELETE FROM enrollment WHERE id=?");
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
