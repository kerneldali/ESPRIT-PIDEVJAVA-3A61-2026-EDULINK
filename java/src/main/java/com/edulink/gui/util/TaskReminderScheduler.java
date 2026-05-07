package com.edulink.gui.util;

import com.edulink.gui.models.journal.PersonalTask;
import com.edulink.gui.services.journal.PersonalTaskService;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskReminderScheduler {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final PersonalTaskService taskService = new PersonalTaskService();

    public static void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                int currentUserId = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId()
                        : -1;
                if (currentUserId == -1)
                    return;

                List<PersonalTask> dueTasks = taskService.getDueReminders();
                for (PersonalTask task : dueTasks) {
                    if (task.getUserId() == currentUserId) {
                        Platform.runLater(() -> showNotification(task));
                        taskService.clearReminder(task.getId());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    private static void showNotification(PersonalTask task) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Task Reminder");
        alert.setHeaderText("Reminder: " + task.getTitle());
        alert.setContentText("It's time to do: " + task.getTitle());
        alert.show();
    }

    public static void stop() {
        scheduler.shutdown();
    }
}
