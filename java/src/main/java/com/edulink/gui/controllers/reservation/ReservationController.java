package com.edulink.gui.controllers.reservation;

import java.net.URL;
import java.util.ResourceBundle;

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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class ReservationController implements Initializable {

    @FXML
    private StackPane rootPane;
    @FXML
    private FlowPane cardContainer;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterCombo;
    @FXML
    private ComboBox<String> sortCombo;

    private ReservationService reservationService = new ReservationService();
    private EventService eventService = new EventService();
    private ObservableList<Reservation> reservationList = FXCollections.observableArrayList();

    private int currentUserId = -1; // Simulated connected user

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        if (SessionManager.getCurrentUser() != null) {
            currentUserId = SessionManager.getCurrentUser().getId();
        }


        filterCombo.setItems(FXCollections.observableArrayList("All"));
        filterCombo.setValue("All");

        sortCombo.setItems(FXCollections.observableArrayList("Newest First", "Oldest First", "Event Name A-Z"));
        sortCombo.setValue("Newest First");

        searchField.textProperty().addListener((obs, oldV, newV) -> filterData());
        filterCombo.valueProperty().addListener((obs, oldV, newV) -> filterData());
        sortCombo.valueProperty().addListener((obs, oldV, newV) -> filterData());

        loadData();
    }

    private void loadData() {
        reservationList.setAll(reservationService.getReservationsByUserId(currentUserId));
        filterData();
    }

    @FXML
    private void handleApplyFilter() {
        filterData();
    }

    private void filterData() {
        cardContainer.getChildren().clear();
        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase();

        ObservableList<ReservationWrapper> wrappers = FXCollections.observableArrayList();

        for (Reservation r : reservationList) {
            Event e = eventService.getEventById(r.getEventId());
            if (e != null) {
                boolean matchesSearch = e.getTitle() != null && e.getTitle().toLowerCase().contains(query);
                if (matchesSearch) {
                    wrappers.add(new ReservationWrapper(r, e));
                }
            }
        }

        // Sorting
        String sortValue = sortCombo.getValue();
        if ("Newest First".equals(sortValue)) {
            wrappers.sort((w1, w2) -> w2.reservation.getReservedAt().compareTo(w1.reservation.getReservedAt()));
        } else if ("Oldest First".equals(sortValue)) {
            wrappers.sort((w1, w2) -> w1.reservation.getReservedAt().compareTo(w2.reservation.getReservedAt()));
        } else if ("Event Name A-Z".equals(sortValue)) {
            wrappers.sort((w1, w2) -> {
                String t1 = w1.event.getTitle() != null ? w1.event.getTitle() : "";
                String t2 = w2.event.getTitle() != null ? w2.event.getTitle() : "";
                return t1.compareToIgnoreCase(t2);
            });
        }

        for (ReservationWrapper w : wrappers) {
            cardContainer.getChildren().add(createCard(w.reservation, w.event));
        }
    }

    private VBox createCard(Reservation r, Event e) {
        VBox card = new VBox(15);
        card.getStyleClass().add("event-card");
        card.setPrefWidth(300);
        card.setMaxWidth(300);

        // Header: Title and Badge
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label title = new Label(e.getTitle() != null ? e.getTitle() : "Untitled Event");
        title.getStyleClass().add("event-title");
        title.setWrapText(true);
        title.setMaxWidth(200);

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        Label badge = new Label("RESERVED");
        badge.getStyleClass().add("badge-online"); // Repurposing green badge style

        header.getChildren().addAll(title, spacer1, badge);

        // Date and Location
        Label dateLbl = new Label("📅 " + (e.getDateStart() != null ? e.getDateStart().toLocalDate() : "TBD"));
        dateLbl.getStyleClass().add("event-date");

        Label locLbl = new Label(e.isOnline()
                ? "🌐 " + (e.getMeetLink() != null && !e.getMeetLink().isEmpty() ? e.getMeetLink() : "Link not set")
                : "📍 " + (e.getLocation() != null && !e.getLocation().isEmpty() ? e.getLocation() : "TBD"));
        locLbl.getStyleClass().add("event-date");

        // Reservation info
        Label resLbl = new Label("🕒 Réservé le: " + r.getReservedAt().toString().replace("T", " ").substring(0, 16));
        resLbl.getStyleClass().add("capacity-text");

        // Action (Cancel)
        Button cancelBtn = new Button("❌ Annuler réservation");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.getStyleClass().add("btn-delete");

        cancelBtn.setOnAction(evt -> {
            evt.consume();
            if (EduAlert.confirm("Annuler Réservation",
                    "Voulez-vous vraiment annuler votre réservation pour '" + e.getTitle() + "' ?")) {
                boolean success = reservationService.deleteReservation(r.getId());
                if (success) {
                    loadData();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur");
                    alert.setHeaderText(null);
                    alert.setContentText("Impossible d'annuler la réservation.");
                    alert.showAndWait();
                }
            }
        });

        Region spacer2 = new Region();
        VBox.setVgrow(spacer2, Priority.ALWAYS);

        card.getChildren().addAll(header, new Separator(), dateLbl, locLbl, resLbl, spacer2, cancelBtn);
        return card;
    }

    private static class ReservationWrapper {
        Reservation reservation;
        Event event;

        public ReservationWrapper(Reservation r, Event e) {
            this.reservation = r;
            this.event = e;
        }
    }
}
