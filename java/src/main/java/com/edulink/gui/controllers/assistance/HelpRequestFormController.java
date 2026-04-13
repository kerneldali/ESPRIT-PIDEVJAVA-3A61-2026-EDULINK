package com.edulink.gui.controllers.assistance;

import com.edulink.gui.models.assistance.HelpRequest;
import com.edulink.gui.services.assistance.HelpRequestService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class HelpRequestFormController implements Initializable {

    @FXML private VBox formContainer;
    @FXML private Label formTitle;
    @FXML private TextField titleField;
    @FXML private TextArea descField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextField bountyField;
    @FXML private CheckBox ticketCheck;
    @FXML private TextField categoryField;
    @FXML private ComboBox<String> difficultyCombo;
    @FXML private TextArea closeReasonField;
    @FXML private Label errorLabel;

    private HelpRequest currentRequest;
    private HelpRequestService service = new HelpRequestService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statusCombo.getItems().addAll("OPEN", "IN_PROGRESS", "CLOSED");
        statusCombo.setValue("OPEN");
        difficultyCombo.getItems().addAll("Easy", "Medium", "Hard");
        difficultyCombo.setValue("Medium");
        
        Platform.runLater(() -> titleField.requestFocus());
    }

    public void setHelpRequest(HelpRequest req) {
        if (req == null) return;
        this.currentRequest = req;
        formTitle.setText("✏ Edit Request #" + req.getId());

        titleField.setText(req.getTitle());
        descField.setText(req.getDescription());
        statusCombo.setValue(req.getStatus());
        bountyField.setText(String.valueOf(req.getBounty()));
        ticketCheck.setSelected(req.isTicket());
        categoryField.setText(req.getCategory());
        difficultyCombo.setValue(req.getDifficulty());
        closeReasonField.setText(req.getCloseReason());
    }

    /**
     * CONTROLE DE SAISIE (Input Validation)
     * Optimized with Regex and strict length checks.
     */
    private boolean validate() {
        boolean isValid = true;
        resetStyles();

        // 1. Title Validation (10-100 chars, Alphanumeric + Basic Punctuation)
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        if (title.length() < 10 || title.length() > 100 || !title.matches("^[a-zA-Z0-9\\s\\-.,!?()]+$")) {
            applyErrorStyle(titleField);
            showError("Title must be 10-100 chars and contain only safe alphanumeric/punctuation.");
            isValid = false;
        }

        // 2. Description Validation (20+ chars, Safe chars)
        String desc = descField.getText() == null ? "" : descField.getText().trim();
        if (desc.length() < 20 || !desc.matches("^[a-zA-Z0-9\\s\\n\\-.,!?()'\"&]+$")) {
            applyErrorStyle(descField);
            if (isValid) showError("Description must be 20+ chars with safe characters only.");
            isValid = false;
        }

        // 3. Bounty Validation (Numeric & Positive Regex)
        String bountyStr = bountyField.getText() == null ? "" : bountyField.getText().trim();
        if (!bountyStr.matches("^[0-9]+$")) {
            applyErrorStyle(bountyField);
            if (isValid) showError("Bounty must be a valid positive integer.");
            isValid = false;
        } else {
            try {
                int bounty = Integer.parseInt(bountyStr);
                if (bounty < 5) {
                    applyErrorStyle(bountyField);
                    if (isValid) showError("Minimum bounty is $5.");
                    isValid = false;
                }
            } catch (Exception e) {
                applyErrorStyle(bountyField);
                if (isValid) showError("Bounty is too large.");
                isValid = false;
            }
        }

        // 4. Category Validation (Alpha only)
        String cat = categoryField.getText() == null ? "" : categoryField.getText().trim();
        if (cat.isEmpty() || !cat.matches("^[a-zA-Z\\s]+$")) {
            applyErrorStyle(categoryField);
            if (isValid) showError("Category must be letters only.");
            isValid = false;
        }

        // 5. Closed Reason logic (Conditional)
        if ("CLOSED".equals(statusCombo.getValue())) {
            String closeReason = closeReasonField.getText() == null ? "" : closeReasonField.getText().trim();
            if (closeReason.isEmpty() || !closeReason.matches("^[a-zA-Z0-9\\s\\-.,!?()]+$")) {
                applyErrorStyle(closeReasonField);
                if (isValid) showError("Please provide a valid reason for closing.");
                isValid = false;
            }
        }

        return isValid;
    }

    @FXML
    public void handleSave() {
        if (!validate()) return; // Stop if validation fails

        boolean isNew = (currentRequest == null);
        if (isNew) currentRequest = new HelpRequest();

        currentRequest.setTitle(titleField.getText().trim());
        currentRequest.setDescription(descField.getText().trim());
        currentRequest.setStatus(statusCombo.getValue());
        currentRequest.setBounty(Integer.parseInt(bountyField.getText().trim()));
        currentRequest.setTicket(ticketCheck.isSelected());
        currentRequest.setCategory(categoryField.getText().trim());
        currentRequest.setDifficulty(difficultyCombo.getValue());
        currentRequest.setCloseReason(closeReasonField.getText().trim());

        if (isNew) service.add(currentRequest);
        else service.edit(currentRequest);

        goBack();
    }

    @FXML
    public void handleCancel() { goBack(); }

    private void goBack() {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/view/assistance/HelpRequestList.fxml"));
            StackPane area = findContentArea();
            if (area != null) area.getChildren().setAll(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void applyErrorStyle(Control c) {
        c.getStyleClass().add("input-error");
    }

    private void resetStyles() {
        titleField.getStyleClass().remove("input-error");
        descField.getStyleClass().remove("input-error");
        bountyField.getStyleClass().remove("input-error");
        closeReasonField.getStyleClass().remove("input-error");
        errorLabel.setText("");
    }

    private StackPane findContentArea() {
        javafx.scene.Node n = formContainer;
        while (n != null) {
            if (n instanceof StackPane && "contentArea".equals(n.getId())) return (StackPane) n;
            n = n.getParent();
        }
        return null;
    }

    private void showError(String m) {
        errorLabel.setText("⚠ " + m);
        errorLabel.getStyleClass().add("error-text");
    }
}
