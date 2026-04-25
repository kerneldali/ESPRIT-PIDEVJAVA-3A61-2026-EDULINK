package com.edulink.gui.controllers.event;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class EventController implements Initializable {

    @FXML private StackPane rootPane;
    @FXML private FlowPane cardContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> onlineComboFilter;
    @FXML private ComboBox<String> sortCombo;

    @FXML private VBox formOverlay;
    @FXML private Label formTitle;
    @FXML private TextField titleField;
    @FXML private TextArea descField;
    @FXML private DatePicker dateStartField;
    @FXML private DatePicker dateEndField;
    @FXML private CheckBox onlineCheck;
    @FXML private TextField meetLinkField;
    @FXML private TextField locationField;
    @FXML private TextField maxCapacityField;
    @FXML private TextField imageField;

    @FXML private Button saveBtn;
    @FXML private Label titleError;
    @FXML private Label descError;
    @FXML private Label datesError;
    @FXML private Label capacityError;

    private final EventService eventService = new EventService();
    private final ReservationService reservationService = new ReservationService();
    private final ObservableList<Event> eventList = FXCollections.observableArrayList();
    private Event currentEditableEvent = null;
    private int currentUserId = -1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (SessionManager.getCurrentUser() != null) {
            currentUserId = SessionManager.getCurrentUser().getId();
        }

        onlineComboFilter.setItems(FXCollections.observableArrayList("All", "Online", "Offline"));
        onlineComboFilter.setValue("All");

        sortCombo.setItems(FXCollections.observableArrayList(
                "Titre A → Z", "Titre Z → A", "Date (récent)", "Date (ancien)"));
        sortCombo.setValue("Titre A → Z");

        searchField.textProperty().addListener((obs, oldV, newV) -> filterData());
        onlineComboFilter.valueProperty().addListener((obs, oldV, newV) -> filterData());
        sortCombo.valueProperty().addListener((obs, oldV, newV) -> filterData());

        onlineCheck.selectedProperty().addListener((obs, oldV, newV) -> {
            meetLinkField.setDisable(!newV);
            locationField.setDisable(newV);
        });

        titleField.textProperty().addListener((obs, old, newV) -> validateForm());
        descField.textProperty().addListener((obs, old, newV) -> validateForm());
        maxCapacityField.textProperty().addListener((obs, old, newV) -> validateForm());
        dateStartField.valueProperty().addListener((obs, old, newV) -> validateForm());
        dateEndField.valueProperty().addListener((obs, old, newV) -> validateForm());

        loadData();
    }

    private void validateForm() {
        boolean valid = true;
        titleError.setText("");
        descError.setText("");
        datesError.setText("");
        capacityError.setText("");

        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            titleError.setText("Title is required");
            valid = false;
        } else if (titleField.getText().trim().length() < 3) {
            titleError.setText("Title must be at least 3 characters");
            valid = false;
        }

        if (descField.getText() == null || descField.getText().trim().isEmpty()) {
            descError.setText("Description is required");
            valid = false;
        }

        if (dateStartField.getValue() == null || dateEndField.getValue() == null) {
            datesError.setText("Both start and end dates are required");
            valid = false;
        } else if (dateStartField.getValue().isAfter(dateEndField.getValue())) {
            datesError.setText("Start date must be before end date");
            valid = false;
        }

        if (maxCapacityField.getText() != null && !maxCapacityField.getText().trim().isEmpty()) {
            try {
                int val = Integer.parseInt(maxCapacityField.getText().trim());
                if (val <= 0) {
                    capacityError.setText("Capacity must be positive");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                capacityError.setText("Capacity must be a number");
                valid = false;
            }
        }

        saveBtn.setDisable(!valid);
    }

    private void loadData() {
        eventList.setAll(eventService.getAllEvents());
        filterData();
    }

    private void filterData() {
        cardContainer.getChildren().clear();
        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String onlineFilter = onlineComboFilter.getValue();
        String sort = sortCombo.getValue();

        java.util.List<Event> filtered = eventList.stream()
                .filter(e -> e.getTitle() == null || e.getTitle().toLowerCase().contains(query))
                .filter(e -> "All".equals(onlineFilter)
                        || ("Online".equals(onlineFilter) && e.isOnline())
                        || ("Offline".equals(onlineFilter) && !e.isOnline()))
                .collect(java.util.stream.Collectors.toList());

        java.util.Comparator<Event> cmp = switch (sort == null ? "" : sort) {
            case "Titre Z → A" -> java.util.Comparator.comparing(Event::getTitle,
                    String.CASE_INSENSITIVE_ORDER.reversed());
            case "Date (récent)" -> java.util.Comparator.comparing(
                    e -> e.getDateStart() == null ? LocalDateTime.MIN : e.getDateStart(),
                    java.util.Comparator.reverseOrder());
            case "Date (ancien)" -> java.util.Comparator.comparing(
                    e -> e.getDateStart() == null ? LocalDateTime.MAX : e.getDateStart());
            default -> java.util.Comparator.comparing(
                    e -> e.getTitle() == null ? "" : e.getTitle(),
                    String.CASE_INSENSITIVE_ORDER);
        };
        filtered.sort(cmp);

        for (Event e : filtered) {
            cardContainer.getChildren().add(createCard(e));
        }
    }

    private VBox createCard(Event e) {
        VBox card = new VBox(15);
        card.getStyleClass().add("event-card");
        card.setPrefWidth(300);
        card.setMaxWidth(300);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(e.getTitle() != null ? e.getTitle() : "Untitled Event");
        title.getStyleClass().add("event-title");
        title.setWrapText(true);
        title.setMaxWidth(200);

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        Label badge = new Label(e.isOnline() ? "ONLINE" : "OFFLINE");
        badge.getStyleClass().add(e.isOnline() ? "badge-online" : "badge-offline");
        header.getChildren().addAll(title, spacer1, badge);

        Label dateLbl = new Label("📅 " + (e.getDateStart() != null ? e.getDateStart().toLocalDate() : "TBD"));
        dateLbl.getStyleClass().add("event-date");

        Label locLbl = new Label(e.isOnline()
                ? "🌐 " + (e.getMeetLink() != null && !e.getMeetLink().isEmpty() ? e.getMeetLink() : "Link not set")
                : "📍 " + (e.getLocation() != null && !e.getLocation().isEmpty() ? e.getLocation() : "TBD"));
        locLbl.getStyleClass().add("event-date");

        Label capLbl = new Label("👥 Places restantes: " + e.getMaxCapacity());
        capLbl.getStyleClass().add("capacity-text");

        Button reserveBtn = new Button("🔥 Réserver");
        reserveBtn.setMaxWidth(Double.MAX_VALUE);
        reserveBtn.getStyleClass().add("btn-reserve");

        boolean isOrganizer = (currentUserId != -1 && e.getOrganizerId() == currentUserId);
        boolean alreadyReserved = (currentUserId != -1
                && reservationService.isAlreadyReserved(currentUserId, e.getId()));

        if (isOrganizer) {
            reserveBtn.setDisable(true);
            reserveBtn.setText("Votre Événement");
            reserveBtn.setStyle("-fx-opacity: 0.5; -fx-cursor: default;");
        } else if (alreadyReserved) {
            reserveBtn.setDisable(true);
            reserveBtn.setText("✅ Déjà réservé");
            reserveBtn.setStyle("-fx-opacity: 0.7; -fx-cursor: default;");
        }

        reserveBtn.setOnAction(evt -> {
            evt.consume();
            if (currentUserId == -1) {
                EduAlert.show(EduAlert.AlertType.WARNING, "Non connecté",
                        "Tu dois être connecté pour réserver un événement.");
                return;
            }
            Reservation res = new Reservation();
            res.setUserId(currentUserId);
            res.setEventId(e.getId());
            res.setReservedAt(LocalDateTime.now());
            // ✅ On passe l'email pour envoyer le mail de confirmation
            String userEmail = SessionManager.getCurrentUser() != null
                    ? SessionManager.getCurrentUser().getEmail() : null;
            boolean success = reservationService.addReservation(res, userEmail);
            if (success) {
                EduAlert.show(EduAlert.AlertType.SUCCESS, "Réservation confirmée !",
                        "Tu es inscrit à : " + e.getTitle());
                loadData();
            } else {
                EduAlert.show(EduAlert.AlertType.ERROR, "Erreur",
                        "Impossible de réserver. Tu es peut-être déjà inscrit.");
            }
        });

        HBox manageBox = new HBox(10);
        manageBox.setAlignment(Pos.CENTER);

        Button editBtn = new Button("✏ Edit");
        editBtn.getStyleClass().add("btn-edit");
        editBtn.setOnAction(evt -> { evt.consume(); showForm(e); });

        Button delBtn = new Button("🗑 Delete");
        delBtn.getStyleClass().add("btn-delete");
        delBtn.setOnAction(evt -> {
            evt.consume();
            if (EduAlert.confirm("Delete Event",
                    "Are you sure you want to delete '" + e.getTitle() + "'?")) {
                eventService.deleteEvent(e.getId());
                loadData();
            }
        });

        manageBox.getChildren().addAll(editBtn, delBtn);

        Region spacer2 = new Region();
        VBox.setVgrow(spacer2, Priority.ALWAYS);

        card.getChildren().addAll(header, new Separator(), dateLbl, locLbl, capLbl, spacer2, reserveBtn, manageBox);
        card.setOnMouseClicked(evt -> showEventDetailsPopup(e));
        com.edulink.gui.util.ThemeManager.applyTheme(card);
        return card;
    }

    private void showEventDetailsPopup(Event e) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initStyle(StageStyle.TRANSPARENT);

        VBox popupContainer = new VBox(0);
        popupContainer.getStyleClass().add("popup-container");
        popupContainer.setMaxWidth(600);
        popupContainer.setPrefWidth(600);

        VBox headerSection = new VBox(10);
        headerSection.getStyleClass().add("popup-header-section");
        headerSection.setAlignment(Pos.CENTER_LEFT);

        HBox topRow = new HBox(15);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label(e.isOnline() ? "ONLINE" : "OFFLINE");
        badge.getStyleClass().add(e.isOnline() ? "badge-online" : "badge-offline");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        Label capacityBadge = new Label("👥 " +
                (e.getMaxCapacity() > 0 ? e.getMaxCapacity() + " places restantes" : "Illimité"));
        capacityBadge.getStyleClass().add("popup-capacity-badge");

        topRow.getChildren().addAll(badge, spacer1, capacityBadge);

        Label titleLbl = new Label(e.getTitle() != null ? e.getTitle() : "Titre de l'événement");
        titleLbl.getStyleClass().add("popup-title-large");
        titleLbl.setWrapText(true);
        titleLbl.setMaxWidth(500);

        headerSection.getChildren().addAll(topRow, titleLbl);

        HBox bodySection = new HBox(25);
        bodySection.getStyleClass().add("popup-body-section");

        VBox detailsCol = new VBox(20);
        detailsCol.setPrefWidth(220);
        detailsCol.setMinWidth(220);

        Label infoTitle = new Label("Détails");
        infoTitle.getStyleClass().add("popup-section-title");

        VBox dateBox = createInfoBox("📅", "Date & Heure",
                e.getDateStart() != null ? e.getDateStart().toString().replace("T", " ") : "TBD");
        VBox locBox = createInfoBox(e.isOnline() ? "🌐" : "📍", e.isOnline() ? "Lien Meet" : "Lieu",
                e.isOnline()
                        ? (e.getMeetLink() != null && !e.getMeetLink().isEmpty() ? e.getMeetLink() : "Non spécifié")
                        : (e.getLocation() != null && !e.getLocation().isEmpty() ? e.getLocation() : "TBD"));
        VBox orgBox = createInfoBox("👤", "Organisateur", "ID: " + e.getOrganizerId());

        detailsCol.getChildren().addAll(infoTitle, dateBox, locBox, orgBox);

        VBox descCol = new VBox(15);
        HBox.setHgrow(descCol, Priority.ALWAYS);

        Label descTitle = new Label("À propos de l'événement");
        descTitle.getStyleClass().add("popup-section-title");

        Label descVal = new Label(e.getDescription() != null && !e.getDescription().isEmpty()
                ? e.getDescription() : "Aucune description fournie.");
        descVal.getStyleClass().add("popup-desc-text");
        descVal.setWrapText(true);

        descCol.getChildren().addAll(descTitle, descVal);

        Separator vertSep = new Separator(javafx.geometry.Orientation.VERTICAL);
        bodySection.getChildren().addAll(detailsCol, vertSep, descCol);

        HBox footerSection = new HBox(15);
        footerSection.getStyleClass().add("popup-footer-section");
        footerSection.setAlignment(Pos.CENTER_RIGHT);

        Button closeBtn = new Button("Fermer");
        closeBtn.getStyleClass().add("btn-close-modern");
        closeBtn.setOnAction(evt -> popupStage.close());

        Button reserveBtn = new Button("🔥 Réserver maintenant");
        reserveBtn.getStyleClass().add("btn-reserve-modern");

        boolean isOrganizer = (currentUserId != -1 && e.getOrganizerId() == currentUserId);
        boolean alreadyReserved = (currentUserId != -1
                && reservationService.isAlreadyReserved(currentUserId, e.getId()));

        if (isOrganizer) {
            reserveBtn.setDisable(true);
            reserveBtn.setText("Votre Événement");
        } else if (alreadyReserved) {
            reserveBtn.setDisable(true);
            reserveBtn.setText("✅ Déjà réservé");
        }

        reserveBtn.setOnAction(evt -> {
            popupStage.close();
            if (currentUserId == -1) {
                EduAlert.show(EduAlert.AlertType.WARNING, "Non connecté",
                        "Tu dois être connecté pour réserver.");
                return;
            }
            Reservation res = new Reservation();
            res.setUserId(currentUserId);
            res.setEventId(e.getId());
            res.setReservedAt(LocalDateTime.now());
            // ✅ On passe l'email pour envoyer le mail de confirmation
            String userEmail = SessionManager.getCurrentUser() != null
                    ? SessionManager.getCurrentUser().getEmail() : null;
            boolean success = reservationService.addReservation(res, userEmail);
            if (success) {
                EduAlert.show(EduAlert.AlertType.SUCCESS, "Réservation confirmée !",
                        "Tu es inscrit à : " + e.getTitle());
                loadData();
            } else {
                EduAlert.show(EduAlert.AlertType.ERROR, "Erreur",
                        "Impossible de réserver. Tu es peut-être déjà inscrit.");
            }
        });

        footerSection.getChildren().addAll(closeBtn, reserveBtn);
        popupContainer.getChildren().addAll(headerSection, bodySection, footerSection);

        Scene scene = new Scene(popupContainer);
        scene.setFill(Color.TRANSPARENT);
        try {
            scene.getStylesheets().add(getClass().getResource("/view/event/event.css").toExternalForm());
        } catch (Exception ex) { ex.printStackTrace(); }

        popupStage.setScene(scene);
        if (rootPane != null && rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
            popupStage.initOwner(rootPane.getScene().getWindow());
        }
        popupStage.showAndWait();
    }

    private VBox createInfoBox(String icon, String label, String value) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("popup-info-label");

        HBox valBox = new HBox(8);
        valBox.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("popup-info-icon");
        Label valLbl = new Label(value);
        valLbl.getStyleClass().add("popup-info-value");
        valLbl.setWrapText(true);
        HBox.setHgrow(valLbl, Priority.ALWAYS);
        valBox.getChildren().addAll(iconLbl, valLbl);

        return new VBox(4, lbl, valBox);
    }

    @FXML private void handleNewEvent() { showForm(null); }
    @FXML private void handleApplyFilter() { filterData(); }

    private void showForm(Event e) {
        currentEditableEvent = e;
        if (e != null) {
            formTitle.setText("Edit Event");
            titleField.setText(e.getTitle());
            descField.setText(e.getDescription());
            dateStartField.setValue(e.getDateStart() != null ? e.getDateStart().toLocalDate() : null);
            dateEndField.setValue(e.getDateEnd() != null ? e.getDateEnd().toLocalDate() : null);
            onlineCheck.setSelected(e.isOnline());
            meetLinkField.setText(e.getMeetLink() != null ? e.getMeetLink() : "");
            locationField.setText(e.getLocation() != null ? e.getLocation() : "");
            maxCapacityField.setText(String.valueOf(e.getMaxCapacity()));
            imageField.setText(e.getImage() != null ? e.getImage() : "");
        } else {
            formTitle.setText("New Event");
            titleField.clear(); descField.clear();
            dateStartField.setValue(null); dateEndField.setValue(null);
            onlineCheck.setSelected(false);
            meetLinkField.clear(); locationField.clear();
            maxCapacityField.setText("0"); imageField.clear();
        }
        formOverlay.setVisible(true);
        formOverlay.toFront();
        validateForm();
    }

    @FXML private void handleCloseForm() { formOverlay.setVisible(false); }

    @FXML
    private void handleSaveEvent() {
        Event result = currentEditableEvent != null ? currentEditableEvent : new Event();
        result.setTitle(titleField.getText().trim());
        result.setDescription(descField.getText().trim());
        if (dateStartField.getValue() != null)
            result.setDateStart(LocalDateTime.of(dateStartField.getValue(), LocalTime.of(9, 0)));
        if (dateEndField.getValue() != null)
            result.setDateEnd(LocalDateTime.of(dateEndField.getValue(), LocalTime.of(18, 0)));
        result.setOnline(onlineCheck.isSelected());
        result.setMeetLink(meetLinkField.getText().trim());
        result.setLocation(locationField.getText().trim());
        result.setImage(imageField.getText().trim());
        try { result.setMaxCapacity(Integer.parseInt(maxCapacityField.getText().trim())); }
        catch (NumberFormatException ex) { result.setMaxCapacity(0); }

        if (currentEditableEvent == null) {
            result.setOrganizerId(currentUserId != -1 ? currentUserId : 0);
            eventService.addEvent(result);
            EduAlert.show(EduAlert.AlertType.SUCCESS, "Succès", "Événement créé !");
        } else {
            eventService.updateEvent(result);
            EduAlert.show(EduAlert.AlertType.SUCCESS, "Succès", "Événement mis à jour !");
        }
        handleCloseForm();
        loadData();
    }
}