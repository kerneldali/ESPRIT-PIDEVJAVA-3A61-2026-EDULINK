package com.edulink.gui.controllers.challenge;

import com.edulink.gui.models.challenge.Challenge;
import com.edulink.gui.models.challenge.ChallengeTask;
import com.edulink.gui.services.challenge.ChallengeService;
import com.edulink.gui.services.challenge.ChallengeTaskService;
import com.edulink.gui.util.EduAlert;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ManageChallengesController implements Initializable {

    // --- Liste ---
    @FXML private VBox cardContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> difficultyFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> sortCombo;

    // --- Formulaire overlay ---
    @FXML private VBox formOverlay;
    @FXML private Label formTitle;
    @FXML private TextField titleField;
    @FXML private TextArea descField;
    @FXML private ComboBox<String> difficultyCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextField xpField;
    @FXML private DatePicker deadlinePicker;
    @FXML private Spinner<Integer> hourSpinner;
    @FXML private Spinner<Integer> minuteSpinner;
    @FXML private Button saveBtn;
    @FXML private Label titleError;
    @FXML private Label descError;
    @FXML private Label xpError;
    @FXML private Label deadlineError;

    // --- Task overlay ---
    @FXML private VBox taskOverlay;
    @FXML private Label taskOverlayTitle;
    @FXML private VBox taskListContainer;
    @FXML private TextField newTaskTitleField;
    @FXML private TextField newTaskDescField;
    @FXML private CheckBox newTaskRequiredCheck;

    private final ChallengeService challengeService     = new ChallengeService();
    private final ChallengeTaskService taskService      = new ChallengeTaskService();
    private final ObservableList<Challenge> challengeList = FXCollections.observableArrayList();
    private Challenge currentEditable  = null;
    private Challenge currentTaskChallenge = null; // challenge dont on gère les tâches

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        difficultyFilterCombo.setItems(FXCollections.observableArrayList("All", "EASY", "MEDIUM", "HARD"));
        difficultyFilterCombo.setValue("All");

        statusFilterCombo.setItems(FXCollections.observableArrayList("All", "OPEN", "CLOSED"));
        statusFilterCombo.setValue("All");

        difficultyCombo.setItems(FXCollections.observableArrayList("EASY", "MEDIUM", "HARD"));
        difficultyCombo.setValue("EASY");

        statusCombo.setItems(FXCollections.observableArrayList("OPEN", "CLOSED"));
        statusCombo.setValue("OPEN");

        sortCombo.setItems(FXCollections.observableArrayList(
                "Titre A → Z", "Titre Z → A", "XP croissant", "XP décroissant", "Deadline proche"));
        sortCombo.setValue("Titre A → Z");

        // Listeners pour le filtrage dynamique
        searchField.textProperty().addListener((obs, o, n) -> filterData());
        difficultyFilterCombo.valueProperty().addListener((obs, o, n) -> filterData());
        statusFilterCombo.valueProperty().addListener((obs, o, n) -> filterData());
        sortCombo.valueProperty().addListener((obs, o, n) -> filterData());

        // Spinners heure (0-23) et minutes (0-59) avec affichage 2 chiffres
        hourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0));
        minuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        styleSpinner(hourSpinner);
        styleSpinner(minuteSpinner);

        // Style du DatePicker
        deadlinePicker.setStyle("-fx-background-color: #2a2a3e;");
        deadlinePicker.setConverter(new StringConverter<LocalDate>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            @Override public String toString(LocalDate d)   { return d != null ? d.format(fmt) : ""; }
            @Override public LocalDate fromString(String s) {
                return (s != null && !s.isEmpty()) ? LocalDate.parse(s, fmt) : null;
            }
        });

        // Listeners validation
        titleField.textProperty().addListener((obs, o, n) -> validateForm());
        descField.textProperty().addListener((obs, o, n) -> validateForm());
        xpField.textProperty().addListener((obs, o, n) -> validateForm());
        deadlinePicker.valueProperty().addListener((obs, o, n) -> validateForm());

        loadData();
    }

    // ======================== DONNÉES ========================

    private void loadData() {
        challengeList.setAll(challengeService.getAll());
        filterData();
    }

    private void filterData() {
        cardContainer.getChildren().clear();
        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String diff  = difficultyFilterCombo.getValue();
        String stat  = statusFilterCombo.getValue();
        String sort  = sortCombo.getValue();

        List<Challenge> filtered = challengeList.stream()
                .filter(c -> c.getTitle().toLowerCase().contains(query))
                .filter(c -> "All".equals(diff) || diff.equals(c.getDifficulty()))
                .filter(c -> "All".equals(stat) || stat.equals(c.getStatus()))
                .collect(Collectors.toList());

        Comparator<Challenge> cmp = switch (sort == null ? "" : sort) {
            case "Titre Z → A"     -> Comparator.comparing(Challenge::getTitle, String.CASE_INSENSITIVE_ORDER).reversed();
            case "XP croissant"    -> Comparator.comparingInt(Challenge::getXpReward);
            case "XP décroissant"  -> Comparator.comparingInt(Challenge::getXpReward).reversed();
            case "Deadline proche" -> Comparator.comparing(c -> c.getDeadline() == null
                                        ? java.time.LocalDateTime.MAX : c.getDeadline());
            default                -> Comparator.comparing(Challenge::getTitle, String.CASE_INSENSITIVE_ORDER);
        };
        filtered.sort(cmp);

        for (Challenge c : filtered) {
            cardContainer.getChildren().add(createRow(c));
        }
    }

    // ======================== CARTE / ROW ========================

    private HBox createRow(Challenge c) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 12 15; -fx-border-color: transparent transparent #ffffff09 transparent;");

        Label titleLabel = new Label(c.getTitle());
        titleLabel.setPrefWidth(220);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        // Badge difficulté
        Label diffLabel = new Label(c.getDifficulty());
        diffLabel.setPrefWidth(90);
        String diffColor = "#3b82f6"; // EASY = bleu
        if ("MEDIUM".equals(c.getDifficulty())) diffColor = "#f59e0b";
        else if ("HARD".equals(c.getDifficulty())) diffColor = "#ef4444";
        diffLabel.setStyle("-fx-background-color: " + diffColor + "; -fx-text-fill: white; -fx-padding: 3 8; " +
                "-fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold; -fx-alignment: center;");

        // XP
        Label xpLabel = new Label("+" + c.getXpReward() + " XP");
        xpLabel.setPrefWidth(80);
        xpLabel.setStyle("-fx-text-fill: #00d289; -fx-font-weight: bold;");

        // Badge statut
        Label statusLabel = new Label(c.getStatus());
        statusLabel.setPrefWidth(70);
        String statColor = "OPEN".equals(c.getStatus()) ? "#00d289" : "#a0a0ab";
        statusLabel.setStyle("-fx-text-fill: " + statColor + "; -fx-font-size: 12px; -fx-font-weight: bold;");

        // Deadline
        String deadlineStr = c.getDeadline() != null ? c.getDeadline().format(FORMATTER) : "—";
        Label deadlineLabel = new Label(deadlineStr);
        deadlineLabel.setPrefWidth(140);
        deadlineLabel.setStyle("-fx-text-fill: #a0a0ab; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; " +
                "-fx-border-color: #ffffff22; -fx-border-radius: 5; -fx-cursor: hand;");
        editBtn.setOnAction(e -> showForm(c));

        Button delBtn = new Button("🗑");
        delBtn.setStyle("-fx-background-color: #ef444433; -fx-text-fill: #ef4444; " +
                "-fx-background-radius: 5; -fx-cursor: hand;");
        delBtn.setOnAction(e -> {
            if (EduAlert.confirm("Delete Challenge", "Delete '" + c.getTitle() + "'?")) {
                challengeService.delete(c.getId());
                loadData();
            }
        });

        Button tasksBtn = new Button("📋 Tasks");
        tasksBtn.setStyle("-fx-background-color: #7c3aed33; -fx-text-fill: #bb86fc; " +
                "-fx-border-color: #7c3aed55; -fx-border-radius: 5; -fx-cursor: hand; -fx-padding: 4 10;");
        tasksBtn.setOnAction(e -> openTaskOverlay(c));

        row.getChildren().addAll(titleLabel, diffLabel, xpLabel, statusLabel, deadlineLabel, spacer, tasksBtn, editBtn, delBtn);
        com.edulink.gui.util.ThemeManager.applyTheme(row);
        return row;
    }

    // ======================== TASK OVERLAY ========================

    private void openTaskOverlay(Challenge c) {
        currentTaskChallenge = c;
        taskOverlayTitle.setText("📋  Tâches — " + c.getTitle());
        newTaskTitleField.clear();
        newTaskDescField.clear();
        newTaskRequiredCheck.setSelected(true);
        refreshTaskList();
        taskOverlay.setVisible(true);
        taskOverlay.toFront();
    }

    private void refreshTaskList() {
        taskListContainer.getChildren().clear();
        if (currentTaskChallenge == null) return;
        List<ChallengeTask> tasks = taskService.getByChallenge(currentTaskChallenge.getId());
        if (tasks.isEmpty()) {
            Label empty = new Label("Aucune tâche pour ce challenge.");
            empty.setStyle("-fx-text-fill: #a0a0ab; -fx-font-size: 13px;");
            taskListContainer.getChildren().add(empty);
            return;
        }
        for (ChallengeTask t : tasks) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 10 12; -fx-background-color: #2a2a3e; " +
                         "-fx-background-radius: 8; -fx-border-color: #ffffff11; -fx-border-radius: 8;");

            Label orderLbl = new Label(String.valueOf(t.getOrderIndex()));
            orderLbl.setStyle("-fx-text-fill: #7B7FA0; -fx-font-size: 11px;");
            orderLbl.setPrefWidth(20);

            Label reqBadge = new Label(t.isRequired() ? "Obligatoire" : "Optionnel");
            reqBadge.setStyle("-fx-background-color: " + (t.isRequired() ? "#ef444433" : "#3b82f633") + "; " +
                    "-fx-text-fill: " + (t.isRequired() ? "#ef4444" : "#3b82f6") + "; " +
                    "-fx-padding: 2 8; -fx-background-radius: 8; -fx-font-size: 10px;");

            Label titleLbl = new Label(t.getTitle());
            titleLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            HBox.setHgrow(titleLbl, Priority.ALWAYS);

            Label descLbl = new Label(t.getDescription() != null ? t.getDescription() : "");
            descLbl.setStyle("-fx-text-fill: #a0a0ab; -fx-font-size: 12px;");
            descLbl.setPrefWidth(180);

            Button delTaskBtn = new Button("🗑");
            delTaskBtn.setStyle("-fx-background-color: #ef444433; -fx-text-fill: #ef4444; " +
                    "-fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 4 8;");
            delTaskBtn.setOnAction(e -> {
                taskService.delete(t.getId());
                refreshTaskList();
            });

            row.getChildren().addAll(orderLbl, reqBadge, titleLbl, descLbl, delTaskBtn);
            taskListContainer.getChildren().add(row);
        }
    }

    @FXML
    private void handleAddTask() {
        if (currentTaskChallenge == null) return;
        String title = newTaskTitleField.getText().trim();
        if (title.isEmpty()) {
            EduAlert.show(EduAlert.AlertType.WARNING, "Champ requis", "Le titre de la tâche est obligatoire.");
            return;
        }
        int order = taskService.getByChallenge(currentTaskChallenge.getId()).size() + 1;
        ChallengeTask task = new ChallengeTask(
                currentTaskChallenge.getId(), title,
                newTaskDescField.getText().trim(), order,
                newTaskRequiredCheck.isSelected());
        taskService.add(task);
        newTaskTitleField.clear();
        newTaskDescField.clear();
        newTaskRequiredCheck.setSelected(true);
        refreshTaskList();
    }

    @FXML
    private void handleCloseTaskOverlay() {
        taskOverlay.setVisible(false);
        currentTaskChallenge = null;
    }

    // ======================== FORMULAIRE ========================

    @FXML
    private void handleNewChallenge() {
        showForm(null);
    }

    private void showForm(Challenge c) {
        currentEditable = c;
        if (c != null) {
            formTitle.setText("Edit Challenge");
            titleField.setText(c.getTitle());
            descField.setText(c.getDescription());
            difficultyCombo.setValue(c.getDifficulty());
            statusCombo.setValue(c.getStatus());
            xpField.setText(String.valueOf(c.getXpReward()));
            if (c.getDeadline() != null) {
                deadlinePicker.setValue(c.getDeadline().toLocalDate());
                hourSpinner.getValueFactory().setValue(c.getDeadline().getHour());
                minuteSpinner.getValueFactory().setValue(c.getDeadline().getMinute());
            } else {
                deadlinePicker.setValue(null);
                hourSpinner.getValueFactory().setValue(0);
                minuteSpinner.getValueFactory().setValue(0);
            }
        } else {
            formTitle.setText("New Challenge");
            titleField.clear();
            descField.clear();
            difficultyCombo.setValue("EASY");
            statusCombo.setValue("OPEN");
            xpField.setText("0");
            deadlinePicker.setValue(null);
            hourSpinner.getValueFactory().setValue(0);
            minuteSpinner.getValueFactory().setValue(0);
        }
        clearErrors();
        formOverlay.setVisible(true);
        formOverlay.toFront();
        validateForm();
    }

    @FXML
    private void handleCloseForm() {
        formOverlay.setVisible(false);
    }

    @FXML
    private void handleSave() {
        if (!isFormValid()) return;

        Challenge result = currentEditable != null ? currentEditable : new Challenge();
        result.setTitle(titleField.getText().trim());
        result.setDescription(descField.getText().trim());
        result.setDifficulty(difficultyCombo.getValue());
        result.setStatus(statusCombo.getValue());

        try {
            result.setXpReward(Integer.parseInt(xpField.getText().trim()));
        } catch (NumberFormatException e) {
            result.setXpReward(0);
        }

        LocalDate selectedDate = deadlinePicker.getValue();
        if (selectedDate != null) {
            int h = hourSpinner.getValue();
            int m = minuteSpinner.getValue();
            result.setDeadline(LocalDateTime.of(selectedDate, java.time.LocalTime.of(h, m)));
        } else {
            result.setDeadline(null);
        }

        if (currentEditable == null) {
            result.setCreatedAt(LocalDateTime.now());
            challengeService.add(result);
            EduAlert.show(EduAlert.AlertType.SUCCESS, "Success", "Challenge created successfully!");
        } else {
            challengeService.edit(result);
            EduAlert.show(EduAlert.AlertType.SUCCESS, "Success", "Challenge updated successfully!");
        }

        handleCloseForm();
        loadData();
    }

    // ======================== VALIDATION ========================

    private void validateForm() {
        saveBtn.setDisable(!isFormValid());
    }

    private boolean isFormValid() {
        clearErrors();
        boolean valid = true;

        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        if (title.isEmpty()) {
            titleError.setText("Title is required");
            valid = false;
        } else if (title.length() < 3) {
            titleError.setText("Title must be at least 3 characters");
            valid = false;
        }

        String desc = descField.getText() == null ? "" : descField.getText().trim();
        if (desc.isEmpty()) {
            descError.setText("Description is required");
            valid = false;
        }

        String xp = xpField.getText() == null ? "" : xpField.getText().trim();
        if (!xp.isEmpty()) {
            try {
                if (Integer.parseInt(xp) < 0) {
                    xpError.setText("XP must be positive");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                xpError.setText("XP must be a number");
                valid = false;
            }
        }

        // La deadline est optionnelle — aucune validation nécessaire si non renseignée

        return valid;
    }

    private void clearErrors() {
        titleError.setText("");
        descError.setText("");
        xpError.setText("");
        deadlineError.setText("");
    }

    /** Affiche les valeurs du spinner sur 2 chiffres (01, 09, 23...) */
    private void styleSpinner(Spinner<Integer> spinner) {
        spinner.setEditable(true);
        spinner.getValueFactory().setConverter(new StringConverter<Integer>() {
            @Override public String toString(Integer v)    { return v != null ? String.format("%02d", v) : "00"; }
            @Override public Integer fromString(String s) {
                try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
            }
        });
        spinner.setStyle("-fx-background-color: #2a2a3e; -fx-text-fill: white;");
    }
}
