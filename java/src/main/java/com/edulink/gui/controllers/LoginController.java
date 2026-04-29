package com.edulink.gui.controllers;

import com.edulink.gui.Main;
import com.edulink.gui.models.User;
import com.edulink.gui.services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.canvas.Canvas;
import javafx.stage.FileChooser;

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
    @FXML private VBox forgotPasswordBox;
    @FXML private VBox faceIdBox;
    
    // Captcha components
    @FXML private Canvas captchaCanvas;
    @FXML private TextField captchaField;
    
    // Forgot Password components
    @FXML private TextField resetEmailField;
    @FXML private TextField otpField;
    @FXML private PasswordField newPasswordField;
    @FXML private Label resetErrorLabel;
    @FXML private Button sendOtpBtn;
    @FXML private Button verifyOtpBtn;

    // Face ID components
    @FXML private ImageView cameraFeed;
    @FXML private Label cameraStatusLabel;
    
    @FXML private Label loadingLabel;

    private UserService userService;
    private com.edulink.gui.services.user.CaptchaService captchaService = new com.edulink.gui.services.user.CaptchaService();
    private com.edulink.gui.services.user.FaceIdService faceIdService = new com.edulink.gui.services.user.FaceIdService();

    @FXML
    public void initialize() {
        userService = new UserService();
        showLogin();
        refreshCaptcha();
    }
    
    @FXML
    public void refreshCaptcha() {
        if (captchaCanvas != null) {
            captchaService.generateVisualCaptcha(captchaCanvas);
        }
        captchaField.clear();
    }

    @FXML
    public void showRegister() {
        loginBox.setVisible(false);
        loginBox.setManaged(false);
        if(forgotPasswordBox != null) {
            forgotPasswordBox.setVisible(false);
            forgotPasswordBox.setManaged(false);
        }
        registerBox.setVisible(true);
        registerBox.setManaged(true);
        errorLabel.setText("");
        regErrorLabel.setText("");
        if(resetErrorLabel != null) resetErrorLabel.setText("");
        refreshCaptcha();
    }

    @FXML
    public void showLogin() {
        registerBox.setVisible(false);
        registerBox.setManaged(false);
        if(forgotPasswordBox != null) {
            forgotPasswordBox.setVisible(false);
            forgotPasswordBox.setManaged(false);
        }
        loginBox.setVisible(true);
        loginBox.setManaged(true);
        errorLabel.setText("");
        regErrorLabel.setText("");
        if(resetErrorLabel != null) resetErrorLabel.setText("");
    }
    
    @FXML
    public void showForgotPassword() {
        loginBox.setVisible(false);
        loginBox.setManaged(false);
        registerBox.setVisible(false);
        registerBox.setManaged(false);
        forgotPasswordBox.setVisible(true);
        forgotPasswordBox.setManaged(true);
        resetErrorLabel.setText("");
        
        otpField.setVisible(false);
        otpField.setManaged(false);
        newPasswordField.setVisible(false);
        newPasswordField.setManaged(false);
        verifyOtpBtn.setVisible(false);
        verifyOtpBtn.setManaged(false);
        
        sendOtpBtn.setVisible(true);
        sendOtpBtn.setManaged(true);
        resetEmailField.setDisable(false);
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
    public void handleFaceIdLogin() {
        String email = emailField.getText();
        if (email.isEmpty()) {
            errorLabel.setText("Enter your email first to use Face ID");
            return;
        }
        User u = userService.findByEmail(email);
        if (u == null) {
            errorLabel.setText("User not found");
            return;
        }
        /* Skip hash check for simulation/demo purposes, assume configuring/authenticating
        if (u.getFaceHash() == null) {
            errorLabel.setText("Face ID not configured for this account.");
            return;
        } */
        
        loginBox.setVisible(false);
        loginBox.setManaged(false);
        faceIdBox.setVisible(true);
        faceIdBox.setManaged(true);
        cameraStatusLabel.setText("Initializing Camera...");
        
        faceIdService.startCamera(cameraFeed, cameraStatusLabel, (success) -> {
            if (success) {
                com.edulink.gui.util.SessionManager.setCurrentUser(u);
                boolean isAdmin = u.hasRole("ROLE_ADMIN") || u.hasRole("ROLE_FACULTY");
                launchDashboard(isAdmin);
            }
        });
    }

    @FXML
    public void cancelFaceId() {
        faceIdService.stopCamera();
        faceIdBox.setVisible(false);
        faceIdBox.setManaged(false);
        loginBox.setVisible(true);
        loginBox.setManaged(true);
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
        
        if (!captchaService.verifyCaptcha(captchaField.getText())) {
            regErrorLabel.setText("Incorrect Captcha. Try again.");
            refreshCaptcha();
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
    
    @FXML
    public void handleSendOtp() {
        String email = resetEmailField.getText();
        if (email.isEmpty()) {
            resetErrorLabel.setText("Please enter your email");
            return;
        }
        
        User u = userService.findByEmail(email);
        if (u == null) {
            resetErrorLabel.setText("Email not found");
            return;
        }
        
        String otp = String.format("%04d", new java.util.Random().nextInt(10000));
        userService.setOtp(email, otp);
        
        // Simulating email send due to potentially missing MailService properties locally,
        // but normally this calls: com.edulink.gui.services.mail.MailService.sendOtpEmail(email, otp);
        com.edulink.gui.services.mail.MailService.sendOtpEmail(email, otp);
        
        resetErrorLabel.setStyle("-fx-text-fill: #10b981;");
        resetErrorLabel.setText("OTP sent to your email!");
        
        resetEmailField.setDisable(true);
        sendOtpBtn.setVisible(false);
        sendOtpBtn.setManaged(false);
        
        otpField.setVisible(true);
        otpField.setManaged(true);
        newPasswordField.setVisible(true);
        newPasswordField.setManaged(true);
        verifyOtpBtn.setVisible(true);
        verifyOtpBtn.setManaged(true);
    }
    
    @FXML
    public void handleVerifyOtpAndReset() {
        String email = resetEmailField.getText();
        String otp = otpField.getText();
        String newPass = newPasswordField.getText();
        
        if (otp.isEmpty() || newPass.isEmpty()) {
            resetErrorLabel.setStyle("-fx-text-fill: #ef4444;");
            resetErrorLabel.setText("Please enter OTP and new password");
            return;
        }
        
        if (userService.verifyOtp(email, otp)) {
            userService.updatePassword(email, newPass);
            resetErrorLabel.setStyle("-fx-text-fill: #10b981;");
            resetErrorLabel.setText("Password reset successfully! Please login.");
            showLogin();
        } else {
            resetErrorLabel.setStyle("-fx-text-fill: #ef4444;");
            resetErrorLabel.setText("Invalid OTP");
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
