package com.edulink.gui.controllers.journal;

import com.edulink.gui.models.journal.Note;
import com.edulink.gui.models.journal.NoteCategory;
import com.edulink.gui.models.journal.Notebook;
import com.edulink.gui.services.journal.NoteCategoryService;
import com.edulink.gui.services.journal.NoteService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class NoteController {
    @FXML
    private FlowPane notesContainer;
    @FXML
    private Label notebookTitleLabel;
    private NoteService noteService = new NoteService();
    private NoteCategoryService categoryService = new NoteCategoryService();
    private Notebook currentNotebook;

    public void setNotebook(Notebook n) {
        this.currentNotebook = n;
        notebookTitleLabel.setText("Notebook: " + n.getTitle());
        loadData();
    }

    @FXML
    public void loadData() {
        notesContainer.getChildren().clear();
        if (currentNotebook == null)
            return;
        List<Note> notes = noteService.getByNotebook(currentNotebook.getId());
        List<NoteCategory> cats = categoryService.getAll();
        for (Note n : notes) {
            String catName = cats.stream()
                    .filter(c -> c.getId() == n.getCategoryId())
                    .map(NoteCategory::getName)
                    .findFirst()
                    .orElse("No Category");
            notesContainer.getChildren().add(createNoteCard(n, catName));
        }
    }

    private VBox createNoteCard(Note n, String categoryName) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(280);

        HBox header = new HBox(10);
        Label title = new Label(n.getTitle());
        title.getStyleClass().add("card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label catLabel = new Label(categoryName);
        catLabel.getStyleClass().addAll("badge", "badge-category");

        header.getChildren().addAll(title, spacer, catLabel);

        Label contentPreview = new Label(n.getContent());
        contentPreview.setWrapText(true);
        contentPreview.setMaxHeight(60);
        contentPreview.getStyleClass().add("card-description");

        HBox actions = new HBox(10);
        Button editBtn = new Button("✏ Edit");
        editBtn.getStyleClass().add("edit-button");
        editBtn.setOnAction(e -> handleEditNote(n));

        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setOnAction(e -> {
            noteService.delete(n.getId());
            loadData();
        });

        Button pdfBtn = new Button("📄 PDF");
        pdfBtn.getStyleClass().add("primary-button");
        pdfBtn.setOnAction(e -> handleExportPdf(n));

        actions.getChildren().addAll(editBtn, pdfBtn, deleteBtn);
        card.getChildren().addAll(header, contentPreview, actions);
        return card;
    }

    private void handleExportPdf(Note n) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Save Note as PDF");
        fileChooser.setInitialFileName(n.getTitle().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        java.io.File file = fileChooser.showSaveDialog(notesContainer.getScene().getWindow());
        if (file != null) {
            try {
                com.edulink.gui.util.PdfExporter.exportNote(n, file);
                showAlert("Success", "Note exported successfully to " + file.getName());
            } catch (Exception e) {
                showAlert("Error", "Failed to export PDF: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void handleBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/journal/NotebookList.fxml"));
            StackPane contentArea = (StackPane) notesContainer.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleNewNote() {
        showNoteEditor(null);
    }

    private void handleEditNote(Note n) {
        showNoteEditor(n);
    }

    private void showNoteEditor(Note existingNote) {
        Dialog<Note> dialog = new Dialog<>();
        dialog.setTitle(existingNote == null ? "New Note" : "Edit Note");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        VBox grid = new VBox(10);
        TextField titleField = new TextField(existingNote == null ? "" : existingNote.getTitle());
        titleField.setPromptText("Title");

        ComboBox<NoteCategory> catCombo = new ComboBox<>();
        List<NoteCategory> categories = categoryService.getAll();
        catCombo.getItems().addAll(categories);
        catCombo.setPromptText("Select Category");
        if (existingNote != null) {
            categories.stream()
                    .filter(c -> c.getId() == existingNote.getCategoryId())
                    .findFirst()
                    .ifPresent(catCombo::setValue);
        }

        TextArea contentField = new TextArea(existingNote == null ? "" : existingNote.getContent());
        contentField.setPromptText("Content");

        grid.getChildren().addAll(new Label("Title:"), titleField, new Label("Category:"), catCombo,
                new Label("Content:"), contentField);
        dialog.getDialogPane().setContent(grid);

        // Validation
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (titleField.getText().trim().isEmpty() || contentField.getText().trim().isEmpty()
                    || catCombo.getValue() == null) {
                showAlert("Validation Error", "All fields (Title, Category, Content) are required!");
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Note res = existingNote == null ? new Note() : existingNote;
                res.setTitle(titleField.getText());
                res.setContent(contentField.getText());
                res.setNotebookId(currentNotebook.getId());
                res.setCategoryId(catCombo.getValue() != null ? catCombo.getValue().getId() : 0);
                res.setTags("");
                return res;
            }
            return null;
        });

        Optional<Note> result = dialog.showAndWait();
        result.ifPresent(n -> {
            if (existingNote == null)
                noteService.add2(n);
            else
                noteService.edit(n);
            loadData();
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
