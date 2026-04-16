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

    private ContentProposalService service = new ContentProposalService();
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
        allProposals = service.getAll();
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

        Label status = new Label(p.getStatus());
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
            p.setStatus("ACCEPTED");
            service.edit(p);
            
            // Logic to convert proposal into actual content
            if ("MATIERE".equals(p.getType())) {
                com.edulink.gui.models.courses.Matiere m = new com.edulink.gui.models.courses.Matiere();
                m.setName(p.getTitle());
                m.setStatus("ACTIVE");
                m.setCreatorId(p.getSuggestedBy());
                m.setCreatedAt(java.time.LocalDateTime.now());
                new com.edulink.gui.services.courses.MatiereService().add(m);
                EduAlert.show(EduAlert.AlertType.SUCCESS, "Success", "Proposal accepted and '" + p.getTitle() + "' was added to the catalogue.");
            } else if ("COURSE".equals(p.getType())) {
                com.edulink.gui.models.courses.Course c = new com.edulink.gui.models.courses.Course();
                c.setTitle(p.getTitle());
                c.setDescription(p.getDescription());
                c.setStatus("ACTIVE");
                c.setAuthorId(1); // Default to admin author to avoid FK issues
                c.setCreatedAt(java.time.LocalDateTime.now());
                
                String d = p.getDescription();
                if (d.contains("[Level: ")) {
                    String lvl = d.substring(d.indexOf("[Level: ") + 8, d.indexOf(",", d.indexOf("[Level: ")));
                    c.setLevel(lvl.trim());
                } else { c.setLevel("BEGINNER"); }
                
                if (d.contains(", XP: ")) {
                    String xpStr = d.substring(d.indexOf(", XP: ") + 6, d.indexOf("]", d.indexOf(", XP: ")));
                    try { c.setXp(Integer.parseInt(xpStr.trim())); } catch(Exception ex) { c.setXp(50); }
                } else { c.setXp(100); }

                if (d.contains("[Category: ")) {
                    String catName = d.substring(d.indexOf("[Category: ") + 11, d.indexOf("]", d.indexOf("[Category: ")));
                    com.edulink.gui.services.courses.MatiereService ms = new com.edulink.gui.services.courses.MatiereService();
                    ms.getAll().stream()
                        .filter(m -> m.getName().equalsIgnoreCase(catName.trim()))
                        .findFirst()
                        .ifPresent(m -> c.setMatiereId(m.getId()));
                }
                
                new com.edulink.gui.services.courses.CourseService().add(c);
                EduAlert.show(EduAlert.AlertType.SUCCESS, "Success", "Course '" + p.getTitle() + "' was added to the corresponding category.");
            } else if ("RESOURCE".equals(p.getType())) {
                com.edulink.gui.models.courses.Resource r = new com.edulink.gui.models.courses.Resource();
                r.setTitle(p.getTitle());
                r.setStatus("ACTIVE");
                r.setAuthorId(1);
                
                String d = p.getDescription();
                if (d.contains("[Type: ")) {
                    r.setType(d.substring(d.indexOf("[Type: ") + 7, d.indexOf("]", d.indexOf("[Type: "))).trim());
                }
                if (d.contains("[URL: ")) {
                    r.setUrl(d.substring(d.indexOf("[URL: ") + 6, d.indexOf("]", d.indexOf("[URL: "))).trim());
                }
                if (d.contains("[Course: ")) {
                    String courseName = d.substring(d.indexOf("[Course: ") + 9, d.indexOf("]", d.indexOf("[Course: ")));
                    com.edulink.gui.services.courses.CourseService cs = new com.edulink.gui.services.courses.CourseService();
                    cs.getAll().stream()
                        .filter(co -> co.getTitle().equalsIgnoreCase(courseName.trim()))
                        .findFirst()
                        .ifPresent(co -> r.setCoursId(co.getId()));
                }
                
                new com.edulink.gui.services.courses.ResourceService().add(r);
                EduAlert.show(EduAlert.AlertType.SUCCESS, "Success", "Resource '" + p.getTitle() + "' was added to the database.");
            } else {
                EduAlert.show(EduAlert.AlertType.SUCCESS, "Accepted", "Proposal '" + p.getTitle() + "' was marked as approved.");
            }
            
            loadData();
        });

        Button rejectBtn = new Button("❌ Reject");
        rejectBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
        rejectBtn.setOnAction(e -> {
            p.setStatus("REJECTED");
            service.edit(p);
            EduAlert.show(EduAlert.AlertType.WARNING, "Rejected", "Proposal '" + p.getTitle() + "' has been rejected.");
            loadData();
        });

        if (!"PENDING".equals(p.getStatus())) {
            acceptBtn.setDisable(true);
            rejectBtn.setDisable(true);
        }

        actions.getChildren().addAll(viewBtn, acceptBtn, rejectBtn);
        card.getChildren().addAll(top, desc, meta, actions);
        return card;
    }
}
