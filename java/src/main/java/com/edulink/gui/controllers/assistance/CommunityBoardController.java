package com.edulink.gui.controllers.assistance;

import com.edulink.gui.models.assistance.ForumThread;
import com.edulink.gui.models.assistance.ForumReply;
import com.edulink.gui.services.assistance.ForumService;
import com.edulink.gui.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import java.net.URL;
import java.util.ResourceBundle;

public class CommunityBoardController implements Initializable {

    @FXML private ListView<ForumThread> threadsList;
    @FXML private TextField searchField;
    
    // Focused Thread Area
    @FXML private VBox threadFocusArea;
    @FXML private Label focusedThreadTitle;
    @FXML private Label focusedThreadAuthor;
    @FXML private Button btnLike;
    @FXML private Button btnLove;
    @FXML private Button btnInsightful;
    @FXML private Button btnFunny;
    @FXML private Button btnSupport;
    @FXML private HBox reactionBar;
    @FXML private HBox adminActionsPane;
    @FXML private Button pinThreadBtn;
    @FXML private Button lockThreadBtn;
    @FXML private TextArea focusedThreadContent;
    @FXML private ListView<ForumReply> repliesList;
    @FXML private TextField newReplyField;
    
    // New Thread Form
    @FXML private VBox newThreadForm;
    @FXML private TextField newThreadTitle;
    @FXML private TextArea newThreadContent;

    private ForumService service = new ForumService();
    private com.edulink.gui.services.assistance.ToxicityService toxicityService = new com.edulink.gui.services.assistance.ToxicityService();
    private ObservableList<ForumThread> allThreads = FXCollections.observableArrayList();
    private ForumThread currentThread = null;
    private boolean isAdmin = false;

