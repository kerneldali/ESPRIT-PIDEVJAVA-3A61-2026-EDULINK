package com.edulink.gui;

import com.edulink.gui.util.MyConnection;
import java.sql.*;

public class DebugDb {
    public static void main(String[] args) {
        try {
            Connection cnx = MyConnection.getInstance().getCnx();
            System.out.println("--- MATIERE TABLE ---");
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM matiere");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + " | Name: " + rs.getString("name") + " | Status: " + rs.getString("status"));
            }
            
            System.out.println("\n--- ENROLLMENT SCHEMA ---");
            DatabaseMetaData md = cnx.getMetaData();
            ResultSet tables = md.getTables(null, null, "%enroll%", null);
            while (tables.next()) {
                String t = tables.getString("TABLE_NAME");
                System.out.println("Table Found: " + t);
                ResultSet cols = md.getColumns(null, null, t, "%");
                while (cols.next()) {
                    System.out.println("  - Col: " + cols.getString("COLUMN_NAME"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
