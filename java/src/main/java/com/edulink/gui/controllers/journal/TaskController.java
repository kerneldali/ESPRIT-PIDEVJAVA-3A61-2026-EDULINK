package com.edulink.gui.controllers.journal;

import com.edulink.gui.models.journal.PersonalTask;
import com.edulink.gui.services.journal.PersonalTaskService;
import com.edulink.gui.util.SessionManager;
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
    private TextField newTaskField;
    private PersonalTaskService taskService = new PersonalTaskService();
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

        taskService.add2(t);
        newTaskField.clear();
        loadData();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
