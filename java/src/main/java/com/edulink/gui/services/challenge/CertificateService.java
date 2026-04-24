package com.edulink.gui.services.challenge;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CertificateService {

    private static final DeviceRgb DARK_BG    = new DeviceRgb(0x1a, 0x1a, 0x2e);
    private static final DeviceRgb GREEN_ACC  = new DeviceRgb(0x00, 0xd2, 0x89);
    private static final DeviceRgb PURPLE_ACC = new DeviceRgb(0x7c, 0x3a, 0xed);
    private static final DeviceRgb LIGHT_TEXT = new DeviceRgb(0xa0, 0xa0, 0xab);

    /**
     * Génère un certificat PDF et le sauvegarde sur le Bureau.
     * @return le chemin du fichier généré, ou null en cas d'erreur
     */
    public String generateCertificate(String studentName, String challengeTitle,
                                       int xpReward, String difficulty) {
        String safeName       = studentName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String safeChallenge  = challengeTitle.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String desktop        = System.getProperty("user.home") + File.separator + "Desktop";
        String filePath       = desktop + File.separator +
                                "Certificat_" + safeName + "_" + safeChallenge + ".pdf";

        try {
            PdfWriter   writer   = new PdfWriter(filePath);
            PdfDocument pdfDoc   = new PdfDocument(writer);
            Document    document = new Document(pdfDoc, PageSize.A4.rotate());
            document.setMargins(40, 50, 40, 50);

            PdfFont fontBold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // ── Cadre extérieur ──────────────────────────────────────────────
            Table outerBorder = new Table(UnitValue.createPercentArray(new float[]{1}))
                    .useAllAvailableWidth();
            Cell outerCell = new Cell()
                    .setBorder(new SolidBorder(GREEN_ACC, 3))
                    .setBackgroundColor(DARK_BG)
                    .setPadding(30);

            // ── Ligne de titre EduLink ────────────────────────────────────────
            Paragraph brand = new Paragraph("⚡  EduLink Academy")
                    .setFont(fontBold).setFontSize(13)
                    .setFontColor(GREEN_ACC)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(2);
            outerCell.add(brand);

            // Ligne décorative verte
            Table line = new Table(UnitValue.createPercentArray(new float[]{1})).useAllAvailableWidth();
            line.addCell(new Cell().setHeight(3).setBackgroundColor(GREEN_ACC)
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            outerCell.add(line);
            outerCell.add(new Paragraph(" ").setFontSize(6));

            // ── Titre principal ───────────────────────────────────────────────
            Paragraph mainTitle = new Paragraph("CERTIFICAT DE COMPLÉTION")
                    .setFont(fontBold).setFontSize(28)
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(4);
            outerCell.add(mainTitle);

            Paragraph subtitle = new Paragraph("est fièrement décerné à")
                    .setFont(fontRegular).setFontSize(13)
                    .setFontColor(LIGHT_TEXT)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10);
            outerCell.add(subtitle);

            // ── Nom étudiant ─────────────────────────────────────────────────
            Paragraph nameP = new Paragraph(studentName)
                    .setFont(fontBold).setFontSize(32)
                    .setFontColor(GREEN_ACC)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10);
            outerCell.add(nameP);

            // ── Texte du corps ────────────────────────────────────────────────
            Paragraph body1 = new Paragraph("pour avoir complété avec succès le challenge")
                    .setFont(fontRegular).setFontSize(14)
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(6);
            outerCell.add(body1);

            Paragraph challengeP = new Paragraph("« " + challengeTitle + " »")
                    .setFont(fontBold).setFontSize(20)
                    .setFontColor(PURPLE_ACC)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(6);
            outerCell.add(challengeP);

            // Difficulté + XP
            String diffIcon = "HARD".equals(difficulty) ? "🔴 HARD" :
                              "MEDIUM".equals(difficulty) ? "🟡 MEDIUM" : "🟢 EASY";
            Paragraph meta = new Paragraph("Difficulté : " + diffIcon + "    •    Récompense : +" + xpReward + " XP")
                    .setFont(fontRegular).setFontSize(12)
                    .setFontColor(LIGHT_TEXT)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            outerCell.add(meta);

            // Ligne décorative
            Table line2 = new Table(UnitValue.createPercentArray(new float[]{1})).useAllAvailableWidth();
            line2.addCell(new Cell().setHeight(1).setBackgroundColor(new DeviceRgb(0x2a, 0x2a, 0x3e))
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            outerCell.add(line2);
            outerCell.add(new Paragraph(" ").setFontSize(4));

            // Date + footer
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy",
                    java.util.Locale.FRENCH));
            Paragraph footer = new Paragraph("Délivré le " + today +
                    "    •    EduLink — Plateforme d'apprentissage gamifiée")
                    .setFont(fontRegular).setFontSize(10)
                    .setFontColor(LIGHT_TEXT)
                    .setTextAlignment(TextAlignment.CENTER);
            outerCell.add(footer);

            outerBorder.addCell(outerCell);
            document.add(outerBorder);
            document.close();

            System.out.println("✅ Certificat généré : " + filePath);
            return filePath;
        } catch (Exception e) {
            System.out.println("❌ Erreur génération certificat : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
