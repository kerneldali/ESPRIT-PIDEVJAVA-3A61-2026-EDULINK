package com.edulink.gui.controllers;

import com.edulink.gui.services.EduTokenService;
import com.edulink.gui.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

public class UserWalletController implements Initializable {

    @FXML private Label balanceLabel;
    @FXML private TextField ethAddressField;
    @FXML private PasswordField privateKeyField; // Never store in plain text in production!
    
    @FXML private TextField buyAmountField;
    @FXML private TextField sendAddressField;
    @FXML private TextField sendAmountField;
    
    @FXML private Label statusLabel;
    @FXML private ListView<String> transactionListView;
    
    private EduTokenService tokenService = new EduTokenService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        com.edulink.gui.models.User user = SessionManager.getCurrentUser();
        if (user != null) {
            if (user.getEthWalletAddress() != null) {
                ethAddressField.setText(user.getEthWalletAddress());
            }
            if (user.getEthPrivateKey() != null) {
                privateKeyField.setText(user.getEthPrivateKey());
            }
            refreshTransactionHistory();
            handleRefreshBalance();
        }
    }

    @FXML
    public void handleGenerateWallet() {
        com.edulink.gui.models.User user = SessionManager.getCurrentUser();
        if (user == null) return;
        
        if (user.getEthWalletAddress() != null && !user.getEthWalletAddress().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Wallet Exists");
            alert.setHeaderText("You already have an active wallet.");
            alert.setContentText("Address: " + user.getEthWalletAddress() + "\n\nEach user is restricted to a single blockchain identity for security.");
            alert.showAndWait();
            return;
        }

        statusLabel.setText("Generating fresh wallet...");
        String[] wallet = tokenService.generateAndSaveWallet(user.getId());
        if (wallet != null) {
            ethAddressField.setText(wallet[0]);
            privateKeyField.setText(wallet[1]);
            user.setEthWalletAddress(wallet[0]);
            user.setEthPrivateKey(wallet[1]);
            
            // Show Congratulatory Popup
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("🎉 Welcome Gift!");
                alert.setHeaderText("Congratulations on joining the Web3 economy!");
                alert.setContentText("A joining gift of 1000 EDU tokens has been minted and sent to your new wallet address. \n\nIt may take a moment to appear on-chain, but your database balance is updated immediately!");
                alert.showAndWait();
            });

            statusLabel.setText("✨ Wallet generated and saved to your profile!");
            handleRefreshBalance();
        } else {
            statusLabel.setText("❌ Wallet generation failed.");
        }
    }

    private void refreshTransactionHistory() {
        com.edulink.gui.models.User user = SessionManager.getCurrentUser();
        if (user == null) return;
        
        new Thread(() -> {
            var history = tokenService.getTransactionHistory(user.getId());
            Platform.runLater(() -> {
                transactionListView.getItems().clear();
                for (var tx : history) {
                    transactionListView.getItems().add(tx.txType + ": " + tx.txHash.substring(0, 10) + " (" + tx.amount + " EDU)");
                }
            });
        }).start();
    }

    @FXML
    public void handleRefreshBalance() {
        String address = ethAddressField.getText();
        if (address.isEmpty() || !address.startsWith("0x")) {
            statusLabel.setText("Invalid ETH address");
            return;
        }
        
        new Thread(() -> {
            try {
                Platform.runLater(() -> statusLabel.setText("Fetching balance from Sepolia..."));
                BigDecimal balance = tokenService.getBalance(address);
                Platform.runLater(() -> {
                    balanceLabel.setText(String.format("%.2f EDU", balance));
                    statusLabel.setText("✓ Balance updated");
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("✗ Failed to fetch balance"));
            }
        }).start();
    }

    private void logTransaction(String type, String hash) {
        Platform.runLater(() -> {
            transactionListView.getItems().add(0, type + ": " + hash.substring(0, 10) + "...");
        });
    }

    @FXML
    public void handleViewOnEtherscan() {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI("https://sepolia.etherscan.io/address/0x59A693aBAF46FF430C265888a26023B2f4308560"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void handleBuyTokens() {
        String address = ethAddressField.getText();
        String pk = privateKeyField.getText();
        String ethAmountStr = buyAmountField.getText();
        
        if (address.isEmpty() || pk.isEmpty() || ethAmountStr.isEmpty()) {
            statusLabel.setText("Please fill all buying fields.");
            return;
        }
        
        try {
            statusLabel.setText("Sending transaction to Sepolia...");
            String hash = tokenService.buyTokens(address, pk, ethAmountStr);
            Platform.runLater(() -> {
                statusLabel.setText("✓ Transaction sent! Hash: " + hash);
                logTransaction("BUY", hash);
                handleRefreshBalance();
            });
        } catch (Exception e) {
            statusLabel.setText("Purchase failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSendTokens() {
        String fromPk = privateKeyField.getText();
        String toAddress = sendAddressField.getText();
        String amountStr = sendAmountField.getText();
        
        if (fromPk.isEmpty() || toAddress.isEmpty() || amountStr.isEmpty()) {
            statusLabel.setText("Please fill all sending fields.");
            return;
        }
        
        try {
            statusLabel.setText("Sending EDU tokens...");
            String fromAddress = ethAddressField.getText();
            String hash = tokenService.transferTokens(fromAddress, fromPk, toAddress, Integer.parseInt(amountStr));
            Platform.runLater(() -> {
                statusLabel.setText("✓ Tokens sent! Hash: " + hash);
                logTransaction("SEND", hash);
                handleRefreshBalance();
            });
        } catch (Exception e) {
            statusLabel.setText("Transfer failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
