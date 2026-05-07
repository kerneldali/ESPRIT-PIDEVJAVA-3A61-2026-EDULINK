package com.edulink.gui.scratch;

import com.edulink.gui.util.MyConnection;
import java.sql.Connection;
import java.sql.Statement;

public class DbFix {
    public static void main(String[] args) {
        try {
            Connection cnx = MyConnection.getInstance().getCnx();
            Statement st = cnx.createStatement();
            
            System.out.println("Checking/Adding Web3 columns to user table...");
            
            try {
                st.execute("ALTER TABLE user ADD COLUMN eth_wallet_address VARCHAR(255) AFTER email");
                System.out.println("Added eth_wallet_address");
            } catch (Exception e) {
                System.out.println("eth_wallet_address might already exist: " + e.getMessage());
            }

            try {
                st.execute("ALTER TABLE user ADD COLUMN eth_private_key VARCHAR(255) AFTER eth_wallet_address");
                System.out.println("Added eth_private_key");
            } catch (Exception e) {
                System.out.println("eth_private_key might already exist: " + e.getMessage());
            }

            System.out.println("Done!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
