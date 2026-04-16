package com.edulink.gui.controllers.courses;

import com.edulink.gui.models.courses.Course;
import com.edulink.gui.models.courses.Enrollment;
import com.edulink.gui.models.courses.Matiere;
import com.edulink.gui.services.courses.CourseService;
import com.edulink.gui.services.courses.EnrollmentService;
import com.edulink.gui.services.courses.MatiereService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;
import javafx.stage.FileChooser;
import com.edulink.gui.util.EduAlert;

public class MyLearningController implements Initializable {

    @FXML private FlowPane cardContainer;

    private EnrollmentService enrollmentService = new EnrollmentService();
    private CourseService courseService = new CourseService();
    private MatiereService matiereService = new MatiereService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadData();
    }

    private void loadData() {
        cardContainer.getChildren().clear();
        int studentId = 1;
        if (com.edulink.gui.util.SessionManager.getCurrentUser() != null) {
            studentId = com.edulink.gui.util.SessionManager.getCurrentUser().getId();
        }
        System.out.println("📚 My Learning loading for studentId=" + studentId);

        List<Enrollment> enrollments = enrollmentService.getEnrollmentByStudent(studentId);
        List<Course> allCourses = courseService.getAll();
        List<Matiere> allMatieres = matiereService.getAll();

        System.out.println("📚 Found " + enrollments.size() + " enrollments, " + allCourses.size() + " courses, " + allMatieres.size() + " matieres");

        if (enrollments == null || enrollments.isEmpty()) {
            VBox emptyState = new VBox(10);
            emptyState.setAlignment(javafx.geometry.Pos.CENTER);
            emptyState.setPrefWidth(600);
            Label icon = new Label("📚");
            icon.setStyle("-fx-font-size: 40px;");
            Label msg = new Label("You haven't enrolled in any courses yet.");
            msg.setStyle("-fx-text-fill: #a0a0ab; -fx-font-size: 16px;");
            Label hint = new Label("Browse Categories to find courses and enroll!");
            hint.setStyle("-fx-text-fill: #666;");
            emptyState.getChildren().addAll(icon, msg, hint);
            cardContainer.getChildren().add(emptyState);
            return;
        }

        for (Enrollment e : enrollments) {
            System.out.println("📚 Enrollment: id=" + e.getId() + ", coursId=" + e.getCoursId() + ", progress=" + e.getProgress());
            Course course = allCourses.stream().filter(c -> c.getId() == e.getCoursId()).findFirst().orElse(null);
            if (course != null) {
                String matiereName = allMatieres.stream()
                        .filter(m -> m.getId() == course.getMatiereId())
                        .map(Matiere::getName)
                        .findFirst().orElse("Unknown");
                cardContainer.getChildren().add(createCard(course, e, matiereName));
            } else {
                System.out.println("⚠ Could not find course with id=" + e.getCoursId());
            }
        }
    }

    private VBox createCard(Course c, Enrollment e, String matiereName) {
        VBox card = new VBox(10);
        card.setPrefWidth(320);
        card.setStyle("-fx-background-color: #1a1a2e; -fx-border-radius: 10; -fx-background-radius: 10; -fx-border-color: #ffffff11;");

        // Top colored header
        StackPane header = new StackPane();
        header.setPrefHeight(80);
        String headerColor = e.getProgress() >= 100 ? "#00d28933" : "#7c3aed33";
        header.setStyle("-fx-background-color: " + headerColor + "; -fx-background-radius: 10 10 0 0;");
        Label emoji = new Label(e.getProgress() >= 100 ? "🏆" : "📖");
        emoji.setStyle("-fx-font-size: 30px;");
        header.getChildren().add(emoji);

        VBox infoBox = new VBox(8);
        infoBox.setPadding(new javafx.geometry.Insets(12));

        Label title = new Label(c.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: white;");

        HBox metaRow = new HBox(10);
        Label matLabel = new Label(matiereName);
        matLabel.setStyle("-fx-text-fill: #a0a0ab; -fx-font-size: 12px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label xpLabel = new Label("+" + c.getXp() + " XP");
        xpLabel.setStyle("-fx-text-fill: #00d289; -fx-font-weight: bold;");
        metaRow.getChildren().addAll(matLabel, spacer, xpLabel);

        // Progress bar
        ProgressBar progress = new ProgressBar(e.getProgress() / 100.0);
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.setStyle("-fx-accent: #00d289;");

        Label progressText = new Label("Progress: " + Math.round(e.getProgress()) + "%");
        progressText.setStyle("-fx-text-fill: #a0a0ab; -fx-font-size: 11px;");

        // Action buttons
        HBox actions = new HBox(10);

        Button statusBtn = new Button();
        HBox.setHgrow(statusBtn, Priority.ALWAYS);
        statusBtn.setMaxWidth(Double.MAX_VALUE);
        if (e.getProgress() >= 100) {
            statusBtn.setText("✔ Completed");
            statusBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #00d289; -fx-border-color: #00d289; -fx-border-radius: 5; -fx-background-radius: 5;");
        } else {
            statusBtn.setText("In Progress");
            statusBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0a0ab; -fx-border-color: #ffffff33; -fx-border-radius: 5; -fx-background-radius: 5;");
        }

        Button certBtn = new Button("🎓 Cert");
        certBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");
        certBtn.setDisable(e.getProgress() < 100);
        certBtn.setOnAction(evt -> {
            // Re-use logic for cert generation
            generateCertificate(c);
        });

        actions.getChildren().addAll(statusBtn, certBtn);

        // View button
        Button viewBtn = new Button("👁 View Course");
        viewBtn.setMaxWidth(Double.MAX_VALUE);
        viewBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: #ffffff22; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");
        viewBtn.setOnAction(evt -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/courses/CourseDetails.fxml"));
                Parent root = loader.load();
                CourseDetailsController controller = loader.getController();
                controller.setCourse(c);
                StackPane contentArea = (StackPane) cardContainer.getScene().lookup("#contentArea");
                if (contentArea != null) contentArea.getChildren().setAll(root);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        infoBox.getChildren().addAll(title, metaRow, progress, progressText, actions, viewBtn);
        card.getChildren().addAll(header, infoBox);
        return card;
    }

    private void generateCertificate(Course currentCourse) {
        com.edulink.gui.models.User student = com.edulink.gui.util.SessionManager.getCurrentUser();
        if (student == null) return;

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
        alert.initOwner(cardContainer.getScene().getWindow());

        java.util.Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() != cancelBtn) {
            boolean isPdf = result.get() == pdfBtn;
            
            FileChooser fc = new FileChooser();
            fc.setTitle("Save Certificate");
            fc.setInitialFileName("Certificate_" + currentCourse.getTitle().replace(" ", "_") + (isPdf ? ".pdf" : ".png"));
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(isPdf ? "PDF Document" : "PNG Image", isPdf ? "*.pdf" : "*.png"));

            File file = fc.showSaveDialog(cardContainer.getScene().getWindow());
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
}
