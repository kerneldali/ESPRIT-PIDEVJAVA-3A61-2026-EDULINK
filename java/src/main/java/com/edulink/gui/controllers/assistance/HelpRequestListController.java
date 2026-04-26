package com.edulink.gui.controllers.assistance;

import com.edulink.gui.models.assistance.HelpRequest;
import com.edulink.gui.models.assistance.HelpSession;
import com.edulink.gui.services.assistance.HelpRequestService;
import com.edulink.gui.services.assistance.HelpSessionService;
import com.edulink.gui.services.assistance.SmartMatchingService;
import com.edulink.gui.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class HelpRequestListController implements Initializable {

    @FXML private FlowPane cardsContainer;
    @FXML private Label statusLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCombo;
    
    private HelpRequestService service = new HelpRequestService();
    private HelpSessionService sessionService = new HelpSessionService();
    private List<HelpRequest> allRequestsCache = new java.util.ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        filterCombo.getItems().addAll("All Requests", "Open Only", "Reported Tickets", "High Bounty (>$50)");
        filterCombo.setValue("All Requests");

        // Real-time Search Listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        
        // Filter Listener
        filterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        loadData();
    }

    @FXML
    public void loadData() {
        if (!service.isConnected()) {
            statusLabel.setText("Database Not Connected");
            return;
        }

        allRequestsCache = service.getAll();
        applyFilters();
    }

    private void applyFilters() {
        cardsContainer.getChildren().clear();
        String searchText = searchField.getText().toLowerCase().trim();
        String filter = filterCombo.getValue();

        List<HelpRequest> filtered = allRequestsCache.stream()
            .filter(req -> {
                // Search check
                boolean matchesSearch = req.getTitle().toLowerCase().contains(searchText) || 
                                       req.getDescription().toLowerCase().contains(searchText);
                
                // Status filter check
                boolean matchesFilter = true;
                if ("Open Only".equals(filter)) matchesFilter = "OPEN".equals(req.getStatus());
                else if ("Reported Tickets".equals(filter)) matchesFilter = req.isTicket();
                else if ("High Bounty (>$50)".equals(filter)) matchesFilter = req.getBounty() > 50;
                
                return matchesSearch && matchesFilter;
            })
            .collect(Collectors.toList());

        for (HelpRequest req : filtered) {
            cardsContainer.getChildren().add(createCard(req));
        }
        
        if (filtered.isEmpty()) {
            showEmptyCard();
        }
        
        statusLabel.setText(filtered.size() + " Request(s) matching your criteria");
    }

    @FXML
    public void handleExportCSV() {
        try {
            java.io.File file = new java.io.File("help_requests_export.csv");
            java.io.PrintWriter pw = new java.io.PrintWriter(file);
            pw.println("ID,Title,Category,Status,Bounty,IsTicket,CreatedAt");
            for (HelpRequest req : allRequestsCache) {
                pw.printf("%d,%s,%s,%s,%d,%b,%s%n", 
                    req.getId(), 
                    escapeCSV(req.getTitle()), 
                    escapeCSV(req.getCategory()), 
                    req.getStatus(), 
                    req.getBounty(), 
                    req.isTicket(), 
                    req.getCreatedAt());
            }
            pw.close();
            showAlert("Export Success", "Saved to: " + file.getAbsolutePath());
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String escapeCSV(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    @FXML
    public void handleExportPDF() {
        try {
            java.io.File dest = new java.io.File("help_requests_export.pdf");
            com.edulink.gui.services.PdfExportService pdfService = new com.edulink.gui.services.PdfExportService();
            pdfService.exportHelpRequests(allRequestsCache, dest);
            showAlert("PDF Export Success", "Saved to: " + dest.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("PDF Export Failed", e.getMessage());
        }
    }

    private void showAlert(String t, String c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t);
        a.setContentText(c);
        a.show();
    }

    private VBox createCard(HelpRequest req) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(320); // Slightly wider for pro look
        card.setMinHeight(220);

        // Header: Badge + Category
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label status = new Label(safeStr(req.getStatus()));
        status.getStyleClass().addAll("badge", "badge-" + safeStr(req.getStatus()).toLowerCase().replace("_", "-"));
        
        Label category = new Label(safeStr(req.getCategory()).isEmpty() ? "No Category" : req.getCategory());
        category.getStyleClass().addAll("badge", "badge-category");
        header.getChildren().addAll(status, category);

        // Body: Title + Desc
        Label title = new Label(safeStr(req.getTitle()));
        title.getStyleClass().add("card-title");
        title.setWrapText(true);

        Label desc = new Label(truncate(safeStr(req.getDescription()), 90));
        desc.getStyleClass().add("card-description");
        desc.setWrapText(true);

        // Details Row
        HBox details = new HBox(15);
        Label bounty = new Label("💰 $" + req.getBounty());
        bounty.getStyleClass().add("card-bounty");
        Label difficulty = new Label("📊 " + safeStr(req.getDifficulty()));
        difficulty.getStyleClass().add("card-info");
        details.getChildren().addAll(bounty, difficulty);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Actions
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        Button editBtn = new Button("✏");
        editBtn.getStyleClass().add("edit-button");
        editBtn.setOnAction(e -> handleEdit(req));
        
        Button delBtn = new Button("🗑");
        delBtn.getStyleClass().add("delete-button");
        delBtn.setOnAction(e -> handleDelete(req));
        
        // Add session logic UI
        int currentUserId = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : -1;
        boolean isOwner = (req.getStudentId() != null && req.getStudentId() == currentUserId);

        if ("OPEN".equals(req.getStatus())) {
            if (isOwner) {
                Button findBtn = new Button("Find Tutors");
                findBtn.setStyle("-fx-background-color:#8b5cf6;-fx-text-fill:white;-fx-background-radius:20;");
                findBtn.setOnAction(e -> handleFindTutors(req));
                actions.getChildren().add(findBtn);
            } else {
                Button joinBtn = new Button("Join as Tutor");
                joinBtn.setStyle("-fx-background-color:#10b981;-fx-text-fill:white;-fx-background-radius:20;");
                joinBtn.setOnAction(e -> handleJoinAsTutor(req));
                actions.getChildren().add(joinBtn);
            }
        } else if ("IN_PROGRESS".equals(req.getStatus())) {
            Button viewBtn = new Button("View Session");
            viewBtn.setStyle("-fx-background-color:#3b82f6;-fx-text-fill:white;-fx-background-radius:20;");
            viewBtn.setOnAction(e -> handleViewSession(req));
            actions.getChildren().add(viewBtn);
        }

        actions.getChildren().addAll(editBtn, delBtn);

        card.getChildren().addAll(header, title, desc, details, spacer, actions);
        com.edulink.gui.util.ThemeManager.applyTheme(card);
        return card;
    }

    private void handleEdit(HelpRequest req) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/assistance/HelpRequestForm.fxml"));
            Parent view = loader.load();
            HelpRequestFormController controller = loader.getController();
            controller.setHelpRequest(req);
            StackPane contentArea = findContentArea();
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    public void handleAddRequest() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/assistance/HelpRequestForm.fxml"));
            Parent view = loader.load();
            StackPane contentArea = findContentArea();
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void handleDelete(HelpRequest req) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + req.getTitle() + "?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                service.delete(req.getId());
                loadData();
            }
        });
    }

    /** Student opens a chat session that already exists for this request */
    private void handleViewSession(HelpRequest req) {
        HelpSession session = sessionService.getActiveSessionForRequest(req.getId());
        if (session == null) {
            showAlert("No active session", "There is no active session for this request.");
            return;
        }
        openChat(session);
    }

    private void handleFindTutors(HelpRequest req) {
        SmartMatchingService matcher = new SmartMatchingService();
        List<SmartMatchingService.TutorMatch> matches = matcher.findMatches(req.getCategory(), req.getDifficulty(), req.getDescription());
        
        if (matches.isEmpty()) {
            showAlert("No Matches", "Could not find any suitable tutors at this moment.");
            return;
        }

        StringBuilder sb = new StringBuilder("Top Tutors for your request:\n\n");
        for (SmartMatchingService.TutorMatch m : matches) {
            sb.append("• ").append(m.fullName).append(" (").append(m.sessionsCompleted).append(" sessions)\n");
            sb.append("  Rating: ").append(String.format("%.1f", m.avgQuality)).append("/100\n");
            sb.append("  AI Note: ").append(m.aiReason).append("\n\n");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Recommended Tutors");
        alert.setHeaderText("AI Matching Results");
        alert.setContentText(sb.toString());
        alert.showAndWait();
    }

    /** Non-owner tutor joins an open request */
    private void handleJoinAsTutor(HelpRequest req) {
        int currentUserId = SessionManager.getCurrentUser() != null
            ? SessionManager.getCurrentUser().getId() : -1;
        if (currentUserId <= 0) {
            showAlert("Not logged in", "Please log in first.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Join as Tutor");
        confirm.setHeaderText("Join \"" + req.getTitle() + "\"?");
        confirm.setContentText("The bounty of " + req.getBounty() + " EDU will be released to you if the session passes the quality check.");
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                try {
                    HelpSession session = sessionService.createSession(
                        req.getId(), currentUserId,
                        req.getStudentId() != null ? req.getStudentId() : 1,
                        req.getBounty());
                    if (session != null) {
                        openChat(session);
                        loadData(); // refresh list
                    }
                } catch (RuntimeException ex) {
                    // Show the explicit refusal reason (Daily limit, Insufficient funds, etc.)
                    Alert refusal = new Alert(Alert.AlertType.WARNING);
                    refusal.setTitle("Session Refused");
                    refusal.setHeaderText("Tutoring Policy Violation");
                    refusal.setContentText(ex.getMessage());
                    refusal.showAndWait();
                }
            }
        });
    }

    private void openChat(HelpSession session) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/assistance/Chat.fxml"));
            Parent view = loader.load();
            com.edulink.gui.controllers.assistance.ChatController ctrl = loader.getController();
            ctrl.setSession(session);
            StackPane contentArea = findContentArea();
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private String safeStr(String s) { return (s == null) ? "" : s; }
    private String truncate(String s, int n) { return s.length() > n ? s.substring(0, n) + "..." : s; }

    private void showEmptyCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);
        card.getChildren().add(new Label("No requests found."));
        cardsContainer.getChildren().add(card);
    }



    private StackPane findContentArea() {
        javafx.scene.Node n = cardsContainer;
        while (n != null) {
            if (n instanceof StackPane && "contentArea".equals(n.getId())) return (StackPane) n;
            n = n.getParent();
        }
        return null;
    }
}
