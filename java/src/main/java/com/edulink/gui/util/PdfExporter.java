package com.edulink.gui.util;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.edulink.gui.models.journal.Note;

import java.io.File;

public class PdfExporter {
    public static void exportNote(Note note, File file) throws Exception {
        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("EduLink Journal Note").setFontSize(10).setItalic());
        document.add(new Paragraph(note.getTitle()).setFontSize(24).setBold());
        document.add(new LineSeparator(new SolidLine()));
        document.add(new Paragraph("Date: " + note.getCreatedAt()));
        document.add(new Paragraph("\n" + note.getContent()));

        document.close();
    }
}
