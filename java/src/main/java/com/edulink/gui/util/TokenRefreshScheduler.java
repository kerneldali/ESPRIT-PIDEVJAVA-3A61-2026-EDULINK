package com.edulink.gui.util;

import com.edulink.gui.models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * TokenRefreshScheduler — automatic EduToken balance refresh.
 *
 * Problem solved:
 *   Previously, wallet_balance was only updated when the user manually
 *   clicked "Refresh Balance" or interacted with the Wallet screen.
 *   After a bounty payment or admin credit, the in-memory User object
 *   would show stale data until re-login.
 *
 * Solution:
 *   After login, start a lightweight background scheduler that reads
 *   the wallet_balance column from DB every REFRESH_INTERVAL_SECONDS
 *   and silently updates the in-memory SessionManager.getCurrentUser().
 *   This prevents forced re-logins while keeping balances current for
 *   the "Not enough tokens" check and UI display.
 *
 * Synchronization:
 *   Both the Java desktop and Symfony web app write wallet_balance to
 *   the same MySQL row. The scheduler reads this shared value, so any
 *   change from either application surface is automatically reflected.
 */
public class TokenRefreshScheduler {

    /** How often to poll the DB for balance updates (seconds). */
    private static final long REFRESH_INTERVAL_SECONDS = 30;

    private static ScheduledExecutorService executor;
    private static ScheduledFuture<?> task;

    /**
     * Starts the background balance refresh scheduler for the given user.
     * Call this immediately after a successful login.
     *
     * @param userId  the logged-in user's database ID
     */
    public static synchronized void start(int userId) {
        stop(); // cancel any previous scheduler if switching users

        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "EduToken-Refresh");
            t.setDaemon(true); // don't block JVM shutdown
            return t;
        });

        task = executor.scheduleAtFixedRate(() -> {
            try {
                refreshBalance(userId);
            } catch (Exception e) {
                System.err.println("[TokenRefreshScheduler] Error during refresh: " + e.getMessage());
            }
        }, REFRESH_INTERVAL_SECONDS, REFRESH_INTERVAL_SECONDS, TimeUnit.SECONDS);

        System.out.println("[TokenRefreshScheduler] Started for userId=" + userId
            + " — interval=" + REFRESH_INTERVAL_SECONDS + "s");
    }

    /**
     * Stops the background refresh. Call on logout or application shutdown.
     */
    public static synchronized void stop() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        System.out.println("[TokenRefreshScheduler] Stopped.");
    }

    /**
     * Reads wallet_balance from the database for the given user and
     * silently updates the in-memory SessionManager if still logged in.
     *
     * Uses a fresh DB connection reference each time to survive
     * connection resets without requiring re-login.
     */
    private static void refreshBalance(int userId) {
        // Re-fetch connection in case it was reset (survivability)
        Connection cnx = MyConnection.getInstance().getCnx();
        if (cnx == null) {
            System.err.println("[TokenRefreshScheduler] DB not available, skipping refresh.");
            return;
        }

        String sql = "SELECT wallet_balance FROM user WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // IMPORTANT: use getDouble — wallet_balance is DOUBLE in the DB.
                // getInt would silently truncate and cause "Not enough tokens" false-positives.
                double freshBalance = rs.getDouble("wallet_balance");

                User currentUser = SessionManager.getCurrentUser();
                if (currentUser != null && currentUser.getId() == userId) {
                    double previous = currentUser.getWalletBalance();
                    if (Math.abs(freshBalance - previous) > 0.001) {
                        // Balance changed — update in-memory user
                        currentUser.setWalletBalance(freshBalance);
                        System.out.println("[TokenRefreshScheduler] Balance refreshed for userId=" + userId
                            + " | " + previous + " → " + freshBalance + " EDU");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[TokenRefreshScheduler] DB query failed: " + e.getMessage());
        }
    }

    /**
     * Force an immediate one-shot balance refresh (e.g., right after a transaction).
     * Does not reset the periodic schedule.
     */
    public static void forceRefreshNow(int userId) {
        if (executor != null && !executor.isShutdown()) {
            executor.submit(() -> refreshBalance(userId));
        } else {
            // Fallback: run inline if scheduler not active
            refreshBalance(userId);
        }
    }
}
