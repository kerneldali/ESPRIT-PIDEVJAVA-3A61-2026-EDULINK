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

public class StudentSuggestionsController implements Initializable {

    @FXML private StackPane rootPane;
    @FXML private FlowPane coursesContainer;
    @FXML private VBox categoriesContainer;
    @FXML private VBox resourcesContainer;
    @FXML private ComboBox<String> statusFilter;

    // Overlay form
    @FXML private VBox formOverlay;
    @FXML private Label formTitle;
    @FXML private TextField titleField;
    @FXML private TextArea descField;
    @FXML private HBox courseMetaRow;
    @FXML private ComboBox<String> levelCombo;
    @FXML private TextField xpField;
    @FXML private Button saveBtn;

    private ContentProposalService proposalService = new ContentProposalService();
    private List<ContentProposal> allProposals;
    private ContentProposal currentEditingProposal;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statusFilter.setItems(FXCollections.observableArrayList("ALL", "PENDING", "ACCEPTED", "REJECTED"));
        statusFilter.setValue("ALL");
        statusFilter.valueProperty().addListener((obs, old, newVal) -> renderProposals());

        levelCombo.setItems(FXCollections.observableArrayList("BEGINNER", "INTERMEDIATE", "ADVANCED"));
        levelCombo.setValue("BEGINNER");

