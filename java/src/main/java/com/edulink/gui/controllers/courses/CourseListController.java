package com.edulink.gui.controllers.courses;

import com.edulink.gui.models.courses.ContentProposal;
import com.edulink.gui.models.courses.Course;
import com.edulink.gui.models.courses.Matiere;
import com.edulink.gui.services.courses.ContentProposalService;
import com.edulink.gui.services.courses.CourseService;
import com.edulink.gui.services.courses.EnrollmentService;
import com.edulink.gui.util.EduAlert;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

public class CourseListController implements Initializable {

    @FXML private StackPane rootPane;
    @FXML private FlowPane courseContainer;
    @FXML private ComboBox<String> levelFilterCombo;
    @FXML private ComboBox<String> sortCombo;
    @FXML private TextField searchField;
    @FXML private Label pageTitle;

    // Suggest overlay
    @FXML private VBox formOverlay;
    @FXML private Label suggestContextLabel;
    @FXML private TextField suggestTitleField;
    @FXML private TextArea suggestDescField;
    @FXML private ComboBox<String> suggestLevelCombo;
    @FXML private TextField suggestXpField;
    @FXML private Button suggestSaveBtn;
    @FXML private Label suggestTitleError;
    @FXML private Label suggestDescError;

    private CourseService courseService = new CourseService();
    private EnrollmentService enrollmentService = new EnrollmentService();
    private List<Course> allCourses;
    private Matiere currentMatiere;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        levelFilterCombo.setItems(FXCollections.observableArrayList("All Levels", "BEGINNER", "INTERMEDIATE", "ADVANCED"));
        levelFilterCombo.setValue("All Levels");

        sortCombo.setItems(FXCollections.observableArrayList("Newest First", "Alphabetical (A-Z)", "Highest XP"));
        sortCombo.setValue("Newest First");

