package com.edulink.gui.controllers.courses;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.WebView;
import javafx.scene.layout.VBox;

public class VideoPlayerController {

    @FXML private WebView webView;
    @FXML private Label videoTitle;

    public void loadVideo(String title, String url) {
        videoTitle.setText(title);
        
        // Convert regular YouTube URL to embed URL if needed
        String embedUrl = url;
        if (url.contains("youtube.com/watch?v=")) {
            embedUrl = url.replace("watch?v=", "embed/");
        } else if (url.contains("youtu.be/")) {
            embedUrl = url.replace("youtu.be/", "youtube.com/embed/");
        }
        
        webView.getEngine().load(embedUrl);
    }

    @FXML
    private void handleClose() {
        webView.getEngine().load(null);
        if (webView.getScene().getRoot() instanceof VBox) {
            // Logic to hide the overlay in the parent if needed
            webView.getScene().getWindow().hide(); 
        }
    }
}
