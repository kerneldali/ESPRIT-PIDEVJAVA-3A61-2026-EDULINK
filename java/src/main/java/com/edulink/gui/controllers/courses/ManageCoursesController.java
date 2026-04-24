package com.edulink.gui.controllers.courses;

import com.edulink.gui.models.courses.Course;
import com.edulink.gui.models.courses.Matiere;
import com.edulink.gui.models.courses.Resource;
import com.edulink.gui.services.courses.CourseService;
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

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

public class ManageCoursesController implements Initializable {

    @FXML private StackPane rootPane;
    @FXML private VBox cardContainer;
    @FXML private Label pageTitle;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> levelComboFilter;
    @FXML private ComboBox<String> sortCombo;

    // Form overlay nodes
    @FXML private VBox formOverlay;
    @FXML private Label formTitle;
    @FXML private TextField titleField;
    @FXML private TextArea descField;
    @FXML private ComboBox<String> levelCombo;
    @FXML private TextField xpField;
    @FXML private Button saveBtn;
    @FXML private Label titleError;
    @FXML private Label descError;
    @FXML private Label xpError;

    private CourseService courseService = new CourseService();
    private ResourceService resourceService = new ResourceService();
    private ObservableList<Course> courseList = FXCollections.observableArrayList();
    private Matiere filteredMatiere;
    private Course currentEditableCourse = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        levelComboFilter.setItems(FXCollections.observableArrayList("All Levels", "BEGINNER", "INTERMEDIATE", "ADVANCED"));
        levelComboFilter.setValue("All Levels");

        sortCombo.setItems(FXCollections.observableArrayList("Newest First", "Oldest First", "Name A-Z"));
        sortCombo.setValue("Newest First");

        levelCombo.setItems(FXCollections.observableArrayList("BEGINNER", "INTERMEDIATE", "ADVANCED"));

        searchField.textProperty().addListener((obs, oldV, newV) -> filterData());
        levelComboFilter.valueProperty().addListener((obs, oldV, newV) -> filterData());
        sortCombo.valueProperty().addListener((obs, oldV, newV) -> filterData());

