package com.edulink.gui.services.mail;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public class QRCodeService {

    /**
     * Génère un QR Code en tant qu'image BufferedImage
     * @param content  le texte à encoder dans le QR Code
     * @param width    largeur en pixels
     * @param height   hauteur en pixels
     */
    public static BufferedImage generateQRCode(String content, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        } catch (WriterException e) {
            System.out.println("❌ Erreur génération QR Code : " + e.getMessage());
            return null;
        }
    }

    /**
     * Convertit un BufferedImage en tableau de bytes PNG
     * (nécessaire pour l'attacher au mail)
     */
    public static byte[] toByteArray(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            System.out.println("❌ Erreur conversion QR Code : " + e.getMessage());
            return null;
        }
    }

    /**
     * Génère directement le QR Code en bytes PNG
     * (combine les deux méthodes ci-dessus)
     */
    public static byte[] generateQRCodeBytes(String content, int width, int height) {
        BufferedImage image = generateQRCode(content, width, height);
        if (image == null) return null;
        return toByteArray(image);
    }
}