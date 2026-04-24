package com.edulink.gui.services.challenge;

import com.edulink.gui.interfaces.IService;
import com.edulink.gui.models.challenge.Challenge;
import com.edulink.gui.util.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChallengeService implements IService<Challenge> {

    private Connection cnx;

    public ChallengeService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    @Override
    public void add(Challenge challenge) {
        String qry = "INSERT INTO challenge (title, description, difficulty, xp_reward, status, deadline, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, challenge.getTitle());
            pstm.setString(2, challenge.getDescription());
            pstm.setString(3, challenge.getDifficulty());
            pstm.setInt(4, challenge.getXpReward());
            pstm.setString(5, challenge.getStatus());
            pstm.setTimestamp(6, challenge.getDeadline() != null ? Timestamp.valueOf(challenge.getDeadline()) : null);
            pstm.setTimestamp(7, Timestamp.valueOf(challenge.getCreatedAt()));
            pstm.executeUpdate();
            System.out.println("✅ Challenge added: " + challenge.getTitle());
        } catch (SQLException e) {
            System.err.println("❌ Challenge add failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void add2(Challenge challenge) {
        // Même logique que add(), présent pour respecter le contrat IService
        add(challenge);
    }

    @Override
    public void edit(Challenge challenge) {
        String qry = "UPDATE challenge SET title=?, description=?, difficulty=?, xp_reward=?, status=?, deadline=? " +
                     "WHERE id=?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, challenge.getTitle());
            pstm.setString(2, challenge.getDescription());
            pstm.setString(3, challenge.getDifficulty());
            pstm.setInt(4, challenge.getXpReward());
            pstm.setString(5, challenge.getStatus());
            pstm.setTimestamp(6, challenge.getDeadline() != null ? Timestamp.valueOf(challenge.getDeadline()) : null);
            pstm.setInt(7, challenge.getId());
            pstm.executeUpdate();
            System.out.println("✅ Challenge updated: " + challenge.getTitle());
        } catch (SQLException e) {
            System.err.println("❌ Challenge edit failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        String qry = "DELETE FROM challenge WHERE id=?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
            System.out.println("✅ Challenge deleted (id=" + id + ")");
        } catch (SQLException e) {
            System.err.println("❌ Challenge delete failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<Challenge> getAll() {
        List<Challenge> list = new ArrayList<>();
        String qry = "SELECT * FROM challenge ORDER BY created_at DESC";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Challenge getAll failed: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Retourne uniquement les challenges avec le statut OPEN.
     * Utilisé pour la vue étudiant.
     */
    public List<Challenge> getOpen() {
        List<Challenge> list = new ArrayList<>();
        String qry = "SELECT * FROM challenge WHERE status = 'OPEN' ORDER BY deadline ASC";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Challenge getOpen failed: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /** Retourne un challenge par son id, ou null. */
    public Challenge getById(int id) {
        String qry = "SELECT * FROM challenge WHERE id=?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) return mapResultSet(rs);
        } catch (SQLException e) {
            System.err.println("❌ Challenge getById failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Filtre les challenges par difficulté.
     */
    public List<Challenge> getByDifficulty(String difficulty) {
        List<Challenge> list = new ArrayList<>();
        String qry = "SELECT * FROM challenge WHERE difficulty=? ORDER BY created_at DESC";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, difficulty);
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Challenge getByDifficulty failed: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // --- Mapper privé ---
    private Challenge mapResultSet(ResultSet rs) throws SQLException {
        Challenge c = new Challenge();
        c.setId(rs.getInt("id"));
        c.setTitle(rs.getString("title"));
        c.setDescription(rs.getString("description"));
        c.setDifficulty(rs.getString("difficulty"));
        c.setXpReward(rs.getInt("xp_reward"));
        c.setStatus(rs.getString("status"));

        Timestamp deadline = rs.getTimestamp("deadline");
        c.setDeadline(deadline != null ? deadline.toLocalDateTime() : null);

        Timestamp createdAt = rs.getTimestamp("created_at");
        c.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now());

        return c;
    }
}
