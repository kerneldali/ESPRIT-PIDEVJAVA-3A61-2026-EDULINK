package com.edulink.gui.controllers.courses;

import com.edulink.gui.models.courses.ContentProposal;
import com.edulink.gui.models.courses.Course;
import com.edulink.gui.models.courses.Enrollment;
import com.edulink.gui.models.courses.Resource;
import com.edulink.gui.services.courses.ContentProposalService;
import com.edulink.gui.services.courses.EnrollmentService;
import com.edulink.gui.services.courses.ResourceService;
import com.edulink.gui.util.EduAlert;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;
import javafx.stage.FileChooser;

public class CourseDetailsController implements Initializable {

    @FXML private StackPane rootPane;
    @FXML private Label courseTitleLabel;
    @FXML private Label courseDescLabel;
    @FXML private Label xpLabel;
    @FXML private Label levelLabel;
    @FXML private VBox notEnrolledBox;
    @FXML private VBox enrolledBox;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private Label enrollmentStatusLabel;
    @FXML private Button certifyBtn;
    @FXML private VBox resourcesContainer;

    // Suggest overlay
    @FXML private VBox formOverlay;
    @FXML private Label suggestContextLabel;
    @FXML private TextField suggestTitleField;
    @FXML private ComboBox<String> suggestTypeCombo;
    @FXML private TextField suggestUrlField;
    @FXML private TextArea suggestDescField;
    @FXML private Button suggestSaveBtn;
    @FXML private Label suggestTitleError;

    private Course currentCourse;
    private Enrollment currentEnrollment;
    private ResourceService resourceService = new ResourceService();
    private EnrollmentService enrollmentService = new EnrollmentService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        suggestTypeCombo.setItems(FXCollections.observableArrayList("VIDEO", "PDF", "LINK", "TEXT"));
        suggestTypeCombo.setValue("VIDEO");

