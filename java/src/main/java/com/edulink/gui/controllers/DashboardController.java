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

    @FXML
    private StackPane contentArea;
    @FXML
    private Label contextLabel;
    @FXML
    private VBox helpSubMenu, challengeSubMenu, eventSubMenu, courseSubMenu, notesSubMenu, userSubMenu;
    @FXML
    private Button adminOnlyTickets, adminUserStats, adminWallets, adminJournalStats;
    
    @FXML
    private Button studentCatalogBtn, studentLearningBtn, studentSuggestionsBtn;
    @FXML
    private Button adminCatalogBtn, adminSuggestionsBtn, adminCourseStatsBtn, adminActivityStatsBtn;
    @FXML
    private Button adminChallengesBtn;
    @FXML
    private Button adminReviewSubmissionsBtn;
    @FXML
    private Button adminChallengeStatsBtn;

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
            if (adminUserStats != null) {
                adminUserStats.setVisible(true);
                adminUserStats.setManaged(true);
            }
            if (adminWallets != null) {
                adminWallets.setVisible(true);
                adminWallets.setManaged(true);
            }
            if (adminJournalStats != null) {
                adminJournalStats.setVisible(true);
                adminJournalStats.setManaged(true);
            }
            if (adminCatalogBtn != null) {
                adminCatalogBtn.setVisible(true);
                adminCatalogBtn.setManaged(true);
            }
            if (adminSuggestionsBtn != null) {
                adminSuggestionsBtn.setVisible(true);
                adminSuggestionsBtn.setManaged(true);
            }
            if (adminCourseStatsBtn != null) {
                adminCourseStatsBtn.setVisible(true);
                adminCourseStatsBtn.setManaged(true);
            }
            if (adminActivityStatsBtn != null) {
                adminActivityStatsBtn.setVisible(true);
                adminActivityStatsBtn.setManaged(true);
            }
            if (adminChallengesBtn != null) {
                adminChallengesBtn.setVisible(true);
                adminChallengesBtn.setManaged(true);
            }
            if (adminReviewSubmissionsBtn != null) {
                adminReviewSubmissionsBtn.setVisible(true);
                adminReviewSubmissionsBtn.setManaged(true);
            }
            if (adminChallengeStatsBtn != null) {
                adminChallengeStatsBtn.setVisible(true);
                adminChallengeStatsBtn.setManaged(true);
            }
            
            if (studentCatalogBtn != null) {
                studentCatalogBtn.setVisible(false);
                studentCatalogBtn.setManaged(false);
            }
            if (studentLearningBtn != null) {
                studentLearningBtn.setVisible(false);
                studentLearningBtn.setManaged(false);
            }
            if (studentSuggestionsBtn != null) {
                studentSuggestionsBtn.setVisible(false);
                studentSuggestionsBtn.setManaged(false);
            }
        } else {
            contextLabel.setText("Student Frontoffice");
        }
    }

    @FXML public void showChallengeList()        { loadView("/view/challenge/ChallengeList.fxml"); }
    @FXML public void showMyChallenges()         { loadView("/view/challenge/MyChallenges.fxml"); }
    @FXML public void showManageChallenges()     { loadView("/view/challenge/ManageChallenges.fxml"); }
    @FXML public void showReviewSubmissions()    { loadView("/view/challenge/ReviewSubmissions.fxml"); }
    @FXML public void showChallengeStats()       { loadView("/view/challenge/ChallengeStats.fxml"); }

    // ── Events ──────────────────────────────────────────────────────────────
    @FXML public void showEvent()                { loadView("/view/event/event.fxml"); }
    @FXML public void showReservation()          { loadView("/view/reservation/reservation.fxml"); }

    @FXML public void showCatalog() { loadView("/view/courses/MatiereList.fxml"); }
    @FXML public void showMyLearning() { loadView("/view/courses/MyLearning.fxml"); }
    @FXML public void showStudentSuggestions() { loadView("/view/courses/StudentSuggestions.fxml"); }
    @FXML public void showManageCatalog() { loadView("/view/courses/ManageMatiere.fxml"); }
    @FXML public void showManageSuggestions() { loadView("/view/courses/ManageSuggestions.fxml"); }
    @FXML public void showCourseStats() { loadView("/view/courses/CourseStats.fxml"); }
    @FXML public void showActivityStats() { loadView("/view/courses/AnalyzeActivity.fxml"); }

    @FXML
    public void showAdminJournalStats() {
        loadView("/view/journal/AdminJournalStats.fxml");
    }

    @FXML
    public void toggleHelpMenu() {
        toggleMenu(helpSubMenu);
    }

    @FXML
    public void toggleChallengeMenu() {
        toggleMenu(challengeSubMenu);
    }

    @FXML
    public void toggleEventMenu() {
        toggleMenu(eventSubMenu);
    }

    @FXML
    public void toggleCourseMenu() {
        toggleMenu(courseSubMenu);
    }

    @FXML
    public void toggleNotesMenu() {
        toggleMenu(notesSubMenu);
    }

    @FXML
    public void toggleUserMenu() {
        toggleMenu(userSubMenu);
    }

    private void toggleMenu(VBox subMenu) {
        boolean isVisible = subMenu.isVisible();
        if (helpSubMenu != null) {
            helpSubMenu.setVisible(false);
            helpSubMenu.setManaged(false);
        }
        if (challengeSubMenu != null) {
            challengeSubMenu.setVisible(false);
            challengeSubMenu.setManaged(false);
        }
        if (eventSubMenu != null) {
            eventSubMenu.setVisible(false);
            eventSubMenu.setManaged(false);
        }
        if (courseSubMenu != null) {
            courseSubMenu.setVisible(false);
            courseSubMenu.setManaged(false);
        }
        if (notesSubMenu != null) {
            notesSubMenu.setVisible(false);
            notesSubMenu.setManaged(false);
        }
        if (userSubMenu != null) {
            userSubMenu.setVisible(false);
            userSubMenu.setManaged(false);
        }

        if (subMenu != null) {
            subMenu.setVisible(!isVisible);
            subMenu.setManaged(!isVisible);
        }
    }

    @FXML
    public void handleExit() {
        try {
            com.edulink.gui.util.SessionManager.clearSession();
            Parent login = FXMLLoader.load(getClass().getResource("/view/Login.fxml"));
            Main.getPrimaryStage().getScene().setRoot(login);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleToggleTheme() {
        com.edulink.gui.util.ThemeManager.toggleTheme();
        if (contentArea.getScene() != null && contentArea.getScene().getRoot() != null) {
            com.edulink.gui.util.ThemeManager.applyTheme(contentArea.getScene().getRoot());
        }
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
    public void showUserProfile() {
        loadView("/view/UserProfile.fxml");
    }

    @FXML
    public void showAdminUserManagement() {
        loadView("/view/AdminUserManagement.fxml");
    }

    @FXML
    public void showAdminWallets() {
        loadView("/view/AdminWalletManagement.fxml");
    }

    @FXML
    public void showNotebooks() {
        loadView("/view/journal/NotebookList.fxml");
    }

    @FXML
    public void showTasks() {
        loadView("/view/journal/TaskList.fxml");
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
                    java.lang.reflect.Method setAdminModeMethod = controller.getClass().getMethod("setAdminMode",
                            boolean.class);
                    setAdminModeMethod.invoke(controller, this.isAdmin);
                } catch (NoSuchMethodException e) {
                    // Controller does not support admin mode toggling, ignore
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            com.edulink.gui.util.ThemeManager.applyTheme(view);
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
