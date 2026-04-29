package com.edulink.gui.controllers.event;

import com.edulink.gui.models.event.Event;
import com.edulink.gui.models.reservation.Reservation;
import com.edulink.gui.services.event.EventService;
import com.edulink.gui.services.qrcode.QRCodeScannerService;
import com.edulink.gui.services.reservation.ReservationService;
import com.edulink.gui.util.EduAlert;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class QRScannerController {

    @FXML private ImageView webcamView;
    @FXML private Label statusLabel;
    @FXML private Label resultLabel;
    @FXML private Button startBtn;
    @FXML private Button stopBtn;
    @FXML private Button importBtn;
    @FXML private VBox resultBox;

    private Webcam webcam;
    private Thread scanThread;
    private final AtomicBoolean scanning = new AtomicBoolean(false);

    private final EventService eventService = new EventService();
    private final ReservationService reservationService = new ReservationService();

    @FXML
    public void initialize() {
        stopBtn.setDisable(true);
        resultBox.setVisible(false);
    }

    // =====================
    // Démarrer la webcam
    // =====================
    @FXML
    public void handleStartScan() {
        webcam = Webcam.getDefault();
        if (webcam == null) {
            EduAlert.show(EduAlert.AlertType.ERROR, "Erreur", "Aucune webcam détectée !");
            return;
        }

        webcam.setViewSize(WebcamResolution.VGA.getSize());
        webcam.open();

        scanning.set(true);
        startBtn.setDisable(true);
        stopBtn.setDisable(false);
        statusLabel.setText("📷 Scan en cours — présentez le QR Code...");

        // Thread pour lire la webcam en continu
        scanThread = new Thread(() -> {
            while (scanning.get()) {
                BufferedImage image = webcam.getImage();
                if (image != null) {
                    // Afficher le flux webcam dans l'interface
                    Image fxImage = convertToFxImage(image);
                    Platform.runLater(() -> webcamView.setImage(fxImage));

                    // Essayer de décoder un QR Code
                    String result = QRCodeScannerService.decodeQRFromBufferedImage(image);
                    if (result != null) {
                        scanning.set(false);
                        Platform.runLater(() -> handleQRResult(result));
                    }
                }
                try { Thread.sleep(100); } catch (InterruptedException e) { break; }
            }
        });
        scanThread.setDaemon(true);
        scanThread.start();
    }

    // =====================
    // Arrêter la webcam
    // =====================
    @FXML
    public void handleStopScan() {
        scanning.set(false);
        if (webcam != null && webcam.isOpen()) webcam.close();
        startBtn.setDisable(false);
        stopBtn.setDisable(true);
        statusLabel.setText("⏹ Scan arrêté");
    }

    // =====================
    // Importer une image
    // =====================
    @FXML
    public void handleImportImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image QR Code");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) importBtn.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            String result = QRCodeScannerService.scanFromImage(file);
            if (result != null) {
                handleQRResult(result);
            } else {
                EduAlert.show(EduAlert.AlertType.ERROR, "Erreur",
                        "Aucun QR Code trouvé dans cette image !");
            }
        }
    }

    // =====================
    // Traiter le résultat du QR Code
    // =====================
    private void handleQRResult(String content) {
        handleStopScan();

        int[] ids = QRCodeScannerService.parseQRContent(content);
        if (ids == null) {
            EduAlert.show(EduAlert.AlertType.ERROR, "Erreur", "QR Code invalide !");
            return;
        }

        int eventId = ids[0];
        int reservationId = ids[1];

        Event event = eventService.getEventById(eventId);
        Reservation reservation = reservationService.getReservationById(reservationId);

        if (event == null || reservation == null) {
            EduAlert.show(EduAlert.AlertType.ERROR, "Erreur",
                    "Réservation ou événement introuvable !");
            return;
        }

        // Afficher les infos
        resultBox.setVisible(true);
        statusLabel.setText("✅ QR Code valide !");
        resultLabel.setText(
                "👤 Utilisateur ID : " + reservation.getUserId() + "\n" +
                "📅 Événement : " + event.getTitle() + "\n" +
                "📆 Date : " + event.getDateStart() + "\n" +
                "🎟 Réservation #" + reservation.getId()
        );

        EduAlert.show(EduAlert.AlertType.SUCCESS, "Accès autorisé ✅",
                "Bienvenue à : " + event.getTitle());
    }

    // =====================
    // Convertir BufferedImage → JavaFX Image
    // =====================
    private Image convertToFxImage(BufferedImage bufferedImage) {
        WritableImage writableImage = new WritableImage(
                bufferedImage.getWidth(), bufferedImage.getHeight());
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        for (int x = 0; x < bufferedImage.getWidth(); x++) {
            for (int y = 0; y < bufferedImage.getHeight(); y++) {
                pixelWriter.setArgb(x, y, bufferedImage.getRGB(x, y));
            }
        }
        return writableImage;
    }

    // Fermer la webcam quand on quitte
    public void cleanup() {
        scanning.set(false);
        if (webcam != null && webcam.isOpen()) webcam.close();
    }
}