package com.edulink.gui.controllers.challenge;

import com.edulink.gui.models.User;
import com.edulink.gui.models.challenge.Challenge;
import com.edulink.gui.models.challenge.ChallengeParticipation;
import com.edulink.gui.services.UserService;
import com.edulink.gui.services.challenge.CertificateService;
import com.edulink.gui.services.challenge.ChallengeParticipationService;
import com.edulink.gui.services.challenge.ChallengeService;
import com.edulink.gui.util.EduAlert;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class ReviewSubmissionsController implements Initializable {

    @FXML private VBox cardContainer;
    @FXML private Label emptyLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;

    private final ChallengeParticipationService participationService = new ChallengeParticipationService();
    private final ChallengeService challengeService                  = new ChallengeService();
    private final UserService userService                            = new UserService();
    private final CertificateService certificateService             = new CertificateService();

    // cache des soumissions en attente pour le filtrage
    private final ObservableList<ChallengeParticipation> pendingList = FXCollections.observableArrayList();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Challenge A → Z", "Challenge Z → A", "Étudiant A → Z", "Soumis (récent)", "Soumis (ancien)"));
        sortCombo.setValue("Challenge A → Z");

        searchField.textProperty().addListener((obs, o, n) -> filterData());
        sortCombo.valueProperty().addListener((obs, o, n) -> filterData());
        loadData();
    }

    // ── Données ───────────────────────────────────────────────────────────────

    private void loadData() {
        pendingList.setAll(participationService.getPendingSubmissions());
        filterData();
    }

    private void filterData() {
        cardContainer.getChildren().clear();
        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String sort  = sortCombo.getValue();

        // Construire des triplets (participation, challenge, user) pour tri
        record PCU(ChallengeParticipation p, Challenge c, User u, String userName) {}
        List<PCU> items = pendingList.stream()
                .map(p -> {
                    Challenge c  = challengeService.getById(p.getChallengeId());
                    String email = getUserEmail(p.getUserId());
                    User u       = userService.findByEmail(email);
                    String name  = u != null ? u.getFullName() : "Utilisateur #" + p.getUserId();
                    return new PCU(p, c, u, name);
                })
                .filter(pcu -> pcu.c() != null)
                .filter(pcu -> pcu.c().getTitle().toLowerCase().contains(query)
                            || pcu.userName().toLowerCase().contains(query))
                .collect(Collectors.toList());

        Comparator<PCU> cmp = switch (sort == null ? "" : sort) {
            case "Challenge Z → A"  -> Comparator.comparing(pcu -> pcu.c().getTitle(),
                                        String.CASE_INSENSITIVE_ORDER.reversed());
            case "Étudiant A → Z"   -> Comparator.comparing(PCU::userName, String.CASE_INSENSITIVE_ORDER);
            case "Soumis (récent)"  -> Comparator.comparing(pcu -> pcu.p().getJoinedAt(),
                                        Comparator.reverseOrder());
            case "Soumis (ancien)"  -> Comparator.comparing(pcu -> pcu.p().getJoinedAt());
            default                 -> Comparator.comparing(pcu -> pcu.c().getTitle(),
                                        String.CASE_INSENSITIVE_ORDER);
        };
        items.sort(cmp);

        for (PCU pcu : items) {
            cardContainer.getChildren().add(createCard(pcu.p(), pcu.c(), pcu.u()));
        }

        if (items.isEmpty() && pendingList.isEmpty()) {
            emptyLabel.setText("✅  Aucune soumission en attente. Tout est à jour !");
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
        } else if (items.isEmpty()) {
            emptyLabel.setText("🔍  Aucun résultat pour \"" + searchField.getText() + "\"");
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
        } else {
            emptyLabel.setVisible(false);
            emptyLabel.setManaged(false);
        }
    }

    // ── Carte soumission ──────────────────────────────────────────────────────

    private VBox createCard(ChallengeParticipation p, Challenge c, User u) {
        VBox card = new VBox(14);
        card.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 20; -fx-background-radius: 10; " +
                      "-fx-border-color: #f59e0b55; -fx-border-radius: 10; -fx-border-width: 1.5;");

        // ── En-tête : challenge + étudiant
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(4);
        Label challengeLbl = new Label("🏆  " + c.getTitle());
        challengeLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px;");

        String userName = u != null ? u.getFullName() : "Utilisateur #" + p.getUserId();
        Label userLbl   = new Label("👤  " + userName);
        userLbl.setStyle("-fx-text-fill: #a0a0ab; -fx-font-size: 13px;");

        String joinedStr = p.getJoinedAt() != null ? "Soumis le " + p.getJoinedAt().format(FMT) : "";
        Label dateLbl = new Label(joinedStr);
        dateLbl.setStyle("-fx-text-fill: #7B7FA0; -fx-font-size: 11px;");

        info.getChildren().addAll(challengeLbl, userLbl, dateLbl);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        int effectiveXp = c.getEffectiveXpReward();
        Label xpLbl = new Label("+" + effectiveXp + " XP");
        xpLbl.setStyle("-fx-text-fill: #00d289; -fx-font-weight: bold; -fx-font-size: 14px;");

        header.getChildren().addAll(info, sp, xpLbl);

        // Visible boost indicator so the admin knows the XP is multiplied.
        if (c.isBoostActive()) {
            Label boostLbl = new Label("+" + c.getXpBoostPct() + "% BOOST");
            boostLbl.setStyle("-fx-background-color: #f59e0b22; -fx-text-fill: #f59e0b; " +
                    "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 8; " +
                    "-fx-background-radius: 8;");
            header.getChildren().add(boostLbl);
        }

        // ── Texte soumis
        VBox submissionBox = new VBox(6);
        if (p.getSubmissionText() != null && !p.getSubmissionText().isBlank()) {
            Label textTitle = new Label("📝  Texte soumis :");
            textTitle.setStyle("-fx-text-fill: #a0a0ab; -fx-font-size: 12px; -fx-font-weight: bold;");

            Label textContent = new Label(p.getSubmissionText());
            textContent.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 13px; " +
                    "-fx-background-color: #2a2a3e; -fx-padding: 10; -fx-background-radius: 6;");
            textContent.setWrapText(true);
            textContent.setMaxWidth(Double.MAX_VALUE);

            submissionBox.getChildren().addAll(textTitle, textContent);
        }

        // ── Fichier joint
        if (p.getSubmissionFilePath() != null && !p.getSubmissionFilePath().isBlank()) {
            String fileName = p.getSubmissionFilePath().contains(java.io.File.separator)
                    ? p.getSubmissionFilePath().substring(p.getSubmissionFilePath().lastIndexOf(java.io.File.separator) + 1)
                    : p.getSubmissionFilePath();

            Label fileLabel = new Label("📎  Fichier : " + fileName);
            fileLabel.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 12px; " +
                    "-fx-background-color: #3b82f615; -fx-padding: 6 10; -fx-background-radius: 6;");
            submissionBox.getChildren().add(fileLabel);
        }

        // ── Boutons Valider / Rejeter
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button rejectBtn = new Button("❌  Rejeter");
        rejectBtn.setStyle("-fx-background-color: #ef444433; -fx-text-fill: #ef4444; " +
                "-fx-font-weight: bold; -fx-padding: 9 20; -fx-background-radius: 7; -fx-cursor: hand;");
        rejectBtn.setOnAction(e -> handleReject(p));

        Button validateBtn = new Button("✅  Valider  +" + effectiveXp + " XP");
        validateBtn.setStyle("-fx-background-color: #00d289; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-padding: 9 20; -fx-background-radius: 7; -fx-cursor: hand;");
        validateBtn.setOnAction(e -> handleValidate(p, c, userName));

        actions.getChildren().addAll(rejectBtn, validateBtn);

        card.getChildren().addAll(header, new Separator(), submissionBox, actions);
        com.edulink.gui.util.ThemeManager.applyTheme(card);
        return card;
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    private void handleValidate(ChallengeParticipation p, Challenge c, String userName) {
        // Apply XP boost if the challenge is currently boosted.
        int xpToAward = c.getEffectiveXpReward();
        String boostNote = c.isBoostActive()
                ? "  (+ " + c.getXpBoostPct() + "% boost actif)" : "";

        boolean ok = EduAlert.confirm("Valider la soumission",
                "Valider le travail de " + userName + " pour \"" + c.getTitle() + "\" ?\n" +
                "+" + xpToAward + " XP seront crédités sur son compte" + boostNote + ".");
        if (!ok) return;

        participationService.validate(p.getId(), p.getUserId(), xpToAward);
        EduAlert.show(EduAlert.AlertType.SUCCESS, "Soumission validée !",
                "+" + xpToAward + " XP accordés à " + userName + boostNote + ".");

        // Demander confirmation avant de générer le certificat
        boolean genCert = EduAlert.confirm("Générer le certificat PDF",
                "Voulez-vous générer un certificat PDF pour " + userName + " ?\n" +
                "Le fichier sera sauvegardé sur le Bureau.");
        if (genCert) {
            String certPath = certificateService.generateCertificate(
                    userName, c.getTitle(), xpToAward, c.getDifficulty());
            if (certPath != null) {
                EduAlert.show(EduAlert.AlertType.SUCCESS, "Certificat généré !",
                        "📄 Fichier sauvegardé :\n" + certPath);
            } else {
                EduAlert.show(EduAlert.AlertType.WARNING, "Certificat",
                        "Le certificat n'a pas pu être généré. Vérifiez la console.");
            }
        }
        loadData();
    }

    private void handleReject(ChallengeParticipation p) {
        boolean ok = EduAlert.confirm("Rejeter la soumission",
                "Rejeter ce travail ? L'étudiant pourra resoumettre.");
        if (!ok) return;

        participationService.reject(p.getId());
        EduAlert.show(EduAlert.AlertType.WARNING, "Soumission rejetée",
                "L'étudiant a été notifié et peut resoumettre son travail.");
        loadData();
    }

    // ── Helper : récupérer l'email d'un user par id ───────────────────────────
    // (workaround car UserService expose findByEmail mais pas findById)
    private String getUserEmail(int userId) {
        String qry = "SELECT email FROM user WHERE id=?";
        try (java.sql.PreparedStatement p =
                     com.edulink.gui.util.MyConnection.getInstance().getCnx().prepareStatement(qry)) {
            p.setInt(1, userId);
            java.sql.ResultSet rs = p.executeQuery();
            if (rs.next()) return rs.getString("email");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
