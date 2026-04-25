package com.edulink.gui.controllers.journal;

import com.edulink.gui.models.journal.PersonalTask;
import com.edulink.gui.services.journal.PersonalTaskService;
import com.edulink.gui.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import java.util.Optional;
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
    private TextField newTaskField;
    private PersonalTaskService taskService = new PersonalTaskService();
    private com.edulink.gui.services.journal.SpeechToTextService sttService = new com.edulink.gui.services.journal.SpeechToTextService();
    private boolean isSortedByIncomplete = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadData();
    }

    @FXML
    public void handleSortTasks() {
        isSortedByIncomplete = !isSortedByIncomplete;
        loadData();
    }

    @FXML
    public void loadData() {
        tasksContainer.getChildren().clear();
        // Fallback user ID 1 if no session (for testing)
        int userId = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 1;
        List<PersonalTask> tasks = taskService.getByUser(userId);

        if (isSortedByIncomplete) {
            tasks.sort((t1, t2) -> Boolean.compare(t1.isCompleted(), t2.isCompleted()));
        }

        for (PersonalTask t : tasks) {
            tasksContainer.getChildren().add(createTaskRow(t));
        }
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
        if (t.isCompleted()) {
            title.setStyle("-fx-strikethrough: true; -fx-opacity: 0.5;");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setOnAction(e -> {
            taskService.delete(t.getId());
            loadData();
        });

        row.getChildren().addAll(cb, title, spacer, deleteBtn);
        com.edulink.gui.util.ThemeManager.applyTheme(row);
        return row;
    }

    @FXML
    public void handleAddTask() {
        String title = newTaskField.getText().trim();
        if (title.isEmpty()) {
            showAlert("Validation Error", "Task title cannot be empty!");
            return;
        }

        PersonalTask t = new PersonalTask();
        t.setTitle(title);
        t.setUserId(SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 1);
        t.setCompleted(false);

        // Show quick reminder dialog
        TextInputDialog timeDialog = new TextInputDialog("12:00");
        timeDialog.setTitle("Set Reminder");
        timeDialog.setHeaderText("Set a reminder for today? (Leave blank for none)");
        timeDialog.setContentText("Format (HH:mm):");

        Optional<String> timeResult = timeDialog.showAndWait();
        if (timeResult.isPresent() && !timeResult.get().trim().isEmpty()) {
            try {
                java.time.LocalDateTime ldt = java.time.LocalDateTime.of(java.time.LocalDate.now(),
                        java.time.LocalTime.parse(timeResult.get().trim()));
                t.setReminderAt(java.sql.Timestamp.valueOf(ldt));
            } catch (Exception e) {
                showAlert("Error", "Invalid time format. Task added without reminder.");
            }
        }

        taskService.add2(t);
        newTaskField.clear();
        loadData();
    }

    @FXML
    public void handleVoiceInput() {
        javafx.concurrent.Task<String> sttTask = sttService.listen();
        sttTask.setOnSucceeded(e -> {
            String result = sttTask.getValue();
            if (result != null && result.startsWith("Error")) {
                showAlert("STT Error", result);
            } else if (result != null && result.startsWith("AI Error")) {
                showAlert("AI Note", result.replace("AI Error: ", ""));
            } else if (result != null && !result.isEmpty()) {
                newTaskField.setText(result);
                handleAddTask();
            }
        });
        sttTask.setOnFailed(e -> {
            Throwable ex = sttTask.getException();
            showAlert("Connection Error", "Is the AI backend running? " + (ex != null ? ex.getMessage() : ""));
        });
        new Thread(sttTask).start();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
