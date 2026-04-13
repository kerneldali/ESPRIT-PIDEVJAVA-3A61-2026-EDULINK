package com.edulink.gui.controllers;

import com.edulink.gui.Main;
import com.edulink.gui.controllers.DashboardController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.io.IOException;

public class SplashController {

    @FXML private Label loadingLabel;
    @FXML private Button frontBtn, backBtn;

    @FXML
    public void handleFrontOffice() {
        launchDashboard(false);
    }

    @FXML
    public void handleBackOffice() {
        launchDashboard(true);
    }

    private void launchDashboard(boolean isAdmin) {
        // Show Loading State
        loadingLabel.setText("Connecting to database... Please wait.");
        loadingLabel.setVisible(true);
        frontBtn.setDisable(true);
        backBtn.setDisable(true);

        // Perform loading in a background thread to prevent UI freezing
        javafx.concurrent.Task<Parent> loadTask = new javafx.concurrent.Task<>() {
            @Override
            protected Parent call() throws Exception {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Dashboard.fxml"));
                Parent root = loader.load();
                
                DashboardController controller = loader.getController();
                javafx.application.Platform.runLater(() -> controller.setAdminMode(isAdmin));
                
                return root;
            }
        };

        loadTask.setOnSucceeded(e -> {
            Main.getPrimaryStage().getScene().setRoot(loadTask.getValue());
        });

        loadTask.setOnFailed(e -> {
            loadingLabel.setText("❌ Connection Failed. Make sure MySQL is running.");
            loadingLabel.setStyle("-fx-text-fill: #ef4444;");
            frontBtn.setDisable(false);
            backBtn.setDisable(false);
            e.getSource().getException().printStackTrace();
        });

        new Thread(loadTask).start();
    }
}
