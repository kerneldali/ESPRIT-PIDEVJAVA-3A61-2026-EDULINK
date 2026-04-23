package com.edulink.gui.controllers;

import com.edulink.gui.Main;
import com.edulink.gui.models.User;
import com.edulink.gui.services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    
    @FXML private TextField regEmailField;
    @FXML private TextField regFullNameField;
    @FXML private PasswordField regPasswordField;
    @FXML private Label regErrorLabel;
    
    @FXML private VBox loginBox;
    @FXML private VBox registerBox;
    
    @FXML private Label loadingLabel;

    private UserService userService;

    @FXML
    public void initialize() {
        userService = new UserService();
        showLogin();
    }

    @FXML
    public void showRegister() {
        loginBox.setVisible(false);
        loginBox.setManaged(false);
        registerBox.setVisible(true);
        registerBox.setManaged(true);
        errorLabel.setText("");
        regErrorLabel.setText("");
    }

    @FXML
    public void showLogin() {
        registerBox.setVisible(false);
        registerBox.setManaged(false);
        loginBox.setVisible(true);
        loginBox.setManaged(true);
        errorLabel.setText("");
        regErrorLabel.setText("");
    }

    @FXML
    public void handleLogin() {
        String email    = emailField.getText();
        String password = passwordField.getText();
        errorLabel.setStyle("-fx-text-fill: #ef4444;");

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in all fields");
            return;
        }
        if (!email.contains("@")) {
            errorLabel.setText("Email must contain '@'");
            return;
        }

        // Vérifier la connexion DB avant de tenter l'auth
        if (!userService.isConnected()) {
            errorLabel.setText("⚠ DB Error: " + com.edulink.gui.util.MyConnection.getLastError());
            return;
        }

        try {
            User user = userService.authenticate(email, password);
            if (user != null) {
                com.edulink.gui.util.SessionManager.setCurrentUser(user);
                boolean isAdmin = user.hasRole("ROLE_ADMIN") || user.hasRole("ROLE_FACULTY");
                launchDashboard(isAdmin);
            } else {
                errorLabel.setText("Invalid email or password");
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Login error: " + e.getMessage());
        }
    }

    @FXML
    public void handleRegister() {
        String email = regEmailField.getText();
        String fullName = regFullNameField.getText();
        String password = regPasswordField.getText();
        
        if (email.isEmpty() || fullName.isEmpty() || password.isEmpty()) {
            regErrorLabel.setText("Please fill in all fields");
            return;
        }

        if (!email.contains("@")) {
            regErrorLabel.setText("Email must contain '@'");
            return;
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setFullName(fullName);
        newUser.setPassword(password);
        newUser.setRoles("[\"ROLE_USER\"]");
        newUser.setVerified(true); // For testing simplicity
        
        try {
            userService.add2(newUser);
            showLogin();
            errorLabel.setStyle("-fx-text-fill: #10b981;"); // Green
            errorLabel.setText("Registration successful! Please log in.");
        } catch (Exception e) {
            regErrorLabel.setText("Registration failed. Email might already exist.");
        }
    }

    private void launchDashboard(boolean isAdmin) {
        if (loadingLabel != null) {
            loadingLabel.setText("Connecting... Please wait");
            loadingLabel.setVisible(true);
        }
        
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
            e.getSource().getException().printStackTrace();
            errorLabel.setText("Error loading dashboard");
            if (loadingLabel != null) loadingLabel.setVisible(false);
        });

        new Thread(loadTask).start();
    }
}
