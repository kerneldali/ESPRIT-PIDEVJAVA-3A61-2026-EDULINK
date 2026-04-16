package com.edulink.gui.services;

import com.edulink.gui.models.assistance.HelpRequest;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

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

        // Edulink Theme Colors
        com.itextpdf.kernel.colors.Color appPurple = new com.itextpdf.kernel.colors.DeviceRgb(124, 58, 237); 
        com.itextpdf.kernel.colors.Color lightPurple = new com.itextpdf.kernel.colors.DeviceRgb(167, 139, 250); // A78BFA
        com.itextpdf.kernel.colors.Color darkBg = new com.itextpdf.kernel.colors.DeviceRgb(15, 23, 42); 
        
        document.setBackgroundColor(darkBg);
        document.setTextAlignment(TextAlignment.CENTER);
        
        // Premium Triple Border (Mixed colors)
        document.setBorder(new com.itextpdf.layout.borders.SolidBorder(appPurple, 12));
        document.setMargins(20, 20, 20, 20);

        // Header Branding
        document.add(new Paragraph("🎓 EDULINK ACADEMY")
                .setFontSize(26).setBold().setFontColor(lightPurple).setMarginTop(20));

        // Main Title
        document.add(new Paragraph("CERTIFICATE OF COMPLETION")
                .setFontSize(50).setBold().setFontColor(ColorConstants.WHITE).setMarginTop(10));

        document.add(new Paragraph("This is to certify the academic merit achievement of")
                .setFontSize(20).setItalic().setFontColor(ColorConstants.LIGHT_GRAY).setMarginTop(20));

        // Student Name
        document.add(new Paragraph(student.getFullName().toUpperCase())
                .setFontSize(46).setBold().setFontColor(appPurple).setUnderline().setMarginTop(10));

        // Mastering Text
        document.add(new Paragraph("for the successful mastery and completion of the course")
                .setFontSize(20).setFontColor(ColorConstants.LIGHT_GRAY).setMarginTop(25));

        // Course Title - PRESTIGIOUS PURPLE
        document.add(new Paragraph(course.getTitle())
                .setFontSize(38).setBold().setFontColor(appPurple).setMarginTop(5));

        // Signature Section
        Table footerTable = new Table(3);
        footerTable.setWidth(UnitValue.createPercentValue(100));
        footerTable.setMarginTop(50);
        
        // Seal
        footerTable.addCell(new Cell().add(new Paragraph("CERTIFIED\nVERIFIED")
            .setFontSize(10).setBold().setFontColor(darkBg).setBackgroundColor(appPurple).setPadding(10))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.BOTTOM));
            
        // UID
        footerTable.addCell(new Cell().add(new Paragraph("Credential ID: EL-" + System.currentTimeMillis() / 1000 + "\nXP AWARDED: " + course.getXp())
            .setFontSize(12).setFontColor(ColorConstants.GRAY))
            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.BOTTOM));
            
        // Signature
        footerTable.addCell(new Cell().add(new Paragraph("__________________________\nAcademy Dean Signature")
            .setFontSize(14).setItalic().setFontColor(ColorConstants.WHITE))
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
        
        // Background
        g2.setColor(new java.awt.Color(15, 23, 42));
        g2.fillRect(0, 0, width, height);
        
        // Border
        g2.setColor(new java.awt.Color(124, 58, 237));
        g2.setStroke(new java.awt.BasicStroke(20));
        g2.drawRect(10, 10, width - 20, height - 20);
        
        // Content
        g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 40));
        g2.setColor(new java.awt.Color(167, 139, 250));
        g2.drawString("🎓 EDULINK ACADEMY", width/2 - 200, 100);
        
        g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 60));
        g2.setColor(java.awt.Color.WHITE);
        g2.drawString("CERTIFICATE", width/2 - 200, 200);
        
        g2.setFont(new java.awt.Font("Arial", java.awt.Font.ITALIC, 24));
        g2.setColor(java.awt.Color.LIGHT_GRAY);
        g2.drawString("This is to certify the academic merit achievement of", width/2 - 250, 280);
        
        g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 54));
        g2.setColor(new java.awt.Color(124, 58, 237));
        g2.drawString(student.getFullName().toUpperCase(), width/2 - 250, 360);
        
        g2.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 24));
        g2.setColor(java.awt.Color.LIGHT_GRAY);
        g2.drawString("for mastering the course", width/2 - 120, 430);
        
        g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 48));
        g2.setColor(new java.awt.Color(124, 58, 237));
        g2.drawString(course.getTitle(), width/2 - 200, 500);
        
        g2.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 18));
        g2.setColor(java.awt.Color.GRAY);
        g2.drawString("VERIFIED CERTIFICATE • CREDENTIAL ID: " + System.currentTimeMillis() / 1000, 100, 700);
        g2.drawString("XP AWARDED: " + course.getXp(), 100, 730);
        
        g2.drawString("_______________________", 800, 700);
        g2.drawString("Academy Dean Signature", 800, 730);
        
        g2.dispose();
        javax.imageio.ImageIO.write(img, "png", destFile);
    }

    private Cell createHeaderCell(String text) {
        return new Cell().add(new Paragraph(text).setBold())
                         .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                         .setTextAlignment(TextAlignment.CENTER);
    }
}
