package com.edulink.gui.services.journal;

import com.edulink.gui.interfaces.IService;
import com.edulink.gui.models.journal.Note;
import com.edulink.gui.util.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoteService implements IService<Note> {
    private Connection cnx;

    public NoteService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    @Override
    public void add(Note n) {
        String qry = "INSERT INTO note (notebook_id, category_id, title, content, tags) VALUES (?,?,?,?,?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, n.getNotebookId());
            if (n.getCategoryId() > 0)
                pstm.setInt(2, n.getCategoryId());
            else
                pstm.setNull(2, java.sql.Types.INTEGER);
            pstm.setString(3, n.getTitle());
            pstm.setString(4, n.getContent());
            pstm.setString(5, n.getTags());
            pstm.executeUpdate();
            System.out.println("✅ Note added successfully: " + n.getTitle());
        } catch (SQLException e) {
            System.err.println("❌ Error adding note: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void edit(Note n) {
        String qry = "UPDATE note SET title=?, content=?, tags=?, category_id=? WHERE id=?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, n.getTitle());
            pstm.setString(2, n.getContent());
            pstm.setString(3, n.getTags());
            if (n.getCategoryId() > 0)
                pstm.setInt(4, n.getCategoryId());
            else
                pstm.setNull(4, java.sql.Types.INTEGER);
            pstm.setInt(5, n.getId());
            pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        String qry = "DELETE FROM note WHERE id=?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Note> getAll() {
        return getByNotebook(-1);
    }

    public List<Note> getByNotebook(int notebookId) {
        List<Note> list = new ArrayList<>();
        String qry = notebookId == -1 ? "SELECT * FROM note" : "SELECT * FROM note WHERE notebook_id = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            if (notebookId != -1)
                pstm.setInt(1, notebookId);
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) {
                list.add(new Note(rs.getInt("id"), rs.getInt("notebook_id"), rs.getInt("category_id"),
                        rs.getString("title"),
                        rs.getString("content"), rs.getString("tags"), rs.getTimestamp("created_at")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Map<String, Integer> getNoteCountByCategory() {
        Map<String, Integer> stats = new HashMap<>();
        String qry = "SELECT c.name, COUNT(n.id) as count FROM note_category c LEFT JOIN note n ON c.id = n.category_id GROUP BY c.name";
        try (Statement stm = cnx.createStatement(); ResultSet rs = stm.executeQuery(qry)) {
            while (rs.next()) {
                stats.put(rs.getString("name"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }
}
