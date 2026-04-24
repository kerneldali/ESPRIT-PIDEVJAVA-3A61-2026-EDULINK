package com.edulink.gui.controllers.courses;

import com.edulink.gui.models.courses.ContentProposal;
import com.edulink.gui.services.courses.ContentProposalService;
import com.edulink.gui.util.EduAlert;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ManageSuggestionsController implements Initializable {

    @FXML private VBox proposalsContainer;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<String> statusFilter;


    private List<ContentProposal> allProposals;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        typeFilter.setItems(FXCollections.observableArrayList("ALL", "MATIERE", "COURSE", "RESOURCE"));
        typeFilter.setValue("ALL");

        statusFilter.setItems(FXCollections.observableArrayList("ALL", "PENDING", "ACCEPTED", "REJECTED"));
        statusFilter.setValue("ALL");

        typeFilter.valueProperty().addListener((obs, o, n) -> filterAndDisplay());
        statusFilter.valueProperty().addListener((obs, o, n) -> filterAndDisplay());

        loadData();
    }

    private void loadData() {
        allProposals = new java.util.ArrayList<>();
        for (com.edulink.gui.models.courses.Matiere m : new com.edulink.gui.services.courses.MatiereService().getAll()) {
            ContentProposal p = new ContentProposal();
            p.setId(m.getId());
            p.setType("MATIERE");
            p.setTitle(m.getName() != null ? m.getName() : "Untitled");
            p.setStatus(m.getStatus());
            p.setCreatedAt(m.getCreatedAt());
            p.setSuggestedBy(m.getCreatorId());
            allProposals.add(p);
        }
        for (com.edulink.gui.models.courses.Course c : new com.edulink.gui.services.courses.CourseService().getAll()) {
            ContentProposal p = new ContentProposal();
            p.setId(c.getId());
            p.setType("COURSE");
            p.setTitle(c.getTitle());
            p.setDescription(c.getDescription());
            p.setStatus(c.getStatus());
            p.setCreatedAt(c.getCreatedAt());
            p.setSuggestedBy(c.getAuthorId());
            allProposals.add(p);
        }
        for (com.edulink.gui.models.courses.Resource r : new com.edulink.gui.services.courses.ResourceService().getAll()) {
            ContentProposal p = new ContentProposal();
            p.setId(r.getId());
            p.setType("RESOURCE");
            p.setTitle(r.getTitle());
            p.setDescription(r.getUrl());
            p.setStatus(r.getStatus());
            p.setSuggestedBy(r.getAuthorId());
            p.setCreatedAt(java.time.LocalDateTime.now());
            allProposals.add(p);
        }
        
        // Sort newest first
        allProposals.sort((a, b) -> (b.getCreatedAt() == null ? java.time.LocalDateTime.MIN : b.getCreatedAt())
                .compareTo(a.getCreatedAt() == null ? java.time.LocalDateTime.MIN : a.getCreatedAt()));

        filterAndDisplay();
    }

    private void filterAndDisplay() {
        proposalsContainer.getChildren().clear();
        String tValue = typeFilter.getValue();
        String sValue = statusFilter.getValue();

        int count = 0;
        for (ContentProposal p : allProposals) {
            boolean mType = "ALL".equals(tValue) || tValue.equals(p.getType());
            boolean mStatus = "ALL".equals(sValue) || sValue.equals(p.getStatus());

            if (mType && mStatus) {
                proposalsContainer.getChildren().add(createCard(p));
                count++;
            }
        }

        if (count == 0) {
            Label nil = new Label("No proposals match the current filter.");
            nil.setStyle("-fx-text-fill: #a0a0ab;");
            proposalsContainer.getChildren().add(nil);
        }
    }

    private VBox createCard(ContentProposal p) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 15; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-color: #ffffff11;");

        // Header with title + type badge + status
        HBox top = new HBox(10);
        top.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label title = new Label(p.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 16px;");

        Label typeBadge = new Label(p.getType());
        String badgeColor = "#3b82f6";
        if ("COURSE".equals(p.getType())) badgeColor = "#7c3aed";
        else if ("RESOURCE".equals(p.getType())) badgeColor = "#f59e0b";
        typeBadge.setStyle("-fx-background-color: " + badgeColor + "; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 10px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = new Label(p.getStatus() != null ? p.getStatus() : "UNKNOWN");
        if ("PENDING".equals(p.getStatus())) status.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
        else if ("ACCEPTED".equals(p.getStatus())) status.setStyle("-fx-text-fill: #00d289; -fx-font-weight: bold;");
        else status.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");

        top.getChildren().addAll(title, typeBadge, spacer, status);

        // Description
        Label desc = new Label(p.getDescription() != null ? p.getDescription() : "No description");
        desc.setStyle("-fx-text-fill: #a0a0ab;");
        desc.setWrapText(true);

        // Submitted by + date
        Label meta = new Label("Submitted by User #" + p.getSuggestedBy() + " • " +
                (p.getCreatedAt() != null ? p.getCreatedAt().toString() : "Unknown date"));
        meta.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        // Action buttons
        HBox actions = new HBox(10);
        Button viewBtn = new Button("👁 View Details");
        viewBtn.setStyle("-fx-background-color: #3b82f633; -fx-text-fill: #3b82f6; -fx-background-radius: 5; -fx-cursor: hand;");
        viewBtn.setOnAction(e -> {
            String details = "Status: " + p.getStatus() + "\n\n" +
                    "Description:\n" + (p.getDescription() != null ? p.getDescription() : "N/A") + "\n\n" +
                    "Submitted by: User #" + p.getSuggestedBy() + "\n" +
                    "Date: " + (p.getCreatedAt() != null ? p.getCreatedAt().toString() : "N/A");
            EduAlert.show(EduAlert.AlertType.INFO, p.getTitle() + " (" + p.getType() + ")", details);
        });

        Button acceptBtn = new Button("✅ Accept");
        acceptBtn.setStyle("-fx-background-color: #00d289; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
        acceptBtn.setOnAction(e -> {
            updateStatus(p, "ACCEPTED");
            EduAlert.show(EduAlert.AlertType.SUCCESS, "Accepted", "Proposal '" + p.getTitle() + "' was marked as approved.");
            loadData();
        });

        Button rejectBtn = new Button("❌ Reject");
        rejectBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
        rejectBtn.setOnAction(e -> {
            updateStatus(p, "REJECTED");
            EduAlert.show(EduAlert.AlertType.WARNING, "Rejected", "Proposal '" + p.getTitle() + "' has been rejected.");
            loadData();
        });

        if (!"PENDING".equals(p.getStatus())) {
            acceptBtn.setDisable(true);
            rejectBtn.setDisable(true);
        }

        actions.getChildren().addAll(viewBtn, acceptBtn, rejectBtn);
        card.getChildren().addAll(top, desc, meta, actions);
        com.edulink.gui.util.ThemeManager.applyTheme(card);
        return card;
    }

    private void updateStatus(ContentProposal p, String newStatus) {
        if ("MATIERE".equals(p.getType())) {
            com.edulink.gui.services.courses.MatiereService ms = new com.edulink.gui.services.courses.MatiereService();
            com.edulink.gui.models.courses.Matiere m = ms.getAll().stream().filter(x -> x.getId() == p.getId()).findFirst().orElse(null);
            if (m != null) {
                m.setStatus(newStatus);
                ms.edit(m);
            }
        } else if ("COURSE".equals(p.getType())) {
            com.edulink.gui.services.courses.CourseService cs = new com.edulink.gui.services.courses.CourseService();
            com.edulink.gui.models.courses.Course c = cs.getAll().stream().filter(x -> x.getId() == p.getId()).findFirst().orElse(null);
            if (c != null) {
                c.setStatus(newStatus);
                cs.edit(c);
            }
        } else if ("RESOURCE".equals(p.getType())) {
            com.edulink.gui.services.courses.ResourceService rs = new com.edulink.gui.services.courses.ResourceService();
            com.edulink.gui.models.courses.Resource r = rs.getAll().stream().filter(x -> x.getId() == p.getId()).findFirst().orElse(null);
            if (r != null) {
                r.setStatus(newStatus);
                rs.edit(r);
            }
        }
    }
}
