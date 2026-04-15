package com.edulink.gui.services.journal;

import com.edulink.gui.interfaces.IService;
import com.edulink.gui.models.journal.Notebook;
import com.edulink.gui.util.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotebookService implements IService<Notebook> {
    private Connection cnx;

    public NotebookService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    @Override
    public void add(Notebook n) {
        String qry = "INSERT INTO notebook (user_id, title, cover_color) VALUES (?,?,?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, n.getUserId());
            pstm.setString(2, n.getTitle());
            pstm.setString(3, n.getCoverColor());
            pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void edit(Notebook n) {
        String qry = "UPDATE notebook SET title=?, cover_color=? WHERE id=?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, n.getTitle());
            pstm.setString(2, n.getCoverColor());
            pstm.setInt(3, n.getId());
            pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        String qry = "DELETE FROM notebook WHERE id=?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Notebook> getAll() {
        return getByUser(-1);
    }

    public List<Notebook> getByUser(int userId) {
        List<Notebook> list = new ArrayList<>();
        String qry = userId == -1 ? "SELECT * FROM notebook"
                : "SELECT * FROM notebook WHERE user_id = ? ORDER BY created_at DESC";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            if (userId != -1)
                pstm.setInt(1, userId);
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) {
                list.add(new Notebook(rs.getInt("id"), rs.getInt("user_id"), rs.getString("title"),
                        rs.getString("cover_color"), rs.getTimestamp("created_at")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