        suggestTitleField.textProperty().addListener((obs, o, n) -> {
            suggestTitleError.setText("");
            suggestSaveBtn.setDisable(n == null || n.trim().isEmpty());
            if (n != null && !n.trim().isEmpty() && n.trim().length() < 2) {
                suggestTitleError.setText("At least 2 characters");
                suggestSaveBtn.setDisable(true);
            }
        });
    }

    public void setCourse(Course course) {
        this.currentCourse = course;
        courseTitleLabel.setText(course.getTitle());
        courseDescLabel.setText(course.getDescription() != null ? course.getDescription() : "");
        xpLabel.setText("+" + course.getXp() + " XP on completion");
        levelLabel.setText("Level: " + (course.getLevel() != null ? course.getLevel() : "N/A"));

        checkEnrollment();
        loadResources();
    }

    private void checkEnrollment() {
        int studentId = com.edulink.gui.util.SessionManager.getCurrentUser() != null ? com.edulink.gui.util.SessionManager.getCurrentUser().getId() : 1;
        List<Enrollment> enrollments = enrollmentService.getEnrollmentByStudent(studentId);
        currentEnrollment = enrollments.stream()
                .filter(e -> e.getCoursId() == currentCourse.getId())
                .findFirst().orElse(null);

        if (currentEnrollment != null) {
            notEnrolledBox.setVisible(false);
            notEnrolledBox.setManaged(false);
            enrolledBox.setVisible(true);
            enrolledBox.setManaged(true);
            
            // Re-calculate progress from resources directly to be safe
            int total = resourceService.findByCourse(currentCourse.getId()).size();
            int completed = resourceService.getCompletedCount(studentId, currentCourse.getId());
            double progress = (total > 0) ? (double) completed / total * 100.0 : 0.0;
            
            // Sync with DB if different
            if (Math.abs(progress - currentEnrollment.getProgress()) > 0.01) {
                currentEnrollment.setProgress(progress);
                enrollmentService.edit(currentEnrollment);
            }

            progressBar.setProgress(progress / 100.0);
            progressLabel.setText(Math.round(progress) + "% Completed");
            
            if (progress >= 100) {
                enrollmentStatusLabel.setText("✅ COMPLETED — Awarded " + currentCourse.getXp() + " XP!");
                enrollmentStatusLabel.setStyle("-fx-text-fill: #00d289; -fx-font-weight: bold;");
                certifyBtn.setVisible(true);
                certifyBtn.setManaged(true);
            } else {
                enrollmentStatusLabel.setText("IN PROGRESS — " + Math.round(progress) + "%");
                enrollmentStatusLabel.setStyle("-fx-text-fill: #a0a0ab;");
                certifyBtn.setVisible(false);
                certifyBtn.setManaged(false);
            }
        } else {
            notEnrolledBox.setVisible(true);
            notEnrolledBox.setManaged(true);
            enrolledBox.setVisible(false);
            enrolledBox.setManaged(false);
        }
    }

    private void loadResources() {
        resourcesContainer.getChildren().clear();
        List<Resource> resources = resourceService.findByCourse(currentCourse.getId());

        if (resources.isEmpty()) {
            Label empty = new Label("No resources available yet.");
            empty.setStyle("-fx-text-fill: #a0a0ab;");
            resourcesContainer.getChildren().add(empty);
            return;
        }

        for (Resource r : resources) {
            HBox row = new HBox(15);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 15; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-color: #ffffff11;");

            // Icon
            Label icon = new Label("▶");
            icon.setStyle("-fx-text-fill: #7c3aed; -fx-font-size: 16px;");
            if ("PDF".equals(r.getType())) icon.setText("📄");
            else if ("LINK".equals(r.getType())) icon.setText("🔗");
            else if ("TEXT".equals(r.getType())) icon.setText("📝");

            VBox infoBox = new VBox(3);
            Label title = new Label(r.getTitle());
            title.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14;");
            Label type = new Label(r.getType());
            type.setStyle("-fx-text-fill: #7c3aed; -fx-font-size: 11px; -fx-font-weight: bold;");
            infoBox.getChildren().addAll(title, type);
            infoBox.setPrefWidth(200);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button viewBtn = new Button("▶ Open");
            viewBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
            viewBtn.setOnAction(e -> openResource(r));

            Button completeBtn = new Button("✔ Complete");
            completeBtn.setDisable(currentEnrollment == null);

            int studentId = com.edulink.gui.util.SessionManager.getCurrentUser() != null ? com.edulink.gui.util.SessionManager.getCurrentUser().getId() : 1;
            boolean isDone = resourceService.isResourceCompleted(studentId, r.getId());

            if (isDone) {
                completeBtn.setText("✔ Done");
                completeBtn.setStyle("-fx-background-color: #3b82f633; -fx-text-fill: #3b82f6; -fx-background-radius: 5;");
                completeBtn.setDisable(true);
            } else {
                completeBtn.setStyle("-fx-background-color: #00d289; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
            }

            completeBtn.setOnAction(e -> {
                if (currentEnrollment != null) {
                    resourceService.markAsCompleted(studentId, r.getId());
                    checkEnrollment(); // This will auto-update progress in DB too
                    loadResources();
                    if (currentEnrollment.getProgress() >= 100) {
                        // Reward student
                        int sid = com.edulink.gui.util.SessionManager.getCurrentUser().getId();
                        int xpReward = currentCourse.getXp();
                        com.edulink.gui.services.UserService us = new com.edulink.gui.services.UserService();
                        us.updateXp(sid, xpReward);
                        us.updateWallet(sid, (double)xpReward); // Match points to wallet balance as requested
                        
                        EduAlert.show(EduAlert.AlertType.SUCCESS, "Course Completed!", 
                            "Congratulations! You gained " + xpReward + " XP and the same amount of coins in your wallet.");
                        
                        checkEnrollment(); // Refresh UI to show Get Certified button
                    }
                }
            });

            row.getChildren().addAll(icon, infoBox, spacer, viewBtn, completeBtn);
            resourcesContainer.getChildren().add(row);
        }
    }

    private void openResource(Resource r) {
        try {
            String path = r.getUrl();
            if (path == null || path.trim().isEmpty()) {
                EduAlert.show(EduAlert.AlertType.WARNING, "No Path", "No URL/path set for this resource.");
                return;
            }
            
            // Integrated Viewer (PDF, Video, YouTube, Link)
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/courses/ResourceViewer.fxml"));
                Parent root = loader.load();
                ResourceViewerController controller = loader.getController();
                
                // Pass title, path, AND type for smart rendering
                controller.loadResource(r.getTitle(), path, r.getType());
                
                javafx.stage.Stage stage = new javafx.stage.Stage();
                stage.setTitle("EduLink Viewer - " + r.getTitle());
                stage.setScene(new javafx.scene.Scene(root));
                stage.initOwner(rootPane.getScene().getWindow());
                stage.show();
            } catch (Exception ex) {
                ex.printStackTrace();
                // Fallback to legacy
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", path).start();
                } else if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(new File(path));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            EduAlert.show(EduAlert.AlertType.ERROR, "Viewer Error", ex.getMessage());
        }
    }

    @FXML
    private void handleGenerateCertificate() {
        if (currentEnrollment == null || currentEnrollment.getProgress() < 100) {
            EduAlert.show(EduAlert.AlertType.WARNING, "Not Ready", "Complete all resources first!");
            return;
        }

        com.edulink.gui.models.User student = com.edulink.gui.util.SessionManager.getCurrentUser();
        com.edulink.gui.services.PdfExportService exporter = new com.edulink.gui.services.PdfExportService();

        // Format Selection Dialog
        ButtonType pdfBtn = new ButtonType("🎓 Professional PDF", ButtonBar.ButtonData.OK_DONE);
        ButtonType pngBtn = new ButtonType("🖼️ High-Res Image (PNG)", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Elite Certification");
        alert.setHeaderText("Congratulations, " + student.getFullName() + "!");
        alert.setContentText("Your achievement is verified. How would you like to save your certificate?");
        alert.getButtonTypes().setAll(pdfBtn, pngBtn, cancelBtn);
        alert.initOwner(rootPane.getScene().getWindow());

        java.util.Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() != cancelBtn) {
            boolean isPdf = result.get() == pdfBtn;
            
            FileChooser fc = new FileChooser();
            fc.setTitle("Save Certificate");
            fc.setInitialFileName("Certificate_" + currentCourse.getTitle().replace(" ", "_") + (isPdf ? ".pdf" : ".png"));
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(isPdf ? "PDF Document" : "PNG Image", isPdf ? "*.pdf" : "*.png"));

            File file = fc.showSaveDialog(rootPane.getScene().getWindow());
            if (file != null) {
                try {
                    if (isPdf) {
                        exporter.exportCertificate(student, currentCourse, file);
                    } else {
                        exporter.exportCertificateAsImage(student, currentCourse, file);
                    }
                    
                    EduAlert.show(EduAlert.AlertType.SUCCESS, "Export Success", 
                        "Your certificate has been saved to:\n" + file.getAbsolutePath());
                    
                    if (System.getProperty("os.name").toLowerCase().contains("win")) {
                        new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", file.getAbsolutePath()).start();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    EduAlert.show(EduAlert.AlertType.ERROR, "Export Error", ex.getMessage());
                }
            }
        }
    }

    @FXML
    private void handleEnroll() {
        int sid = com.edulink.gui.util.SessionManager.getCurrentUser() != null ? com.edulink.gui.util.SessionManager.getCurrentUser().getId() : 1;
        System.out.println("🚀 [DEBUG] User clicking Enroll. StudentID=" + sid + ", CourseID=" + currentCourse.getId());

        Enrollment e = new Enrollment();
        e.setStudentId(sid);
        e.setCoursId(currentCourse.getId());
        e.setProgress(0.0);
        e.setEnrolledAt(LocalDateTime.now());
        e.setLastAccessed(LocalDateTime.now());

        enrollmentService.add2(e);
        EduAlert.show(EduAlert.AlertType.SUCCESS, "Success!", "You are now enrolled in \"" + currentCourse.getTitle() + "\".");
        
        checkEnrollment();
        loadResources();
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/courses/CourseList.fxml"));
            Parent root = loader.load();
            CourseListController controller = loader.getController();
            com.edulink.gui.services.courses.MatiereService ms = new com.edulink.gui.services.courses.MatiereService();
            com.edulink.gui.models.courses.Matiere m = ms.getAll().stream()
                    .filter(x -> x.getId() == currentCourse.getMatiereId())
                    .findFirst().orElse(null);
            if (m != null) controller.setMatiere(m);
            StackPane contentArea = (StackPane) rootPane.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleSuggestResource() {
        suggestTitleField.clear();
        suggestUrlField.clear();
        suggestDescField.clear();
        suggestTypeCombo.setValue("VIDEO");
        suggestTitleError.setText("");
        suggestSaveBtn.setDisable(true);
        suggestContextLabel.setText("For course: " + (currentCourse != null ? currentCourse.getTitle() : ""));
        formOverlay.setVisible(true);
        formOverlay.toFront();
    }

    @FXML
    private void handleCloseSuggest() {
        formOverlay.setVisible(false);
    }

    @FXML
    private void handleSubmitSuggest() {
        ContentProposal p = new ContentProposal();
        p.setType("RESOURCE");
        p.setTitle(suggestTitleField.getText().trim());
        String desc = suggestDescField.getText() != null ? suggestDescField.getText().trim() : "";
        p.setDescription(desc + " [Type: " + suggestTypeCombo.getValue() + "] [URL: " + suggestUrlField.getText() + "] [Course: " + currentCourse.getTitle() + "]");
        p.setStatus("PENDING");
        p.setCreatedAt(LocalDateTime.now());
        int sid = com.edulink.gui.util.SessionManager.getCurrentUser() != null ? com.edulink.gui.util.SessionManager.getCurrentUser().getId() : 1;
        p.setSuggestedBy(sid);

        new ContentProposalService().add2(p);
        handleCloseSuggest();
        EduAlert.show(EduAlert.AlertType.SUCCESS, "Submitted", "Proposal sent for review.");
    }
}
