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
        String studentName = "Student";
        if (com.edulink.gui.util.SessionManager.getCurrentUser() != null) {
            studentName = com.edulink.gui.util.SessionManager.getCurrentUser().getFullName();
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Save Certificate");
        fc.setInitialFileName("Certificate_" + currentCourse.getTitle().replace(" ", "_") + ".txt");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Certificate", "*.txt"));

        File file = fc.showSaveDialog(cardContainer.getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("=================================================");
                writer.println("                EDULINK ACADEMY                ");
                writer.println("=================================================");
                writer.println();
                writer.println("           CERTIFICATE OF COMPLETION           ");
                writer.println();
                writer.println("This is to certify that:");
                writer.println("      " + studentName.toUpperCase());
                writer.println();
                writer.println("Has successfully completed the course:");
                writer.println("      " + currentCourse.getTitle().toUpperCase());
                writer.println();
                writer.println("Level: " + currentCourse.getLevel());
                writer.println("XP Gained: " + currentCourse.getXp());
                writer.println("Date: " + LocalDateTime.now().toLocalDate());
                writer.println();
                writer.println("=================================================");
                writer.println("            KEEP LEARNING, KEEP GROWING          ");
                writer.println("=================================================");
                
                EduAlert.show(EduAlert.AlertType.SUCCESS, "Certificate Saved", "Your certificate has been saved to:\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                EduAlert.show(EduAlert.AlertType.ERROR, "Export Error", "Failed to save certificate: " + ex.getMessage());
            }
        }
    }
}
