package com.edulink.gui.util;

import com.edulink.gui.models.journal.PersonalTask;
import com.edulink.gui.services.journal.PersonalTaskService;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskReminderScheduler {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "TaskReminderThread");
        t.setDaemon(true);
        return t;
    });

    // Track already-shown reminders so we don't spam the user
    private static final Set<Integer> shownReminderIds = new HashSet<>();

    public static void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                int currentUserId = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId()
                        : -1;
                if (currentUserId == -1)
                    return;

                // Create a fresh service each time to avoid stale DB connections
                PersonalTaskService taskService = new PersonalTaskService();
                List<PersonalTask> dueTasks = taskService.getDueReminders();

                for (PersonalTask task : dueTasks) {
                    if (task.getUserId() == currentUserId && !shownReminderIds.contains(task.getId())) {
                        shownReminderIds.add(task.getId());
                        Platform.runLater(() -> showReminderPopup(task));
                        taskService.clearReminder(task.getId());
                    }
                }
            } catch (Exception e) {
                System.err.println("⚠️ Reminder scheduler error: " + e.getMessage());
            }
        }, 5, 15, TimeUnit.SECONDS); // Start after 5s, check every 15s

        System.out.println("🔔 Task Reminder Scheduler started (checks every 15 seconds)");
    }

    private static void showReminderPopup(PersonalTask task) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.getDialogPane().getButtonTypes().add(ButtonType.OK);
        alert.setTitle("⏰ Task Reminder");

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);
        content.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);"
                        + "-fx-background-radius: 10;");

        Label bellEmoji = new Label("🔔");
        bellEmoji.setStyle("-fx-font-size: 40px;");

        Label titleLabel = new Label("Reminder!");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        titleLabel.setStyle("-fx-text-fill: #fbbf24;");

        Label taskTitle = new Label(task.getTitle());
        taskTitle.setWrapText(true);
        taskTitle.setFont(Font.font("System", FontWeight.NORMAL, 16));
        taskTitle.setStyle("-fx-text-fill: #e2e8f0;");
        taskTitle.setMaxWidth(350);

        Label timeLabel = new Label("⏰ It's time to work on this task!");
        timeLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        content.getChildren().addAll(bellEmoji, titleLabel, taskTitle, timeLabel);

        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setPrefWidth(420);
        alert.getDialogPane().setStyle("-fx-background-color: #1a1a2e;");

        // Style the OK button
        javafx.scene.control.Button okBtn = (javafx.scene.control.Button) alert.getDialogPane()
                .lookupButton(ButtonType.OK);
        okBtn.setText("Got It! ✅");
        okBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #fbbf24, #f59e0b);"
                        + "-fx-text-fill: #1a1a2e;"
                        + "-fx-font-weight: bold;"
                        + "-fx-font-size: 13px;"
                        + "-fx-padding: 8 24;"
                        + "-fx-background-radius: 16;");

        alert.showAndWait();
    }

    public static void stop() {
        scheduler.shutdown();
    }
}
