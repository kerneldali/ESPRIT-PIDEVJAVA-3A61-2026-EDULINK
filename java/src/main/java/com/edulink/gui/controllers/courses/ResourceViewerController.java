package com.edulink.gui.controllers.courses;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.scene.layout.VBox;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

public class ResourceViewerController {

    @FXML private WebView webView;
    @FXML private Label viewerTitle;

    private String originalPath;

    public void loadResource(String title, String path, String type) {
        System.out.println("📂 [UNIVERSAL_VIEWER] Type: " + type + ", Path: " + path);
        this.viewerTitle.setText(title);
        this.originalPath = path;
        
        webView.setVisible(true);
        String lowerType = (type != null) ? type.toLowerCase() : "link";

        if (path.contains("youtube.com") || path.contains("youtu.be")) {
            loadYouTube(path);
        } else if ("video".equals(lowerType) || path.toLowerCase().endsWith(".mp4")) {
            loadVideoHtml(path);
        } else if ("pdf".equals(lowerType) || path.toLowerCase().endsWith(".pdf")) {
            loadPdf(path);
        } else {
            loadGenericWeb(path);
        }
    }

    private void loadVideoHtml(String path) {
        String source = path.startsWith("http") ? path : new File(path).toURI().toString();
        String html = "<!DOCTYPE html><html><head><style>" +
                "body, html { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; background-color: #000; }" +
                "video { width: 100%; height: 100%; object-fit: contain; }" +
                "</style></head><body>" +
                "<video controls autoplay name=\"media\"><source src=\"" + source + "\" type=\"video/mp4\"></video>" +
                "</body></html>";
        webView.getEngine().loadContent(html);
    }

    private void loadYouTube(String url) {
        String videoId = "";
        try {
            if (url.contains("v=")) videoId = url.split("v=")[1].split("&")[0];
            else if (url.contains("youtu.be/")) videoId = url.split("youtu.be/")[1].split("\\?")[0];
        } catch (Exception e) {}

        String embedUrl = "https://www.youtube.com/embed/" + videoId + "?autoplay=1";
        String html = "<!DOCTYPE html><html><head><style>body,html{margin:0;padding:0;width:100%;height:100%;background:#000;}iframe{width:100%;height:100%;border:none;}</style></head>" +
                     "<body><iframe src=\"" + embedUrl + "\" allowfullscreen></iframe></body></html>";
        webView.getEngine().loadContent(html);
    }

    private void loadPdf(String path) {
        // High-Reliability PDF rendering using PDF.js via CDN + Local Injection
        String pdfJsUrl = "https://mozilla.github.io/pdf.js/web/viewer.html";
        
        if (path.startsWith("http")) {
            try {
                String encodedUrl = java.net.URLEncoder.encode(path, "UTF-8");
                webView.getEngine().load(pdfJsUrl + "?file=" + encodedUrl);
            } catch (Exception e) {
                webView.getEngine().load(path);
            }
        } else {
            File f = new File(path);
            if (f.exists()) {
                try {
                    // Method: Inject Base64 PDF into an internal viewer
                    byte[] bytes = Files.readAllBytes(f.toPath());
                    String base64 = Base64.getEncoder().encodeToString(bytes);
                    
                    String html = "<!DOCTYPE html><html><head>" +
                            "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/pdf.js/2.16.105/pdf.min.js\"></script>" +
                            "<style>body,html{margin:0;padding:20px;background:#1e293b;display:flex;flex-direction:column;align-items:center;}" +
                            "canvas{box-shadow:0 10px 15px -3px rgba(0,0,0,0.5);margin-bottom:20px;max-width:100%;}</style></head><body>" +
                            "<div id=\"viewer\"></div>" +
                            "<script>" +
                            "var pdfData = atob('" + base64 + "');" +
                            "var loadingTask = pdfjsLib.getDocument({data: pdfData});" +
                            "loadingTask.promise.then(function(pdf) {" +
                            "  for(var i=1; i<=pdf.numPages; i++) {" +
                            "    pdf.getPage(i).then(function(page) {" +
                            "      var scale = 1.5;" +
                            "      var viewport = page.getViewport({scale: scale});" +
                            "      var canvas = document.createElement('canvas');" +
                            "      var context = canvas.getContext('2d');" +
                            "      canvas.height = viewport.height;" +
                            "      canvas.width = viewport.width;" +
                            "      document.getElementById('viewer').appendChild(canvas);" +
                            "      page.render({canvasContext: context, viewport: viewport});" +
                            "    });" +
                            "  }" +
                            "});" +
                            "</script></body></html>";
                    webView.getEngine().loadContent(html);
                } catch (Exception e) {
                    e.printStackTrace();
                    webView.getEngine().load(f.toURI().toString());
                }
            } else {
                loadGenericWeb(path);
            }
        }
    }

    private void loadGenericWeb(String path) {
        String url = path.startsWith("http") ? path : new File(path).toURI().toString();
        webView.getEngine().load(url);
    }

    @FXML
    private void handleWatchOnline() {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(originalPath));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose() {
        webView.getEngine().load(null);
        webView.getScene().getWindow().hide(); 
    }
}
