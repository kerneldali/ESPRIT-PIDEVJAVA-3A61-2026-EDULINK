package com.edulink.gui.services.courses;

import com.edulink.gui.interfaces.IService;
import com.edulink.gui.models.courses.Matiere;
import com.edulink.gui.util.MyConnection;

import java.time.LocalDateTime;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MatiereService implements IService<Matiere> {
    private Connection cnx;

    public MatiereService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    @Override
    public void add(Matiere matiere) {
        try {
            PreparedStatement pst = cnx.prepareStatement(
                    "INSERT INTO matiere (creator_id, name, status, image_url, created_at) VALUES (?, ?, ?, ?, ?)");
            pst.setInt(1, matiere.getCreatorId());
            pst.setString(2, matiere.getName());
            pst.setString(3, matiere.getStatus());
            pst.setString(4, matiere.getImageUrl());
            pst.setTimestamp(5, Timestamp.valueOf(matiere.getCreatedAt()));
            pst.executeUpdate();
            System.out.println("✅ Matiere added!");
        } catch (SQLException e) {
            System.err.println("⚠ Matiere add with all columns failed, trying minimal: " + e.getMessage());
            addMinimal(matiere);
        }
    }

    @Override
    public void add2(Matiere matiere) {
        try {
            PreparedStatement pst = cnx.prepareStatement(
                    "INSERT INTO matiere (creator_id, name, status, image_url, created_at) VALUES (?, ?, ?, ?, ?)");
            pst.setInt(1, matiere.getCreatorId());
            pst.setString(2, matiere.getName());
            pst.setString(3, matiere.getStatus());
            pst.setString(4, matiere.getImageUrl());
            pst.setTimestamp(5, Timestamp.valueOf(matiere.getCreatedAt()));
            int rows = pst.executeUpdate();
            System.out.println("✅ Matiere added! Rows: " + rows);
        } catch (SQLException e) {
            System.err.println("⚠ Matiere add2 with all columns failed, trying minimal: " + e.getMessage());
            addMinimal(matiere);
        }
    }

    private void addMinimal(Matiere matiere) {
        try {
            PreparedStatement pst = cnx.prepareStatement(
                    "INSERT INTO matiere (name, status) VALUES (?, ?)");
            pst.setString(1, matiere.getName());
            pst.setString(2, matiere.getStatus());
            int rows = pst.executeUpdate();
            System.out.println("✅ Matiere added (minimal)! Rows: " + rows);
        } catch (SQLException e2) {
            System.err.println("❌ Matiere add completely failed: " + e2.getMessage());
            e2.printStackTrace();
        }
    }

    @Override
    public void edit(Matiere matiere) {
        try {
            PreparedStatement pst = cnx.prepareStatement(
                    "UPDATE matiere SET name=?, status=?, image_url=? WHERE id=?");
            pst.setString(1, matiere.getName());
            pst.setString(2, matiere.getStatus());
            pst.setString(3, matiere.getImageUrl());
            pst.setInt(4, matiere.getId());
            pst.executeUpdate();
            System.out.println("✅ Matiere updated!");
        } catch (SQLException e) {
            // Try without image_url
            try {
                PreparedStatement pst = cnx.prepareStatement(
                        "UPDATE matiere SET name=?, status=? WHERE id=?");
                pst.setString(1, matiere.getName());
                pst.setString(2, matiere.getStatus());
                pst.setInt(3, matiere.getId());
                pst.executeUpdate();
                System.out.println("✅ Matiere updated (without image_url)!");
            } catch (SQLException e2) {
                System.err.println("❌ Matiere edit failed: " + e2.getMessage());
            }
        }
    }

    @Override
    public void delete(int id) {
        try {
            PreparedStatement pst = cnx.prepareStatement("DELETE FROM matiere WHERE id=?");
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("✅ Matiere deleted!");
        } catch (SQLException e) {
            System.err.println("❌ Matiere delete failed: " + e.getMessage());
        }
    }

    @Override
    public List<Matiere> getAll() {
        List<Matiere> matieres = new ArrayList<>();
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM matiere");
            while (rs.next()) {
                Matiere m = new Matiere();
                m.setId(rs.getInt("id"));
                try { m.setCreatorId(rs.getInt("creator_id")); } catch (SQLException e) { m.setCreatorId(1); }
                m.setName(rs.getString("name"));
                m.setStatus(rs.getString("status"));
                try { m.setImageUrl(rs.getString("image_url")); } catch (SQLException e) { m.setImageUrl(null); }
                try {
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) m.setCreatedAt(ts.toLocalDateTime());
                    else m.setCreatedAt(java.time.LocalDateTime.now());
                } catch (SQLException e) { m.setCreatedAt(java.time.LocalDateTime.now()); }
                matieres.add(m);
            }
        } catch (SQLException e) {
            System.err.println("❌ Matiere getAll failed: " + e.getMessage());
        }
        return matieres;
    }
}
