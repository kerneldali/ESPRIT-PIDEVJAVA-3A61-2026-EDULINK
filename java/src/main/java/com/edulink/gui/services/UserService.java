package com.edulink.gui.services;

import com.edulink.gui.interfaces.IService;
import com.edulink.gui.models.User;
import com.edulink.gui.util.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IService<User> {
    private Connection cnx;

    public UserService() {
        cnx = MyConnection.getInstance().getCnx();
        ensureXpColumn();
    }

    private void ensureXpColumn() {
        try (Statement st = cnx.createStatement()) {
            st.execute("ALTER TABLE user ADD COLUMN IF NOT EXISTS xp INT DEFAULT 0");
        } catch (SQLException e) {
            // Might already exist or dialect doesn't support IF NOT EXISTS
        }
    }

    public boolean isConnected() {
        return MyConnection.getInstance().isConnected();
    }

    @Override
    public void add(User user) {
        String qry = "INSERT INTO user (email, full_name, password, roles, is_verified) VALUES (?,?,?,?,?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, user.getEmail());
            pstm.setString(2, user.getFullName());
            pstm.setString(3, user.getPassword());
            pstm.setString(4, user.getRoles() != null ? user.getRoles() : "[\"ROLE_USER\"]");
            pstm.setBoolean(5, user.isVerified());
            pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void add2(User user) {
        String qry = "INSERT INTO user (email, full_name, password, roles, is_verified) VALUES (?,?,?,?,?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, user.getEmail());
            pstm.setString(2, user.getFullName());
            pstm.setString(3, user.getPassword());
            pstm.setString(4, user.getRoles() != null ? user.getRoles() : "[\"ROLE_USER\"]");
            pstm.setBoolean(5, user.isVerified());
            pstm.executeUpdate();
            System.out.println("✅ User added (add2)!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void edit(User user) {
        String qry = "UPDATE user SET email=?, full_name=?, roles=?, is_verified=? WHERE id=?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, user.getEmail());
            pstm.setString(2, user.getFullName());
            pstm.setString(3, user.getRoles());
            pstm.setBoolean(4, user.isVerified());
            pstm.setInt(5, user.getId());
            pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public User findByEmail(String email) {
        String qry = "SELECT * FROM user WHERE email = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, email);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateXp(int userId, int amountToAdd) {
        String qry = "UPDATE user SET xp = xp + ? WHERE id=?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, amountToAdd);
            pstm.setInt(2, userId);
            pstm.executeUpdate();
            addTransactionLog(userId, "Gained " + amountToAdd + " XP points!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateWallet(int userId, double amountToAdd) {
        String qry = "UPDATE user SET wallet_balance = wallet_balance + ? WHERE id=?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setDouble(1, amountToAdd);
            pstm.setInt(2, userId);
            pstm.executeUpdate();
            addTransactionLog(userId, "Wallet credited with " + amountToAdd + " coins.");
        } catch (SQLException e) {
            System.err.println("Could not update wallet: " + e.getMessage());
        }
    }

    public void addTransactionLog(int userId, String message) {
        String qry = "INSERT INTO transaction_log (user_id, message) VALUES (?, ?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, userId);
            pstm.setString(2, message);
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to insert log: " + e.getMessage());
        }
    }

    public List<String> getUserTransactions(int userId) {
        List<String> logs = new ArrayList<>();
        String qry = "SELECT message, created_at FROM transaction_log WHERE user_id = ? ORDER BY id ASC";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, userId);
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) {
                String msg = rs.getString("message");
                String date = rs.getString("created_at");
                logs.add("[" + date + "] " + msg);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    @Override
    public void delete(int id) {
        String qry = "DELETE FROM user WHERE id=?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public User authenticate(String email, String password) {
        String qry = "SELECT * FROM user WHERE email = ? AND password = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, email);
            pstm.setString(2, password);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<User> getAll() {
        return fetchUsers("SELECT * FROM user ORDER BY id DESC");
    }

    /**
     * Retrieve users matching ROLE_STUDENT.
     * Note: Depending on Symfony auth (ROLE_USER is default), we check either or just ROLE_STUDENT.
     */
    public List<User> getStudents() {
        // In Symfony, default users are often ROLE_USER or explicitly ROLE_STUDENT
        // We'll search for ROLE_STUDENT or the absence of ROLE_FACULTY / ROLE_ADMIN
        String qry = "SELECT * FROM user WHERE roles LIKE '%ROLE_STUDENT%' OR (roles NOT LIKE '%ROLE_FACULTY%' AND roles NOT LIKE '%ROLE_ADMIN%') ORDER BY id DESC";
        return fetchUsers(qry);
    }

    /**
     * Retrieve users matching ROLE_FACULTY.
     */
    public List<User> getFaculty() {
        String qry = "SELECT * FROM user WHERE roles LIKE '%ROLE_FACULTY%' OR roles LIKE '%ROLE_TEACHER%' ORDER BY id DESC";
        return fetchUsers(qry);
    }

    private List<User> fetchUsers(String query) {
        List<User> list = new ArrayList<>();
        if (cnx == null) return list;

        try (Statement stm = cnx.createStatement();
             ResultSet rs = stm.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setEmail(rs.getString("email"));
        u.setFullName(rs.getString("full_name"));
        // Some users might have null full names. Fallback to extracting from email.
        if (u.getFullName() == null || u.getFullName().trim().isEmpty()) {
            String tempName = u.getEmail() != null ? u.getEmail().split("@")[0] : "Unknown";
            u.setFullName(tempName.substring(0, 1).toUpperCase() + tempName.substring(1));
        }
        u.setPassword(rs.getString("password"));
        u.setRoles(rs.getString("roles"));
        u.setResetOtp(rs.getString("reset_otp"));
        u.setVerified(rs.getBoolean("is_verified"));
        u.setWalletBalance(rs.getDouble("wallet_balance"));
        
        // Use real XP from DB
        try {
            u.setXp(rs.getInt("xp"));
        } catch (SQLException e) {
            u.setXp(0);
        }
        return u;
    }
}
