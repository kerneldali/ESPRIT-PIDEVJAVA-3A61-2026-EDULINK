package com.edulink.gui.scratch;

import com.edulink.gui.util.MyConnection;
import com.edulink.gui.util.Web3Config;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdminSeeder {
    public static void main(String[] args) {
        try {
            Connection cnx = MyConnection.getInstance().getCnx();
            
            // 1. Find the admin (email contains admin)
            String sql = "SELECT id, email FROM user WHERE roles LIKE '%ROLE_ADMIN%' LIMIT 1";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int id = rs.getInt("id");
                String email = rs.getString("email");
                System.out.println("Found Admin: " + email + " (ID: " + id + ")");
                
                // 2. Set unlimited credits and sync private key
                String update = "UPDATE user SET wallet_balance = 999999, eth_private_key = ? WHERE id = ?";
                PreparedStatement ups = cnx.prepareStatement(update);
                ups.setString(1, Web3Config.ADMIN_PRIVATE_KEY);
                ups.setInt(2, id);
                ups.executeUpdate();
                
                System.out.println("✅ Admin " + email + " now has unlimited (999,999) credits in DB.");
                System.out.println("✅ Admin private key synced with Web3Config.");
            } else {
                System.err.println("❌ No user with ROLE_ADMIN found. Please create one first.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
