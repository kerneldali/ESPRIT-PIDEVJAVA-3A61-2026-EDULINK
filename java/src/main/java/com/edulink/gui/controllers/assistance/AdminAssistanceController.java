package com.edulink.gui.controllers.assistance;

import com.edulink.gui.models.assistance.HelpRequest;
import com.edulink.gui.services.assistance.HelpRequestService;
import com.edulink.gui.services.assistance.SessionSummaryService;
import com.edulink.gui.services.assistance.SentimentService;
import com.edulink.gui.services.GroqService;
import com.edulink.gui.util.MyConnection;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.net.URL;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import com.edulink.gui.services.assistance.ForumService;

public class AdminAssistanceController implements Initializable {

    @FXML private Label totalRequestsLabel, openTicketsLabel, reportedLabel, bountyLabel, rateLabel, dbStatusLabel;
    @FXML private FlowPane ticketsContainer;
    @FXML private FlowPane forumReportsContainer;
    @FXML private ComboBox<String> searchByCombo;
    @FXML private TextField searchField;
    @FXML private Label avgSentimentLabel, toxicBlockedLabel, healthyCategoryLabel;

    private HelpRequestService service = new HelpRequestService();
    private ForumService forumService = new ForumService();
    private List<HelpRequest> allTickets;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Table is replaced by card layout
        searchByCombo.setItems(FXCollections.observableArrayList("Title", "Category", "Status", "Difficulty"));
        searchByCombo.getSelectionModel().selectFirst();
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterCards(newValue);
        });

        refreshAll();
        loadForumReports();
    }

    private void filterCards(String keyword) {
        if (allTickets == null) return;
        
        final String lowerKey = keyword == null ? "" : keyword.toLowerCase();
        String searchBy = searchByCombo.getValue();
        
        List<HelpRequest> filtered = allTickets.stream().filter(req -> {
            if (lowerKey.isEmpty()) return true;
            
            if ("Title".equals(searchBy) && req.getTitle() != null) {
                return req.getTitle().toLowerCase().contains(lowerKey);
            } else if ("Category".equals(searchBy) && req.getCategory() != null) {
                return req.getCategory().toLowerCase().contains(lowerKey);
            } else if ("Status".equals(searchBy) && req.getStatus() != null) {
                return req.getStatus().toLowerCase().contains(lowerKey);
            } else if ("Difficulty".equals(searchBy) && req.getDifficulty() != null) {
                return req.getDifficulty().toLowerCase().contains(lowerKey);
            }
            return false;
        }).collect(Collectors.toList());

        ticketsContainer.getChildren().clear();
        for (HelpRequest req : filtered) {
            ticketsContainer.getChildren().add(createTicketCard(req));
        }

        if (filtered.isEmpty()) {
            Label noData = new Label("No tickets matched your search criteria.");
            noData.setStyle("-fx-text-fill: gray; -fx-font-size: 14px; -fx-padding: 20;");
            ticketsContainer.getChildren().add(noData);
        }
    }

    private void handleResolve(HelpRequest req) {
        TextInputDialog dialog = new TextInputDialog("Resolved successfully.");
        dialog.setTitle("Resolve Ticket");
        dialog.setHeaderText("Resolving Ticket #" + req.getId());
        dialog.setContentText("Enter the resolution message (min 5 chars):");

        dialog.showAndWait().ifPresent(message -> {
            if (message.trim().length() < 5) {
                Alert error = new Alert(Alert.AlertType.ERROR, "Resolution message must be at least 5 characters long.");
                error.showAndWait();
                return;
            }
            if (!message.matches("^[a-zA-Z0-9\\s\\-.,!?()]+$")) {
                Alert error = new Alert(Alert.AlertType.ERROR, "Resolution message contains invalid characters.");
                error.showAndWait();
                return;
            }
            
            req.setCloseReason(message.trim());
            service.resolveTicket(req.getId());
            refreshAll();
        });
    }

    private void handleDelete(HelpRequest req) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Ticket #" + req.getId() + "?");
        confirm.setContentText("Are you sure you want to permanently delete this ticket?");
        
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                service.delete(req.getId());
                refreshAll();
            }
        });
    }

    private VBox createTicketCard(HelpRequest req) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        // Removed hardcoded background styles; relies on .card dark CSS
        card.setPrefWidth(320);

        HBox header = new HBox(5);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label idLabel = new Label("#" + req.getId());
        idLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #BB86FC; -fx-font-size: 14px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        String status = req.getStatus() != null ? req.getStatus().toUpperCase() : "OPEN";
        Label statusLabel = new Label(status);
        String statusColor = status.equals("OPEN") ? "#f59e0b" : status.equals("REPORTED") ? "#ef4444" : "#03DAC6";
        statusLabel.setStyle("-fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 12; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-color: " + statusColor + ";");
        
        header.getChildren().addAll(idLabel, spacer, statusLabel);

        Label titleLabel = new Label(req.getTitle());
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setWrapText(true);

        Label descLabel = new Label(req.getDescription() != null ? req.getDescription() : "No description provided.");
        descLabel.setWrapText(true);
        descLabel.getStyleClass().add("card-description");
        descLabel.setMaxHeight(60);

        HBox actions = new HBox(10);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        
        Button resolveBtn = new Button("✅ Resolve");
        resolveBtn.getStyleClass().add("edit-button");
        resolveBtn.setStyle("-fx-font-size: 12px; -fx-padding: 6 12;");
        resolveBtn.setOnAction(e -> handleResolve(req));

        Button deleteBtn = new Button("🗑 Delete");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setStyle("-fx-font-size: 12px; -fx-padding: 6 12;");
        deleteBtn.setOnAction(e -> handleDelete(req));

        actions.getChildren().addAll(resolveBtn, deleteBtn);

        card.getChildren().addAll(header, titleLabel, descLabel, new Separator(), actions);
        com.edulink.gui.util.ThemeManager.applyTheme(card);
        return card;
    }

    @FXML
    public void refreshAll() {
        if (!service.isConnected()) {
            dbStatusLabel.setText("Database: OFFLINE ❌");
            dbStatusLabel.setStyle("-fx-text-fill: #ef4444;");
            return;
        }
        dbStatusLabel.setText("Database: ONLINE ✅");
        dbStatusLabel.setStyle("-fx-text-fill: #10b981;");

        // Refresh Stats
        Map<String, Object> stats = service.getStats();
        totalRequestsLabel.setText(String.valueOf(stats.getOrDefault("totalRequests", 0)));
        openTicketsLabel.setText(String.valueOf(stats.getOrDefault("openRequests", 0)));
        reportedLabel.setText(String.valueOf(stats.getOrDefault("reportedTickets", 0)));
        bountyLabel.setText("$" + stats.getOrDefault("totalBounty", 0));
        rateLabel.setText(stats.getOrDefault("resolutionRate", 0) + "%");

        // Refresh Table with Cards
        allTickets = service.getAll();
        filterCards(searchField.getText());
        refreshAnalytics();
    }

    @FXML
    public void refreshAnalytics() {
        // Pull real DB data for analytics
        new Thread(() -> {
            Connection cnx = MyConnection.getInstance().getCnx();
            if (cnx == null) return;

            // 1. Toxic messages count from chat_message
            int toxicCount = 0;
            try (Statement st = cnx.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM chat_message WHERE is_toxic = 1")) {
                if (rs.next()) toxicCount = rs.getInt(1);
            } catch (Exception e) {
                System.err.println("[AdminAssistance] toxic count: " + e.getMessage());
            }

            // 2. Most active category in help requests
            String topCategory = "N/A";
            try (Statement st = cnx.createStatement();
                 ResultSet rs = st.executeQuery(
                     "SELECT category, COUNT(*) AS cnt FROM help_request WHERE category IS NOT NULL " +
                     "GROUP BY category ORDER BY cnt DESC LIMIT 1")) {
                if (rs.next()) topCategory = rs.getString("category");
            } catch (Exception e) {
                System.err.println("[AdminAssistance] top category: " + e.getMessage());
            }

            // 3. Average session quality
            double avgQuality = 0;
            try (Statement st = cnx.createStatement();
                 ResultSet rs = st.executeQuery("SELECT AVG(quality_score) FROM help_session WHERE bounty_paid = 1")) {
                if (rs.next()) avgQuality = rs.getDouble(1);
            } catch (Exception e) {
                System.err.println("[AdminAssistance] avg quality: " + e.getMessage());
            }

            final int finalToxic = toxicCount;
            final String finalCat = topCategory;
            final double finalQuality = avgQuality;
            javafx.application.Platform.runLater(() -> {
                toxicBlockedLabel.setText(String.valueOf(finalToxic));
                healthyCategoryLabel.setText(finalCat);
                String sentiment = finalQuality >= 70 ? "Positive ("+String.format("%.0f",finalQuality)+"%)" :
                                   finalQuality >= 40 ? "Neutral ("+String.format("%.0f",finalQuality)+"%)" :
                                                       "Negative (" + String.format("%.0f",finalQuality) + "%)";
                avgSentimentLabel.setText(sentiment);
            });
        }).start();
    }

    @FXML
    public void handleGenerateAiReport() {
        new Thread(() -> {
            try {
                Connection cnx = MyConnection.getInstance().getCnx();
                int totalSessions = 0, bountyPaid = 0, toxicBlocked = 0;
                String topTutor = "N/A", topCat = "N/A";

                if (cnx != null) {
                    try (Statement st = cnx.createStatement()) {
                        try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM help_session")) { if (rs.next()) totalSessions = rs.getInt(1); }
                        try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM help_session WHERE bounty_paid=1")) { if (rs.next()) bountyPaid = rs.getInt(1); }
                        try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM chat_message WHERE is_toxic=1")) { if (rs.next()) toxicBlocked = rs.getInt(1); }
                        try (ResultSet rs = st.executeQuery("SELECT u.full_name, COUNT(*) c FROM help_session hs JOIN user u ON u.id=hs.tutor_id GROUP BY hs.tutor_id ORDER BY c DESC LIMIT 1")) { if (rs.next()) topTutor = rs.getString(1); }
                        try (ResultSet rs = st.executeQuery("SELECT category, COUNT(*) c FROM help_request WHERE category IS NOT NULL GROUP BY category ORDER BY c DESC LIMIT 1")) { if (rs.next()) topCat = rs.getString(1); }
                    }
                }

                String prompt = String.format(
                    "You are the EduLink AI reporting assistant. Generate a 4-point executive summary from these platform stats:\n" +
                    "- Total tutoring sessions: %d | Bounty paid: %d\n" +
                    "- Toxic messages blocked: %d\n" +
                    "- Most active tutor: %s | Most active category: %s\n" +
                    "Provide 4 actionable insights. Keep it concise and professional.",
                    totalSessions, bountyPaid, toxicBlocked, topTutor, topCat);

                String aiReport = new GroqService().ask(prompt);
                final String report = (aiReport != null && !aiReport.isBlank()) ? aiReport :
                    "1. Sessions completed: " + totalSessions + "\n" +
                    "2. Bounties paid: " + bountyPaid + "\n" +
                    "3. Toxic messages blocked: " + toxicBlocked + "\n" +
                    "4. Top category: " + topCat;

                javafx.application.Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("AI Executive Report");
                    alert.setHeaderText("🤖 Platform Intelligence Summary");
                    alert.setContentText(report);
                    alert.getDialogPane().setPrefWidth(520);
                    alert.show();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleExportLog() {
        try {
            java.io.File file = new java.io.File("backoffice_export.csv");
            java.io.PrintWriter pw = new java.io.PrintWriter(file);
            pw.println("ID,Title,Status,Bounty,Difficulty");
            for (HelpRequest req : service.getAll()) {
                pw.printf("%d,%s,%s,%d,%s%n", req.getId(), req.getTitle(), req.getStatus(), req.getBounty(), req.getDifficulty());
            }
            pw.close();
            new Alert(Alert.AlertType.INFORMATION, "Exported to " + file.getAbsolutePath()).show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void loadForumReports() {
        if (forumReportsContainer == null) return;
        forumReportsContainer.getChildren().clear();
        List<Map<String, Object>> reports = forumService.getPendingReports();
        
        for (Map<String, Object> report : reports) {
            forumReportsContainer.getChildren().add(createReportCard(report));
        }
        
        if (reports.isEmpty()) {
            Label noData = new Label("No pending forum reports at this time. 🎉");
            noData.setStyle("-fx-text-fill: gray; -fx-font-size: 14px; -fx-padding: 20;");
            forumReportsContainer.getChildren().add(noData);
        }
    }

    private VBox createReportCard(Map<String, Object> report) {
        int reportId = (int) report.get("id");
        int postId = (int) report.get("postId");
        String postTitle = (String) report.get("postTitle");
        String reason = (String) report.get("reason");
        String reporter = (String) report.get("reporter");
        
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(320);

        HBox header = new HBox(5);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label idLabel = new Label("Report #" + reportId);
        idLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #ef4444; -fx-font-size: 14px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        header.getChildren().addAll(idLabel, spacer);

        Label titleLabel = new Label("Post: " + (postTitle != null ? postTitle : "Unknown Post"));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
        titleLabel.setWrapText(true);

        Label reporterLabel = new Label("Reporter: " + reporter);
        reporterLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");

        Label descLabel = new Label("Reason: " + reason);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #d1d5db;");
        descLabel.setMaxHeight(60);

        HBox actions = new HBox(10);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        
        Button dismissBtn = new Button("Dismiss");
        dismissBtn.setStyle("-fx-background-color: #4b5563; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 6 12;");
        dismissBtn.setOnAction(e -> {
            forumService.dismissReport(reportId);
            loadForumReports();
        });

        Button deletePostBtn = new Button("Delete Post");
        deletePostBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 6 12;");
        deletePostBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete the reported post permanently?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) {
                    forumService.actionReport(reportId, postId);
                    loadForumReports();
                }
            });
        });

        actions.getChildren().addAll(dismissBtn, deletePostBtn);

        card.getChildren().addAll(header, titleLabel, reporterLabel, descLabel, new Separator(), actions);
        com.edulink.gui.util.ThemeManager.applyTheme(card);
        return card;
    }
}
