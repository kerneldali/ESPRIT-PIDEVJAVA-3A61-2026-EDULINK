package com.edulink.gui.controllers.challenge;

import com.edulink.gui.models.challenge.Challenge;
import com.edulink.gui.services.challenge.ChallengeImageService;
import com.edulink.gui.services.challenge.ChallengeParticipationService;
import com.edulink.gui.services.challenge.ChallengeService;
import com.edulink.gui.util.EduAlert;
import com.edulink.gui.util.SessionManager; // utilisé pour currentUserId
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class ChallengeListController implements Initializable {

    @FXML private VBox cardContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> difficultyFilterCombo;
    @FXML private ComboBox<String> sortCombo;

    private final ChallengeService challengeService                = new ChallengeService();
    private final ChallengeParticipationService participationService = new ChallengeParticipationService();
    private final ChallengeImageService imageService               = new ChallengeImageService();
    private final ObservableList<Challenge> challengeList          = FXCollections.observableArrayList();

    private int currentUserId;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUserId = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getId() : -1;

        difficultyFilterCombo.setItems(FXCollections.observableArrayList("All", "EASY", "MEDIUM", "HARD"));
        difficultyFilterCombo.setValue("All");

        sortCombo.setItems(FXCollections.observableArrayList(
                "Titre A → Z", "Titre Z → A", "XP croissant", "XP décroissant", "Deadline proche"));
        sortCombo.setValue("Titre A → Z");

        searchField.textProperty().addListener((obs, o, n) -> filterData());
        difficultyFilterCombo.valueProperty().addListener((obs, o, n) -> filterData());
        sortCombo.valueProperty().addListener((obs, o, n) -> filterData());

        // One-shot backfill: any pre-existing challenge without a cover gets one.
        // The work is just URL-building + UPDATE statements, so it's cheap.
        try { challengeService.backfillImageUrls(imageService); } catch (Exception ignore) {}

        loadData();
    }

    // ── Données ───────────────────────────────────────────────────────────────

    private void loadData() {
        challengeList.setAll(challengeService.getOpen());
        filterData();
    }

    private void filterData() {
        cardContainer.getChildren().clear();
        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String diff  = difficultyFilterCombo.getValue();
        String sort  = sortCombo.getValue();

        List<Challenge> filtered = challengeList.stream()
                .filter(c -> c.getTitle().toLowerCase().contains(query))
                .filter(c -> "All".equals(diff) || diff.equals(c.getDifficulty()))
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
            cardContainer.getChildren().add(createCard(c));
        }

        if (cardContainer.getChildren().isEmpty()) {
            Label empty = new Label("Aucun challenge disponible pour le moment. Reviens plus tard !");
            empty.setStyle("-fx-text-fill: #a0a0ab; -fx-font-size: 14px;");
            cardContainer.getChildren().add(empty);
        }
    }

    // ── Carte ─────────────────────────────────────────────────────────────────

    private VBox createCard(Challenge c) {
        boolean joined    = currentUserId != -1 && participationService.hasJoined(currentUserId, c.getId());
        boolean completed = currentUserId != -1 && participationService.hasCompleted(currentUserId, c.getId());

        VBox card = new VBox(12);
        // Border priority: gold for FEATURED, then green for completed, purple for joined.
        String borderColor;
        if (c.isFeatured())      borderColor = "#f59e0b";
        else if (completed)      borderColor = "#00d289";
        else if (joined)         borderColor = "#7c3aed";
        else                     borderColor = "#ffffff11";
        double borderWidth = c.isFeatured() ? 2.5 : 1.5;
        // Padding 0 on top so the cover image hugs the rounded corners; we re-add
        // horizontal/bottom padding inside the content rows.
        card.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 10; " +
                      "-fx-border-color: " + borderColor + "; -fx-border-radius: 10; " +
                      "-fx-border-width: " + borderWidth + ";");

        // ── Cover image (Pollinations AI) ──────────────────────────────────────
        if (c.getImageUrl() != null && !c.getImageUrl().isBlank()) {
            try {
                ImageView iv = new ImageView();
                // background loading: don't block the UI thread
                iv.setImage(new Image(c.getImageUrl(), 600, 200, true, true, true));
                iv.setFitWidth(600);
                iv.setFitHeight(160);
                iv.setPreserveRatio(false);
                iv.setSmooth(true);
                // Round only the top corners so the image fits the card's rounded outline.
                Rectangle clip = new Rectangle(600, 160);
                clip.setArcWidth(20);
                clip.setArcHeight(20);
                iv.setClip(clip);
                StackPane imgWrap = new StackPane(iv);
                imgWrap.setStyle("-fx-background-color: #0e0e1a; -fx-background-radius: 10 10 0 0;");
                card.getChildren().add(imgWrap);
            } catch (Exception ignore) {
                // bad URL → silently skip the cover, keep the rest of the card
            }
        }

        // Inner content holder so we still get padding around text rows even when
        // the cover image is flush with the card edges.
        VBox content = new VBox(12);
        content.setStyle("-fx-padding: 18;");

        // ── Ligne 1 : Titre + badges ─────────────────────────────────────────
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(c.getTitle());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label diffLabel = new Label(c.getDifficulty());
        String diffColor = "#3b82f6";
        if ("MEDIUM".equals(c.getDifficulty())) diffColor = "#f59e0b";
        else if ("HARD".equals(c.getDifficulty())) diffColor = "#ef4444";
        diffLabel.setStyle("-fx-background-color: " + diffColor + "; -fx-text-fill: white; " +
                "-fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Show effective XP (with boost) if active, otherwise base reward.
        int xpDisplay = c.getEffectiveXpReward();
        Label xpLabel = new Label("+" + xpDisplay + " XP");
        xpLabel.setStyle("-fx-text-fill: #00d289; -fx-font-weight: bold; -fx-font-size: 14px;");

        header.getChildren().addAll(titleLabel, diffLabel);
        if (c.isFeatured()) {
            Label featBadge = new Label("⭐ FEATURED");
            featBadge.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: #1a1a2e; " +
                    "-fx-padding: 3 10; -fx-background-radius: 12; " +
                    "-fx-font-size: 11px; -fx-font-weight: bold;");
            header.getChildren().add(featBadge);
        }
        if (c.isBoostActive()) {
            Label boostBadge = new Label("+" + c.getXpBoostPct() + "% XP");
            boostBadge.setStyle("-fx-background-color: #00d28922; -fx-text-fill: #00d289; " +
                    "-fx-padding: 3 10; -fx-background-radius: 12; " +
                    "-fx-font-size: 11px; -fx-font-weight: bold; " +
                    "-fx-border-color: #00d28966; -fx-border-radius: 12; -fx-border-width: 1;");
            header.getChildren().add(boostBadge);
        }
        header.getChildren().addAll(spacer, xpLabel);

        // ── Ligne 2 : Description ─────────────────────────────────────────────
        Label descLabel = new Label(c.getDescription());
        descLabel.setStyle("-fx-text-fill: #a0a0ab; -fx-font-size: 13px;");
        descLabel.setWrapText(true);

        // ── Ligne 3 : Deadline + bouton d'action ─────────────────────────────
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_LEFT);

        // Deadline
        boolean hasDeadline = c.getDeadline() != null;
        String deadlineStr  = hasDeadline ? "⏰  " + c.getDeadline().format(FORMATTER) : "Pas de deadline";
        boolean isUrgent    = hasDeadline && c.getDeadline().isBefore(LocalDateTime.now().plusDays(3));
        Label deadlineLabel = new Label(deadlineStr);
        deadlineLabel.setStyle("-fx-text-fill: " + (isUrgent ? "#ef4444" : "#7B7FA0") + "; -fx-font-size: 12px;");

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        // Bouton d'action selon l'état de participation
        Button actionBtn = buildActionButton(c, joined, completed);

        footer.getChildren().addAll(deadlineLabel, footerSpacer, actionBtn);

        content.getChildren().addAll(header, descLabel, footer);
        card.getChildren().add(content);
        com.edulink.gui.util.ThemeManager.applyTheme(card);
        return card;
    }

    // ── Bouton contextuel ─────────────────────────────────────────────────────

    private Button buildActionButton(Challenge c, boolean joined, boolean completed) {
        if (completed) {
            Button btn = new Button("✅  Complété");
            btn.setStyle("-fx-background-color: #00d28922; -fx-text-fill: #00d289; " +
                         "-fx-font-weight: bold; -fx-padding: 8 18; -fx-background-radius: 8;");
            btn.setDisable(true);
            return btn;
        }
        if (joined) {
            // Déjà rejoint → indique d'aller dans "Mes Challenges" pour soumettre
            Button btn = new Button("📁  Voir dans Mes Challenges");
            btn.setStyle("-fx-background-color: #7c3aed33; -fx-text-fill: #bb86fc; " +
                         "-fx-font-weight: bold; -fx-padding: 8 18; -fx-background-radius: 8;");
            btn.setDisable(true);
            return btn;
        }
        // Pas encore inscrit → bouton "Rejoindre"
        Button btn = new Button("🚀  Rejoindre");
        btn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; " +
                     "-fx-font-weight: bold; -fx-padding: 8 18; -fx-background-radius: 8; -fx-cursor: hand;");
        btn.setOnAction(e -> handleJoin(c));
        return btn;
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    private void handleJoin(Challenge c) {
        if (currentUserId == -1) {
            EduAlert.show(EduAlert.AlertType.WARNING, "Non connecté",
                    "Tu dois être connecté pour rejoindre un challenge.");
            return;
        }
        boolean ok = participationService.join(currentUserId, c.getId());
        if (ok) {
            EduAlert.show(EduAlert.AlertType.SUCCESS, "Challenge rejoint !",
                    "Tu as rejoint \"" + c.getTitle() + "\".\n" +
                    "Va dans \"Mes Challenges\" pour soumettre ton travail 💪");
            loadData();
        } else {
            EduAlert.show(EduAlert.AlertType.ERROR, "Erreur",
                    "Impossible de rejoindre ce challenge. Tu es peut-être déjà inscrit.");
        }
    }
}
