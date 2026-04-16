package com.edulink.gui.controllers.reservation;

import com.edulink.gui.models.event.Event;
import com.edulink.gui.models.reservation.Reservation;
import com.edulink.gui.services.event.EventService;
import com.edulink.gui.services.reservation.ReservationService;
import com.edulink.gui.util.EduAlert;
import com.edulink.gui.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.net.URL;
import java.util.ResourceBundle;

public class ReservationController implements Initializable {

    @FXML private StackPane rootPane;
    @FXML private FlowPane cardContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCombo;
    @FXML private ComboBox<String> sortCombo;

    private final ReservationService reservationService = new ReservationService();
    private final EventService eventService             = new EventService();
    private final ObservableList<Reservation> reservationList = FXCollections.observableArrayList();

    private int currentUserId = -1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Récupération de l'utilisateur connecté via SessionManager
        if (SessionManager.getCurrentUser() != null) {
            currentUserId = SessionManager.getCurrentUser().getId();
        }

        filterCombo.setItems(FXCollections.observableArrayList("All"));
        filterCombo.setValue("All");

        sortCombo.setItems(FXCollections.observableArrayList(
                "Événement A → Z", "Événement Z → A", "Réservé (récent)", "Réservé (ancien)"));
        sortCombo.setValue("Réservé (récent)");

        searchField.textProperty().addListener((obs, oldV, newV) -> filterData());
        filterCombo.valueProperty().addListener((obs, oldV, newV) -> filterData());
        sortCombo.valueProperty().addListener((obs, oldV, newV) -> filterData());

        loadData();
    }

    private void loadData() {
        if (currentUserId != -1) {
            reservationList.setAll(reservationService.getReservationsByUserId(currentUserId));
        } else {
            reservationList.clear();
        }
        filterData();
    }

    @FXML private void handleApplyFilter() { filterData(); }

    private void filterData() {
        cardContainer.getChildren().clear();

        if (currentUserId == -1) {
            Label lbl = new Label("Tu dois être connecté pour voir tes réservations.");
            lbl.setStyle("-fx-text-fill: #a0a0ab; -fx-font-size: 14px;");
            cardContainer.getChildren().add(lbl);
            return;
        }

        String query     = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String sort      = sortCombo.getValue();

        record RW(Reservation r, Event e) {}
        java.util.List<RW> wrappers = reservationList.stream()
                .map(r -> new RW(r, eventService.getEventById(r.getEventId())))
                .filter(rw -> rw.e() != null)
                .filter(rw -> rw.e().getTitle() != null
                           && rw.e().getTitle().toLowerCase().contains(query))
                .collect(java.util.stream.Collectors.toList());

        java.util.Comparator<RW> cmp = switch (sort == null ? "" : sort) {
            case "Événement Z → A"  -> java.util.Comparator.comparing(rw -> rw.e().getTitle(),
                                        String.CASE_INSENSITIVE_ORDER.reversed());
            case "Réservé (ancien)" -> java.util.Comparator.comparing(rw -> rw.r().getReservedAt());
            case "Événement A → Z"  -> java.util.Comparator.comparing(rw -> rw.e().getTitle(),
                                        String.CASE_INSENSITIVE_ORDER);
            default                 -> java.util.Comparator.comparing(rw -> rw.r().getReservedAt(),
                                        java.util.Comparator.reverseOrder());
        };
        wrappers.sort(cmp);

        for (RW rw : wrappers) {
            cardContainer.getChildren().add(createCard(rw.r(), rw.e()));
        }

        if (cardContainer.getChildren().isEmpty()) {
            Label lbl = reservationList.isEmpty()
                    ? new Label("Tu n'as encore réservé aucun événement.")
                    : new Label("Aucun résultat pour \"" + searchField.getText() + "\"");
            lbl.setStyle("-fx-text-fill: #a0a0ab; -fx-font-size: 14px;");
            cardContainer.getChildren().add(lbl);
        }
    }

    private VBox createCard(Reservation r, Event e) {
        VBox card = new VBox(15);
        card.getStyleClass().add("event-card");
        card.setPrefWidth(300);
        card.setMaxWidth(300);

        // Header: Titre + Badge RESERVED
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label title = new Label(e.getTitle() != null ? e.getTitle() : "Untitled Event");
        title.getStyleClass().add("event-title");
        title.setWrapText(true);
        title.setMaxWidth(200);

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        Label badge = new Label("RESERVED");
        badge.getStyleClass().add("badge-online");

        header.getChildren().addAll(title, spacer1, badge);

        Label dateLbl = new Label("📅 " + (e.getDateStart() != null ? e.getDateStart().toLocalDate() : "TBD"));
        dateLbl.getStyleClass().add("event-date");

        Label locLbl = new Label(e.isOnline()
                ? "🌐 " + (e.getMeetLink() != null && !e.getMeetLink().isEmpty() ? e.getMeetLink() : "Link not set")
                : "📍 " + (e.getLocation() != null && !e.getLocation().isEmpty() ? e.getLocation() : "TBD"));
        locLbl.getStyleClass().add("event-date");

        Label resLbl = new Label("🕒 Réservé le: "
                + r.getReservedAt().toString().replace("T", " ").substring(0, 16));
        resLbl.getStyleClass().add("capacity-text");

        // Bouton Annuler réservation
        Button cancelBtn = new Button("❌ Annuler réservation");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.getStyleClass().add("btn-delete");

        cancelBtn.setOnAction(evt -> {
            evt.consume();
            if (EduAlert.confirm("Annuler Réservation",
                    "Voulez-vous vraiment annuler votre réservation pour '" + e.getTitle() + "' ?")) {
                if (reservationService.deleteReservation(r.getId())) {
                    loadData();
                } else {
                    EduAlert.show(EduAlert.AlertType.ERROR, "Erreur",
                            "Impossible d'annuler la réservation.");
                }
            }
        });

        Region spacer2 = new Region();
        VBox.setVgrow(spacer2, Priority.ALWAYS);

        card.getChildren().addAll(header, new Separator(), dateLbl, locLbl, resLbl, spacer2, cancelBtn);
        return card;
    }
}
