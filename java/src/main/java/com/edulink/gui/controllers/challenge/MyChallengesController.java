package com.edulink.gui.controllers.challenge;

import com.edulink.gui.models.challenge.Challenge;
import com.edulink.gui.models.challenge.ChallengeParticipation;
import com.edulink.gui.models.challenge.ChallengeTask;
import com.edulink.gui.services.UserService;
import com.edulink.gui.services.challenge.ChallengeParticipationService;
import com.edulink.gui.services.challenge.ChallengeService;
import com.edulink.gui.services.challenge.ChallengeTaskService;
import com.edulink.gui.util.EduAlert;
import com.edulink.gui.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class MyChallengesController implements Initializable {

    @FXML private VBox cardContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> sortCombo;

    // XP badge in header
    @FXML private HBox xpBadge;
    @FXML private Label xpLabel;
    @FXML private Label xpLevelLabel;
    @FXML private ProgressBar xpProgressBar;

    // Overlay de soumission
    @FXML private VBox submissionOverlay;
    @FXML private Label overlayTitle;
    @FXML private VBox taskChecklistContainer;
    @FXML private TextArea submissionTextArea;
    @FXML private Label selectedFileLabel;
    @FXML private Label submissionHint; // message rejet éventuel

    private final ChallengeService challengeService               = new ChallengeService();
    private final ChallengeParticipationService participationService = new ChallengeParticipationService();
    private final ChallengeTaskService taskService                = new ChallengeTaskService();
    private final UserService userService                         = new UserService();

    // ── Level math (tweak constants in one place) ─────────────────────────────
    private static final int XP_PER_LEVEL = 100;

    private int currentUserId;
    private int currentChallengeId = -1;
    private String selectedFilePath = null;
    private ChallengeParticipation currentParticipation = null; // participation en cours de soumission
    private final ObservableList<ChallengeParticipation> participationList = FXCollections.observableArrayList();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUserId = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getId() : -1;

        statusFilterCombo.setItems(FXCollections.observableArrayList(
                "Tous", "JOINED", "SUBMITTED", "COMPLETED", "REJECTED"));
        statusFilterCombo.setValue("Tous");

        sortCombo.setItems(FXCollections.observableArrayList(
                "Titre A → Z", "Titre Z → A", "Date rejoint (récent)", "Date rejoint (ancien)", "XP décroissant"));
        sortCombo.setValue("Titre A → Z");

        searchField.textProperty().addListener((obs, o, n) -> filterData());
        statusFilterCombo.valueProperty().addListener((obs, o, n) -> filterData());
        sortCombo.valueProperty().addListener((obs, o, n) -> filterData());

        loadData();
        refreshXpBadge();
    }

    /**
     * Pulls the latest XP from DB and updates the header badge (XP, level, progress).
     * Safe to call multiple times — used at init and after a successful submission.
     */
    private void refreshXpBadge() {
        if (xpLabel == null || xpLevelLabel == null || xpProgressBar == null) return;
        if (currentUserId == -1) {
            if (xpBadge != null) xpBadge.setVisible(false);
            return;
        }
        int xp = userService.getXpById(currentUserId);
        int level = (xp / XP_PER_LEVEL) + 1;
        int xpInLevel = xp % XP_PER_LEVEL;
        double progress = xpInLevel / (double) XP_PER_LEVEL;

        xpLabel.setText(xp + " XP");
        xpLevelLabel.setText("Niveau " + level + " — " + xpInLevel + "/" + XP_PER_LEVEL);
        xpProgressBar.setProgress(progress);
    }

    // ── Données ───────────────────────────────────────────────────────────────

    private void loadData() {
        if (currentUserId == -1) {
            participationList.clear();
        } else {
            participationList.setAll(participationService.getByUser(currentUserId));
        }
        filterData();
    }

    private void filterData() {
        cardContainer.getChildren().clear();

        if (currentUserId == -1) {
            Label lbl = new Label("Tu dois être connecté pour voir tes challenges.");
            lbl.setStyle("-fx-text-fill: #a0a0ab; -fx-font-size: 14px;");
            cardContainer.getChildren().add(lbl);
            return;
        }

        String query  = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String filter = statusFilterCombo.getValue();
        String sort   = sortCombo.getValue();

        // Construire une liste (participation, challenge) pour pouvoir trier
        record PC(ChallengeParticipation p, Challenge c) {}
        List<PC> pairs = participationList.stream()
                .map(p -> new PC(p, challengeService.getById(p.getChallengeId())))
                .filter(pc -> pc.c() != null)
                .filter(pc -> pc.c().getTitle().toLowerCase().contains(query)
                           || pc.c().getDescription().toLowerCase().contains(query))
                .filter(pc -> "Tous".equals(filter) || filter.equals(pc.p().getStatus().name()))
                .collect(Collectors.toList());

        Comparator<PC> cmp = switch (sort == null ? "" : sort) {
            case "Titre Z → A"          -> Comparator.comparing(pc -> pc.c().getTitle(),
                                            String.CASE_INSENSITIVE_ORDER.reversed());
            case "Date rejoint (récent)"-> Comparator.comparing(pc -> pc.p().getJoinedAt(),
                                            Comparator.reverseOrder());
            case "Date rejoint (ancien)"-> Comparator.comparing(pc -> pc.p().getJoinedAt());
            case "XP décroissant"       -> Comparator.comparingInt((PC pc) -> pc.c().getXpReward()).reversed();
            default                     -> Comparator.comparing(pc -> pc.c().getTitle(),
                                            String.CASE_INSENSITIVE_ORDER);
        };
        pairs.sort(cmp);

        for (PC pc : pairs) {
            cardContainer.getChildren().add(createCard(pc.p(), pc.c()));
        }

        if (cardContainer.getChildren().isEmpty()) {
            Label lbl = participationList.isEmpty()
                    ? new Label("Tu n'as encore rejoint aucun challenge. Explore les challenges actifs !")
                    : new Label("Aucun challenge ne correspond à ta recherche.");
            lbl.setStyle("-fx-text-fill: #a0a0ab; -fx-font-size: 14px;");
            cardContainer.getChildren().add(lbl);
        }
    }

    // ── Carte ─────────────────────────────────────────────────────────────────

    private VBox createCard(ChallengeParticipation p, Challenge c) {
        VBox card = new VBox(12);
        String borderColor = getBorderColor(p.getStatus());
        card.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 18; -fx-background-radius: 10; " +
                      "-fx-border-color: " + borderColor + "; -fx-border-radius: 10; -fx-border-width: 1.5;");

        // ── Titre + badge difficulté + XP
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(c.getTitle());
        titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");

        Label diffLbl = new Label(c.getDifficulty());
        String diffColor = "MEDIUM".equals(c.getDifficulty()) ? "#f59e0b"
                         : "HARD".equals(c.getDifficulty())   ? "#ef4444" : "#3b82f6";
        diffLbl.setStyle("-fx-background-color: " + diffColor + "; -fx-text-fill: white; " +
                "-fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        Label xpLbl = new Label("+" + c.getXpReward() + " XP");
        xpLbl.setStyle("-fx-text-fill: #00d289; -fx-font-weight: bold;");

        header.getChildren().addAll(titleLbl, diffLbl, sp, xpLbl);

        // ── Badge statut
        Label statusBadge = buildStatusBadge(p.getStatus());

        // ── Description (courte)
        Label descLbl = new Label(c.getDescription());
        descLbl.setStyle("-fx-text-fill: #a0a0ab; -fx-font-size: 12px;");
        descLbl.setWrapText(true);

        // ── Footer : date + action
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_LEFT);
        Label dateLbl = new Label("Rejoint le " + p.getJoinedAt().format(FMT));
        dateLbl.setStyle("-fx-text-fill: #7B7FA0; -fx-font-size: 11px;");
        Region footSp = new Region(); HBox.setHgrow(footSp, Priority.ALWAYS);

        footer.getChildren().add(dateLbl);
        footer.getChildren().add(footSp);

        // Bouton d'action selon le statut
        switch (p.getStatus()) {
            case JOINED -> {
                Button submitBtn = new Button("📤  Soumettre mon travail");
                submitBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-padding: 8 18; -fx-background-radius: 8; -fx-cursor: hand;");
                submitBtn.setOnAction(e -> openSubmissionOverlay(p, c, null));
                footer.getChildren().add(submitBtn);
            }
            case SUBMITTED -> {
                Label pending = new Label("⏳  En attente de validation...");
                pending.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 12px;");
                footer.getChildren().add(pending);
            }
            case COMPLETED -> {
                Label done = new Label("✅  Validé · +" + c.getXpReward() + " XP gagnés");
                done.setStyle("-fx-text-fill: #00d289; -fx-font-weight: bold; -fx-font-size: 12px;");
                footer.getChildren().add(done);
            }
            case REJECTED -> {
                Button resubmitBtn = new Button("🔄  Resoumettre");
                resubmitBtn.setStyle("-fx-background-color: #ef444433; -fx-text-fill: #ef4444; " +
                        "-fx-font-weight: bold; -fx-padding: 8 18; -fx-background-radius: 8; -fx-cursor: hand;");
                resubmitBtn.setOnAction(e -> openSubmissionOverlay(p, c, "Soumission rejetée. Corrige ton travail et resoumet."));
                footer.getChildren().add(resubmitBtn);
            }
        }

        card.getChildren().addAll(header, statusBadge, descLbl, footer);
        com.edulink.gui.util.ThemeManager.applyTheme(card);
        return card;
    }

    // ── Overlay de soumission ─────────────────────────────────────────────────

    private void openSubmissionOverlay(ChallengeParticipation p, Challenge c, String hint) {
        currentParticipation = p;
        currentChallengeId   = c.getId();
        selectedFilePath     = null;

        overlayTitle.setText("Soumettre pour : " + c.getTitle());
        submissionTextArea.clear();
        selectedFileLabel.setText("Aucun fichier sélectionné");

        if (hint != null) {
            submissionHint.setText("⚠  " + hint);
            submissionHint.setVisible(true);
            submissionHint.setManaged(true);
        } else {
            submissionHint.setVisible(false);
            submissionHint.setManaged(false);
        }

        loadTaskChecklist(c.getId(), p.getId());

        submissionOverlay.setVisible(true);
        submissionOverlay.toFront();
    }

    private void loadTaskChecklist(int challengeId, int participationId) {
        taskChecklistContainer.getChildren().clear();
        List<ChallengeTask> tasks = taskService.getByChallenge(challengeId);
        if (tasks.isEmpty()) {
            taskChecklistContainer.setVisible(false);
            taskChecklistContainer.setManaged(false);
            return;
        }
        taskChecklistContainer.setVisible(true);
        taskChecklistContainer.setManaged(true);

        Set<Integer> completed = new HashSet<>(taskService.getCompletedTaskIds(participationId));

        for (ChallengeTask task : tasks) {
            String label = task.getTitle() + (task.isRequired() ? "  *" : "");
            CheckBox cb = new CheckBox(label);
            cb.setSelected(completed.contains(task.getId()));
            cb.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

            cb.selectedProperty().addListener((obs, wasOn, isOn) -> {
                if (isOn) {
                    taskService.markCompleted(participationId, task.getId());
                } else {
                    taskService.markUncompleted(participationId, task.getId());
                }
            });
            taskChecklistContainer.getChildren().add(cb);
        }
    }

    @FXML
    private void handleChooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un fichier");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"),
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("Code", "*.java", "*.py", "*.js", "*.zip")
        );
        File file = chooser.showOpenDialog(cardContainer.getScene().getWindow());
        if (file != null) {
            selectedFilePath = file.getAbsolutePath();
            selectedFileLabel.setText("📎  " + file.getName());
        }
    }

    @FXML
    private void handleSubmit() {
        if (currentParticipation == null) return;

        // Vérifier que toutes les tâches obligatoires sont cochées
        List<ChallengeTask> tasks = taskService.getByChallenge(currentChallengeId);
        if (!tasks.isEmpty() && !taskService.allRequiredCompleted(currentParticipation.getId(), currentChallengeId)) {
            EduAlert.show(EduAlert.AlertType.WARNING, "Tâches incomplètes",
                    "Complète toutes les tâches obligatoires (*) avant de soumettre.");
            return;
        }

        String text = submissionTextArea.getText().trim();
        if (text.isEmpty() && selectedFilePath == null) {
            EduAlert.show(EduAlert.AlertType.WARNING, "Soumission vide",
                    "Ajoute au moins un texte ou un fichier avant de soumettre.");
            return;
        }

        participationService.submit(currentUserId, currentParticipation.getChallengeId(),
                text.isEmpty() ? null : text, selectedFilePath);

        handleCloseOverlay();
        EduAlert.show(EduAlert.AlertType.SUCCESS, "Soumission envoyée !",
                "Ton travail a été envoyé. L'admin va le valider prochainement.");
        loadData();
    }

    @FXML
    private void handleCloseOverlay() {
        submissionOverlay.setVisible(false);
        currentParticipation = null;
        currentChallengeId   = -1;
        selectedFilePath     = null;
        if (taskChecklistContainer != null) taskChecklistContainer.getChildren().clear();
    }

    // ── Helpers visuels ───────────────────────────────────────────────────────

    private Label buildStatusBadge(ChallengeParticipation.Status status) {
        String text, color, bg;
        switch (status) {
            case SUBMITTED -> { text = "⏳  En attente"; color = "#f59e0b"; bg = "#f59e0b22"; }
            case COMPLETED -> { text = "✅  Complété";   color = "#00d289"; bg = "#00d28922"; }
            case REJECTED  -> { text = "❌  Rejeté";     color = "#ef4444"; bg = "#ef444422"; }
            default        -> { text = "🚀  Rejoint";    color = "#3b82f6"; bg = "#3b82f622"; }
        }
        Label lbl = new Label(text);
        lbl.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + color + "; " +
                     "-fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 12; " +
                     "-fx-background-radius: 12;");
        return lbl;
    }

    private String getBorderColor(ChallengeParticipation.Status status) {
        return switch (status) {
            case SUBMITTED -> "#f59e0b";
            case COMPLETED -> "#00d289";
            case REJECTED  -> "#ef4444";
            default        -> "#7c3aed";
        };
    }
}
