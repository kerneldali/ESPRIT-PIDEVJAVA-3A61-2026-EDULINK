package com.edulink.gui.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton DB connection.
 *
 * Configuration is loaded at runtime from src/main/resources/db.properties.
 * If db.properties is missing, it is auto-created from db.properties.example
 * so that each developer has a working local config without committing it.
 *
 * Why this design:
 *   - Different OSes use different MySQL ports (Windows XAMPP=3306, macOS XAMPP=3307).
 *   - Hardcoding the URL caused regressions on every git pull.
 *   - db.properties is git-ignored; db.properties.example is versioned.
 */
public class MyConnection {

    private static final String CONFIG_FILE  = "db.properties";
    private static final String EXAMPLE_FILE = "db.properties.example";

    private static MyConnection instance = null;
    private Connection cnx;
    private static String lastError = "None";

    public static String getLastError() { return lastError; }

    private MyConnection() {
        Properties props = loadConfig();
        if (props == null) {
            cnx = null;
            return;
        }

        String host     = props.getProperty("db.host",     "127.0.0.1");
        String port     = props.getProperty("db.port",     "3306");
        String name     = props.getProperty("db.name",     "edulinkpi");
        String user     = props.getProperty("db.user",     "root");
        String password = props.getProperty("db.password", "");
        String params   = props.getProperty("db.params",
                "useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + name + "?" + params;
        System.out.println("[DB] Connecting to: " + url + " (user=" + user + ")");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            cnx = DriverManager.getConnection(url, user, password);
            System.out.println("[DB] Connection OK.");
        } catch (ClassNotFoundException e) {
            lastError = "MySQL Driver not found. Check your pom.xml.";
            System.err.println("[DB] " + lastError);
            cnx = null;
        } catch (SQLException e) {
            lastError = "Connection failed: " + e.getMessage();
            System.err.println("[DB] " + lastError);
            System.err.println("[DB] Hint: verify host/port in src/main/resources/" + CONFIG_FILE
                    + " (XAMPP macOS often uses 3307, Windows uses 3306).");
            cnx = null;
        }
    }

    /**
     * Loads db.properties from the classpath. If absent, copies db.properties.example
     * into the resources folder and uses it. Returns null if neither file is found.
     */
    private Properties loadConfig() {
        Properties props = new Properties();

        // 1) Try to load db.properties first.
        try (InputStream in = MyConnection.class.getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {
            if (in != null) {
                props.load(in);
                return props;
            }
        } catch (IOException e) {
            System.err.println("[DB] Failed to read " + CONFIG_FILE + ": " + e.getMessage());
        }

        // 2) Fallback: try the example template, and bootstrap a local copy on disk.
        System.out.println("[DB] " + CONFIG_FILE + " not found on classpath. Falling back to "
                + EXAMPLE_FILE + ".");
        try (InputStream in = MyConnection.class.getClassLoader()
                .getResourceAsStream(EXAMPLE_FILE)) {
            if (in == null) {
                lastError = "Missing both " + CONFIG_FILE + " and " + EXAMPLE_FILE
                        + " in src/main/resources/. Cannot connect.";
                System.err.println("[DB] " + lastError);
                return null;
            }
            byte[] content = in.readAllBytes();
            props.load(new java.io.ByteArrayInputStream(content));
            bootstrapLocalConfig(content);
            System.err.println("[DB] WARNING: using example defaults. Edit "
                    + "src/main/resources/" + CONFIG_FILE
                    + " with your local MySQL host/port (e.g. 3307 on macOS XAMPP).");
            return props;
        } catch (IOException e) {
            lastError = "Failed to read " + EXAMPLE_FILE + ": " + e.getMessage();
            System.err.println("[DB] " + lastError);
            return null;
        }
    }

    /**
     * Best-effort: copy the example into src/main/resources/db.properties so the dev
     * sees the file in their IDE next time. Silent no-op if the path is not writable
     * (e.g. running from a packaged jar).
     */
    private void bootstrapLocalConfig(byte[] content) {
        try {
            java.nio.file.Path target = java.nio.file.Paths.get(
                    "src", "main", "resources", CONFIG_FILE);
            if (java.nio.file.Files.exists(target)) return;
            java.nio.file.Files.createDirectories(target.getParent());
            try (OutputStream out = java.nio.file.Files.newOutputStream(target)) {
                out.write(content);
            }
            System.out.println("[DB] Created " + target.toAbsolutePath()
                    + " from template. Edit it with your local DB config.");
        } catch (Exception ignored) {
            // Not fatal: the app still works with example defaults loaded in memory.
        }
    }

    /**
     * Returns the singleton. If the previous attempt failed (cnx == null),
     * retries automatically (handy when MySQL was started after the app).
     */
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
     * Returns true if the connection is open and valid.
     */
    public boolean isConnected() {
        try {
            return cnx != null && !cnx.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Force reconnect (useful after MySQL was started late or db.properties edited).
     */
    public static void reconnect() {
        instance = null;
        getInstance();
    }
}