    public void setAdminMode(boolean admin) {
        this.isAdmin = admin;
        if (adminActionsPane != null) {
            adminActionsPane.setVisible(admin);
            adminActionsPane.setManaged(admin);
        }
        setupThreadList(); // Refresh cells for reply delete buttons
        loadThreads();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupThreadList();
        loadThreads();

        threadsList.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                showThreadDetails(newSel);
            }
        });
        
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterThreads(newVal));
    }

    private void setupThreadList() {
        threadsList.setCellFactory(param -> new ListCell<ForumThread>() {
            @Override
            protected void updateItem(ForumThread item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox root = new VBox(5);
                    Label title = new Label(item.getTitle());
                    title.setStyle("-fx-font-weight: bold;");
                    Label meta = new Label("By " + (item.getAuthorName() != null ? item.getAuthorName() : "Unknown") + 
                        " • " + item.getReplyCount() + " replies");
                    meta.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
                    root.getChildren().addAll(title, meta);
                    setGraphic(root);
                }
            }
        });
        
        repliesList.setCellFactory(param -> new ListCell<ForumReply>() {
            @Override
            protected void updateItem(ForumReply item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox root = new VBox(2);
                    HBox header = new HBox(5);
                    Label author = new Label(item.getAuthorName() != null ? item.getAuthorName() : "Unknown");
                    author.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
                    header.getChildren().add(author);
                    
                    if (isAdmin) {
                        Button delBtn = new Button("🗑");
                        delBtn.setStyle("-fx-text-fill: red; -fx-background-color: transparent; -fx-padding: 0;");
                        delBtn.setOnAction(e -> {
                            service.deleteReply(item.getId());
                            showThreadDetails(currentThread); // Reload 
                        });
                        header.getChildren().add(delBtn);
                    }
                    
                    Label content = new Label(item.getContent());
                    content.setWrapText(true);
                    root.getChildren().addAll(header, content);
                    setGraphic(root);
                }
            }
        });
    }

    private void loadThreads() {
        allThreads.setAll(service.getAllThreads());
        threadsList.setItems(allThreads);
    }

    private void filterThreads(String query) {
        if (query == null || query.isEmpty()) {
            threadsList.setItems(allThreads);
            return;
        }
        String q = query.toLowerCase();
        ObservableList<ForumThread> filtered = allThreads.filtered(t -> 
            (t.getTitle() != null && t.getTitle().toLowerCase().contains(q)) || 
            (t.getContent() != null && t.getContent().toLowerCase().contains(q))
        );
        threadsList.setItems(filtered);
    }

    private void showThreadDetails(ForumThread thread) {
        currentThread = thread;
        focusedThreadTitle.setText(thread.getTitle());
        focusedThreadAuthor.setText("Posted by: " + (thread.getAuthorName() != null ? thread.getAuthorName() : "Anonymous"));
        focusedThreadContent.setText(thread.getContent());
        
        loadReactions();

        // Load replies
        ObservableList<ForumReply> replies = FXCollections.observableArrayList(service.getRepliesForThread(thread.getId()));
        repliesList.setItems(replies);
        
        if (isAdmin && pinThreadBtn != null && lockThreadBtn != null) {
            pinThreadBtn.setVisible(true);
            lockThreadBtn.setVisible(true);
            pinThreadBtn.setText(thread.isPinned() ? "📌 Unpin" : "📌 Pin Thread");
            lockThreadBtn.setText(thread.isLocked() ? "🔓 Unlock" : "🔒 Lock Thread");
        }

        // Apply visual lock to students
        if (thread.isLocked()) {
            newReplyField.setDisable(true);
            newReplyField.setPromptText("🔒 This topic is locked by a moderator.");
        } else {
            newReplyField.setDisable(false);
            newReplyField.setPromptText("Write a thoughtful comment...");
        }
    }

    @FXML
    public void actionReportThread() {
        if (currentThread == null) return;
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Report Topic");
        dialog.setHeaderText("Report: " + currentThread.getTitle());
        dialog.setContentText("Reason for reporting:");

        dialog.showAndWait().ifPresent(reason -> {
            if (reason.trim().length() < 3) {
                showAlert("Error", "Please provide a valid reason.");
                return;
            }
            int reporterId = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 1;
            service.reportPost(currentThread.getId(), reporterId, reason.trim());
            showAlert("Report Received", "Thank you. A moderator has been alerted and will review this topic shortly.");
        });
    }

    @FXML
    public void handleAdminDeleteThread() {
        if (currentThread == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Delete Thread");
        confirm.setContentText("Are you sure you want to delete this thread and all replies?");
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                service.deleteThread(currentThread.getId());
                currentThread = null;
                focusedThreadTitle.setText("Select a topic to view");
                focusedThreadAuthor.setText("Posted by: ...");
                focusedThreadContent.clear();
                repliesList.getItems().clear();
                loadThreads();
            }
        });
    }

    @FXML
    public void handleAdminPinThread() {
        if (currentThread == null) return;
        service.setThreadStatus(currentThread.getId(), !currentThread.isPinned(), currentThread.isLocked());
        loadThreads();
    }

    @FXML
    public void handleAdminLockThread() {
        if (currentThread == null) return;
        service.setThreadStatus(currentThread.getId(), currentThread.isPinned(), !currentThread.isLocked());
        loadThreads();
    }

    @FXML
    public void postReply() {
        if (currentThread == null) {
            showAlert("Action Required", "Please select a discussion topic first.");
            return;
        }

        if (currentThread.isLocked()) {
            showAlert("Error", "This topic is locked. You cannot post new replies.");
            return;
        }
        
        String content = newReplyField.getText();
        if (content == null || content.trim().length() < 5) {
            showAlert("Input Error", "Your reply must contain at least 5 characters.");
            return;
        }
        if (content.length() > 500) {
            showAlert("Input Error", "Reply is too long (max 500 characters).");
            return;
        }
        
        if (toxicityService.analyze(content).isToxic) {
            showAlert("Content Blocked", "Your reply was flagged for inappropriate or toxic language. Please revise it.");
            return;
        }

        ForumReply r = new ForumReply();
        r.setThreadId(currentThread.getId());
        r.setContent(content.trim());
        int userId = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 1;
        r.setAuthorId(userId); 

        service.addReply(r);
        newReplyField.clear();
        
        // Refresh replies list
        showThreadDetails(currentThread);
    }

    @FXML
    public void showNewThreadForm() {
        newThreadForm.setVisible(true);
        newThreadForm.setManaged(true);
    }

    @FXML
    public void hideNewThreadForm() {
        newThreadForm.setVisible(false);
        newThreadForm.setManaged(false);
        newThreadTitle.clear();
        newThreadContent.clear();
    }

    @FXML
    public void postThread() {
        String title = newThreadTitle.getText();
        String content = newThreadContent.getText();
        
        if (title == null || title.trim().length() < 5) {
            showAlert("Input Error", "The topic title must be at least 5 characters long.");
            return;
        }
        if (title.length() > 100) {
            showAlert("Input Error", "Title is too long (max 100 characters).");
            return;
        }
        
        if (content == null || content.trim().length() < 10) {
            showAlert("Input Error", "Topic content must be at least 10 characters long.");
            return;
        }
        
        if (toxicityService.analyze(title).isToxic || toxicityService.analyze(content).isToxic) {
            showAlert("Content Blocked", "Your post was flagged for inappropriate or toxic language. Please revise it.");
            return;
        }

        ForumThread t = new ForumThread();
        t.setTitle(title.trim());
        t.setContent(content.trim());
        t.setBoardId(1);
        int authorId = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 1;
        t.setAuthorId(authorId); 

        service.addThread(t);
        hideNewThreadForm();
        loadThreads();
    }
    
    private static final String[] REACTION_TYPES = {"LIKE", "LOVE", "INSIGHTFUL", "FUNNY", "SUPPORT"};
    private static final String[] REACTION_EMOJIS = {"👍", "❤️", "💡", "😂", "🤝"};

    private void loadReactions() {
        if (currentThread == null || btnLike == null) return;
        java.util.Map<String, Integer> reactions = service.getReactions(currentThread.getId());
        int userId = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 1;
        String myReact = service.getUserReaction(currentThread.getId(), userId);

        Button[] btns = {btnLike, btnLove, btnInsightful, btnFunny, btnSupport};
        for (int i = 0; i < btns.length; i++) {
            if (btns[i] == null) continue;
            int count = reactions.getOrDefault(REACTION_TYPES[i], 0);
            btns[i].setText(REACTION_EMOJIS[i] + " " + count);
            boolean isActive = REACTION_TYPES[i].equals(myReact);
            btns[i].setStyle(isActive
                ? "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 14; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 4 10;"
                : "-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 14; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 4 10;");
        }
    }

    private void doReaction(String type) {
        if (currentThread == null) return;
        int userId = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 1;
        String myReact = service.getUserReaction(currentThread.getId(), userId);
        if (type.equals(myReact)) {
            service.removeReaction(currentThread.getId(), userId);
        } else {
            service.addReaction(currentThread.getId(), userId, type);
        }
        loadReactions();
    }

    @FXML public void handleReactionLike()       { doReaction("LIKE"); }
    @FXML public void handleReactionLove()       { doReaction("LOVE"); }
    @FXML public void handleReactionInsightful() { doReaction("INSIGHTFUL"); }
    @FXML public void handleReactionFunny()      { doReaction("FUNNY"); }
    @FXML public void handleReactionSupport()    { doReaction("SUPPORT"); }

    /** kept for backward compat if any old FXML references it */
    @FXML public void handleReaction() { doReaction("LIKE"); }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}