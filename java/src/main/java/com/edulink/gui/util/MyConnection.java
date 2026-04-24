package com.edulink.gui.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton Design Pattern for database connection.
 * - Private constructor
 * - Static container initialized to null
 * - Static getter with verification
 */
public class MyConnection {
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private static MyConnection instance = null;
    private Connection cnx;
    private static String lastError = "None";

    public static String getLastError() { return lastError; }

    private MyConnection() {
        String simplifiedUrl = "jdbc:mysql://127.0.0.1:3307/edulinkpi?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        System.out.println("🔄 Attempting to connect to: " + simplifiedUrl);
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✅ Driver loaded successfully.");
            
            System.out.println("⏳ Calling DriverManager.getConnection...");
            cnx = DriverManager.getConnection(simplifiedUrl, USER, PASSWORD);
            System.out.println("✅ Database Connected Successfully!");
        } catch (ClassNotFoundException e) {
            lastError = "MySQL Driver not found! Check your pom.xml.";
            System.err.println("❌ " + lastError);
            cnx = null;
        } catch (SQLException e) {
            lastError = "Connection Failed: " + e.getMessage();
            System.err.println("❌ " + lastError);
            cnx = null;
        }
    }

    // 3. Static Getter with verification (if exists, use it; otherwise create new)
    // If the previous attempt failed (cnx == null), retry automatically.
    public static MyConnection getInstance() {
        if (instance == null || instance.cnx == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }

    /**
     * Check if the connection is alive and valid.
     */
    public boolean isConnected() {
        try {
            return cnx != null && !cnx.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Force reconnect (useful after MySQL was started late).
     */
    public static void reconnect() {
        instance = null;
        getInstance();
    }
}
