package com.edulink.gui.controllers;

import com.edulink.gui.models.User;
import com.edulink.gui.services.UserService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AdminWalletManagementController implements Initializable {

    @FXML private TableView<User> walletTable;
    @FXML private TableColumn<User, Integer> idCol;
    @FXML private TableColumn<User, String> nameCol;
    @FXML private TableColumn<User, String> emailCol;
    @FXML private TableColumn<User, String> balanceCol;

    @FXML private Label totalPlatformFundsLabel;
    @FXML private Label totalActiveWalletsLabel;
    
    // Search
    @FXML private TextField searchField;
    
    // Action Form
    @FXML private VBox actionPane;
    @FXML private Label actionUserNameLabel;
    @FXML private Label actionUserEmailLabel;
    @FXML private Label actionUserBalanceLabel;
    @FXML private TextField actionAmountField;
    @FXML private Label formStatusLabel;
    @FXML private ListView<String> logsListView;
    @FXML private VBox emptyPrompt;
    
    private UserService userService;
    private ObservableList<User> userList;
    private FilteredList<User> filteredData;
    private User selectedUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = new UserService();
        userList = FXCollections.observableArrayList();
        
        setupTable();
        refreshData();
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(user -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                
                if (user.getFullName() != null && user.getFullName().toLowerCase().contains(lowerCaseFilter)) return true;
                if (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerCaseFilter)) return true;
                return false;
            });
        });

        walletTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectUser(newSel);
            }
        });
    }

    private void setupTable() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        balanceCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%.2f XP", cellData.getValue().getWalletBalance())));
        
        filteredData = new FilteredList<>(userList, p -> true);
        walletTable.setItems(filteredData);
    }

    private void refreshData() {
        List<User> users = userService.getAll();
        userList.setAll(users);
        
        double totalFunds = users.stream().mapToDouble(User::getWalletBalance).sum();
        long activeWallets = users.stream().filter(u -> u.getWalletBalance() > 0).count();
        
        totalPlatformFundsLabel.setText(String.format("%.2f XP", totalFunds));
        totalActiveWalletsLabel.setText(String.valueOf(activeWallets));
        
        clearSelection();
    }

    private void selectUser(User user) {
        selectedUser = user;
        actionPane.setVisible(true);
        actionPane.setManaged(true);
        emptyPrompt.setVisible(false);
        emptyPrompt.setManaged(false);
        
        actionUserNameLabel.setText(user.getFullName());
        actionUserEmailLabel.setText(user.getEmail());
        actionUserBalanceLabel.setText(String.format("%.2f XP", user.getWalletBalance()));
        
        actionAmountField.clear();
        formStatusLabel.setText("");

        // Load logs
        logsListView.getItems().clear();
        logsListView.getItems().setAll(userService.getUserTransactions(user.getId()));
    }

    private void clearSelection() {
        walletTable.getSelectionModel().clearSelection();
        selectedUser = null;
        actionPane.setVisible(false);
        actionPane.setManaged(false);
        emptyPrompt.setVisible(true);
        emptyPrompt.setManaged(true);
        formStatusLabel.setText("");
        logsListView.getItems().clear();
    }

    @FXML
    public void handleRefundOrAdd() {
        if (selectedUser == null) return;
        
        try {
            double amount = Double.parseDouble(actionAmountField.getText());
            userService.updateWallet(selectedUser.getId(), amount);
            
            // Add a permanent transaction log for the user
            String action = amount > 0 ? "added" : "deducted";
            userService.addTransactionLog(selectedUser.getId(), "Admin " + action + " " + Math.abs(amount) + " XP from your secure wallet.");
            
            formStatusLabel.setStyle("-fx-text-fill: #10b981;");
            if (amount > 0) {
                formStatusLabel.setText("Successfully funded " + amount + " XP.");
            } else {
                formStatusLabel.setText("Successfully deducted " + Math.abs(amount) + " XP.");
            }
            // Update local state temporarily for UI snappiness
            selectedUser.setWalletBalance(selectedUser.getWalletBalance() + amount);
            actionUserBalanceLabel.setText(String.format("%.2f XP", selectedUser.getWalletBalance()));
            
            // Refresh table visually
            walletTable.refresh();
            
            // Refresh logs
            logsListView.getItems().clear();
            logsListView.getItems().setAll(userService.getUserTransactions(selectedUser.getId()));
            
            // Re-calculate totals
            double totalFunds = userList.stream().mapToDouble(User::getWalletBalance).sum();
            totalPlatformFundsLabel.setText(String.format("%.2f XP", totalFunds));
            long activeWallets = userList.stream().filter(u -> u.getWalletBalance() > 0).count();
            totalActiveWalletsLabel.setText(String.valueOf(activeWallets));
            
            actionAmountField.clear();
            
        } catch (NumberFormatException e) {
            formStatusLabel.setStyle("-fx-text-fill: #ef4444;");
            formStatusLabel.setText("Please enter a valid numeric amount.");
        } catch (Exception e) {
            formStatusLabel.setStyle("-fx-text-fill: #ef4444;");
            formStatusLabel.setText("Error executing transaction.");
            e.printStackTrace();
        }
    }
}
