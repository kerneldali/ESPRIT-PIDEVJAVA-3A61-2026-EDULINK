package com.edulink.gui.controllers.courses;

import com.edulink.gui.models.courses.ContentProposal;
import com.edulink.gui.models.courses.Matiere;

import com.edulink.gui.util.EduAlert;
import com.edulink.gui.services.courses.MatiereService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

public class MatiereListController implements Initializable {

    @FXML private StackPane rootPane;
    @FXML private FlowPane matiereContainer;
    @FXML private ComboBox<String> filterCombo;
    @FXML private TextField searchField;
    @FXML private TextField recommendField;

    // Suggest overlay
    @FXML private VBox formOverlay;
    @FXML private TextField suggestNameField;
    @FXML private TextArea suggestDescField;
    @FXML private Button suggestSaveBtn;
    @FXML private Label suggestNameError;

    private MatiereService matiereService = new MatiereService();
    private List<Matiere> allMatieres;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        filterCombo.setItems(javafx.collections.FXCollections.observableArrayList("Alphabetical (A-Z)", "Newest First"));
        filterCombo.setValue("Alphabetical (A-Z)");

        allMatieres = matiereService.getAll();
        applyFilters();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Suggestion form validation
        suggestNameField.textProperty().addListener((obs, old, nv) -> validateSuggestForm());
        suggestDescField.textProperty().addListener((obs, old, nv) -> validateSuggestForm());
    }

    private void validateSuggestForm() {
        suggestNameError.setText("");
        boolean valid = true;
        
        String name = suggestNameField.getText();
        if (name == null || name.trim().isEmpty()) {
            suggestNameError.setText("Name is required");
            valid = false;
        } else if (name.trim().length() < 2) {
            suggestNameError.setText("At least 2 characters");
            valid = false;
        }
        
        String desc = suggestDescField.getText();
        if (desc == null || desc.trim().isEmpty()) {
            if (valid) suggestNameError.setText("Description is required"); // Reusing error label for simplicity
            valid = false;
        }
        
        suggestSaveBtn.setDisable(!valid);
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        java.util.List<Matiere> filtered = new java.util.ArrayList<>(allMatieres.stream()
                .filter(m -> "ACCEPTED".equalsIgnoreCase(m.getStatus()))
                .filter(m -> m.getName() != null && m.getName().toLowerCase().contains(query))
                .toList());

        String sortType = filterCombo.getValue();
        if ("Alphabetical (A-Z)".equals(sortType)) {
            filtered.sort((a, b) -> (a.getName() == null ? "" : a.getName()).compareToIgnoreCase(b.getName() == null ? "" : b.getName()));
        } else if ("Newest First".equals(sortType)) {
            filtered.sort((a, b) -> (b.getCreatedAt() == null ? LocalDateTime.MIN : b.getCreatedAt()).compareTo(a.getCreatedAt() == null ? LocalDateTime.MIN : a.getCreatedAt()));
        }

        displayMatieres(filtered);
    }

    private void displayMatieres(List<Matiere> list) {
        matiereContainer.getChildren().clear();
        if (list == null || list.isEmpty()) {
            Label empty = new Label("No categories found.");
            empty.setStyle("-fx-text-fill: #a0a0ab; -fx-font-size: 14px;");
            matiereContainer.getChildren().add(empty);
            return;
        }
        for (Matiere m : list) {
            matiereContainer.getChildren().add(createCard(m));
        }
    }

    private VBox createCard(Matiere m) {
        VBox card = new VBox(0);
        card.setPrefWidth(280);
        card.setStyle("-fx-background-color: #1a1a2e; -fx-border-radius: 10; -fx-background-radius: 10; -fx-border-color: #ffffff11;");

        StackPane imageBox = new StackPane();
        imageBox.setPrefHeight(120);
        imageBox.setStyle("-fx-background-color: #2a2a40; -fx-background-radius: 10 10 0 0;");
        if (m.getImageUrl() != null && !m.getImageUrl().isEmpty()) {
            try {
                ImageView img = new ImageView(new Image(new File(m.getImageUrl()).toURI().toString()));
                img.setFitHeight(120);
                img.setFitWidth(280);
                img.setPreserveRatio(false);
                imageBox.getChildren().add(img);
            } catch (Exception e) { /* ignore */ }
        }

        VBox infoBox = new VBox(8);
        infoBox.setPadding(new javafx.geometry.Insets(12));

        Label title = new Label(m.getName() != null ? m.getName() : "Untitled");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: white;");

        Button viewBtn = new Button("View Courses →");
        viewBtn.setMaxWidth(Double.MAX_VALUE);
        viewBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");
        viewBtn.setOnAction(e -> showCourseList(m));

        infoBox.getChildren().addAll(title, viewBtn);
        card.getChildren().addAll(imageBox, infoBox);
        com.edulink.gui.util.ThemeManager.applyTheme(card);
        return card;
    }

    private void showCourseList(Matiere m) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/courses/CourseList.fxml"));
            Parent root = loader.load();
            CourseListController controller = loader.getController();
            controller.setMatiere(m);

            StackPane contentArea = (StackPane) rootPane.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Suggestion Overlay ---
    @FXML
    private void handleSuggestMatiere() {
        suggestNameField.clear();
        suggestDescField.clear();
        suggestNameError.setText("");
        suggestSaveBtn.setDisable(true);
        formOverlay.setVisible(true);
        formOverlay.toFront();
    }

    @FXML
    private void handleCloseSuggest() {
        formOverlay.setVisible(false);
    }

    @FXML
    private void handleSubmitSuggest() {
        Matiere p = new Matiere();
        p.setName(suggestNameField.getText().trim());
        p.setDescription(suggestDescField.getText().trim());
        
        boolean isAdmin = false;
        if (com.edulink.gui.util.SessionManager.getCurrentUser() != null) {
            com.edulink.gui.models.User u = com.edulink.gui.util.SessionManager.getCurrentUser();
            p.setCreatorId(u.getId());
            if (u.hasRole("ROLE_ADMIN") || u.hasRole("ROLE_FACULTY")) {
                isAdmin = true;
            }
        } else {
            p.setCreatorId(1);
        }
        
        p.setStatus(isAdmin ? "ACCEPTED" : "PENDING");
        p.setCreatedAt(LocalDateTime.now());

        new MatiereService().add2(p);
        
        // Refresh the list immediately if it's automatically accepted
        allMatieres = matiereService.getAll();
        applyFilters();

        handleCloseSuggest();
        
        if (isAdmin) {
            EduAlert.show(EduAlert.AlertType.SUCCESS, "Category Added",
                    "The category has been automatically accepted and added.");
        } else {
            EduAlert.show(EduAlert.AlertType.SUCCESS, "Proposal Submitted",
                    "Your category suggestion has been sent to admin for review.");
        }
    }

    @FXML
    private void handleRecommend() {
        String query = recommendField.getText();
        if (query == null || query.trim().isEmpty()) {
            applyFilters();
            return;
        }
        
        // Clear search field so it doesn't conflict visually
        searchField.setText("");

        List<com.edulink.gui.util.TfIdfRecommender.MatchResult> results = 
            com.edulink.gui.util.TfIdfRecommender.recommend(query.trim(), allMatieres);

        List<Matiere> recommended = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(5, results.size()); i++) {
            if (results.get(i).score > 0 || recommended.isEmpty()) {
                recommended.add(results.get(i).matiere);
            }
        }
        
        displayMatieres(recommended);
    }
}
