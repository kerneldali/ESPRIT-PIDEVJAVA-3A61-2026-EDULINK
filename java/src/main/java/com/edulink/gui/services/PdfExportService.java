package com.edulink.gui.services;

import com.edulink.gui.models.assistance.HelpRequest;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import java.io.File;
import java.util.List;

public class PdfExportService {

    public void exportHelpRequests(List<HelpRequest> requests, File destFile) throws Exception {
        PdfWriter writer = new PdfWriter(destFile.getAbsolutePath());
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Title
        Paragraph title = new Paragraph("EduLink Help Requests Report")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        // Table with 5 columns
        float[] columnWidths = {1, 3, 2, 2, 2};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));

        // Header cells
        table.addHeaderCell(createHeaderCell("ID"));
        table.addHeaderCell(createHeaderCell("Title"));
        table.addHeaderCell(createHeaderCell("Category"));
        table.addHeaderCell(createHeaderCell("Status"));
        table.addHeaderCell(createHeaderCell("Bounty"));

        // Data cells
        for (HelpRequest req : requests) {
            table.addCell(new Cell().add(new Paragraph(String.valueOf(req.getId()))));
            table.addCell(new Cell().add(new Paragraph(req.getTitle() != null ? req.getTitle() : "")));
            table.addCell(new Cell().add(new Paragraph(req.getCategory() != null ? req.getCategory() : "")));
            table.addCell(new Cell().add(new Paragraph(req.getStatus() != null ? req.getStatus() : "")));
            table.addCell(new Cell().add(new Paragraph("$" + req.getBounty())));
        }

        document.add(table);
        document.close();
    }

    public void exportCertificate(com.edulink.gui.models.User student, com.edulink.gui.models.courses.Course course, File destFile) throws Exception {
        PdfWriter writer = new PdfWriter(destFile.getAbsolutePath());
        PdfDocument pdf = new PdfDocument(writer);
        com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf, com.itextpdf.kernel.geom.PageSize.A4.rotate());

        // --- White/Gold/Black Design System ---
        com.itextpdf.kernel.colors.Color GOLD      = new com.itextpdf.kernel.colors.DeviceRgb(184, 134, 11); // DarkGoldenRod
        com.itextpdf.kernel.colors.Color DEEP_NAVY = new com.itextpdf.kernel.colors.DeviceRgb(15, 23, 42); 
        com.itextpdf.kernel.colors.Color PURE_WHITE = ColorConstants.WHITE;
        com.itextpdf.kernel.colors.Color TEXT_GRAY = new com.itextpdf.kernel.colors.DeviceRgb(71, 85, 105);
        
        document.setBackgroundColor(PURE_WHITE);
        document.setTextAlignment(TextAlignment.CENTER);
        
        // --- Elegant Double Border ---
        document.setBorder(new com.itextpdf.layout.borders.SolidBorder(GOLD, 10));
        document.setMargins(25, 35, 25, 35);

        // --- Branding ---
        document.add(new Paragraph("🎓 EDULINK ACADEMY")
                .setFontSize(22).setBold().setFontColor(DEEP_NAVY).setMarginTop(15).setCharacterSpacing(1.5f));

        // --- Main Title ---
        document.add(new Paragraph("CERTIFICATE")
                .setFontSize(60).setBold().setFontColor(DEEP_NAVY).setMarginTop(10));
        
        document.add(new Paragraph("OF ACHIEVEMENT")
                .setFontSize(24).setBold().setFontColor(GOLD).setMarginBottom(15));

        document.add(new Paragraph("This is to certify the achievement of")
                .setFontSize(18).setItalic().setFontColor(TEXT_GRAY).setMarginTop(5));

        // --- Student Name ---
        document.add(new Paragraph(student.getFullName().toUpperCase())
                .setFontSize(52).setBold().setFontColor(DEEP_NAVY).setMarginTop(5).setUnderline());

        // --- Course Section ---
        document.add(new Paragraph("for the successful completion of the course")
                .setFontSize(18).setFontColor(TEXT_GRAY).setMarginTop(15));

        document.add(new Paragraph(course.getTitle())
                .setFontSize(38).setBold().setFontColor(GOLD).setMarginTop(2));

        // --- Footer Section with Seal ---
        Table footerTable = new Table(3);
        footerTable.setWidth(UnitValue.createPercentValue(100));
        footerTable.setMarginTop(30);
        
        // Seal Image
        try {
            String sealPath = "src/main/resources/images/certificate_seal.png";
            com.itextpdf.layout.element.Image seal = new com.itextpdf.layout.element.Image(
                com.itextpdf.io.image.ImageDataFactory.create(sealPath))
                .setWidth(110).setHeight(110);
            footerTable.addCell(new Cell().add(seal)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.BOTTOM));
        } catch (Exception e) {
            footerTable.addCell(new Cell().add(new Paragraph("PLATINUM\nVERIFIED")
                .setFontSize(10).setBold().setFontColor(PURE_WHITE).setBackgroundColor(GOLD).setPadding(12))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.BOTTOM));
        }
            
        // Credential Details
        footerTable.addCell(new Cell().add(new Paragraph("ID: EL-" + (System.currentTimeMillis() / 1000) + "\n" + course.getXp() + " XP AWARDED")
            .setFontSize(11).setFontColor(TEXT_GRAY))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.BOTTOM));
            
        // Signature
        footerTable.addCell(new Cell().add(new Paragraph("__________________________\nAcademy Director Signature")
            .setFontSize(12).setItalic().setFontColor(DEEP_NAVY))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.BOTTOM));
            
        document.add(footerTable);
        document.close();
    }

    public void exportCertificateAsImage(com.edulink.gui.models.User student, com.edulink.gui.models.courses.Course course, File destFile) throws Exception {
        int width = 1200;
        int height = 800;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // --- Background ---
        g2.setColor(java.awt.Color.WHITE);
        g2.fillRect(0, 0, width, height);
        
        // --- Elegant Borders ---
        g2.setColor(new java.awt.Color(184, 134, 11)); // Gold
        g2.setStroke(new java.awt.BasicStroke(20));
        g2.drawRect(15, 15, width - 30, height - 30);
        
        g2.setColor(new java.awt.Color(15, 23, 42)); // Deep Navy
        g2.setStroke(new java.awt.BasicStroke(2));
        g2.drawRect(35, 35, width - 70, height - 70);
        
        // --- Branding ---
        g2.setFont(new java.awt.Font("Serif", java.awt.Font.BOLD, 32));
        g2.setColor(new java.awt.Color(15, 23, 42));
        g2.drawString("🎓 EDULINK ACADEMY", width/2 - 160, 100);
        
        // --- Title ---
        g2.setFont(new java.awt.Font("Serif", java.awt.Font.BOLD, 72));
        g2.drawString("CERTIFICATE", width/2 - 240, 200);
        
        g2.setFont(new java.awt.Font("Serif", java.awt.Font.BOLD, 28));
        g2.setColor(new java.awt.Color(184, 134, 11));
        g2.drawString("OF ACHIEVEMENT", width/2 - 120, 240);
        
        g2.setFont(new java.awt.Font("SansSerif", java.awt.Font.ITALIC, 24));
        g2.setColor(new java.awt.Color(71, 85, 105));
        g2.drawString("This is to certify the achievement of", width/2 - 180, 300);
        
        // --- Student Name ---
        g2.setFont(new java.awt.Font("Serif", java.awt.Font.BOLD, 64));
        g2.setColor(new java.awt.Color(15, 23, 42));
        String name = student.getFullName().toUpperCase();
        g2.drawString(name, width/2 - (name.length() * 19), 400);
        
        // --- Completion Text ---
        g2.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 24));
        g2.setColor(new java.awt.Color(71, 85, 105));
        g2.drawString("for successful completion of the course", width/2 - 200, 480);
        
        // --- Course Title ---
        g2.setFont(new java.awt.Font("Serif", java.awt.Font.BOLD, 48));
        g2.setColor(new java.awt.Color(184, 134, 11));
        g2.drawString(course.getTitle(), width/2 - (course.getTitle().length() * 14), 560);
        
        // --- Footer ---
        g2.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 16));
        g2.setColor(java.awt.Color.GRAY);
        g2.drawString("VERIFIED CERTIFICATE • CREDENTIAL ID: " + (System.currentTimeMillis() / 1000), 100, 720);
        g2.drawString("XP AWARDED: " + course.getXp(), 100, 750);
        
        // Draw Seal if exists
        try {
            java.io.File sealFile = new java.io.File("java/src/main/resources/images/certificate_seal.png");
            if (sealFile.exists()) {
                java.awt.Image sealImg = javax.imageio.ImageIO.read(sealFile);
                g2.drawImage(sealImg, 80, 560, 140, 140, null);
            }
        } catch (Exception e) {}

        g2.setColor(new java.awt.Color(15, 23, 42));
        g2.drawString("_______________________", 850, 720);
        g2.drawString("Academy Director Signature", 850, 750);
        
        g2.dispose();
        javax.imageio.ImageIO.write(img, "png", destFile);
    }

    private Cell createHeaderCell(String text) {
        return new Cell().add(new Paragraph(text))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
    }
}
