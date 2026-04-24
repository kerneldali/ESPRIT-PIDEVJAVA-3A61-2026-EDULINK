package com.edulink.gui.controllers.journal;

import com.edulink.gui.models.journal.Notebook;
import com.edulink.gui.services.journal.NotebookService;
import com.edulink.gui.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class NotebookController implements Initializable {
    @FXML
    private FlowPane notebooksContainer;
    private NotebookService notebookService = new NotebookService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadData();
    }

    @FXML
    public void loadData() {
        notebooksContainer.getChildren().clear();
        int userId = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 1;
        List<Notebook> notebooks = notebookService.getByUser(userId);
        for (Notebook n : notebooks) {
            notebooksContainer.getChildren().add(createNotebookCard(n));
        }
    }

    private VBox createNotebookCard(Notebook n) {
        VBox card = new VBox(15);
        card.getStyleClass().add("card");
        card.setPrefWidth(220);

        Label title = new Label(n.getTitle());
        title.getStyleClass().add("card-title");

        Label date = new Label(
                "Created: " + (n.getCreatedAt() != null ? n.getCreatedAt().toString().substring(0, 10) : "N/A"));
        date.getStyleClass().add("card-date");

        HBox actions = new HBox(10);
        Button openBtn = new Button("📂 Open");
        openBtn.getStyleClass().add("edit-button");
        openBtn.setOnAction(e -> openNotebook(n));

        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setOnAction(e -> {
            notebookService.delete(n.getId());
            loadData();
        });

        actions.getChildren().addAll(openBtn, deleteBtn);
        card.getChildren().addAll(title, date, actions);

        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2)
                openNotebook(n);
        });

        com.edulink.gui.util.ThemeManager.applyTheme(card);

        return card;
    }

    private void openNotebook(Notebook n) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/journal/NoteList.fxml"));
            Parent root = loader.load();
            NoteController controller = loader.getController();
            controller.setNotebook(n);

            StackPane contentArea = (StackPane) notebooksContainer.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleNewNotebook() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Notebook");
        dialog.setHeaderText("Create a new notebook category");
        dialog.setContentText("Enter title:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(title -> {
            if (!title.trim().isEmpty()) {
                Notebook n = new Notebook();
                n.setTitle(title);
                n.setCoverColor("#BB86FC");
                n.setUserId(SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 1);
                notebookService.add2(n);
                loadData();
            } else {
                showAlert("Validation Error", "Notebook title cannot be empty!");
            }
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
