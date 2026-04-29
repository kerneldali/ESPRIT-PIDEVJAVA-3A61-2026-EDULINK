package com.edulink.gui;

import com.edulink.gui.services.ml.MLServerManager;

public class AppLauncher {
    public static void main(String[] args) {
        // Lancer le serveur ML automatiquement
        MLServerManager.start();
        
        // Lancer l'app JavaFX
        Main.main(args);
    }
}