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

    private Cell createHeaderCell(String text) {
        return new Cell().add(new Paragraph(text).setBold())
                         .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                         .setTextAlignment(TextAlignment.CENTER);
    }
}