        suggestLevelCombo.setItems(FXCollections.observableArrayList("BEGINNER", "INTERMEDIATE", "ADVANCED"));
        suggestLevelCombo.setValue("BEGINNER");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        levelFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        sortCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        suggestTitleField.textProperty().addListener((obs, o, n) -> validateSuggest());
        suggestDescField.textProperty().addListener((obs, o, n) -> validateSuggest());
    }

    private void applyFilters() {
        if (allCourses == null) return;

        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String level = levelFilterCombo.getValue();

        java.util.List<Course> filtered = new java.util.ArrayList<>(allCourses.stream()
                .filter(c -> "ACCEPTED".equalsIgnoreCase(c.getStatus()))
                .filter(c -> c.getTitle() != null && c.getTitle().toLowerCase().contains(query))
                .filter(c -> "All Levels".equals(level) || level.equals(c.getLevel()))
                .toList());

        String sortType = sortCombo.getValue();
        if ("Alphabetical (A-Z)".equals(sortType)) {
            filtered.sort((a, b) -> (a.getTitle() == null ? "" : a.getTitle()).compareToIgnoreCase(b.getTitle() == null ? "" : b.getTitle()));
        } else if ("Newest First".equals(sortType)) {
            filtered.sort((a, b) -> (b.getCreatedAt() == null ? LocalDateTime.MIN : b.getCreatedAt()).compareTo(a.getCreatedAt() == null ? LocalDateTime.MIN : a.getCreatedAt()));
        } else if ("Highest XP".equals(sortType)) {
            filtered.sort((a, b) -> Integer.compare(b.getXp(), a.getXp()));
        }

        displayCourses(filtered);
    }

    private void validateSuggest() {
        boolean valid = true;
        suggestTitleError.setText("");
        suggestDescError.setText("");
        if (suggestTitleField.getText() == null || suggestTitleField.getText().trim().isEmpty()) {
            suggestTitleError.setText("Title is required");
            valid = false;
        }
        if (suggestDescField.getText() == null || suggestDescField.getText().trim().isEmpty()) {
            suggestDescError.setText("Description is required");
            valid = false;
        }
        suggestSaveBtn.setDisable(!valid);
    }

    public void setMatiere(Matiere matiere) {
        this.currentMatiere = matiere;
        this.pageTitle.setText("Courses in " + matiere.getName());
        this.allCourses = courseService.findByMatiere(matiere.getId());
        displayCourses(this.allCourses);
    }

    private void displayCourses(List<Course> list) {
        courseContainer.getChildren().clear();
        if (list == null || list.isEmpty()) {
            Label empty = new Label("No courses available in this category yet.");
            empty.setStyle("-fx-text-fill: #a0a0ab; -fx-font-size: 14px;");
            courseContainer.getChildren().add(empty);
            return;
        }
        for (Course c : list) {
            courseContainer.getChildren().add(createCard(c));
        }
    }

    private VBox createCard(Course c) {
        VBox card = new VBox(10);
        card.setPrefWidth(280);
        card.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 15; -fx-border-radius: 10; -fx-background-radius: 10; -fx-border-color: #ffffff11;");

        Label title = new Label(c.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: white;");

        Label desc = new Label(c.getDescription() != null ? c.getDescription() : "");
        desc.setStyle("-fx-text-fill: #a0a0ab; -fx-font-size: 12px;");
        desc.setWrapText(true);
        desc.setMaxHeight(36);

        Label level = new Label(c.getLevel() != null ? c.getLevel() : "N/A");
        String levelColor = "#3b82f6";
        if ("INTERMEDIATE".equals(c.getLevel())) levelColor = "#f59e0b";
        else if ("ADVANCED".equals(c.getLevel())) levelColor = "#ef4444";
        level.setStyle("-fx-background-color: " + levelColor + "; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 10px; -fx-font-weight: bold;");

        Label xp = new Label("+" + c.getXp() + " XP");
        xp.setStyle("-fx-text-fill: #00d289; -fx-font-weight: bold;");

        HBox meta = new HBox(15);
        meta.getChildren().addAll(level, xp);

        // Check enrollment
        int sid = 1;
        if (com.edulink.gui.util.SessionManager.getCurrentUser() != null)
            sid = com.edulink.gui.util.SessionManager.getCurrentUser().getId();
        boolean enrolled = enrollmentService.getEnrollmentByStudent(sid).stream().anyMatch(e -> e.getCoursId() == c.getId());

        Button actionBtn = new Button();
        actionBtn.setMaxWidth(Double.MAX_VALUE);

        if (enrolled) {
            actionBtn.setText("Continue Learning →");
            actionBtn.setStyle("-fx-background-color: #00d289; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
            actionBtn.setOnAction(e -> showCourseDetails(c));
        } else {
            actionBtn.setText("Enroll Now");
            actionBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
            final int studentId = sid;
                actionBtn.setOnAction(e -> {
                    try {
                        System.out.println("🔄 [UI_DEBUG] Enroll request: studentId=" + studentId + ", courseId=" + c.getId());
                        com.edulink.gui.models.courses.Enrollment enr = new com.edulink.gui.models.courses.Enrollment();
                        enr.setStudentId(studentId);
                        enr.setCoursId(c.getId());
                        enr.setProgress(0.0);
                        
                        enrollmentService.add2(enr);
                        
                        // Final verification check for UI feedback
                        List<com.edulink.gui.models.courses.Enrollment> enrs = enrollmentService.getEnrollmentByStudent(studentId);
                        boolean verified = enrs.stream().anyMatch(en -> en.getCoursId() == c.getId());

                        if (verified) {
                            EduAlert.show(EduAlert.AlertType.SUCCESS, "Enrolled!",
                                    "You've been enrolled in \"" + c.getTitle() + "\".\nGo to My Learning to start!");
                        } else {
                            String debugInfo = "Tried Table: enrollment/enrollement\nUser ID: " + studentId + "\nCourse ID: " + c.getId();
                            EduAlert.show(EduAlert.AlertType.ERROR, "Enrollment Failed",
                                    "The database did not save the enrollment.\n" + debugInfo + "\nPlease check your console for SQL errors.");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        EduAlert.show(EduAlert.AlertType.ERROR, "Critical Logic Error", ex.getClass().getName() + ": " + ex.getMessage());
                    }
                    // Refresh current view
                    allCourses = courseService.findByMatiere(currentMatiere.getId());
                    displayCourses(allCourses);
                });
        }

        card.getChildren().addAll(title, desc, meta, actionBtn);
        com.edulink.gui.util.ThemeManager.applyTheme(card);
        return card;
    }



    private void showCourseDetails(Course c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/courses/CourseDetails.fxml"));
            Parent root = loader.load();
            CourseDetailsController controller = loader.getController();
            controller.setCourse(c);
            StackPane contentArea = (StackPane) rootPane.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/courses/MatiereList.fxml"));
            Parent root = loader.load();
            StackPane contentArea = (StackPane) rootPane.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- Suggestion Overlay ---
    @FXML
    private void handleSuggestCourse() {
        suggestTitleField.clear();
        suggestDescField.clear();
        suggestXpField.clear();
        suggestLevelCombo.setValue("BEGINNER");
        suggestTitleError.setText("");
        suggestDescError.setText("");
        suggestSaveBtn.setDisable(true);
        suggestContextLabel.setText("Proposing for: " + (currentMatiere != null ? currentMatiere.getName() : "Unknown"));
        formOverlay.setVisible(true);
        formOverlay.toFront();
    }

    @FXML
    private void handleCloseSuggest() {
        formOverlay.setVisible(false);
    }

    @FXML
    private void handleSubmitSuggest() {
        Course p = new Course();
        p.setTitle(suggestTitleField.getText().trim());
        p.setDescription(suggestDescField.getText() != null ? suggestDescField.getText().trim() : "");
        p.setLevel(suggestLevelCombo.getValue());
        
        try {
            p.setXp(Integer.parseInt(suggestXpField.getText().trim()));
        } catch (Exception e) {
            p.setXp(0);
        }
        
        if (currentMatiere != null) p.setMatiereId(currentMatiere.getId());
        p.setCreatedAt(LocalDateTime.now());
        
        boolean isAdmin = false;
        if (com.edulink.gui.util.SessionManager.getCurrentUser() != null) {
            com.edulink.gui.models.User u = com.edulink.gui.util.SessionManager.getCurrentUser();
            p.setAuthorId(u.getId());
            if (u.hasRole("ROLE_ADMIN") || u.hasRole("ROLE_FACULTY")) {
                isAdmin = true;
            }
        } else {
            p.setAuthorId(1);
        }
        
        p.setStatus(isAdmin ? "ACCEPTED" : "PENDING");

        courseService.add2(p);
        
        // Refresh immediately if it's automatically accepted
        if (currentMatiere != null) {
            allCourses = courseService.findByMatiere(currentMatiere.getId());
            applyFilters();
        }

        handleCloseSuggest();
        
        if (isAdmin) {
            EduAlert.show(EduAlert.AlertType.SUCCESS, "Course Added",
                    "The course has been automatically accepted and added.");
        } else {
            EduAlert.show(EduAlert.AlertType.SUCCESS, "Proposal Submitted",
                    "Your course suggestion has been sent to admin for review.");
        }
    }
}
