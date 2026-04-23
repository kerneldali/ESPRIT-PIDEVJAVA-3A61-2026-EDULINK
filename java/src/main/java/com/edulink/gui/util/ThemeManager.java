package com.edulink.gui.util;

import javafx.scene.Node;
import javafx.scene.Parent;

public class ThemeManager {
    // True because we just made the inline styles dark in previous step,
    // so we assume the "default" loaded from FXMLs is Dark.
    private static boolean isDarkTheme = true;
    
    public static void toggleTheme() {
        isDarkTheme = !isDarkTheme;
    }
    
    public static boolean isDarkTheme() {
        return isDarkTheme;
    }
    
    public static void applyTheme(Node root) {
        if (root == null) return;
        
        String style = root.getStyle();
        if (style != null && !style.isEmpty()) {
            if (!isDarkTheme) {
                // Convert Dark inline styles back to Light
                style = style.replaceAll("(?i)-fx-background-color\\s*:\\s*#1a1a2e\\s*;?", "-fx-background-color: white;");
                style = style.replaceAll("(?i)-fx-background-color\\s*:\\s*#0f0f1a\\s*;?", "-fx-background-color: #f4f7fb;");
                style = style.replaceAll("(?i)-fx-background-color\\s*:\\s*#2a2a3e\\s*;?", "-fx-background-color: #f8fafc;");
                style = style.replaceAll("(?i)-fx-background-color\\s*:\\s*linear-gradient\\(to bottom right, #0f172a, #1e293b, #334155\\)\\s*;?", "-fx-background-color: linear-gradient(to bottom right, #eff6ff, #dbeafe, #bfdbfe);");
                style = style.replaceAll("(?i)-fx-text-fill\\s*:\\s*white\\s*;?", "-fx-text-fill: #1e293b;");
                style = style.replaceAll("(?i)-fx-text-fill\\s*:\\s*#a0a0ab\\s*;?", "-fx-text-fill: #64748b;");
                style = style.replaceAll("(?i)-fx-border-color\\s*:\\s*#ffffff11\\s*;?", "-fx-border-color: #cbd5e1;");
                style = style.replaceAll("(?i)-fx-control-inner-background\\s*:\\s*#2a2a3e\\s*;?", "-fx-control-inner-background: #f8fafc;");
            } else {
                // Convert Light to Dark
                style = style.replaceAll("(?i)-fx-background-color\\s*:\\s*white\\s*;?", "-fx-background-color: #1a1a2e;");
                style = style.replaceAll("(?i)-fx-background-color\\s*:\\s*#f4f7fb\\s*;?", "-fx-background-color: #0f0f1a;");
                style = style.replaceAll("(?i)-fx-background-color\\s*:\\s*#f8fafc\\s*;?", "-fx-background-color: #2a2a3e;");
                style = style.replaceAll("(?i)-fx-background-color\\s*:\\s*linear-gradient\\(to bottom right, #eff6ff, #dbeafe, #bfdbfe\\)\\s*;?", "-fx-background-color: linear-gradient(to bottom right, #0f172a, #1e293b, #334155);");
                style = style.replaceAll("(?i)-fx-text-fill\\s*:\\s*#1e293b\\s*;?", "-fx-text-fill: white;");
                style = style.replaceAll("(?i)-fx-text-fill\\s*:\\s*#0f172a\\s*;?", "-fx-text-fill: white;");
                style = style.replaceAll("(?i)-fx-text-fill\\s*:\\s*#111827\\s*;?", "-fx-text-fill: white;");
                style = style.replaceAll("(?i)-fx-text-fill\\s*:\\s*#334155\\s*;?", "-fx-text-fill: white;");
                style = style.replaceAll("(?i)-fx-text-fill\\s*:\\s*#64748b\\s*;?", "-fx-text-fill: #a0a0ab;");
                style = style.replaceAll("(?i)-fx-text-fill\\s*:\\s*#475569\\s*;?", "-fx-text-fill: #a0a0ab;");
                style = style.replaceAll("(?i)-fx-text-fill\\s*:\\s*#94a3b8\\s*;?", "-fx-text-fill: #a0a0ab;");
                style = style.replaceAll("(?i)-fx-border-color\\s*:\\s*#cbd5e1\\s*;?", "-fx-border-color: #ffffff11;");
                style = style.replaceAll("(?i)-fx-border-color\\s*:\\s*#e2e8f0\\s*;?", "-fx-border-color: #ffffff11;");
                style = style.replaceAll("(?i)-fx-control-inner-background\\s*:\\s*#f8fafc\\s*;?", "-fx-control-inner-background: #2a2a3e;");
            }
            root.setStyle(style);
        }
        
        // Force override backgrounds for specific classes
        if (root.getStyleClass().contains("main-container")) {
            String currentStyle = root.getStyle() == null ? "" : root.getStyle();
            if (!isDarkTheme) {
                root.setStyle(currentStyle + "; -fx-background-color: #f4f7fb;");
            } else {
                root.setStyle(currentStyle + "; -fx-background-color: #0f0f1a;");
            }
        }
        if (root.getStyleClass().contains("community-root") || root.getStyleClass().contains("community-split-pane")) {
            String currentStyle = root.getStyle() == null ? "" : root.getStyle();
            if (!isDarkTheme) {
                root.setStyle(currentStyle + "; -fx-background-color: #f4f7fb;");
            } else {
                root.setStyle(currentStyle + "; -fx-background-color: #0F0F0F;");
            }
        }
        if (root.getStyleClass().contains("content-area")) {
            String currentStyle = root.getStyle() == null ? "" : root.getStyle();
            if (!isDarkTheme) {
                root.setStyle(currentStyle + "; -fx-background-color: #f4f7fb;");
            } else {
                root.setStyle(currentStyle + "; -fx-background-color: #0F0F0F;");
            }
        }
        if (root.getStyleClass().contains("filter-bar")) {
            String currentStyle = root.getStyle() == null ? "" : root.getStyle();
            if (!isDarkTheme) {
                root.setStyle(currentStyle + "; -fx-background-color: white;");
            } else {
                root.setStyle(currentStyle + "; -fx-background-color: #1a1a2e;");
            }
        }
        if (root.getStyleClass().contains("title-header") || root.getStyleClass().contains("page-title") || root.getStyleClass().contains("event-title") || root.getStyleClass().contains("popup-title-large")) {
            String currentStyle = root.getStyle() == null ? "" : root.getStyle();
            if (!isDarkTheme) {
                root.setStyle(currentStyle + "; -fx-text-fill: #1e293b;");
            } else {
                root.setStyle(currentStyle + "; -fx-text-fill: white;");
            }
        }
        if (root.getStyleClass().contains("event-date") || root.getStyleClass().contains("popup-info-label") || root.getStyleClass().contains("popup-desc-text")) {
            String currentStyle = root.getStyle() == null ? "" : root.getStyle();
            if (!isDarkTheme) {
                root.setStyle(currentStyle + "; -fx-text-fill: #64748b;");
            } else {
                root.setStyle(currentStyle + "; -fx-text-fill: #a0a0ab;");
            }
        }
        if (root.getStyleClass().contains("popup-info-value")) {
            String currentStyle = root.getStyle() == null ? "" : root.getStyle();
            if (!isDarkTheme) {
                root.setStyle(currentStyle + "; -fx-text-fill: #1e293b;");
            } else {
                root.setStyle(currentStyle + "; -fx-text-fill: #f3f4f6;");
            }
        }
        
        if (root instanceof Parent) {
            // Special handling for scene root stylesheets if applicable
            Parent parent = (Parent) root;
            if (parent.getScene() != null && parent == parent.getScene().getRoot()) {
                String lightThemeUrl;
                java.net.URL res = ThemeManager.class.getResource("/styles/light-theme.css");
                if (res != null) {
                    lightThemeUrl = res.toExternalForm();
                } else {
                    java.io.File file = new java.io.File("ESPRIT-PIDEVJAVA-3A61-2026-EDULINK/java/src/main/resources/styles/light-theme.css");
                    if (!file.exists()) {
                        file = new java.io.File("src/main/resources/styles/light-theme.css"); // Fallback if CWD is java
                    }
                    if (!file.exists()) {
                        file = new java.io.File("../java/src/main/resources/styles/light-theme.css");
                    }
                    lightThemeUrl = file.toURI().toString();
                }
                
                System.out.println("ThemeManager resolving light theme url: " + lightThemeUrl);
                
                if (!isDarkTheme) {
                    parent.setStyle("-fx-base: #f4f7fb; -fx-background: #f4f7fb;");
                    if (!parent.getScene().getStylesheets().contains(lightThemeUrl)) {
                        parent.getScene().getStylesheets().add(lightThemeUrl);
                    }
                    System.out.println("Scene stylesheets: " + parent.getScene().getStylesheets());
                } else {
                    parent.setStyle("-fx-base: #1a1a2e; -fx-background: #0f0f1a;");
                    parent.getScene().getStylesheets().remove(lightThemeUrl);
                }
            }
            
            // Override local stylesheets precedence
            if (!parent.getStylesheets().isEmpty()) {
                String lightThemeUrl;
                java.net.URL res = ThemeManager.class.getResource("/styles/light-theme.css");
                if (res != null) {
                    lightThemeUrl = res.toExternalForm();
                } else {
                    java.io.File file = new java.io.File("ESPRIT-PIDEVJAVA-3A61-2026-EDULINK/java/src/main/resources/styles/light-theme.css");
                    if (!file.exists()) {
                        file = new java.io.File("src/main/resources/styles/light-theme.css");
                    }
                    if (!file.exists()) {
                        file = new java.io.File("../java/src/main/resources/styles/light-theme.css");
                    }
                    lightThemeUrl = file.toURI().toString();
                }
                
                if (!isDarkTheme && !parent.getStylesheets().contains(lightThemeUrl)) {
                    parent.getStylesheets().add(lightThemeUrl);
                    System.out.println("Added to parent stylesheets. Now: " + parent.getStylesheets());
                } else if (isDarkTheme) {
                    parent.getStylesheets().remove(lightThemeUrl);
                }
            }
            
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyTheme(child);
            }
            
            if (root instanceof javafx.scene.control.ScrollPane) {
                Node content = ((javafx.scene.control.ScrollPane) root).getContent();
                if (content != null) applyTheme(content);
            } else if (root instanceof javafx.scene.control.TabPane) {
                for (javafx.scene.control.Tab tab : ((javafx.scene.control.TabPane) root).getTabs()) {
                    if (tab.getContent() != null) applyTheme(tab.getContent());
                }
            } else if (root instanceof javafx.scene.control.SplitPane) {
                for (Node item : ((javafx.scene.control.SplitPane) root).getItems()) {
                    applyTheme(item);
                }
            } else if (root instanceof javafx.scene.control.TitledPane) {
                Node content = ((javafx.scene.control.TitledPane) root).getContent();
                if (content != null) applyTheme(content);
            }
        }
    }
}
