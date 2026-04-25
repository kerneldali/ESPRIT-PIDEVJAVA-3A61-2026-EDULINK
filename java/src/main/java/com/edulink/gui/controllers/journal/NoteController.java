package com.edulink.gui.controllers.journal;

import com.edulink.gui.models.journal.Note;
import com.edulink.gui.models.journal.NoteCategory;
import com.edulink.gui.models.journal.Notebook;
import com.edulink.gui.services.journal.NoteCategoryService;
import com.edulink.gui.services.journal.NoteService;
import com.edulink.gui.services.journal.SentimentAnalysisService;
import com.edulink.gui.services.journal.NoteAIService;
import com.edulink.gui.util.EduAlert;
import com.edulink.gui.util.ThemeManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class NoteController {
    @FXML
    private FlowPane notesContainer;
    @FXML
    private VBox remindersContainer;
    @FXML
    private Label notebookTitleLabel;

    private Notebook currentNotebook;
    private NoteService noteService = new NoteService();
    private NoteCategoryService categoryService = new NoteCategoryService();
    private SentimentAnalysisService sentimentService = new SentimentAnalysisService();
    private NoteAIService aiService = new NoteAIService();

    public void setNotebook(Notebook n) {
        this.currentNotebook = n;
        notebookTitleLabel.setText("Notebook: " + (n != null ? n.getTitle() : "All Notes"));
        loadData();
    }

    @FXML
    public void loadData() {
        loadNotes();
        loadReminders();
    }

    private void loadNotes() {
        notesContainer.getChildren().clear();
        if (currentNotebook == null)
            return;
        List<Note> notes = noteService.getByNotebook(currentNotebook.getId());
        List<NoteCategory> cats = categoryService.getAll();
        for (Note n : notes) {
            if (!n.isShared()) {
                String catName = cats.stream()
                        .filter(c -> c.getId() == n.getCategoryId())
                        .map(NoteCategory::getName)
                        .findFirst()
                        .orElse("No Category");
                notesContainer.getChildren().add(createNoteCard(n, catName));
            }
        }
    }

    private void loadReminders() {
        if (remindersContainer == null)
            return;
        remindersContainer.getChildren().clear();
        List<Note> allNotes = noteService.getAll();
        for (Note note : allNotes) {
            if (note.isShared()) {
                remindersContainer.getChildren().add(createReminderItem(note));
            }
        }
    }

    private VBox createReminderItem(Note note) {
        VBox item = new VBox(5);
        item.getStyleClass().add("reminder-item");
        item.setStyle(
                "-fx-background-color: #1a1a2e; -fx-padding: 10; -fx-background-radius: 8; -fx-border-color: #333355; -fx-border-radius: 8;");

        Label title = new Label(note.getTitle());
        title.setStyle("-fx-text-fill: #EDEDED; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label date = new Label(
                "🔔 " + (note.getReminderAt() != null ? note.getReminderAt().toString() : "No time set"));
        date.setStyle("-fx-text-fill: #BB86FC; -fx-font-size: 12px;");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button editBtn = new Button("✎");
        editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #7B7FA0; -fx-cursor: hand;");
        editBtn.setOnAction(e -> handleEditNote(note));

        Button delBtn = new Button("🗑");
        delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #CF6679; -fx-cursor: hand;");
        delBtn.setOnAction(e -> {
            noteService.delete(note.getId());
            loadData();
        });
        actions.getChildren().addAll(editBtn, delBtn);

        item.getChildren().addAll(title, date, actions);
        ThemeManager.applyTheme(item);
        return item;
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
        ThemeManager.applyTheme(card);
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
    public void handleWeeklySummary() {
        // Fetch only personal notes (exclude shared reminders) for the summary
        List<Note> allNotes = noteService.getAll().stream()
                .filter(n -> !n.isShared())
                .toList();

        if (allNotes.isEmpty()) {
            EduAlert.show(EduAlert.AlertType.INFO, "Gemini AI Weekly Summary", "No personal notes found to summarize.");
            return;
        }

        String jsonRes = aiService.generateWeeklySummary(allNotes);

        String summary = "Summary failed";
        String cat = "General";

        try {
            // Find summary
            if (jsonRes.contains("\"summary\":")) {
                int sIdx = jsonRes.indexOf("\"summary\":") + 10;
                int start = jsonRes.indexOf("\"", sIdx) + 1;
                int end = jsonRes.indexOf("\"", start);
                summary = jsonRes.substring(start, end);
            }
            // Find category
            if (jsonRes.contains("\"category\":")) {
                int cIdx = jsonRes.indexOf("\"category\":") + 11;
                int start = jsonRes.indexOf("\"", cIdx) + 1;
                int end = jsonRes.indexOf("\"", start);
                cat = jsonRes.substring(start, end);
            }
        } catch (Exception e) {
            summary = jsonRes;
        }

        EduAlert.show(EduAlert.AlertType.INFO, "Gemini AI Weekly Summary",
                summary + "\n\nSuggested Category: " + cat);
    }

    @FXML
    public void handleNewNote() {
        showNoteEditor(null, false);
    }

    @FXML
    public void handleNewReminder() {
        showNoteEditor(null, true);
    }

    private void handleEditNote(Note n) {
        showNoteEditor(n, n.isShared());
    }

    private void showNoteEditor(Note existingNote, boolean isReminder) {
        Dialog<Note> dialog = new Dialog<>();
        dialog.setTitle(existingNote == null ? (isReminder ? "Add Reminder" : "New Note") : "Edit Note");

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

        DatePicker datePicker = new DatePicker();
        TextField timeField = new TextField("12:00");
        timeField.setPromptText("HH:mm");
        if (existingNote != null && existingNote.getReminderAt() != null) {
            datePicker.setValue(existingNote.getReminderAt().toLocalDateTime().toLocalDate());
            timeField.setText(existingNote.getReminderAt().toLocalDateTime().toLocalTime().toString());
        }

        grid.getChildren().addAll(new Label("Title:"), titleField);
        if (!isReminder) {
            grid.getChildren().addAll(new Label("Category:"), catCombo);
        }
        grid.getChildren().addAll(new Label("Content:"), contentField,
                new Label("Reminder Date:"), datePicker, new Label("Reminder Time (HH:mm):"), timeField);
        dialog.getDialogPane().setContent(grid);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            boolean isTitleEmpty = titleField.getText().trim().isEmpty();
            boolean isContentEmpty = contentField.getText().trim().isEmpty();
            boolean isCatEmpty = !isReminder && catCombo.getValue() == null;

            if (isTitleEmpty || isContentEmpty || isCatEmpty) {
                showAlert("Validation Error", "Please fill in all required fields!");
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Note res = existingNote == null ? new Note() : existingNote;
                res.setTitle(titleField.getText());
                res.setContent(contentField.getText());
                res.setNotebookId(currentNotebook != null ? currentNotebook.getId() : 0);
                res.setCategoryId(catCombo.getValue() != null ? catCombo.getValue().getId() : 0);
                res.setTags("");
                res.setShared(isReminder);

                if (datePicker.getValue() != null) {
                    try {
                        String timeStr = timeField.getText().isEmpty() ? "00:00" : timeField.getText();
                        java.time.LocalDateTime ldt = java.time.LocalDateTime.of(datePicker.getValue(),
                                java.time.LocalTime.parse(timeStr));
                        res.setReminderAt(Timestamp.valueOf(ldt));
                    } catch (Exception e) {
                        System.err.println("Invalid time format: " + timeField.getText());
                    }
                }
                return res;
            }
            return null;
        });

        Optional<Note> result = dialog.showAndWait();
        result.ifPresent(n -> {
            String sentiment = sentimentService.predict(n.getContent());
            n.setSentiment(sentiment);

            if (existingNote == null)
                noteService.add2(n);
            else
                noteService.edit(n);

            if ("negative".equals(sentiment)) {
                EduAlert.show(EduAlert.AlertType.WARNING, "Stay Positive!",
                        "We noticed your note has a negative tone. Remember: 'Every challenge is an opportunity to grow!' You've got this!");
            }
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
