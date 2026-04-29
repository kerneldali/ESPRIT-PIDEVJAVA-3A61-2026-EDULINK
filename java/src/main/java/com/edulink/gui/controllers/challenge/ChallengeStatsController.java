package com.edulink.gui.controllers.challenge;

import com.edulink.gui.services.challenge.ChallengeService;
import com.edulink.gui.services.challenge.ChallengeService.AutoDecisionResult;
import com.edulink.gui.util.MyConnection;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class ChallengeStatsController implements Initializable {

    @FXML private Label totalChallengesLbl;
    @FXML private Label totalParticipationsLbl;
    @FXML private Label totalXpLbl;
    @FXML private Label completionRateLbl;
    @FXML private VBox topStudentsContainer;
    @FXML private VBox perChallengeContainer;
    @FXML private Label autoDecisionResultLbl;

    private Connection cnx;
    private final ChallengeService challengeService = new ChallengeService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cnx = MyConnection.getInstance().getCnx();
        loadStats();
    }

    private void loadStats() {
        loadTopCards();
        loadTopStudents();
        loadPerChallenge();
    }

    private void loadTopCards() {
        try {
            // Total challenges
            ResultSet rs = cnx.createStatement().executeQuery("SELECT COUNT(*) FROM challenge");
            if (rs.next()) totalChallengesLbl.setText(String.valueOf(rs.getInt(1)));

            // Total participations
            rs = cnx.createStatement().executeQuery("SELECT COUNT(*) FROM challenge_participation");
            if (rs.next()) totalParticipationsLbl.setText(String.valueOf(rs.getInt(1)));

            // Total XP distributed (from completed participations joined with challenge)
            rs = cnx.createStatement().executeQuery(
                "SELECT COALESCE(SUM(c.xp_reward),0) FROM challenge_participation cp " +
                "JOIN challenge c ON cp.challenge_id = c.id WHERE cp.status='COMPLETED'");
            if (rs.next()) totalXpLbl.setText(rs.getInt(1) + " XP");

            // Completion rate
            rs = cnx.createStatement().executeQuery(
                "SELECT COUNT(*) FROM challenge_participation");
            int total = 0;
            if (rs.next()) total = rs.getInt(1);
            rs = cnx.createStatement().executeQuery(
                "SELECT COUNT(*) FROM challenge_participation WHERE status='COMPLETED'");
            int completed = 0;
            if (rs.next()) completed = rs.getInt(1);
            String rate = total > 0 ? String.format("%.0f%%", (completed * 100.0 / total)) : "0%";
            completionRateLbl.setText(rate);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadTopStudents() {
        topStudentsContainer.getChildren().clear();

        // Header row
        HBox header = new HBox(10);
        header.setStyle("-fx-padding: 8 12; -fx-border-color: transparent transparent #ffffff11 transparent;");
        addCell(header, "#",      40,  "#a0a0ab", true);
        addCell(header, "Étudiant", 200, "#a0a0ab", true);
        addCell(header, "XP Total", 100, "#a0a0ab", true);
        addCell(header, "Complétés", 90, "#a0a0ab", true);
        topStudentsContainer.getChildren().add(header);

        try {
            String sql = "SELECT u.nom, u.prenom, u.email, " +
                         "COALESCE(SUM(CASE WHEN cp.status='COMPLETED' THEN c.xp_reward ELSE 0 END),0) AS total_xp, " +
                         "COUNT(CASE WHEN cp.status='COMPLETED' THEN 1 END) AS completed_count " +
                         "FROM user u " +
                         "LEFT JOIN challenge_participation cp ON u.id = cp.user_id " +
                         "LEFT JOIN challenge c ON cp.challenge_id = c.id " +
                         "GROUP BY u.id, u.nom, u.prenom, u.email " +
                         "ORDER BY total_xp DESC LIMIT 5";
            ResultSet rs = cnx.createStatement().executeQuery(sql);
            int rank = 1;
            while (rs.next()) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                String bg = rank == 1 ? "#00d28915" : rank == 2 ? "#7c3aed15" : "transparent";
                row.setStyle("-fx-padding: 10 12; -fx-background-color: " + bg + "; -fx-background-radius: 6;");

                String medal = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : String.valueOf(rank);
                addCell(row, medal, 40, "white", false);

                String name = rs.getString("nom") + " " + rs.getString("prenom");
                if (name.isBlank() || name.equals("null null")) name = rs.getString("email");
                addCell(row, name, 200, "white", false);
                addCell(row, rs.getInt("total_xp") + " XP", 100, "#00d289", false);
                addCell(row, String.valueOf(rs.getInt("completed_count")), 90, "#a0a0ab", false);

                topStudentsContainer.getChildren().add(row);
                rank++;
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadPerChallenge() {
        perChallengeContainer.getChildren().clear();

        // Header
        HBox header = new HBox(10);
        header.setStyle("-fx-padding: 8 12; -fx-border-color: transparent transparent #ffffff11 transparent;");
        addCell(header, "Challenge",   220, "#a0a0ab", true);
        addCell(header, "Difficulté",  100, "#a0a0ab", true);
        addCell(header, "Inscrits",    80,  "#a0a0ab", true);
        addCell(header, "Complétés",   90,  "#a0a0ab", true);
        addCell(header, "Taux",        150, "#a0a0ab", true);
        perChallengeContainer.getChildren().add(header);

        try {
            String sql = "SELECT c.title, c.difficulty, c.featured, c.xp_boost_pct, c.boost_until, " +
                         "COUNT(cp.id) AS total, " +
                         "COUNT(CASE WHEN cp.status='COMPLETED' THEN 1 END) AS completed " +
                         "FROM challenge c " +
                         "LEFT JOIN challenge_participation cp ON c.id = cp.challenge_id " +
                         "GROUP BY c.id, c.title, c.difficulty, c.featured, c.xp_boost_pct, c.boost_until " +
                         "ORDER BY c.featured DESC, total DESC";
            ResultSet rs = cnx.createStatement().executeQuery(sql);
            while (rs.next()) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 9 12; -fx-border-color: transparent transparent #ffffff08 transparent;");

                // Title cell with optional FEATURED / BOOST badges.
                HBox titleCell = new HBox(6);
                titleCell.setAlignment(Pos.CENTER_LEFT);
                titleCell.setPrefWidth(220);
                Label titleLbl = new Label(rs.getString("title"));
                titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
                titleCell.getChildren().add(titleLbl);

                int featured = 0;
                try { featured = rs.getInt("featured"); } catch (SQLException ignore) {}
                if (featured == 1) {
                    Label feat = new Label("⭐ FEATURED");
                    feat.setStyle("-fx-background-color: #f59e0b22; -fx-text-fill: #f59e0b; " +
                            "-fx-padding: 1 6; -fx-background-radius: 6; " +
                            "-fx-font-size: 9px; -fx-font-weight: bold;");
                    titleCell.getChildren().add(feat);
                }

                int boost = 0;
                java.sql.Timestamp boostUntilTs = null;
                try {
                    boost = rs.getInt("xp_boost_pct");
                    boostUntilTs = rs.getTimestamp("boost_until");
                } catch (SQLException ignore) {}
                boolean boostActive = boost > 0 && boostUntilTs != null
                        && boostUntilTs.toLocalDateTime().isAfter(java.time.LocalDateTime.now());
                if (boostActive) {
                    Label bo = new Label("+" + boost + "% BOOST");
                    bo.setStyle("-fx-background-color: #00d28922; -fx-text-fill: #00d289; " +
                            "-fx-padding: 1 6; -fx-background-radius: 6; " +
                            "-fx-font-size: 9px; -fx-font-weight: bold;");
                    titleCell.getChildren().add(bo);
                }

                row.getChildren().add(titleCell);

                // Difficulty badge
                String diff = rs.getString("difficulty");
                String diffColor = "HARD".equals(diff) ? "#ef4444" : "MEDIUM".equals(diff) ? "#f59e0b" : "#3b82f6";
                Label diffLbl = new Label(diff);
                diffLbl.setPrefWidth(100);
                diffLbl.setStyle("-fx-background-color: " + diffColor + "33; -fx-text-fill: " + diffColor + "; " +
                        "-fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");
                row.getChildren().add(diffLbl);

                int total = rs.getInt("total");
                int completed = rs.getInt("completed");
                addCell(row, String.valueOf(total),     80,  "#a0a0ab", false);
                addCell(row, String.valueOf(completed), 90,  "#00d289", false);

                // Progress bar
                double pct = total > 0 ? (completed * 100.0 / total) : 0;
                StackPane bar = buildBar(pct, 150);
                row.getChildren().add(bar);

                perChallengeContainer.getChildren().add(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void addCell(HBox row, String text, double width, String color, boolean bold) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(width);
        lbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px;" +
                     (bold ? " -fx-font-weight: bold;" : ""));
        lbl.setWrapText(false);
        row.getChildren().add(lbl);
    }

    private StackPane buildBar(double pct, double width) {
        StackPane stack = new StackPane();
        stack.setPrefWidth(width);
        stack.setAlignment(Pos.CENTER_LEFT);

        HBox bg = new HBox();
        bg.setPrefWidth(width);
        bg.setPrefHeight(10);
        bg.setStyle("-fx-background-color: #ffffff15; -fx-background-radius: 5;");

        HBox fill = new HBox();
        fill.setPrefWidth(width * pct / 100);
        fill.setPrefHeight(10);
        String color = pct >= 70 ? "#00d289" : pct >= 40 ? "#f59e0b" : "#ef4444";
        fill.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 5;");

        Label pctLbl = new Label(String.format("%.0f%%", pct));
        pctLbl.setStyle("-fx-text-fill: #a0a0ab; -fx-font-size: 11px;");
        pctLbl.setTranslateX(width + 6);

        stack.getChildren().addAll(bg, fill, pctLbl);
        return stack;
    }

    @FXML
    private void handleRefresh() {
        loadStats();
    }

    /**
     * Triggers the stat-driven auto-decision engine and reflects the outcome
     * in the UI. The actual logic lives in ChallengeService.runAutoDecisions()
     * — keeping this handler thin lets the same engine be reused (e.g. by a
     * scheduled job later) without any UI dependency.
     */
    @FXML
    private void handleApplyAutoDecisions() {
        AutoDecisionResult result = challengeService.runAutoDecisions();

        StringBuilder ui = new StringBuilder();
        if (result.summary != null) ui.append(result.summary);
        if (result.boostUntil != null && !result.boostedTitles.isEmpty()) {
            ui.append("  Boost actif jusqu'au ")
              .append(result.boostUntil.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
              .append(".");
        }
        if (!result.boostedTitles.isEmpty()) {
            ui.append("\nChallenges boostés : ")
              .append(String.join(", ", result.boostedTitles));
        }

        if (autoDecisionResultLbl != null) {
            autoDecisionResultLbl.setText(ui.toString());
        }
        // Refresh the stats panels so the user immediately sees the new ordering
        // / featured state if the data is reflected later in the UI.
        loadStats();
    }
}
