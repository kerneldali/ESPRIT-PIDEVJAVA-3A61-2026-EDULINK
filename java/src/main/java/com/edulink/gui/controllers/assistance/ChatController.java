package com.edulink.gui.controllers.assistance;

import com.edulink.gui.models.assistance.ChatMessage;
import com.edulink.gui.models.assistance.HelpSession;
import com.edulink.gui.services.assistance.HelpSessionService;
import com.edulink.gui.services.assistance.SuggestedRepliesService;
import com.edulink.gui.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Live tutoring session chat controller.
 * Handles sending messages, toxicity warnings, AI suggestions, Jitsi link, and session close.
 */
public class ChatController implements Initializable {

    @FXML private Label sessionTitleLabel;
    @FXML private Label jitsiLinkLabel;
    @FXML private Label sessionInfoLabel;
    @FXML private ScrollPane messagesScroll;
    @FXML private VBox messagesContainer;
    @FXML private TextField messageInput;
    @FXML private HBox suggestionsBar;
    @FXML private Button sendBtn;
    @FXML private Button closeSessionBtn;
    @FXML private Label statusLabel;

    private HelpSessionService sessionService = new HelpSessionService();
    private SuggestedRepliesService suggestionsService = new SuggestedRepliesService();
    private HelpSession currentSession;
    private int currentUserId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUserId = (SessionManager.getCurrentUser() != null)
            ? SessionManager.getCurrentUser().getId() : 1;
    }

    public void setSession(HelpSession session) {
        this.currentSession = session;
        sessionTitleLabel.setText("Session #" + session.getId() + " — " + session.getJitsiRoomId());
        jitsiLinkLabel.setText("📹 meet.jit.si/" + session.getJitsiRoomId());
        jitsiLinkLabel.setOnMouseClicked(e -> openJitsi(session.getJitsiRoomId()));
        sessionInfoLabel.setText("Tutor: " + nvl(session.getTutorName())
            + "  |  Student: " + nvl(session.getStudentName())
            + "  |  Bounty: " + session.getBountyEscrowed() + " EDU");
        refreshMessages();
        loadSuggestions();
    }

    @FXML
    public void handleSend() {
        String text = messageInput.getText().trim();
        if (text.isEmpty() || currentSession == null) return;

        messageInput.setDisable(true);
        sendBtn.setDisable(true);
        statusLabel.setText("Checking message...");

        new Thread(() -> {
            ChatMessage saved = sessionService.sendMessage(currentSession.getId(), currentUserId, text);
            Platform.runLater(() -> {
                messageInput.setDisable(false);
                sendBtn.setDisable(false);
                if (saved == null) {
                    statusLabel.setText("Failed to send.");
                    return;
                }
                if (saved.isToxic()) {
                    statusLabel.setStyle("-fx-text-fill:#ef4444;");
                    statusLabel.setText("⚠ Toxic content detected and flagged.");
                } else {
                    statusLabel.setText("✓ Sent");
                }
                messageInput.clear();
                refreshMessages();
                loadSuggestions();
            });
        }).start();
    }

    @FXML
    public void handleCloseSession() {
        if (currentSession == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("End Session");
        confirm.setHeaderText("Close this tutoring session?");
        confirm.setContentText("The AI will evaluate the session and transfer the bounty if quality meets the threshold.");
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                closeSessionBtn.setDisable(true);
                statusLabel.setText("🤖 Generating AI summary...");
                new Thread(() -> {
                    String summary = sessionService.closeSession(currentSession.getId());
                    Platform.runLater(() -> {
                        statusLabel.setText("✓ Session closed.");
                        Alert info = new Alert(Alert.AlertType.INFORMATION);
                        info.setTitle("Session Closed");
                        info.setHeaderText("Session Summary");
                        info.setContentText(summary);
                        info.showAndWait();
                        navigateBack();
                    });
                }).start();
            }
        });
    }

    private void refreshMessages() {
        if (currentSession == null) return;
        new Thread(() -> {
            List<ChatMessage> messages = sessionService.getMessages(currentSession.getId());
            Platform.runLater(() -> {
                messagesContainer.getChildren().clear();
                for (ChatMessage msg : messages) {
                    messagesContainer.getChildren().add(buildMessageBubble(msg));
                }
                messagesScroll.setVvalue(1.0);
            });
        }).start();
    }

    private void loadSuggestions() {
        new Thread(() -> {
            List<ChatMessage> recent = sessionService.getMessages(
                currentSession != null ? currentSession.getId() : -1);
            List<String> suggestions = suggestionsService.suggest(recent);
            Platform.runLater(() -> {
                suggestionsBar.getChildren().clear();
                for (String s : suggestions) {
                    Button btn = new Button(s);
                    btn.getStyleClass().add("suggestion-chip");
                    btn.setStyle("-fx-background-color:#1e293b;-fx-text-fill:#a5b4fc;"
                        + "-fx-border-color:#6366f1;-fx-border-radius:20;-fx-background-radius:20;"
                        + "-fx-padding:4 12;-fx-cursor:hand;");
                    btn.setOnAction(e -> messageInput.setText(s));
                    suggestionsBar.getChildren().add(btn);
                }
            });
        }).start();
    }

    private HBox buildMessageBubble(ChatMessage msg) {
        HBox row = new HBox();
        row.setPadding(new Insets(4, 12, 4, 12));

        VBox bubble = new VBox(3);
        bubble.setMaxWidth(500);
        bubble.setPadding(new Insets(10, 14, 10, 14));

        Label sender = new Label(nvl(msg.getSenderName()));
        sender.setFont(Font.font("System", FontWeight.BOLD, 11));

        Label content = new Label(msg.getContent());
        content.setWrapText(true);
        content.setFont(Font.font(13));

        bubble.getChildren().add(sender);

        if (msg.isToxic()) {
            Label toxicBadge = new Label("⚠ Flagged as toxic");
            toxicBadge.setStyle("-fx-text-fill:#ef4444;-fx-font-size:11;");
            content.setStyle("-fx-text-fill:#9ca3af;");
            bubble.getChildren().addAll(content, toxicBadge);
        } else {
            bubble.getChildren().add(content);
        }

        Label time = new Label(msg.getTimestamp() != null
            ? new java.text.SimpleDateFormat("HH:mm").format(msg.getTimestamp()) : "");
        time.setStyle("-fx-font-size:10;-fx-text-fill:#6b7280;");
        bubble.getChildren().add(time);

        if (msg.isMine()) {
            bubble.setStyle("-fx-background-color:#4f46e5;-fx-background-radius:12 12 2 12;");
            sender.setStyle("-fx-text-fill:#c7d2fe;");
            content.setStyle("-fx-text-fill:white;");
            row.setAlignment(Pos.CENTER_RIGHT);
        } else {
            bubble.setStyle("-fx-background-color:#1e293b;-fx-background-radius:12 12 12 2;");
            sender.setStyle("-fx-text-fill:#a5b4fc;");
            content.setStyle("-fx-text-fill:#e2e8f0;");
            row.setAlignment(Pos.CENTER_LEFT);
        }

        row.getChildren().add(bubble);
        return row;
    }

    private void openJitsi(String roomId) {
        try {
            Desktop.getDesktop().browse(new URI("https://meet.jit.si/" + roomId));
        } catch (Exception e) {
            statusLabel.setText("Cannot open browser: " + e.getMessage());
        }
    }

    private void navigateBack() {
        javafx.scene.Node n = messagesContainer;
        while (n != null) {
            if (n instanceof StackPane s && "contentArea".equals(s.getId())) {
                try {
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                        getClass().getResource("/view/assistance/HelpRequestList.fxml"));
                    s.getChildren().setAll((javafx.scene.Node) loader.load());
                } catch (Exception e) { e.printStackTrace(); }
                return;
            }
            n = n.getParent();
        }
    }

    private String nvl(String s) { return (s == null) ? "Unknown" : s; }
}
