package com.edulink.gui.controllers.courses;

import com.edulink.gui.models.courses.Matiere;
import com.edulink.gui.services.courses.MatiereService;
import com.edulink.gui.util.EduAlert;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class ManageMatiereController implements Initializable {

    @FXML private StackPane rootPane;
    @FXML private FlowPane cardContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCombo;

    // Form overlay nodes
    @FXML private VBox formOverlay;
    @FXML private Label formTitle;
    @FXML private TextField nameField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextField imagePathField;
    @FXML private Button saveBtn;
    @FXML private Label nameError;

    private MatiereService matiereService = new MatiereService();
    private ObservableList<Matiere> matiereList = FXCollections.observableArrayList();
    private Matiere currentEditableMatiere = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        filterCombo.setItems(FXCollections.observableArrayList("Alphabetical (A-Z)", "Newest First"));
        filterCombo.setValue("Alphabetical (A-Z)");

        statusCombo.setItems(FXCollections.observableArrayList("ACCEPTED", "PENDING", "REJECTED"));

        searchField.textProperty().addListener((obs, oldV, newV) -> filterData(newV));
        filterCombo.valueProperty().addListener((obs, oldV, newV) -> filterData(searchField.getText()));
        nameField.textProperty().addListener((obs, old, newV) -> validateForm());

        loadData();
    }

    private void validateForm() {
        boolean valid = true;
        nameError.setText("");
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            nameError.setText("Category name is required");
            valid = false;
        } else if (nameField.getText().trim().length() < 2) {
            nameError.setText("Name must be at least 2 characters");
            valid = false;
        }
        saveBtn.setDisable(!valid);
    }

    private void loadData() {
        matiereList.setAll(matiereService.getAll());
        filterData(searchField.getText());
    }

    private void filterData(String query) {
        cardContainer.getChildren().clear();
        String lowerQuery = query == null ? "" : query.toLowerCase();
        
        java.util.List<Matiere> filtered = matiereList.stream()
            .filter(m -> lowerQuery.isEmpty() || (m.getName() != null && m.getName().toLowerCase().contains(lowerQuery)))
            .collect(java.util.stream.Collectors.toList());

        // Apply Sorting
        String sortType = filterCombo.getValue();
        if ("Alphabetical (A-Z)".equals(sortType)) {
            filtered.sort((a, b) -> (a.getName() == null ? "" : a.getName()).compareToIgnoreCase(b.getName() == null ? "" : b.getName()));
        } else if ("Newest First".equals(sortType)) {
            filtered.sort((a, b) -> (b.getCreatedAt() == null ? LocalDateTime.MIN : b.getCreatedAt()).compareTo(a.getCreatedAt() == null ? LocalDateTime.MIN : a.getCreatedAt()));
        }

        for (Matiere m : filtered) {
            cardContainer.getChildren().add(createCard(m));
        }
    }

    private VBox createCard(Matiere m) {
        VBox card = new VBox(0);
        card.setPrefWidth(300);
        card.setPrefHeight(250);
        card.setStyle("-fx-padding: 0; -fx-background-color: #1a1a2e; -fx-border-radius: 10; -fx-background-radius: 10; -fx-border-color: #ffffff11;");

        // Image section
        StackPane imageBox = new StackPane();
        imageBox.setPrefHeight(120);
        imageBox.setStyle("-fx-background-color: #2a2a40; -fx-background-radius: 10 10 0 0;");
        if (m.getImageUrl() != null && !m.getImageUrl().isEmpty()) {
            try {
                ImageView img = new ImageView(new Image(new File(m.getImageUrl()).toURI().toString()));
                img.setFitHeight(120);
                img.setFitWidth(300);
                img.setPreserveRatio(false);
                imageBox.getChildren().add(img);
            } catch (Exception e) { /* image not found */ }
        }

        // Info section
        VBox infoBox = new VBox(10);
        infoBox.setPadding(new javafx.geometry.Insets(15));

        HBox titleRow = new HBox(10);
        Label title = new Label(m.getName() != null ? m.getName() : "Draft");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: white;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label statusLabel = new Label(m.getStatus() != null ? m.getStatus() : "N/A");
        statusLabel.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 10px;");
        titleRow.getChildren().addAll(title, spacer, statusLabel);

        HBox actionRow = new HBox(10);
        Button manageBtn = new Button("Manage");
        manageBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        manageBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(manageBtn, Priority.ALWAYS);
        manageBtn.setOnAction(e -> navigateToCourses(m));

        Button editBtn = new Button("✎");
        editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: #ffffff33; -fx-border-radius: 5; -fx-cursor: hand;");
        editBtn.setOnAction(e -> showForm(m));

        Button delBtn = new Button("🗑");
        delBtn.setStyle("-fx-background-color: #ef444433; -fx-text-fill: #ef4444; -fx-background-radius: 5; -fx-cursor: hand;");
        delBtn.setOnAction(e -> {
            if (EduAlert.confirm("Delete Category", "Are you sure you want to delete '" + m.getName() + "'?")) {
                matiereService.delete(m.getId());
                loadData();
            }
        });

        actionRow.getChildren().addAll(manageBtn, editBtn, delBtn);
        infoBox.getChildren().addAll(titleRow, actionRow);
        card.getChildren().addAll(imageBox, infoBox);
        com.edulink.gui.util.ThemeManager.applyTheme(card);
        return card;
    }

    @FXML
    private void handleNewMatiere() {
        showForm(null);
    }

    private void showForm(Matiere m) {
        currentEditableMatiere = m;
        if (m != null) {
            formTitle.setText("Edit Category");
            nameField.setText(m.getName());
            statusCombo.setValue(m.getStatus());
            imagePathField.setText(m.getImageUrl() != null ? m.getImageUrl() : "");
        } else {
            formTitle.setText("New Category");
            nameField.clear();
            statusCombo.setValue("ACCEPTED");
            imagePathField.clear();
        }
        formOverlay.setVisible(true);
        formOverlay.toFront();
        validateForm();
    }

    @FXML
    private void handleCloseForm() {
        formOverlay.setVisible(false);
    }

    @FXML
    private void handleBrowseImage() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = fc.showOpenDialog(rootPane.getScene().getWindow());
        if (file != null) {
            imagePathField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void handleSaveMatiere() {
        Matiere result = currentEditableMatiere != null ? currentEditableMatiere : new Matiere();
        result.setName(nameField.getText().trim());
        result.setStatus(statusCombo.getValue());
        result.setImageUrl(imagePathField.getText());
        if (currentEditableMatiere == null) {
            result.setCreatorId(1);
            result.setCreatedAt(LocalDateTime.now());
        }

        try {
            if (currentEditableMatiere == null) matiereService.add2(result);
            else matiereService.edit(result);
            handleCloseForm();
            loadData();
        } catch (Exception e) {
            try {
                java.sql.Connection cnx = com.edulink.gui.util.MyConnection.getInstance().getCnx();
                java.sql.PreparedStatement pst;
                if (currentEditableMatiere == null) {
                    pst = cnx.prepareStatement("INSERT INTO matiere (name, status) VALUES (?, ?)");
                    pst.setString(1, result.getName());
                    pst.setString(2, result.getStatus());
                } else {
                    pst = cnx.prepareStatement("UPDATE matiere SET name=?, status=? WHERE id=?");
                    pst.setString(1, result.getName());
                    pst.setString(2, result.getStatus());
                    pst.setInt(3, result.getId());
                }
                pst.executeUpdate();
                handleCloseForm();
                loadData();
            } catch (Exception e2) {
                handleCloseForm();
                EduAlert.show(EduAlert.AlertType.ERROR, "Database Error", e.getMessage());
            }
        }
    }

    private void navigateToCourses(Matiere m) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/courses/ManageCourses.fxml"));
            Parent root = loader.load();
            ManageCoursesController controller = loader.getController();
            controller.setMatiereFilter(m);

            StackPane contentArea = (StackPane) rootPane.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
