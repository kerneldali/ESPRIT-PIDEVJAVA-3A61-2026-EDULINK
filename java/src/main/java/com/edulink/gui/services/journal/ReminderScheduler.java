package com.edulink.gui.services.journal;

import com.edulink.gui.models.journal.Note;
import com.edulink.gui.models.journal.PersonalTask;
import com.edulink.gui.util.EduAlert;
import com.edulink.gui.util.SessionManager;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background scheduler to check for reminders.
 */
public class ReminderScheduler {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final NoteService noteService = new NoteService();
    private final PersonalTaskService taskService = new PersonalTaskService();

    public void start() {
        scheduler.scheduleAtFixedRate(this::checkReminders, 0, 1, TimeUnit.MINUTES);
        System.out.println("⏰ Reminder Scheduler started.");
    }

    public void stop() {
        scheduler.shutdown();
    }

    private void checkReminders() {
        if (SessionManager.getCurrentUser() == null)
            return;

        int userId = SessionManager.getCurrentUser().getId();
        long now = System.currentTimeMillis();

        // Check Notes
        try {
            List<Note> notes = noteService.getAll();
            System.out.println(
                    "--- [REMINDER] Checking " + notes.size() + " notes. Now: " + new java.util.Date(now) + " ---");
            for (Note note : notes) {
                if (note.getReminderAt() != null) {
                    long reminderTime = note.getReminderAt().getTime();

                    // Trigger if it's due now or was missed in the past (within last 24h)
                    if (reminderTime <= now && (now - reminderTime) < (24 * 60 * 60 * 1000)) {
                        System.out.println("!!! [REMINDER] Triggering for Note: " + note.getTitle());
                        showNotification(note.getTitle(), "Reminder: " + note.getTitle());
                        // Clear reminder to avoid multiple alerts
                        note.setReminderAt(null);
                        noteService.edit(note);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking note reminders: " + e.getMessage());
        }

        // Check Tasks
        try {
            List<PersonalTask> tasks = taskService.getByUser(userId);
            for (PersonalTask task : tasks) {
                if (task.getReminderAt() != null && !task.isCompleted()) {
                    long reminderTime = task.getReminderAt().getTime();
                    if (reminderTime <= now && (now - reminderTime) < (24 * 60 * 60 * 1000)) {
                        System.out.println("!!! [REMINDER] Triggering for Task: " + task.getTitle());
                        showNotification(task.getTitle(), "Task due: " + task.getTitle());
                        // Clear reminder
                        task.setReminderAt(null);
                        taskService.edit(task);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking task reminders: " + e.getMessage());
        }
    }

    private void showNotification(String title, String message) {
        Platform.runLater(() -> {
            EduAlert.show(EduAlert.AlertType.INFO, title, message);
        });
    }
}