        // Form validation listeners
        titleField.textProperty().addListener((obs, old, newV) -> validateForm());
        descField.textProperty().addListener((obs, old, newV) -> validateForm());
        xpField.textProperty().addListener((obs, old, newV) -> validateForm());
    }

    private void validateForm() {
        boolean valid = true;
        titleError.setText("");
        descError.setText("");
        xpError.setText("");

        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            titleError.setText("Title is required");
            valid = false;
        } else if (titleField.getText().trim().length() < 3) {
            titleError.setText("Title must be at least 3 characters");
            valid = false;
        }

        if (descField.getText() == null || descField.getText().trim().isEmpty()) {
            descError.setText("Description is required");
            valid = false;
        }

        if (xpField.getText() != null && !xpField.getText().trim().isEmpty()) {
            try {
                int val = Integer.parseInt(xpField.getText().trim());
                if (val < 0) { xpError.setText("XP must be positive"); valid = false; }
            } catch (NumberFormatException e) {
                xpError.setText("XP must be a number");
                valid = false;
            }
        }

        saveBtn.setDisable(!valid);
    }

    public void setMatiereFilter(Matiere m) {
        this.filteredMatiere = m;
        pageTitle.setText(m.getName());
        loadData();
    }

    private void loadData() {
        if (filteredMatiere != null) {
            courseList.setAll(courseService.findByMatiere(filteredMatiere.getId()));
        } else {
            courseList.setAll(courseService.getAll());
        }
        filterData();
    }

    private void filterData() {
        cardContainer.getChildren().clear();
        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String level = levelComboFilter.getValue();

        java.util.List<Course> filtered = courseList.stream()
            .filter(c -> (c.getTitle() != null && c.getTitle().toLowerCase().contains(query)))
            .filter(c -> "All Levels".equals(level) || level.equals(c.getLevel()))
            .collect(java.util.stream.Collectors.toList());

        // Apply Sorting
        String sortType = sortCombo.getValue();
        if ("Name A-Z".equals(sortType)) {
            filtered.sort((a, b) -> (a.getTitle() == null ? "" : a.getTitle()).compareToIgnoreCase(b.getTitle() == null ? "" : b.getTitle()));
        } else if ("Newest First".equals(sortType)) {
            filtered.sort((a, b) -> (b.getCreatedAt() == null ? LocalDateTime.MIN : b.getCreatedAt()).compareTo(a.getCreatedAt() == null ? LocalDateTime.MIN : a.getCreatedAt()));
        } else if ("Oldest First".equals(sortType)) {
            filtered.sort((a, b) -> (a.getCreatedAt() == null ? LocalDateTime.MIN : a.getCreatedAt()).compareTo(b.getCreatedAt() == null ? LocalDateTime.MIN : b.getCreatedAt()));
        }

        for (Course c : filtered) {
            cardContainer.getChildren().add(createRow(c));
        }
    }

    private HBox createRow(Course c) {
        HBox row = new HBox(10);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 12 15; -fx-border-color: transparent transparent #ffffff09 transparent;");

        Label title = new Label(c.getTitle());
        title.setPrefWidth(200);
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Label levelLabel = new Label(c.getLevel() != null ? c.getLevel() : "N/A");
        levelLabel.setPrefWidth(120);
        String levelColor = "#3b82f6";
        if ("INTERMEDIATE".equals(c.getLevel())) levelColor = "#f59e0b";
        else if ("ADVANCED".equals(c.getLevel())) levelColor = "#ef4444";
        levelLabel.setStyle("-fx-background-color: " + levelColor + "; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label xp = new Label("+" + c.getXp() + " XP");
        xp.setPrefWidth(80);
        xp.setStyle("-fx-text-fill: #00d289; -fx-font-weight: bold;");

        // Count resources
        int resCount = resourceService.findByCourse(c.getId()).size();
        Label resLabel = new Label(String.valueOf(resCount));
        resLabel.setPrefWidth(80);
        resLabel.setStyle("-fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button manageRes = new Button("Manage Resources");
        manageRes.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0a0ab; -fx-cursor: hand;");
        manageRes.setOnAction(e -> navigateToResources(c));

        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: #ffffff22; -fx-border-radius: 5; -fx-cursor: hand;");
        editBtn.setOnAction(e -> showForm(c));

        Button delBtn = new Button("🗑");
        delBtn.setStyle("-fx-background-color: #ef444433; -fx-text-fill: #ef4444; -fx-background-radius: 5; -fx-cursor: hand;");
        delBtn.setOnAction(e -> {
            if (EduAlert.confirm("Delete Course", "Are you sure you want to delete '" + c.getTitle() + "'?")) {
                courseService.delete(c.getId());
                loadData();
            }
        });

        row.getChildren().addAll(title, levelLabel, xp, resLabel, spacer, manageRes, editBtn, delBtn);
        com.edulink.gui.util.ThemeManager.applyTheme(row);
        return row;
    }

    @FXML
    private void handleNewCourse() {
        showForm(null);
    }

    @FXML
    private void handleApplyFilter() {
        filterData();
    }

    private void showForm(Course c) {
        currentEditableCourse = c;
        if (c != null) {
            formTitle.setText("Edit Course");
            titleField.setText(c.getTitle());
            descField.setText(c.getDescription());
            levelCombo.setValue(c.getLevel());
            xpField.setText(String.valueOf(c.getXp()));
        } else {
            formTitle.setText("New Course");
            titleField.clear();
            descField.clear();
            levelCombo.setValue("BEGINNER");
            xpField.setText("0");
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
    private void handleSaveCourse() {
        Course result = currentEditableCourse != null ? currentEditableCourse : new Course();
        result.setTitle(titleField.getText().trim());
        result.setDescription(descField.getText().trim());
        result.setLevel(levelCombo.getValue());
        result.setPricePoints(0); // No price concept
        try {
            result.setXp(Integer.parseInt(xpField.getText().trim()));
        } catch (NumberFormatException e) {
            result.setXp(0);
        }
        if (currentEditableCourse == null) {
            result.setMatiereId(filteredMatiere.getId());
            result.setAuthorId(1);
            result.setStatus("ACCEPTED");
            result.setCreatedAt(LocalDateTime.now());
        }

        try {
            if (currentEditableCourse == null) courseService.add2(result);
            else courseService.edit(result);
            handleCloseForm();
            loadData();
        } catch (Exception e) {
            try {
                java.sql.Connection cnx = com.edulink.gui.util.MyConnection.getInstance().getCnx();
                java.sql.PreparedStatement pst;
                if (currentEditableCourse == null) {
                    pst = cnx.prepareStatement("INSERT INTO cours (matiere_id, title, description, level, xp, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)");
                    pst.setInt(1, result.getMatiereId());
                    pst.setString(2, result.getTitle());
                    pst.setString(3, result.getDescription());
                    pst.setString(4, result.getLevel());
                    pst.setInt(5, result.getXp());
                    pst.setString(6, result.getStatus());
                    pst.setTimestamp(7, java.sql.Timestamp.valueOf(result.getCreatedAt()));
                } else {
                    pst = cnx.prepareStatement("UPDATE cours SET title=?, description=?, level=?, xp=? WHERE id=?");
                    pst.setString(1, result.getTitle());
                    pst.setString(2, result.getDescription());
                    pst.setString(3, result.getLevel());
                    pst.setInt(4, result.getXp());
                    pst.setInt(5, result.getId());
                }
                pst.executeUpdate();
                handleCloseForm();
                loadData();
            } catch (Exception e2) {
                handleCloseForm();
                EduAlert.show(EduAlert.AlertType.ERROR, "Database Error", e.getMessage() + "\n" + e2.getMessage());
            }
        }
    }

    private void navigateToResources(Course c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/courses/ManageResources.fxml"));
            Parent root = loader.load();
            ManageResourcesController controller = loader.getController();
            controller.setCourseFilter(c);

            StackPane contentArea = (StackPane) rootPane.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(root);
        } catch (Exception e) {
            e.printStackTrace();
            EduAlert.show(EduAlert.AlertType.ERROR, "Navigation Error", "Failed to load Resources view: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/courses/ManageMatiere.fxml"));
            Parent root = loader.load();
            StackPane contentArea = (StackPane) rootPane.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