        loadData();
    }

    @FXML
    public void loadData() {
        int studentId = 1;
        if(com.edulink.gui.util.SessionManager.getCurrentUser() != null) {
            studentId = com.edulink.gui.util.SessionManager.getCurrentUser().getId();
        }
        allProposals = new java.util.ArrayList<>();
        int finalStudentId = studentId;

        for (com.edulink.gui.models.courses.Matiere m : new com.edulink.gui.services.courses.MatiereService().getAll()) {
            if (m.getCreatorId() == finalStudentId) {
                ContentProposal p = new ContentProposal();
                p.setId(m.getId());
                p.setType("MATIERE");
                p.setTitle(m.getName() != null ? m.getName() : "Untitled");
                p.setStatus(m.getStatus());
                p.setCreatedAt(m.getCreatedAt());
                p.setSuggestedBy(m.getCreatorId());
                allProposals.add(p);
            }
        }
        for (com.edulink.gui.models.courses.Course c : new com.edulink.gui.services.courses.CourseService().getAll()) {
            if (c.getAuthorId() == finalStudentId) {
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
        }
        for (com.edulink.gui.models.courses.Resource r : new com.edulink.gui.services.courses.ResourceService().getAll()) {
            if (r.getAuthorId() == finalStudentId) {
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
        }
        
        allProposals.sort((a, b) -> (b.getCreatedAt() == null ? java.time.LocalDateTime.MIN : b.getCreatedAt())
                .compareTo(a.getCreatedAt() == null ? java.time.LocalDateTime.MIN : a.getCreatedAt()));

        renderProposals();
    }

    private void renderProposals() {
        coursesContainer.getChildren().clear();
        categoriesContainer.getChildren().clear();
        resourcesContainer.getChildren().clear();

        String statusVal = statusFilter.getValue();
        List<ContentProposal> filtered = allProposals.stream()
                .filter(p -> "ALL".equals(statusVal) || statusVal.equals(p.getStatus()))
                .toList();

        List<ContentProposal> courses = filtered.stream().filter(p -> "COURSE".equals(p.getType())).toList();
        List<ContentProposal> categories = filtered.stream().filter(p -> "MATIERE".equals(p.getType())).toList();
        List<ContentProposal> resources = filtered.stream().filter(p -> "RESOURCE".equals(p.getType())).toList();

        if (courses.isEmpty()) coursesContainer.getChildren().add(createEmptyLabel("No course proposals."));
        else courses.forEach(p -> coursesContainer.getChildren().add(createCard(p)));

        if (categories.isEmpty()) categoriesContainer.getChildren().add(createEmptyLabel("No category proposals."));
        else categories.forEach(p -> categoriesContainer.getChildren().add(createListRow(p)));

        if (resources.isEmpty()) resourcesContainer.getChildren().add(createEmptyLabel("No resource proposals."));
        else resources.forEach(p -> resourcesContainer.getChildren().add(createListRow(p)));
    }

    private Label createEmptyLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #a0a0ab; -fx-font-style: italic;");
        return l;
    }

    private VBox createCard(ContentProposal p) {
        VBox card = new VBox(10);
        card.setPrefWidth(300);
        card.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 15; -fx-border-radius: 10; -fx-background-radius: 10; -fx-border-color: #ffffff11;");
        Label title = new Label(p.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
        Label status = createStatusBadge(p.getStatus());
        HBox top = new HBox(10, title, new Region(), status);
        HBox.setHgrow(top.getChildren().get(1), Priority.ALWAYS);
        Label desc = new Label(p.getDescription());
        desc.setStyle("-fx-text-fill: #a0a0ab; -fx-font-size: 11px;");
        desc.setWrapText(true);
        desc.setMaxHeight(40);
        HBox actions = createActionButtons(p);
        card.getChildren().addAll(top, desc, actions);
        com.edulink.gui.util.ThemeManager.applyTheme(card);
        return card;
    }

    private HBox createListRow(ContentProposal p) {
        HBox row = new HBox(15);
        row.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 10 15; -fx-border-radius: 8; -fx-background-radius: 8; -fx-alignment: center-left; -fx-border-color: #ffffff11;");
        Label title = new Label(p.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
        row.getChildren().addAll(title, new Region(), createStatusBadge(p.getStatus()), createActionButtons(p));
        HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
        com.edulink.gui.util.ThemeManager.applyTheme(row);
        return row;
    }

    private Label createStatusBadge(String statusStr) {
        Label status = new Label(statusStr != null ? statusStr : "UNKNOWN");
        String color = "#f59e0b";
        if ("ACCEPTED".equals(statusStr)) color = "#00d289";
        else if ("REJECTED".equals(statusStr)) color = "#ef4444";
        status.setStyle("-fx-background-color: " + color + "33; -fx-text-fill: " + color + "; -fx-padding: 2 8; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 10px;");
        return status;
    }

    private HBox createActionButtons(ContentProposal p) {
        HBox actions = new HBox(8);
        if ("PENDING".equals(p.getStatus())) {
            Button editBtn = new Button("✎");
            editBtn.setStyle("-fx-background-color: #3b82f633; -fx-text-fill: #3b82f6; -fx-cursor: hand;");
            editBtn.setOnAction(e -> showEditForm(p));
            Button delBtn = new Button("🗑");
            delBtn.setStyle("-fx-background-color: #ef444433; -fx-text-fill: #ef4444; -fx-cursor: hand;");
            delBtn.setOnAction(e -> {
                if (EduAlert.confirm("Delete", "Remove this proposal?")) {
                    if ("MATIERE".equals(p.getType())) {
                        new com.edulink.gui.services.courses.MatiereService().delete(p.getId());
                    } else if ("COURSE".equals(p.getType())) {
                        new com.edulink.gui.services.courses.CourseService().delete(p.getId());
                    } else if ("RESOURCE".equals(p.getType())) {
                        new com.edulink.gui.services.courses.ResourceService().delete(p.getId());
                    }
                    loadData();
                }
            });
            actions.getChildren().addAll(editBtn, delBtn);
        } else {
            Label l = new Label("Locked");
            l.setStyle("-fx-text-fill: #555; -fx-font-size: 10px;");
            actions.getChildren().add(l);
        }
        return actions;
    }

    private void showEditForm(ContentProposal p) {
        currentEditingProposal = p;
        formTitle.setText("Edit " + p.getType() + " Proposal");
        titleField.setText(p.getTitle());
        descField.setText(p.getDescription());
        
        boolean isCourse = "COURSE".equals(p.getType());
        courseMetaRow.setVisible(isCourse);
        courseMetaRow.setManaged(isCourse);
        
        if (isCourse) {
            // Try to extract level/xp from description if it was saved that way
            String desc = p.getDescription();
            if (desc != null && desc.contains("[Level: ")) {
                String lvl = desc.substring(desc.indexOf("[Level: ") + 8, desc.indexOf(",", desc.indexOf("[Level: ")));
                levelCombo.setValue(lvl);
            }
            if (desc != null && desc.contains(", XP: ")) {
                String xp = desc.substring(desc.indexOf(", XP: ") + 6, desc.indexOf("]", desc.indexOf(", XP: ")));
                xpField.setText(xp);
            }
        }

        formOverlay.setVisible(true);
        formOverlay.toFront();
    }

    @FXML
    private void handleCloseForm() {
        formOverlay.setVisible(false);
        currentEditingProposal = null;
    }

    @FXML
    private void handleSaveEdit() {
        if (currentEditingProposal == null) return;
        String newTitle = titleField.getText().trim();
        if (newTitle.isEmpty()) return;

        String newDesc = descField.getText() != null ? descField.getText().trim() : "";
        if ("COURSE".equals(currentEditingProposal.getType())) {
            // Re-embed metadata into description to keep it consistent
            newDesc += "\n[Level: " + levelCombo.getValue() + ", XP: " + xpField.getText() + "]";
        }

        try {
            if ("MATIERE".equals(currentEditingProposal.getType())) {
                com.edulink.gui.services.courses.MatiereService ms = new com.edulink.gui.services.courses.MatiereService();
                com.edulink.gui.models.courses.Matiere m = ms.getAll().stream().filter(x -> x.getId() == currentEditingProposal.getId()).findFirst().orElse(null);
                if (m != null) {
                    m.setName(newTitle);
                    ms.edit(m);
                }
            } else if ("COURSE".equals(currentEditingProposal.getType())) {
                com.edulink.gui.services.courses.CourseService cs = new com.edulink.gui.services.courses.CourseService();
                com.edulink.gui.models.courses.Course c = cs.getAll().stream().filter(x -> x.getId() == currentEditingProposal.getId()).findFirst().orElse(null);
                if (c != null) {
                    c.setTitle(newTitle);
                    c.setDescription(newDesc);
                    cs.edit(c);
                }
            } else if ("RESOURCE".equals(currentEditingProposal.getType())) {
                com.edulink.gui.services.courses.ResourceService rs = new com.edulink.gui.services.courses.ResourceService();
                com.edulink.gui.models.courses.Resource r = rs.getAll().stream().filter(x -> x.getId() == currentEditingProposal.getId()).findFirst().orElse(null);
                if (r != null) {
                    r.setTitle(newTitle);
                    r.setUrl(newDesc);
                    rs.edit(r);
                }
            }
            
            EduAlert.show(EduAlert.AlertType.SUCCESS, "Success", "Proposal updated!");
            handleCloseForm();
            loadData();
        } catch (Exception e) {
            EduAlert.show(EduAlert.AlertType.ERROR, "Error", e.getMessage());
        }
    }
}
