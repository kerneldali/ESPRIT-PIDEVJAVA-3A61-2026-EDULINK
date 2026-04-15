package com.edulink.gui.services.journal;

import com.edulink.gui.interfaces.IService;
import com.edulink.gui.models.journal.PersonalTask;
import com.edulink.gui.util.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PersonalTaskService implements IService<PersonalTask> {
    private Connection cnx;

    public PersonalTaskService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    @Override
    public void add(PersonalTask t) {
        String qry = "INSERT INTO personal_tasks (user_id, title, is_completed) VALUES (?,?,?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, t.getUserId());
            pstm.setString(2, t.getTitle());
            pstm.setBoolean(3, t.isCompleted());
            pstm.executeUpdate();
            System.out.println("✅ Task added successfully: " + t.getTitle());
        } catch (SQLException e) {
            System.err.println("❌ Error adding task: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void edit(PersonalTask t) {
        String qry = "UPDATE personal_tasks SET title=?, is_completed=?, completed_at=? WHERE id=?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, t.getTitle());
            pstm.setBoolean(2, t.isCompleted());
            pstm.setTimestamp(3, t.isCompleted() ? new Timestamp(System.currentTimeMillis()) : null);
            pstm.setInt(4, t.getId());
            pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        String qry = "DELETE FROM personal_tasks WHERE id=?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<PersonalTask> getAll() {
        return getByUser(-1);
    }

    public List<PersonalTask> getByUser(int userId) {
        List<PersonalTask> list = new ArrayList<>();
        String qry = userId == -1 ? "SELECT * FROM personal_tasks"
                : "SELECT * FROM personal_tasks WHERE user_id = ? ORDER BY created_at DESC";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            if (userId != -1)
                pstm.setInt(1, userId);
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) {
                list.add(new PersonalTask(rs.getInt("id"), rs.getInt("user_id"), rs.getString("title"),
                        rs.getBoolean("is_completed"), rs.getTimestamp("created_at"), rs.getTimestamp("completed_at")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
