package com.edulink.gui.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Themed alert dialogs that match EduLink's dark theme.
 * Replaces ugly white JavaFX Alert dialogs.
 */
public class EduAlert {

    public enum AlertType { SUCCESS, ERROR, INFO, WARNING, CONFIRM }

    /**
     * Shows a themed alert and waits for user to close it.
     */
    public static void show(AlertType type, String title, String message) {
        Stage dialog = createStage(type, title, message);
        dialog.showAndWait();
    }

    /**
     * Shows a confirmation dialog. Returns true if user clicks "Confirm".
     */
    public static boolean confirm(String title, String message) {
        final boolean[] result = {false};
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setResizable(false);

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #16162a; -fx-border-color: #f59e0b44; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");
        root.setPrefWidth(400);

        Label iconLabel = new Label("⚠");
        iconLabel.setStyle("-fx-font-size: 36px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 18px;");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-text-fill: #a0a0ab; -fx-font-size: 13px;");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(350);

        HBox buttons = new HBox(15);
        buttons.setAlignment(Pos.CENTER);

        Button noBtn = new Button("Cancel");
        noBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0a0ab; -fx-border-color: #ffffff22; -fx-border-radius: 6; -fx-padding: 8 25; -fx-cursor: hand; -fx-font-size: 13px;");
        noBtn.setOnAction(e -> { result[0] = false; dialog.close(); });

        Button yesBtn = new Button("Confirm");
        yesBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 25; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 13px;");
        yesBtn.setOnAction(e -> { result[0] = true; dialog.close(); });

        buttons.getChildren().addAll(noBtn, yesBtn);
        root.getChildren().addAll(iconLabel, titleLabel, msgLabel, buttons);

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
        return result[0];
    }

    private static Stage createStage(AlertType type, String title, String message) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setResizable(false);

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setPrefWidth(420);

        String borderColor;
        String icon;
        String accentColor;

        switch (type) {
            case SUCCESS:
                borderColor = "#00d28944";
                icon = "✅";
                accentColor = "#00d289";
                break;
            case ERROR:
                borderColor = "#ef444444";
                icon = "❌";
                accentColor = "#ef4444";
                break;
            case WARNING:
                borderColor = "#f59e0b44";
                icon = "⚠️";
                accentColor = "#f59e0b";
                break;
            default:
                borderColor = "#3b82f644";
                icon = "ℹ️";
                accentColor = "#3b82f6";
                break;
        }

        root.setStyle("-fx-background-color: #16162a; -fx-border-color: " + borderColor + "; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 36px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 18px;");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-text-fill: #a0a0ab; -fx-font-size: 13px;");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(370);

        Button okBtn = new Button("OK");
        okBtn.setStyle("-fx-background-color: " + accentColor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 30; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 13px;");
        okBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(iconLabel, titleLabel, msgLabel, okBtn);

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        return dialog;
    }
}
