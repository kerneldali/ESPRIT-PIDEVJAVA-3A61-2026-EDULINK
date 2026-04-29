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
    @FXML private TextArea descField;
    @FXML private Button saveBtn;
    @FXML private Label nameError;
    @FXML private Label descError;

    private MatiereService matiereService = new MatiereService();
    private ObservableList<Matiere> matiereList = FXCollections.observableArrayList();
    private Matiere currentEditableMatiere = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        filterCombo.setItems(FXCollections.observableArrayList("Alphabetical (A-Z)", "Newest First"));
        filterCombo.setValue("Alphabetical (A-Z)");



        searchField.textProperty().addListener((obs, oldV, newV) -> filterData(newV));
        filterCombo.valueProperty().addListener((obs, oldV, newV) -> filterData(searchField.getText()));
        nameField.textProperty().addListener((obs, old, newV) -> validateForm());
        descField.textProperty().addListener((obs, old, newV) -> validateForm());

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
        if (descField.getText() == null || descField.getText().trim().isEmpty()) {
            descError.setText("Description is required");
            valid = false;
        } else {
            descError.setText("");
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

        Button genImgBtn = new Button("🖼 Generate Image");
        genImgBtn.setStyle("-fx-background-color: #3b82f633; -fx-text-fill: #3b82f6; -fx-background-radius: 5; -fx-cursor: hand;");
        genImgBtn.setOnAction(e -> {
            genImgBtn.setText("Generating...");
            genImgBtn.setDisable(true);
            new Thread(() -> {
                String img = generateMatiereImage(m.getName(), m.getDescription() != null ? m.getDescription() : m.getName());
                javafx.application.Platform.runLater(() -> {
                    if (img != null) {
                        m.setImageUrl(img);
                        matiereService.edit(m);
                        loadData();
                    } else {
                        genImgBtn.setText("Failed");
                        genImgBtn.setDisable(false);
                    }
                });
            }).start();
        });

        actionRow.getChildren().addAll(manageBtn, editBtn, genImgBtn, delBtn);
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
            descField.setText(m.getDescription() != null ? m.getDescription() : "");
        } else {
            formTitle.setText("New Category");
            nameField.clear();
            descField.clear();
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
    private void handleSaveMatiere() {
        Matiere result = currentEditableMatiere != null ? currentEditableMatiere : new Matiere();
        result.setName(nameField.getText().trim());
        result.setDescription(descField.getText().trim());
        
        result.setStatus("ACCEPTED");
        
        boolean isNew = currentEditableMatiere == null;
        if (isNew) {
            result.setCreatorId(1);
            result.setCreatedAt(LocalDateTime.now());
        }

        saveBtn.setText("Generating Image...");
        saveBtn.setDisable(true);
        
        new Thread(() -> {
            String existingImg = currentEditableMatiere != null ? currentEditableMatiere.getImageUrl() : null;
            if (existingImg == null || existingImg.isEmpty()) {
                String newImg = generateMatiereImage(result.getName(), result.getDescription());
                result.setImageUrl(newImg != null ? newImg : "src/main/resources/images/default_category.jpg"); // Fallback placeholder
            }

            javafx.application.Platform.runLater(() -> {
                try {
                    if (isNew) matiereService.add2(result);
                    else matiereService.edit(result);
                } catch (Exception e) {
                    try {
                        java.sql.Connection cnx = com.edulink.gui.util.MyConnection.getInstance().getCnx();
                        java.sql.PreparedStatement pst;
                        if (isNew) {
                            pst = cnx.prepareStatement("INSERT INTO matiere (name, description, status) VALUES (?, ?, ?)");
                            pst.setString(1, result.getName());
                            pst.setString(2, result.getDescription());
                            pst.setString(3, result.getStatus());
                        } else {
                            pst = cnx.prepareStatement("UPDATE matiere SET name=?, description=?, status=? WHERE id=?");
                            pst.setString(1, result.getName());
                            pst.setString(2, result.getDescription());
                            pst.setString(3, result.getStatus());
                            pst.setInt(4, result.getId());
                        }
                        pst.executeUpdate();
                    } catch (Exception e2) {
                        EduAlert.show(EduAlert.AlertType.ERROR, "Database Error", e2.getMessage());
                    }
                }
                handleCloseForm();
                loadData();
                saveBtn.setText("Save Category");
                saveBtn.setDisable(false);
            });
        }).start();
    }

    private String generateMatiereImage(String title, String description) {
        try {
            String prompt = "Create a modern educational illustration representing: " + title + ". Context: " + description + ". Clean style, professional, inspiring, course platform thumbnail.";
            String encodedPrompt = java.net.URLEncoder.encode(prompt, java.nio.charset.StandardCharsets.UTF_8);
            String urlStr = "https://image.pollinations.ai/prompt/" + encodedPrompt;
            
            java.net.URL url = java.net.URI.create(urlStr).toURL();
            java.io.InputStream in = url.openStream();
            
            java.io.File destDir = new java.io.File(System.getProperty("user.dir"), "src/main/resources/images/categories");
            if (!destDir.exists() && new java.io.File(System.getProperty("user.dir"), "java/src/main/resources").exists()) {
                destDir = new java.io.File(System.getProperty("user.dir"), "java/src/main/resources/images/categories");
            } else if (!destDir.exists()) {
                destDir = new java.io.File("src/main/resources/images/categories");
            }
            if (!destDir.exists()) destDir.mkdirs();
            
            String filename = "cat_" + System.currentTimeMillis() + ".jpg";
            java.io.File destFile = new java.io.File(destDir, filename);
            
            java.nio.file.Files.copy(in, destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return "src/main/resources/images/categories/" + filename;
        } catch (Exception e) {
            System.err.println("Failed to generate image: " + e.getMessage());
            return null;
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
