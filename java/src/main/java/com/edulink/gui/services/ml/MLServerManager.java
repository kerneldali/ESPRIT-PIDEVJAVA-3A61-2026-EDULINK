package com.edulink.gui.services.ml;

import java.io.File;

public class MLServerManager {

    private static Process flaskProcess = null;

    public static void start() {
        try {
            // Trouver le chemin du dossier ml/
           String mlPath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator + "ml";

            ProcessBuilder pb = new ProcessBuilder("python", "app.py");
            pb.directory(new File(mlPath));
            pb.redirectErrorStream(true);

            flaskProcess = pb.start();
            System.out.println("✅ Serveur ML lancé !");

            // Donner 2 secondes à Flask pour démarrer
            Thread.sleep(2000);

            // Arrêter Flask automatiquement quand Java s'arrête
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                stop();
            }));

        } catch (Exception e) {
            System.out.println("❌ Erreur lancement ML : " + e.getMessage());
        }
    }

    public static void stop() {
        if (flaskProcess != null && flaskProcess.isAlive()) {
            flaskProcess.destroy();
            System.out.println("⏹ Serveur ML arrêté !");
        }
    }
}