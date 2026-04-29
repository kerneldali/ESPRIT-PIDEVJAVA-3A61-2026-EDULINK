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

        // Edulink Theme Colors (Friend's Premium Dark Theme)
        com.itextpdf.kernel.colors.Color appPurple = new com.itextpdf.kernel.colors.DeviceRgb(124, 58, 237); 
        com.itextpdf.kernel.colors.Color lightPurple = new com.itextpdf.kernel.colors.DeviceRgb(167, 139, 250); 
        com.itextpdf.kernel.colors.Color darkBg = new com.itextpdf.kernel.colors.DeviceRgb(15, 23, 42); 
        
        document.setBackgroundColor(darkBg);
        document.setTextAlignment(TextAlignment.CENTER);
        
        // Premium Triple Border
        document.setBorder(new com.itextpdf.layout.borders.SolidBorder(appPurple, 12));
        document.setMargins(20, 20, 20, 20);

        // Header Branding
        document.add(new Paragraph("🎓 EDULINK ACADEMY")
                .setFontSize(26).setBold().setFontColor(lightPurple).setMarginTop(20));

        // Main Title (Merged fix: Friend's style + My length/size fix)
        document.add(new Paragraph("CERTIFICATE OF COMPLETION")
                .setFontSize(38).setBold().setFontColor(ColorConstants.WHITE).setMarginTop(20));

        document.add(new Paragraph("This is to certify the academic merit achievement of")
                .setFontSize(20).setItalic().setFontColor(ColorConstants.LIGHT_GRAY).setMarginTop(20));

        // Student Name
        document.add(new Paragraph(student.getFullName().toUpperCase())
                .setFontSize(46).setBold().setFontColor(appPurple).setUnderline().setMarginTop(10));

        // Mastering Text
        document.add(new Paragraph("for the successful mastery and completion of the course")
                .setFontSize(20).setFontColor(ColorConstants.LIGHT_GRAY).setMarginTop(25));

        // Course Title
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
            
        // UID & XP (Friend's new feature)
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
        g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Background - Dark (Friend's choice)
        g2.setColor(new java.awt.Color(15, 23, 42));
        g2.fillRect(0, 0, width, height);
        
        // Premium Border
        g2.setColor(new java.awt.Color(124, 58, 237)); // appPurple
        g2.setStroke(new java.awt.BasicStroke(20));
        g2.drawRect(10, 10, width - 20, height - 20);
        
        // Helper for centering text
        java.awt.FontMetrics fm;
        
        // Header
        g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 36));
        g2.setColor(new java.awt.Color(167, 139, 250)); // lightPurple
        String header = "🎓 EDULINK ACADEMY";
        fm = g2.getFontMetrics();
        g2.drawString(header, (width - fm.stringWidth(header)) / 2, 100);
        
        // Main Title (My fix for length and centering)
        g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 64));
        g2.setColor(java.awt.Color.WHITE);
        String title = "CERTIFICATE OF COMPLETION";
        fm = g2.getFontMetrics();
        g2.drawString(title, (width - fm.stringWidth(title)) / 2, 210);
        
        // Certification Text
        g2.setFont(new java.awt.Font("Arial", java.awt.Font.ITALIC, 24));
        g2.setColor(java.awt.Color.LIGHT_GRAY);
        String certify = "This is to certify the academic merit achievement of";
        fm = g2.getFontMetrics();
        g2.drawString(certify, (width - fm.stringWidth(certify)) / 2, 290);
        
        // Student Name
        g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 56));
        g2.setColor(new java.awt.Color(124, 58, 237));
        String name = student.getFullName().toUpperCase();
        fm = g2.getFontMetrics();
        g2.drawString(name, (width - fm.stringWidth(name)) / 2, 380);
        
        // Mastering Text
        g2.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 24));
        g2.setColor(java.awt.Color.LIGHT_GRAY);
        String mastering = "for the successful mastery and completion of the course";
        fm = g2.getFontMetrics();
        g2.drawString(mastering, (width - fm.stringWidth(mastering)) / 2, 460);
        
        // Course Title
        g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 48));
        g2.setColor(new java.awt.Color(124, 58, 237));
        String courseTitle = course.getTitle();
        fm = g2.getFontMetrics();
        g2.drawString(courseTitle, (width - fm.stringWidth(courseTitle)) / 2, 540);
        
        // Footer Details (Friend's feature)
        g2.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 18));
        g2.setColor(java.awt.Color.GRAY);
        g2.drawString("VERIFIED CERTIFICATE • CREDENTIAL ID: EL-" + System.currentTimeMillis() / 1000, 100, 700);
        g2.drawString("XP AWARDED: " + course.getXp(), 100, 730);
        
        // Signature
        g2.setFont(new java.awt.Font("Arial", java.awt.Font.ITALIC, 22));
        g2.setColor(java.awt.Color.WHITE);
        g2.drawString("_______________________", 800, 700);
        g2.drawString("Academy Dean Signature", 800, 730);
        
        // Official Seal Decoration (Friend's work)
        g2.setColor(new java.awt.Color(124, 58, 237));
        g2.fillOval(width/2 - 50, 650, 100, 100);
        g2.setColor(java.awt.Color.WHITE);
        g2.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));
        g2.drawString("CERTIFIED", width/2 - 35, 695);
        g2.drawString("VERIFIED", width/2 - 32, 715);
        
        g2.dispose();
        javax.imageio.ImageIO.write(img, "png", destFile);
    }

    private Cell createHeaderCell(String text) {
        return new Cell().add(new Paragraph(text).setBold())
                         .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                         .setTextAlignment(TextAlignment.CENTER);
    }
}
