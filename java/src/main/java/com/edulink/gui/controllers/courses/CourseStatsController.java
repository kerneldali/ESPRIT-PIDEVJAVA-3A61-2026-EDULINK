package com.edulink.gui.controllers.courses;

import com.edulink.gui.models.courses.Course;
import com.edulink.gui.services.courses.CourseService;
import com.edulink.gui.services.courses.ResourceService;
import com.edulink.gui.services.courses.EnrollmentService;
import com.edulink.gui.util.EduAlert;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.net.URL;
import java.util.ResourceBundle;

public class CourseStatsController implements Initializable {

    @FXML private VBox cardContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusComboFilter;

    private CourseService courseService = new CourseService();
    private ResourceService resourceService = new ResourceService();
    private EnrollmentService enrollmentService = new EnrollmentService();
    private ObservableList<Course> courseList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statusComboFilter.setItems(FXCollections.observableArrayList("All Statuses", "ACTIVE", "ARCHIVED", "ACCEPTED", "PENDING"));
        statusComboFilter.setValue("All Statuses");

        searchField.textProperty().addListener((obs, oldV, newV) -> filterData());
        statusComboFilter.valueProperty().addListener((obs, oldV, newV) -> filterData());

        loadData();
    }

    @FXML
    private void loadData() {
        courseList.setAll(courseService.getAll());
        filterData();
    }

    private void filterData() {
        cardContainer.getChildren().clear();
        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String status = statusComboFilter.getValue();

        java.util.List<Course> filtered = courseList.stream()
            .filter(c -> (c.getTitle() != null && c.getTitle().toLowerCase().contains(query)))
            .filter(c -> {
                if ("All Statuses".equals(status)) return true;
                if ("ACTIVE".equals(status)) return "ACCEPTED".equalsIgnoreCase(c.getStatus()) || "FORCE_ACTIVE".equalsIgnoreCase(c.getStatus());
                if ("ARCHIVED".equals(status)) return "ARCHIVED".equalsIgnoreCase(c.getStatus()) || "FORCE_ARCHIVED".equalsIgnoreCase(c.getStatus());
                return status.equalsIgnoreCase(c.getStatus());
            })
            .collect(java.util.stream.Collectors.toList());

        for (Course c : filtered) {
            cardContainer.getChildren().add(createStatsCard(c));
        }
    }

    private VBox createStatsCard(Course c) {
        VBox card = new VBox(10);
        card.setStyle("-fx-padding: 15; -fx-background-color: #2a2a3e; -fx-border-color: #ffffff11; -fx-border-radius: 8; -fx-background-radius: 8;");

        // --- Stats & Score Calculation ---
        int enrollCount = 0;
        double totalProgress = 0.0;
        java.util.List<com.edulink.gui.models.courses.Enrollment> enrolls = enrollmentService.getAll().stream()
            .filter(e -> e.getCoursId() == c.getId())
            .collect(java.util.stream.Collectors.toList());
        enrollCount = enrolls.size();
        for (com.edulink.gui.models.courses.Enrollment e : enrolls) {
            totalProgress += e.getProgress();
        }
        double avgProgress = enrollCount > 0 ? totalProgress / enrollCount : 0.0;
        int resCount = resourceService.findByCourse(c.getId()).size();
        int quizzes = c.getQuizCount(); 
        int summaries = c.getSummaryCount(); 
        double score = Math.min(enrollCount * 5, 25) 
                     + (avgProgress * 0.25) 
                     + Math.min(resCount * 2, 20) 
                     + Math.min(quizzes * 2, 15) 
                     + Math.min(summaries * 2, 15);
                     
        if (score > 100) score = 100;

        boolean needsAutoArchive = false;
        if ("ACCEPTED".equals(c.getStatus()) && score < 25) {
            long daysPassed = java.time.temporal.ChronoUnit.DAYS.between(
                c.getCreatedAt() != null ? c.getCreatedAt() : java.time.LocalDateTime.now(), 
                java.time.LocalDateTime.now()
            );
            if (daysPassed >= 7) {
                c.setStatus("ARCHIVED");
                courseService.edit(c);
                sendEmailToOwner(c, "Automated system: Low performance (Score: " + Math.round(score) + "/100) after 7 days", true);
                needsAutoArchive = true;
            }
        }

        // Top Header
        HBox topRow = new HBox(10);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label title = new Label(c.getTitle());
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");

        String displayStatus = c.getStatus() != null ? c.getStatus() : "ACTIVE";
        if ("FORCE_ACTIVE".equals(displayStatus)) displayStatus = "ACTIVE (FORCED)";
        if ("FORCE_ARCHIVED".equals(displayStatus)) displayStatus = "ARCHIVED (FORCED)";
        Label statusLabel = new Label(displayStatus);
        
        String statusColor = ("ARCHIVED".equals(c.getStatus()) || "FORCE_ARCHIVED".equals(c.getStatus())) ? "#ef4444" : "#10b981";
        statusLabel.setStyle("-fx-background-color: " + statusColor + "; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label scoreLabel = new Label("Performance Score: " + Math.round(score) + "/100");
        scoreLabel.setStyle("-fx-text-fill: #eab308; -fx-font-weight: bold; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        boolean isArchived = "ARCHIVED".equals(c.getStatus()) || "FORCE_ARCHIVED".equals(c.getStatus());
        Button archiveBtn = new Button(isArchived ? "Re open Course" : "Archive Course");
        archiveBtn.setStyle(isArchived ? "-fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-weight: bold;" 
                                       : "-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-weight: bold;");
        archiveBtn.setOnAction(e -> {
            if (isArchived) {
                if (EduAlert.confirm("Re open Course", "Re open '" + c.getTitle() + "' manually?")) {
                    c.setStatus("FORCE_ACTIVE");
                    courseService.edit(c);
                    sendEmailToOwner(c, "Manual admin decision", false);
                    loadData();
                }
            } else {
                if (EduAlert.confirm("Archive Course", "Archive '" + c.getTitle() + "' manually?")) {
                    c.setStatus("FORCE_ARCHIVED");
                    courseService.edit(c);
                    sendEmailToOwner(c, "Manual admin decision", true);
                    loadData();
                }
            }
        });

        topRow.getChildren().addAll(title, statusLabel, spacer, scoreLabel, archiveBtn);

        // Stats details
        HBox statsRow = new HBox(15);
        statsRow.setStyle("-fx-padding: 10 0 0 0; -fx-border-color: #ffffff11 transparent transparent transparent;");
        
        Label l1 = new Label("👥 Enrolled: " + enrollCount);
        Label l2 = new Label("📈 Avg Progress: " + Math.round(avgProgress) + "%");
        Label l3 = new Label("📚 Resources: " + resCount);
        Label l4 = new Label("❓ Quizzes: " + quizzes);
        Label l5 = new Label("📝 Summaries: " + summaries);

        String statStyle = "-fx-text-fill: #a0a0ab; -fx-font-size: 13px;";
        l1.setStyle(statStyle); l2.setStyle(statStyle); l3.setStyle(statStyle);
        l4.setStyle(statStyle); l5.setStyle(statStyle);

        statsRow.getChildren().addAll(l1, l2, l3, l4, l5);

        card.getChildren().addAll(topRow, statsRow);
        com.edulink.gui.util.ThemeManager.applyTheme(card);

        if (needsAutoArchive) {
            javafx.application.Platform.runLater(this::loadData);
        }

        return card;
    }

    private void sendEmailToOwner(Course c, String reason, boolean isArchivedMode) {
        com.edulink.gui.services.UserService us = new com.edulink.gui.services.UserService();
        com.edulink.gui.models.User author = null;
        for (com.edulink.gui.models.User u : us.getAll()) {
            if (u.getId() == c.getAuthorId()) {
                author = u;
                break;
            }
        }
        
        String ownerEmail = (author != null && author.getEmail() != null) ? author.getEmail() : "admin@edulink.com";
        String ownerName = (author != null && author.getFullName() != null) ? author.getFullName() : "Course Owner";
        
        String subject = isArchivedMode ? "Course Archived: " + c.getTitle() : "Course Re-opened: " + c.getTitle();
        
        String colorTheme = isArchivedMode ? "#ef4444" : "#10b981";
        String statusText = isArchivedMode ? "has been archived" : "has been successfully re-opened";
        String nextSteps = isArchivedMode ? "Please update or improve the course content to re-activate it." : "Students can now enroll and interact with your course again!";
        
        String htmlBody = "<div style=\"background-color: #f4f6f9; padding: 20px; font-family: Arial, sans-serif; color: #333;\">"
                + "<div style=\"max-width: 600px; margin: 0 auto; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);\">"
                + "<div style=\"background-color: #2a2a3e; color: white; padding: 20px; text-align: center;\">"
                + "<h1 style=\"margin: 0; font-size: 24px;\">\uD83C\uDF93 EduLink</h1>"
                + "<p style=\"margin: 5px 0 0; font-size: 14px; opacity: 0.8;\">Plateforme \u00E9tudiante ESPRIT</p>"
                + "</div>"
                + "<div style=\"padding: 30px;\">"
                + "<h2 style=\"color: #2a2a3e; margin-top: 0;\">Course Update Notification</h2>"
                + "<p>Hello <strong>" + ownerName + "</strong>,</p>"
                + "<p>Your course '<strong>" + c.getTitle() + "</strong>' " + statusText + ".</p>"
                + "<div style=\"background-color: #f8fafc; border-left: 4px solid " + colorTheme + "; padding: 15px; margin: 20px 0; border-radius: 4px;\">"
                + "<p style=\"margin: 0;\"><strong>Reason:</strong> " + reason + "</p>"
                + "</div>"
                + "<p>" + nextSteps + "</p>"
                + "<p>If you have any questions, please contact the administration.</p>"
                + "<p style=\"margin-bottom: 0;\">Best regards,<br><strong>The EduLink Admin Team</strong></p>"
                + "</div></div></div>";
                    
        System.out.println("================= EMAIL NOTIFICATION =================");
        System.out.println("To: " + ownerEmail);
        System.out.println("Subject: " + subject);
        System.out.println("HTML EMAIL PREPARED");
        System.out.println("======================================================");
        
        // Actual SMTP Sending
        new Thread(() -> {
            try {
                java.util.Properties props = new java.util.Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");
                
                final String username = "eya.makhlouf.isic@gmail.com"; 
                final String password = "baunvjikeeyfhqfy"; 
                
                jakarta.mail.Session session = jakarta.mail.Session.getInstance(props,
                  new jakarta.mail.Authenticator() {
                    protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                        return new jakarta.mail.PasswordAuthentication(username, password);
                    }
                  });

                jakarta.mail.Message message = new jakarta.mail.internet.MimeMessage(session);
                message.setFrom(new jakarta.mail.internet.InternetAddress(username));
                message.setRecipients(jakarta.mail.Message.RecipientType.TO, jakarta.mail.internet.InternetAddress.parse(ownerEmail));
                message.setSubject(subject);
                message.setContent(htmlBody, "text/html; charset=utf-8");

                try {
                    jakarta.mail.Transport.send(message);
                    System.out.println("✅ Real SMTP Email sent successfully to " + ownerEmail);
                } catch (Exception sendEx) {
                    System.err.println("❌ SMTP Failed (Ensure credentials are set): " + sendEx.getMessage());
                }
            } catch (Exception ex) {
                System.err.println("❌ Error setting up SMTP email: " + ex.getMessage());
            }
        }).start();
        
        String alertTitle = isArchivedMode ? "Course Archived" : "Course Re-opened";
        String alertMessage = isArchivedMode ? "The course was archived. An email notification has been triggered for the owner:\n" + ownerEmail + "\nReason: " + reason
                                             : "The course was re-opened. An email notification has been triggered for the owner:\n" + ownerEmail + "\nReason: " + reason;
        
        javafx.application.Platform.runLater(() -> {
            EduAlert.show(EduAlert.AlertType.INFO, alertTitle, alertMessage);
        });
    }
}
