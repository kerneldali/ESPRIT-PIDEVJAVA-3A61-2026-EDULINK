package com.edulink.gui.controllers.courses;

import com.edulink.gui.models.User;
import com.edulink.gui.models.courses.Enrollment;
import com.edulink.gui.services.UserService;
import com.edulink.gui.services.courses.EnrollmentService;
import com.edulink.gui.util.EduAlert;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;

public class AnalyzeActivityController implements Initializable {

    @FXML private TableView<User> activityTable;
    @FXML private TableColumn<User, Integer> idCol;
    @FXML private TableColumn<User, String> nameCol;
    @FXML private TableColumn<User, String> emailCol;
    @FXML private TableColumn<User, Integer> scoreCol;
    @FXML private TableColumn<User, Integer> xpCol;
    @FXML private TableColumn<User, Integer> coursesCol;
    @FXML private TableColumn<User, String> lastActivityCol;
    @FXML private TableColumn<User, String> statusCol;
    @FXML private TableColumn<User, Void> actionCol;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusCombo;

    private UserService userService;
    private EnrollmentService enrollmentService;
    private ObservableList<User> userList;
    private FilteredList<User> filteredData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = new UserService();
        enrollmentService = new EnrollmentService();
        userList = FXCollections.observableArrayList();

        statusCombo.setItems(FXCollections.observableArrayList("All", "Active", "Medium", "Inactive"));
        statusCombo.getSelectionModel().selectFirst();

        setupTable();
        refreshData();

        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterTable());
        statusCombo.valueProperty().addListener((observable, oldValue, newValue) -> filterTable());
    }

    public void setAdminMode(boolean admin) {
        // Just empty to support Dashboard loader interface
    }

    private void filterTable() {
        filteredData.setPredicate(user -> {
            String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            String status = statusCombo.getValue();

            boolean matchesSearch = (user.getFullName() != null && user.getFullName().toLowerCase().contains(search))
                    || (user.getEmail() != null && user.getEmail().toLowerCase().contains(search));
            
            boolean matchesStatus = "All".equals(status) || (user.getEngagementStatus() != null && user.getEngagementStatus().equals(status));

            return matchesSearch && matchesStatus;
        });
    }

    private void setupTable() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("engagementScore"));
        xpCol.setCellValueFactory(new PropertyValueFactory<>("xp"));
        coursesCol.setCellValueFactory(new PropertyValueFactory<>("coursesTaken"));
        lastActivityCol.setCellValueFactory(new PropertyValueFactory<>("lastActivity"));

        statusCol.setCellValueFactory(new PropertyValueFactory<>("engagementStatus"));
        statusCol.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("Active".equals(item)) setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    else if ("Medium".equals(item)) setStyle("-fx-text-fill: #eab308; -fx-font-weight: bold;");
                    else setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                }
            }
        });

        actionCol.setCellFactory(param -> new TableCell<User, Void>() {
            private final Button remindBtn = new Button("Remind User");
            {
                remindBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 11px;");
                remindBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    sendReminderEmail(user, true);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User u = getTableView().getItems().get(getIndex());
                    if (u.getEngagementScore() < 30 || "Inactive".equals(u.getEngagementStatus())) {
                        setGraphic(remindBtn);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        filteredData = new FilteredList<>(userList, p -> true);
        SortedList<User> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(activityTable.comparatorProperty());
        activityTable.setItems(sortedData);
    }

    private void refreshData() {
        List<User> users = userService.getStudents();
        for (User u : users) {
            calculateEngagement(u);
        }
        userList.setAll(users);
        filterTable();
    }

    private void calculateEngagement(User user) {
        List<Enrollment> enrolls = enrollmentService.getEnrollmentByStudent(user.getId());
        int coursesTaken = enrolls.size();
        user.setCoursesTaken(coursesTaken);

        List<String> logs = userService.getUserTransactions(user.getId());
        if (!logs.isEmpty()) {
            String last = logs.get(logs.size() - 1);
            if (last.contains("]")) {
                user.setLastActivity(last.substring(1, last.indexOf("]")));
            } else {
                user.setLastActivity("Recent");
            }
        } else {
            user.setLastActivity("N/A");
        }

        int xpScore = Math.min(user.getXp() / 10, 40);
        int coursesScore = Math.min(coursesTaken * 10, 40);
        int activeScore = Math.min(logs.size() * 2, 20);

        int totalScore = xpScore + coursesScore + activeScore;
        user.setEngagementScore(totalScore);

        if (totalScore >= 70) user.setEngagementStatus("Active");
        else if (totalScore >= 30) user.setEngagementStatus("Medium");
        else user.setEngagementStatus("Inactive");

        // Auto-remind if extremely low and we haven't reminded recently
        if (totalScore < 30) {
            LocalDateTime lastReminded = user.getLastRemindedAt();
            if (lastReminded == null || ChronoUnit.DAYS.between(lastReminded, LocalDateTime.now()) >= 7) {
                sendReminderEmail(user, false);
            }
        }
    }

    private void sendReminderEmail(User user, boolean manual) {
        String toEmail = (user.getEmail() != null) ? user.getEmail() : "admin@edulink.com";
        String userName = (user.getFullName() != null) ? user.getFullName() : "Student";

        String subject = "We miss you on EduLink!";
        String body = "Hello " + userName + ",\n\n"
                + "We noticed it's been a while since you were active on EduLink.\n"
                + "Your current engagement score is " + user.getEngagementScore() + "/100.\n\n"
                + "Come back and keep progressing!\n"
                + "New courses and challenges await you.\n\n"
                + "Continue your learning journey with us today.\n\n"
                + "Best regards,\nThe EduLink Team";

        System.out.println("====== REMINDER EMAIL ======");
        System.out.println("To: " + toEmail);
        System.out.println("Subject: " + subject);
        System.out.println(body);
        System.out.println("============================");

        new Thread(() -> {
            try {
                java.util.Properties props = new java.util.Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");

                final String username = "eya.makhlouf.isic@gmail.com";
                final String password = "Maamoura123";

                javax.mail.Session session = javax.mail.Session.getInstance(props,
                        new javax.mail.Authenticator() {
                            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                                return new javax.mail.PasswordAuthentication(username, password);
                            }
                        });

                javax.mail.Message message = new javax.mail.internet.MimeMessage(session);
                message.setFrom(new javax.mail.internet.InternetAddress(username));
                message.setRecipients(javax.mail.Message.RecipientType.TO, javax.mail.internet.InternetAddress.parse(toEmail));
                message.setSubject(subject);
                message.setText(body);

                javax.mail.Transport.send(message);
                System.out.println("✅ Reminder sent to " + toEmail);

                userService.updateLastRemindedAt(user.getId());
                user.setLastRemindedAt(LocalDateTime.now());

                if (manual) {
                    Platform.runLater(() -> {
                        EduAlert.show(EduAlert.AlertType.SUCCESS, "Reminder Sent", "Reminder email has been sent successfully to " + user.getEmail());
                    });
                }
            } catch (Exception ex) {
                System.err.println("❌ Error sending reminder: " + ex.getMessage());
                if (manual) {
                    Platform.runLater(() -> EduAlert.show(EduAlert.AlertType.ERROR, "Error", "Failed to send email. Check logs."));
                }
            }
        }).start();
    }
}
