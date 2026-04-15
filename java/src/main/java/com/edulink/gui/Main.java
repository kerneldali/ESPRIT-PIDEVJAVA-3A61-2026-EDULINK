package com.edulink.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        try {
            // Check if Login FXML exists
            URL fxmlUrl = Main.class.getResource("/view/Login.fxml");
            if (fxmlUrl == null) {
                showCriticalError("FXML Not Found", "Could not find /view/Login.fxml in resources.");
                return;
            }

            FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
            Parent root = fxmlLoader.load();
            
            Scene scene = new Scene(root, 1100, 750); // Slightly larger for pro look
            
            // Check if CSS exists
            URL cssUrl = Main.class.getResource("/styles/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("Warning: /styles/style.css not found.");
            }
            
            stage.setTitle("EduLink - Modern Learning Platform");
            stage.setScene(scene);
            stage.show();
            
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR during startup:");
            e.printStackTrace();
            showCriticalError("Startup Error", e.getMessage());
        }
    }

    private void showCriticalError(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("EduLink - Critical Error");
            alert.setHeaderText(title);
            alert.setContentText(content + "\n\nPlease check the console for details.");
            alert.showAndWait();
        });
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        try {
            launch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
