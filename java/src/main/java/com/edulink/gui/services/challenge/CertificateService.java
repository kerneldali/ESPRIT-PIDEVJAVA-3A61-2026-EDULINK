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

    private static final DeviceRgb PURE_WHITE = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb GOLD       = new DeviceRgb(184, 134, 11);
    private static final DeviceRgb DEEP_NAVY  = new DeviceRgb(15, 23, 42);
    private static final DeviceRgb TEXT_GRAY  = new DeviceRgb(71, 85, 105);

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
            document.setBackgroundColor(PURE_WHITE);
            document.setMargins(40, 50, 40, 50);

            PdfFont fontBold    = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
            PdfFont fontRegular = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);

            // Cadre extérieur
            Table outerBorder = new Table(UnitValue.createPercentArray(new float[]{1})).useAllAvailableWidth();
            Cell outerCell = new Cell()
                    .setBorder(new SolidBorder(GOLD, 6))
                    .setBackgroundColor(PURE_WHITE)
                    .setPadding(35);

            // Branding
            outerCell.add(new Paragraph("⚡ EDULINK ELITE ACADEMY")
                    .setFont(fontBold).setFontSize(14)
                    .setFontColor(DEEP_NAVY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setCharacterSpacing(2f));

            // Titre Principal
            outerCell.add(new Paragraph("CERTIFICATE")
                    .setFont(fontBold).setFontSize(48)
                    .setFontColor(DEEP_NAVY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(10));
            
            outerCell.add(new Paragraph("OF EXCELLENCE")
                    .setFont(fontBold).setFontSize(22)
                    .setFontColor(GOLD)
                    .setTextAlignment(TextAlignment.CENTER));

            outerCell.add(new Paragraph("This is to certify the achievement of")
                    .setFont(fontRegular).setFontSize(16)
                    .setFontColor(TEXT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(5));

            // Nom Étudiant
            outerCell.add(new Paragraph(studentName.toUpperCase())
                    .setFont(fontBold).setFontSize(44)
                    .setFontColor(DEEP_NAVY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(10).setUnderline());

            // Corps
            outerCell.add(new Paragraph("for outstanding performance in the challenge")
                    .setFont(fontRegular).setFontSize(16)
                    .setFontColor(TEXT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(15));

            outerCell.add(new Paragraph("« " + challengeTitle + " »")
                    .setFont(fontBold).setFontSize(24)
                    .setFontColor(GOLD)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(5));

            // Stats
            String diffIcon = "HARD".equals(difficulty) ? "🔴 HARD" :
                              "MEDIUM".equals(difficulty) ? "🟡 MEDIUM" : "🟢 EASY";
            outerCell.add(new Paragraph("Difficulty: " + diffIcon + "    |    Reward: +" + xpReward + " XP")
                    .setFont(fontRegular).setFontSize(12)
                    .setFontColor(TEXT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20));

            // Footer
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy", java.util.Locale.ENGLISH));
            Table footerTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .useAllAvailableWidth()
                    .setMarginTop(30);
            
            footerTable.addCell(new Cell().add(new Paragraph("Délivré le " + today)
                    .setFontColor(TEXT_GRAY).setFontSize(10))
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            
            footerTable.addCell(new Cell().add(new Paragraph("__________________________\nAuthorized Signature")
                    .setFontColor(DEEP_NAVY).setFontSize(12).setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));

            outerCell.add(footerTable);
            outerBorder.addCell(outerCell);
            document.add(outerBorder);
            document.close();

            System.out.println("✅ Certificat généré : " + filePath);
            return filePath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
