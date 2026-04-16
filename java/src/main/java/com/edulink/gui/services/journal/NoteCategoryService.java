package com.edulink.gui.services.journal;

import com.edulink.gui.interfaces.IService;
import com.edulink.gui.models.journal.NoteCategory;
import com.edulink.gui.util.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NoteCategoryService implements IService<NoteCategory> {
    private Connection cnx;

    public NoteCategoryService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    @Override
    public void add(NoteCategory c) {
        String qry = "INSERT INTO note_category (name) VALUES (?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, c.getName());
            pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void add2(NoteCategory c) {
        String qry = "INSERT INTO note_category (name) VALUES (?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, c.getName());
            pstm.executeUpdate();
            System.out.println("✅ Note category added (add2): " + c.getName());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void edit(NoteCategory c) {
        String qry = "UPDATE note_category SET name=? WHERE id=?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, c.getName());
            pstm.setInt(2, c.getId());
            pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        String qry = "DELETE FROM note_category WHERE id=?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<NoteCategory> getAll() {
        List<NoteCategory> list = new ArrayList<>();
        String qry = "SELECT * FROM note_category ORDER BY name ASC";
        try (Statement stm = cnx.createStatement(); ResultSet rs = stm.executeQuery(qry)) {
            while (rs.next()) {
                list.add(new NoteCategory(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
