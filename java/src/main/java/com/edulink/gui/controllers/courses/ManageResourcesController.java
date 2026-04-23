package com.edulink.gui.controllers.courses;

import com.edulink.gui.models.courses.Course;
import com.edulink.gui.models.courses.Resource;
import com.edulink.gui.services.courses.ResourceService;
import com.edulink.gui.util.EduAlert;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class ManageResourcesController implements Initializable {

    @FXML private StackPane rootPane;
    @FXML private VBox cardContainer;
    @FXML private Label pageTitle;

    // Overlay form nodes
    @FXML private VBox formOverlay;
    @FXML private Label formTitle;
    @FXML private TextField titleField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField urlField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Button browseBtn;
    @FXML private Button saveBtn;
    @FXML private Label titleError;

    private ResourceService resourceService = new ResourceService();
    private ObservableList<Resource> resourceList = FXCollections.observableArrayList();
    private Course filteredCourse;
    private Resource currentEditableResource = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        typeCombo.setItems(FXCollections.observableArrayList("VIDEO", "PDF", "LINK", "TEXT"));
        statusCombo.setItems(FXCollections.observableArrayList("ACTIVE", "INACTIVE"));

        typeCombo.valueProperty().addListener((obs, o, n) -> {
            browseBtn.setDisable(!"PDF".equals(n) && !"VIDEO".equals(n));
        });

        browseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            String type = typeCombo.getValue();
            if ("PDF".equals(type) || "TEXT".equals(type)) {
                fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Documents (PDF, Word)", "*.pdf", "*.doc", "*.docx")
                );
            } else if ("VIDEO".equals(type)) {
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi", "*.mkv"));
            }
            File file = fc.showOpenDialog(rootPane.getScene().getWindow());
            if (file != null) urlField.setText(file.getAbsolutePath());
        });

        titleField.textProperty().addListener((o, oldV, newV) -> validateForm());
        urlField.textProperty().addListener((o, oldV, newV) -> validateForm());
        typeCombo.valueProperty().addListener((o, oldV, newV) -> validateForm());

        loadData();
    }

    private void validateForm() {
        boolean valid = true;
        if (titleError != null) titleError.setText("");

        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            if (titleError != null) titleError.setText("Title is required");
            valid = false;
        } else if (titleField.getText().trim().length() < 2) {
            if (titleError != null) titleError.setText("Title must be at least 2 characters");
            valid = false;
        }

        if (typeCombo.getValue() == null) valid = false;
        if (urlField.getText() == null || urlField.getText().trim().isEmpty()) valid = false;

        saveBtn.setDisable(!valid);
    }

    public void setCourseFilter(Course c) {
        this.filteredCourse = c;
        pageTitle.setText(c.getTitle());
        loadData();
    }

    private void loadData() {
        cardContainer.getChildren().clear();
        if (filteredCourse != null) {
            resourceList.setAll(resourceService.findByCourse(filteredCourse.getId()));
        } else {
            resourceList.setAll(resourceService.getAll());
        }

        if (resourceList.isEmpty()) {
            Label empty = new Label("No resources yet. Click '+ Add New Resource' to create one.");
            empty.setStyle("-fx-text-fill: #a0a0ab;");
            cardContainer.getChildren().add(empty);
            return;
        }

        for (Resource r : resourceList) {
            cardContainer.getChildren().add(createRow(r));
        }
    }

    private HBox createRow(Resource r) {
        HBox row = new HBox(15);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 15; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-color: #ffffff11;");

        Label icon = new Label("▶");
        icon.setStyle("-fx-text-fill: #7c3aed; -fx-font-size: 16px;");
        if ("PDF".equals(r.getType())) icon.setText("📄");
        else if ("LINK".equals(r.getType())) icon.setText("🔗");
        else if ("TEXT".equals(r.getType())) icon.setText("📝");

        VBox info = new VBox(2);
        Label title = new Label(r.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px;");
        Label type = new Label(r.getType() + " • " + (r.getUrl() != null ? r.getUrl() : ""));
        type.setStyle("-fx-text-fill: #7c3aed; -fx-font-size: 11px;");
        type.setMaxWidth(300);
        info.getChildren().addAll(title, type);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editBtn = new Button("✎ Edit");
        editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: #ffffff22; -fx-border-radius: 5; -fx-cursor: hand;");
        editBtn.setOnAction(e -> showForm(r));

        Button delBtn = new Button("🗑");
        delBtn.setStyle("-fx-background-color: #ef444433; -fx-text-fill: #ef4444; -fx-background-radius: 5; -fx-cursor: hand;");
        delBtn.setOnAction(e -> {
            if (EduAlert.confirm("Delete Resource", "Are you sure you want to delete '" + r.getTitle() + "'?")) {
                resourceService.delete(r.getId());
                EduAlert.show(EduAlert.AlertType.SUCCESS, "Deleted", "Resource removed successfully.");
                loadData();
            }
        });

        row.getChildren().addAll(icon, info, spacer, editBtn, delBtn);
        com.edulink.gui.util.ThemeManager.applyTheme(row);
        return row;
    }

    @FXML
    private void handleNewResource() {
        showForm(null);
    }

    private void showForm(Resource r) {
        currentEditableResource = r;
        if (r != null) {
            formTitle.setText("Edit Resource");
            titleField.setText(r.getTitle());
            typeCombo.setValue(r.getType());
            urlField.setText(r.getUrl());
            statusCombo.setValue(r.getStatus());
        } else {
            formTitle.setText("New Resource");
            titleField.clear();
            typeCombo.setValue("VIDEO");
            urlField.clear();
            statusCombo.setValue("ACTIVE");
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
    private void handleSaveResource() {
        Resource result = currentEditableResource != null ? currentEditableResource : new Resource();
        result.setTitle(titleField.getText().trim());
        result.setType(typeCombo.getValue());
        result.setUrl(urlField.getText().trim());
        result.setStatus(statusCombo.getValue());
        if (currentEditableResource == null && filteredCourse != null) {
            result.setCoursId(filteredCourse.getId());
            result.setAuthorId(1);
        }

        boolean success = false;
        String errorMsg = "";

        try {
            if (currentEditableResource == null) {
                resourceService.add2(result);
            } else {
                resourceService.edit(result);
            }
            // Verify it was actually saved by reloading
            int countBefore = resourceList.size();
            if (filteredCourse != null) {
                resourceList.setAll(resourceService.findByCourse(filteredCourse.getId()));
            } else {
                resourceList.setAll(resourceService.getAll());
            }
            // If editing, count stays same; if adding, count should increase
            if (currentEditableResource != null || resourceList.size() > countBefore) {
                success = true;
            } else {
                // Count didn't increase — might have failed silently, try direct SQL
                success = directSqlInsert(result);
            }
        } catch (Exception e) {
            errorMsg = e.getMessage();
            success = directSqlInsert(result);
        }

        handleCloseForm();
        if (success) {
            loadData();
            EduAlert.show(EduAlert.AlertType.SUCCESS, "Resource Saved",
                    currentEditableResource != null ? "Resource updated successfully!" : "New resource added to " + (filteredCourse != null ? filteredCourse.getTitle() : "course") + "!");
        } else {
            EduAlert.show(EduAlert.AlertType.ERROR, "Save Failed",
                    "Could not save the resource.\n" + errorMsg + "\nCheck console for details.");
        }
    }

    /**
     * Direct SQL fallback insert — tries multiple column combinations.
     */
    private boolean directSqlInsert(Resource r) {
        try {
            java.sql.Connection conn = com.edulink.gui.util.MyConnection.getInstance().getCnx();
            // Try minimal columns
            java.sql.PreparedStatement pst = conn.prepareStatement(
                    "INSERT INTO resource (cours_id, title, url, type, status) VALUES (?, ?, ?, ?, ?)");
            pst.setInt(1, r.getCoursId());
            pst.setString(2, r.getTitle());
            pst.setString(3, r.getUrl());
            pst.setString(4, r.getType());
            pst.setString(5, r.getStatus());
            int rows = pst.executeUpdate();
            System.out.println("✅ Direct SQL insert success! Rows: " + rows);
            return rows > 0;
        } catch (Exception e2) {
            System.err.println("❌ Direct SQL insert also failed: " + e2.getMessage());
            e2.printStackTrace();
            return false;
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/courses/ManageCourses.fxml"));
            Parent root = loader.load();
            ManageCoursesController controller = loader.getController();

            com.edulink.gui.services.courses.MatiereService ms = new com.edulink.gui.services.courses.MatiereService();
            com.edulink.gui.models.courses.Matiere mat = ms.getAll().stream()
                    .filter(m -> m.getId() == filteredCourse.getMatiereId())
                    .findFirst().orElse(null);
            if (mat != null) controller.setMatiereFilter(mat);

            StackPane contentArea = (StackPane) rootPane.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
