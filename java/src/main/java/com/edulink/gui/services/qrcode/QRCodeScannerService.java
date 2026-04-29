package com.edulink.gui.services.qrcode;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import com.github.sarxos.webcam.Webcam;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

public class QRCodeScannerService {

    // =====================
    // Scanner depuis la webcam
    // =====================
    public static String scanFromWebcam() {
        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            System.out.println("❌ Aucune webcam détectée !");
            return null;
        }

        try {
            webcam.open();
            System.out.println("📷 Webcam ouverte — recherche d'un QR Code...");

            // Essaye pendant 30 secondes max
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < 30000) {
                BufferedImage image = webcam.getImage();
                if (image != null) {
                    String result = decodeImage(image);
                    if (result != null) {
                        System.out.println("✅ QR Code détecté : " + result);
                        webcam.close();
                        return result;
                    }
                }
                // Petite pause pour ne pas surcharger le CPU
                Thread.sleep(100);
            }

            System.out.println("⏱ Timeout — aucun QR Code détecté en 30 secondes");
            webcam.close();
            return null;

        } catch (Exception e) {
            System.out.println("❌ Erreur webcam : " + e.getMessage());
            if (webcam.isOpen()) webcam.close();
            return null;
        }
    }

    // =====================
    // Scanner depuis une image
    // =====================
    public static String scanFromImage(File imageFile) {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                System.out.println("❌ Impossible de lire l'image !");
                return null;
            }
            String result = decodeImage(image);
            if (result != null) {
                System.out.println("✅ QR Code décodé : " + result);
            } else {
                System.out.println("❌ Aucun QR Code trouvé dans l'image");
            }
            return result;
        } catch (Exception e) {
            System.out.println("❌ Erreur lecture image : " + e.getMessage());
            return null;
        }
    }

    // =====================
    // Décoder une BufferedImage
    // =====================
    private static String decodeImage(BufferedImage image) {
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new MultiFormatReader().decode(bitmap);
            return result.getText();
        } catch (NotFoundException e) {
            // Pas de QR Code dans cette image — normal
            return null;
        }
    }

    // =====================
    // Parser le contenu du QR Code
    // =====================
    public static int[] parseQRContent(String content) {
        // Format attendu : "edulink://event?id=42&reservation=15"
        try {
            int eventId = Integer.parseInt(
                content.split("id=")[1].split("&")[0]
            );
            int reservationId = Integer.parseInt(
                content.split("reservation=")[1]
            );
            return new int[]{eventId, reservationId};
        } catch (Exception e) {
            System.out.println("❌ Format QR Code invalide : " + content);
            return null;
        }
    }

    // Scanner directement depuis un BufferedImage (pour la webcam)
    public static String decodeQRFromBufferedImage(BufferedImage image) {
        return decodeImage(image);
    }
}