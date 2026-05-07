package com.edulink.gui.controllers.journal;

import com.edulink.gui.models.journal.PersonalTask;
import com.edulink.gui.services.STTService;
import com.edulink.gui.services.journal.PersonalTaskService;
import com.edulink.gui.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class TaskController implements Initializable {
    @FXML
    private VBox tasksContainer;
    @FXML
    private VBox remindersContainer;
    @FXML
    private TextField newTaskField;
    @FXML
    private Button voiceTaskBtn;
    @FXML
    private Button sortBtn;
    @FXML
    private Label voiceTaskStatus;
    @FXML
    private TextField reminderTitleField;
    @FXML
    private DatePicker reminderDateField;
    @FXML
    private TextField reminderTimeField;

    private PersonalTaskService taskService = new PersonalTaskService();
    private STTService sttService = new STTService();
    private boolean isRecordingTask = false;
    private boolean sortedByDone = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadData();
    }

    @FXML
    public void loadData() {
        tasksContainer.getChildren().clear();
        remindersContainer.getChildren().clear();

        int userId = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 1;
        List<PersonalTask> allTasks = taskService.getByUser(userId);
        List<PersonalTask> regularTasks = allTasks.stream()
                .filter(t -> t.getReminderAt() == null)
                .collect(java.util.stream.Collectors.toList());

        if (sortedByDone) {
            regularTasks.sort((a, b) -> Boolean.compare(!a.isCompleted(), !b.isCompleted())); // done first
        }

        for (PersonalTask t : regularTasks) {
            tasksContainer.getChildren().add(createTaskRow(t));
        }
        allTasks.stream().filter(t -> t.getReminderAt() != null)
                .forEach(t -> remindersContainer.getChildren().add(createReminderRow(t)));
    }

    @FXML
    public void handleSort() {
        sortedByDone = !sortedByDone;
        if (sortBtn != null) {
            sortBtn.setText(sortedByDone ? "✅ Sorted: Done First" : "⬇ Sort: Done First");
            sortBtn.setStyle(sortedByDone
                    ? "-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 8;"
                    : "-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 8;");
        }
        loadData();
    }

    private HBox createTaskRow(PersonalTask t) {
        HBox row = new HBox(15);
        row.getStyleClass().add("card");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setPadding(new javafx.geometry.Insets(10, 15, 10, 15));

        CheckBox cb = new CheckBox();
        cb.setSelected(t.isCompleted());
        cb.setOnAction(e -> {
            t.setCompleted(cb.isSelected());
            taskService.edit(t);
            loadData();
        });

        Label title = new Label(t.getTitle());
        title.getStyleClass().add("card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setOnAction(e -> {
            taskService.delete(t.getId());
            loadData();
        });

        row.getChildren().addAll(cb, title, spacer, deleteBtn);
        return row;
    }

    private HBox createReminderRow(PersonalTask t) {
        HBox row = new HBox(15);
        row.getStyleClass().add("card");
        row.setStyle("-fx-border-color: #8e44ad; -fx-border-width: 0 0 0 5;");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setPadding(new javafx.geometry.Insets(10, 15, 10, 15));

        VBox info = new VBox(2);
        Label title = new Label(t.getTitle());
        title.getStyleClass().add("card-title");
        Label time = new Label("⏰ " + t.getReminderAt().toString());
        time.setStyle("-fx-font-size: 11px; -fx-text-fill: #8e44ad;");
        info.getChildren().addAll(title, time);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editBtn = new Button("✏");
        editBtn.setOnAction(e -> handleEditReminder(t));

        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setOnAction(e -> {
            taskService.delete(t.getId());
            loadData();
        });

        row.getChildren().addAll(info, spacer, editBtn, deleteBtn);
        return row;
    }

    @FXML
    public void handleAddTask() {
        String title = newTaskField.getText().trim();
        if (title.isEmpty())
            return;
        PersonalTask t = new PersonalTask();
        t.setTitle(title);
        t.setUserId(SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 1);
        t.setCompleted(false);
        taskService.add(t);
        newTaskField.clear();
        loadData();
    }

    @FXML
    public void handleVoiceTask() {
        if (!isRecordingTask) {
            try {
                sttService.startRecording();
                isRecordingTask = true;
                voiceTaskBtn.setText("⏹ Stop");
                voiceTaskBtn.setStyle(
                        "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 8;");
                voiceTaskStatus.setText("🔴 Recording...");
            } catch (Exception ex) {
                voiceTaskStatus.setText("❌ Mic error");
            }
        } else {
            isRecordingTask = false;
            voiceTaskBtn.setText("🎙 Voice");
            voiceTaskBtn.setStyle(
                    "-fx-background-color: #6366f1; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 8;");
            voiceTaskStatus.setText("⌛ Transcribing...");
            new Thread(() -> {
                try {
                    String text = sttService.stopRecordingAndTranscribe();
                    Platform.runLater(() -> {
                        newTaskField.setText(text);
                        voiceTaskStatus.setText("✅ Done — press Enter to add!");
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> voiceTaskStatus.setText("❌ Transcription error"));
                }
            }).start();
        }
    }

    @FXML
    public void handleAddReminder() {
        String title = reminderTitleField.getText().trim();
        java.time.LocalDate date = reminderDateField.getValue();
        String timeStr = reminderTimeField.getText().trim();

        if (title.isEmpty() || date == null || timeStr.isEmpty()) {
            showAlert("Input Error", "Please fill all reminder fields (Title, Date, Time).");
            return;
        }

        try {
            java.time.LocalTime time = java.time.LocalTime.parse(timeStr);
            java.time.LocalDateTime ldt = java.time.LocalDateTime.of(date, time);
            java.sql.Timestamp reminderTs = java.sql.Timestamp.valueOf(ldt);

            PersonalTask t = new PersonalTask();
            t.setTitle(title);
            t.setUserId(SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 1);
            t.setReminderAt(reminderTs);
            t.setCompleted(false);

            taskService.add(t);
            clearReminderInputs();
            loadData();
        } catch (Exception e) {
            showAlert("Format Error", "Invalid time format. Use HH:mm (e.g. 14:30)");
        }
    }

    private void handleEditReminder(PersonalTask t) {
        // Simplified edit: load into inputs
        reminderTitleField.setText(t.getTitle());
        reminderDateField.setValue(t.getReminderAt().toLocalDateTime().toLocalDate());
        reminderTimeField.setText(t.getReminderAt().toLocalDateTime().toLocalTime().toString());
        taskService.delete(t.getId()); // Delete original, user will save new
        loadData();
    }

    private void clearReminderInputs() {
        reminderTitleField.clear();
        reminderDateField.setValue(null);
        reminderTimeField.clear();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
