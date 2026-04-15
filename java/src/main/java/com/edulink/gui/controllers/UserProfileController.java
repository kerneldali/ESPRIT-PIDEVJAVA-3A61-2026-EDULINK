package com.edulink.gui.controllers;

import com.edulink.gui.models.User;
import com.edulink.gui.services.UserService;
import com.edulink.gui.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class UserProfileController implements Initializable {

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    
    @FXML private Label statusLabel;
    @FXML private Label nameDisplayLabel;
    @FXML private Label roleDisplayLabel;
    
    @FXML private Label walletBalanceLabel;
    @FXML private TextField friendEmailField;
    @FXML private TextField sendAmountField;
    
    @FXML private ListView<String> logsListView;

    private UserService userService;
    private User currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = new UserService();
        currentUser = SessionManager.getCurrentUser();
        
        if (currentUser != null) {
            loadUserData();
            addLog("Dashboard initialized successfully.");
        } else {
            statusLabel.setText("No user session found.");
        }
    }

    private void loadUserData() {
        // Refresh currentUser from DB to get the latest wallet balance
        currentUser = userService.findByEmail(currentUser.getEmail());
        SessionManager.setCurrentUser(currentUser);
        
        fullNameField.setText(currentUser.getFullName());
        emailField.setText(currentUser.getEmail());
        nameDisplayLabel.setText(currentUser.getFullName());
        roleDisplayLabel.setText("Roles: " + currentUser.getRoles());
        
        walletBalanceLabel.setText(String.format("%.2f TND", currentUser.getWalletBalance()));
        
        // Load persistent logs
        logsListView.getItems().clear();
        for (String log : userService.getUserTransactions(currentUser.getId())) {
            logsListView.getItems().add(0, log); // Add to top
        }
    }

    private void addLog(String message) {
        if (currentUser != null) {
            userService.addTransactionLog(currentUser.getId(), message);
            loadUserData(); // refreshing re-loads the latest DB logs
        }
    }

    @FXML
    public void handleUpdateProfile() {
        if (currentUser == null) return;
        
        String newName = fullNameField.getText();
        String newEmail = emailField.getText();
        
        if (newName.isEmpty() || newEmail.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: #ef4444;");
            statusLabel.setText("Name and Email cannot be empty.");
            return;
        }

        currentUser.setFullName(newName);
        currentUser.setEmail(newEmail);
        
        try {
            userService.edit(currentUser);
            SessionManager.setCurrentUser(currentUser);
            loadUserData();
            
            statusLabel.setStyle("-fx-text-fill: #10b981;");
            statusLabel.setText("Profile updated successfully.");
            addLog("Profile updated: Name set to '" + newName + "', Email set to '" + newEmail + "'.");
            
        } catch (Exception e) {
            statusLabel.setStyle("-fx-text-fill: #ef4444;");
            statusLabel.setText("Error updating profile.");
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSendFunds() {
        if (currentUser == null) return;
        
        String friendEmail = friendEmailField.getText();
        if (friendEmail.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: #ef4444;");
            statusLabel.setText("Please enter friend's email address.");
            return;
        }

        if (friendEmail.equalsIgnoreCase(currentUser.getEmail())) {
            statusLabel.setStyle("-fx-text-fill: #ef4444;");
            statusLabel.setText("You cannot send funds to yourself.");
            return;
        }
        
        try {
            double amount = Double.parseDouble(sendAmountField.getText());
            if (amount <= 0) {
                statusLabel.setStyle("-fx-text-fill: #ef4444;");
                statusLabel.setText("Enter a valid amount > 0");
                return;
            }

            if (currentUser.getWalletBalance() < amount) {
                statusLabel.setStyle("-fx-text-fill: #ef4444;");
                statusLabel.setText("Insufficient funds.");
                return;
            }

            User friend = userService.findByEmail(friendEmail);
            if (friend == null) {
                statusLabel.setStyle("-fx-text-fill: #ef4444;");
                statusLabel.setText("User with that email does not exist.");
                return;
            }
            
            // Deduct from sender
            userService.updateWallet(currentUser.getId(), -amount);
            currentUser.setWalletBalance(currentUser.getWalletBalance() - amount);
            
            // Add to friend
            userService.updateWallet(friend.getId(), amount);
            
            loadUserData();
            
            friendEmailField.clear();
            sendAmountField.clear();
            statusLabel.setStyle("-fx-text-fill: #10b981;");
            statusLabel.setText("Successfully sent " + amount + " TND to " + friend.getFullName() + ".");
            addLog("Sent " + amount + " TND to friend at '" + friendEmail + "'.");
            
        } catch (NumberFormatException e) {
            statusLabel.setStyle("-fx-text-fill: #ef4444;");
            statusLabel.setText("Invalid number format for amount.");
        } catch (Exception e) {
            statusLabel.setStyle("-fx-text-fill: #ef4444;");
            statusLabel.setText("An error occurred during the transaction.");
            e.printStackTrace();
        }
    }
}
