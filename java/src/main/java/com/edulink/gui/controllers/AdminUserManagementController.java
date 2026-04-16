package com.edulink.gui.controllers;

import com.edulink.gui.models.User;
import com.edulink.gui.services.UserService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class AdminUserManagementController implements Initializable {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> idCol;
    @FXML private TableColumn<User, String> nameCol;
    @FXML private TableColumn<User, String> emailCol;
    @FXML private TableColumn<User, String> rolesCol;

    @FXML private Label totalUsersLabel;
    @FXML private Label totalVerifiedLabel;
    
    // Search and Sort
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    
    // Detail Pane
    @FXML private Label selectedNameLabel;
    @FXML private Label selectedRolesLabel;
    @FXML private Label selectedWalletLabel;
    @FXML private VBox detailBadgeBox;

    // Form Pane
    @FXML private TextField formNameField;
    @FXML private TextField formEmailField;
    @FXML private TextField formPasswordField;
    @FXML private ComboBox<String> formRoleCombo;
    @FXML private Label formStatusLabel;
    @FXML private ListView<String> logsListView;
    
    private UserService userService;
    private ObservableList<User> userList;
    private FilteredList<User> filteredData;
    private User currentlySelectedUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = new UserService();
        userList = FXCollections.observableArrayList();
        
        setupTable();
        refreshData();
        
        formRoleCombo.setItems(FXCollections.observableArrayList("[\"ROLE_USER\"]", "[\"ROLE_STUDENT\"]", "[\"ROLE_FACULTY\"]", "[\"ROLE_ADMIN\"]"));
        formRoleCombo.getSelectionModel().selectFirst();
        
        // Setup Search
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(user -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                
                if (user.getFullName() != null && user.getFullName().toLowerCase().contains(lowerCaseFilter)) return true;
                if (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerCaseFilter)) return true;
                if (user.getRoles() != null && user.getRoles().toLowerCase().contains(lowerCaseFilter)) return true;
                return false;
            });
        });

        // Setup Sort Combo
        sortCombo.setItems(FXCollections.observableArrayList("ID (Ascending)", "ID (Descending)", "Name (A-Z)", "Wallet (High to Low)"));
        sortCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> performSort(newV));

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                showUserDetails(newSel);
            }
        });
    }

    private void performSort(String sortType) {
        if (sortType == null) return;
        Comparator<User> comparator = null;
        switch (sortType) {
            case "ID (Ascending)": comparator = Comparator.comparingInt(User::getId); break;
            case "ID (Descending)": comparator = Comparator.comparingInt(User::getId).reversed(); break;
            case "Name (A-Z)": comparator = Comparator.comparing(u -> u.getFullName().toLowerCase()); break;
            case "Wallet (High to Low)": comparator = Comparator.comparingDouble(User::getWalletBalance).reversed(); break;
        }
        
        if (comparator != null) {
             FXCollections.sort(userList, comparator);
        }
    }

    private void setupTable() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        rolesCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRoles()));
        
        filteredData = new FilteredList<>(userList, p -> true);
        SortedList<User> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(userTable.comparatorProperty());
        userTable.setItems(sortedData);
    }

    private void refreshData() {
        List<User> users = userService.getAll();
        userList.setAll(users);
        
        // Maintain active sort
        if (sortCombo.getValue() != null) performSort(sortCombo.getValue());
        
        long verifiedCount = users.stream().filter(User::isVerified).count();
        totalUsersLabel.setText(String.valueOf(users.size()));
        totalVerifiedLabel.setText(String.valueOf(verifiedCount));
        
        clearSelection();
    }

    private void showUserDetails(User user) {
        currentlySelectedUser = user;
        detailBadgeBox.setVisible(true);
        detailBadgeBox.setManaged(true);
        selectedNameLabel.setText(user.getFullName());
        selectedRolesLabel.setText("System Roles: " + user.getRoles());
        selectedWalletLabel.setText(String.format("%.2f TND", user.getWalletBalance()));
        
        formNameField.setText(user.getFullName());
        formEmailField.setText(user.getEmail());
        formPasswordField.setPromptText("Leave blank to keep current");
        formRoleCombo.setValue(user.getRoles());

        // Load logs
        logsListView.getItems().clear();
        logsListView.getItems().setAll(userService.getUserTransactions(user.getId()));
    }

    @FXML
    public void handleClear() {
        clearSelection();
    }

    private void clearSelection() {
        userTable.getSelectionModel().clearSelection();
        currentlySelectedUser = null;
        detailBadgeBox.setVisible(false);
        detailBadgeBox.setManaged(false);
        formNameField.clear();
        formEmailField.clear();
        formPasswordField.clear();
        formPasswordField.setPromptText("Required for new user");
        formRoleCombo.getSelectionModel().selectFirst();
        formStatusLabel.setText("");
        logsListView.getItems().clear();
    }

    @FXML
    public void handleSaveUser() {
        String name = formNameField.getText();
        String email = formEmailField.getText();
        String password = formPasswordField.getText();
        String roles = formRoleCombo.getValue();
        
        if (name.isEmpty() || email.isEmpty()) {
            formStatusLabel.setText("Name and Email are required.");
            formStatusLabel.setStyle("-fx-text-fill: #ef4444;");
            return;
        }

        if (currentlySelectedUser == null) {
            // Create New
            if (password.isEmpty()) {
                formStatusLabel.setText("Password required for new account.");
                formStatusLabel.setStyle("-fx-text-fill: #ef4444;");
                return;
            }
            User newUser = new User();
            newUser.setFullName(name);
            newUser.setEmail(email);
            newUser.setPassword(password);
            newUser.setRoles(roles);
            newUser.setVerified(true);
            try {
                userService.add2(newUser);
                formStatusLabel.setText("User created successfully!");
                formStatusLabel.setStyle("-fx-text-fill: #10b981;");
                refreshData();
            } catch (Exception e) {
                formStatusLabel.setText("Error creating user."); e.printStackTrace();
            }
        } else {
            // Update Existing
            currentlySelectedUser.setFullName(name);
            currentlySelectedUser.setEmail(email);
            currentlySelectedUser.setRoles(roles);
            try {
                userService.edit(currentlySelectedUser);
                formStatusLabel.setText("User updated successfully!");
                formStatusLabel.setStyle("-fx-text-fill: #10b981;");
                refreshData();
            } catch (Exception e) {
                formStatusLabel.setText("Error updating user."); e.printStackTrace();
            }
        }
    }

    @FXML
    public void handleDeleteUser() {
        if (currentlySelectedUser == null) {
            formStatusLabel.setText("Select a user to delete.");
            formStatusLabel.setStyle("-fx-text-fill: #ef4444;");
            return;
        }
        
        try {
            userService.delete(currentlySelectedUser.getId());
            formStatusLabel.setText("User deleted successfully!");
            formStatusLabel.setStyle("-fx-text-fill: #10b981;");
            refreshData();
        } catch (Exception e) {
            formStatusLabel.setText("Error deleting user."); e.printStackTrace();
        }
    }
}
