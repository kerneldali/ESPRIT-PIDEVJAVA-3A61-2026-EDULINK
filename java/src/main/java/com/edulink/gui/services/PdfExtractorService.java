package com.edulink.gui.services;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;

import java.io.File;

public class PdfExtractorService {
    public String extractText(String filePath) {
        StringBuilder text = new StringBuilder();
        try {
            System.out.println("Extracting text from: " + filePath);
            File file = new File(filePath);
            if (!file.exists() && !file.isAbsolute() && !filePath.startsWith("http")) {
                File relFile = new File(System.getProperty("user.dir"), filePath);
                if (relFile.exists()) {
                    file = relFile;
                }
            }

            if (file.exists()) {
                try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(file))) {
                    int pages = pdfDoc.getNumberOfPages();
                    // Read up to 15 pages to keep context size reasonable for the API
                    int maxPages = Math.min(pages, 15);
                    for (int i = 1; i <= maxPages; i++) {
                        text.append(PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i))).append("\n");
                    }
                }
            } else {
                return "File not found.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            text.append("Error reading PDF: ").append(e.getMessage());
        }
        return text.toString();
    }
}
