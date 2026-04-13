package com.edulink.gui.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import com.edulink.gui.Main;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Label contextLabel;
    @FXML private VBox helpSubMenu, challengeSubMenu, eventSubMenu, courseSubMenu, notesSubMenu, userSubMenu;
    @FXML private Button adminOnlyTickets, adminUserStats, adminWallets;

    private boolean isAdmin = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Load the Help Request List view by default
        showHelpRequests();
    }

    public void setAdminMode(boolean admin) {
        this.isAdmin = admin;
        if (admin) {
            contextLabel.setText("Admin Backoffice");
            contextLabel.getStyleClass().add("label-admin");
            adminOnlyTickets.setVisible(true);
            adminOnlyTickets.setManaged(true);
            if (adminUserStats != null) { adminUserStats.setVisible(true); adminUserStats.setManaged(true); }
            if (adminWallets != null) { adminWallets.setVisible(true); adminWallets.setManaged(true); }
        } else {
            contextLabel.setText("Student Frontoffice");
        }
    }

    @FXML public void toggleHelpMenu() { toggleMenu(helpSubMenu); }
    @FXML public void toggleChallengeMenu() { toggleMenu(challengeSubMenu); }   
    @FXML public void toggleEventMenu() { toggleMenu(eventSubMenu); }
    @FXML public void toggleCourseMenu() { toggleMenu(courseSubMenu); }
    @FXML public void toggleNotesMenu() { toggleMenu(notesSubMenu); }
    @FXML public void toggleUserMenu() { toggleMenu(userSubMenu); }

    private void toggleMenu(VBox subMenu) {
        boolean isVisible = subMenu.isVisible();
        if (helpSubMenu != null) { helpSubMenu.setVisible(false); helpSubMenu.setManaged(false); }
        if (challengeSubMenu != null) { challengeSubMenu.setVisible(false); challengeSubMenu.setManaged(false); }
        if (eventSubMenu != null) { eventSubMenu.setVisible(false); eventSubMenu.setManaged(false); }
        if (courseSubMenu != null) { courseSubMenu.setVisible(false); courseSubMenu.setManaged(false); }
        if (notesSubMenu != null) { notesSubMenu.setVisible(false); notesSubMenu.setManaged(false); }
        if (userSubMenu != null) { userSubMenu.setVisible(false); userSubMenu.setManaged(false); }

        if (subMenu != null) {
            subMenu.setVisible(!isVisible);
            subMenu.setManaged(!isVisible);
        }
    }

    @FXML
    public void handleExit() {
        try {
            Parent splash = FXMLLoader.load(getClass().getResource("/view/Splash.fxml"));
            Main.getPrimaryStage().getScene().setRoot(splash);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    public void showHelpRequests() {
        loadView("/view/assistance/HelpRequestList.fxml");
    }

    @FXML
    public void showAdminAssistance() {
        loadView("/view/assistance/AdminAssistance.fxml");
    }

    @FXML
    public void showAddRequest() {
        loadView("/view/assistance/HelpRequestForm.fxml");
    }

    @FXML
    public void showCommunityBoard() {
        loadView("/view/assistance/CommunityBoard.fxml");
    }

    @FXML
    public void showPlaceholder() {
        Label placeholder = new Label("🚧 This module is currently under construction.");
        placeholder.setStyle("-fx-font-size: 20px; -fx-text-fill: #a0a0ab; -fx-font-weight: bold;");
        contentArea.getChildren().setAll(placeholder);
    }

    private void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent view = loader.load();
            
            Object controller = loader.getController();
            if (controller != null) {
                try {
                    java.lang.reflect.Method setAdminModeMethod = controller.getClass().getMethod("setAdminMode", boolean.class);
                    setAdminModeMethod.invoke(controller, this.isAdmin);
                } catch (NoSuchMethodException e) {
                    // Controller does not support admin mode toggling, ignore
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Error loading view: " + fxmlFile);
            e.printStackTrace();
        }
    }

    /**
     * Public method so child controllers can navigate back.
     */
    public StackPane getContentArea() {
        return contentArea;
    }
}
