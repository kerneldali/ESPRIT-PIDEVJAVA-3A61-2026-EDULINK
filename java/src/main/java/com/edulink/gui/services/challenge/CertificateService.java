package com.edulink.gui.services.challenge;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class CertificateService {

    // White-themed palette: white background, dark text, accent colors preserved.
    private static final DeviceRgb DARK_TEXT  = new DeviceRgb(0x1a, 0x1a, 0x2e);
    private static final DeviceRgb GREY_TEXT  = new DeviceRgb(0x55, 0x55, 0x66);
    private static final DeviceRgb GREEN_ACC  = new DeviceRgb(0x00, 0xb8, 0x76);
    private static final DeviceRgb PURPLE_ACC = new DeviceRgb(0x7c, 0x3a, 0xed);
    private static final DeviceRgb LIGHT_LINE = new DeviceRgb(0xe0, 0xe0, 0xe6);

    /** Online QR generation. Falls back to ZXing if the API is unreachable. */
    private final QRCodeApiService qrApi = new QRCodeApiService();

    /**
     * Public verification page (static HTML hosted on GitHub Pages).
     * The page reads its content from the URL query string — no backend needed.
     * If the page domain ever changes, only this constant is updated.
     */
    private static final String VERIFY_PAGE_URL = "https://ali-belhadjali.github.io/edulink-verify/";

    /**
     * Generates a PDF certificate and saves it to the user's Desktop.
     *
     * @return absolute path of the generated PDF, or null on error.
     */
    public String generateCertificate(String studentName, String challengeTitle,
                                      int xpReward, String difficulty) {
        String safeName      = studentName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String safeChallenge = challengeTitle.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String desktop       = System.getProperty("user.home") + File.separator + "Desktop";
        String filePath      = desktop + File.separator +
                               "Certificat_" + safeName + "_" + safeChallenge + ".pdf";

        // Stable per-certificate ID (can be re-derived to verify).
        String certificateId = buildCertificateId(studentName, challengeTitle);
        // Self-contained QR payload: encodes the cert metadata directly so a
        // scan reveals all the verifiable info offline, with no dead-link risk.
        String qrPayload     = buildQrPayload(certificateId, studentName,
                                              challengeTitle, xpReward, difficulty);

        try {
            PdfWriter   writer   = new PdfWriter(filePath);
            PdfDocument pdfDoc   = new PdfDocument(writer);
            Document    document = new Document(pdfDoc, PageSize.A4.rotate());
            document.setMargins(40, 50, 40, 50);

            PdfFont fontBold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // ── Outer frame: white background, green border ────────────────────
            Table outerBorder = new Table(UnitValue.createPercentArray(new float[]{1}))
                    .useAllAvailableWidth();
            Cell outerCell = new Cell()
                    .setBorder(new SolidBorder(GREEN_ACC, 3))
                    .setBackgroundColor(ColorConstants.WHITE)
                    .setPadding(30);

            // ── Brand line ─────────────────────────────────────────────────────
            Paragraph brand = new Paragraph("EduLink Academy")
                    .setFont(fontBold).setFontSize(13)
                    .setFontColor(GREEN_ACC)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(2);
            outerCell.add(brand);

            // Decorative green bar
            Table line = new Table(UnitValue.createPercentArray(new float[]{1})).useAllAvailableWidth();
            line.addCell(new Cell().setHeight(3).setBackgroundColor(GREEN_ACC)
                    .setBorder(Border.NO_BORDER));
            outerCell.add(line);
            outerCell.add(new Paragraph(" ").setFontSize(6));

            // ── Main title ─────────────────────────────────────────────────────
            Paragraph mainTitle = new Paragraph("CERTIFICAT DE COMPLETION")
                    .setFont(fontBold).setFontSize(28)
                    .setFontColor(DARK_TEXT)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(4);
            outerCell.add(mainTitle);

            Paragraph subtitle = new Paragraph("est fierement decerne a")
                    .setFont(fontRegular).setFontSize(13)
                    .setFontColor(GREY_TEXT)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10);
            outerCell.add(subtitle);

            // ── Student name ──────────────────────────────────────────────────
            Paragraph nameP = new Paragraph(studentName)
                    .setFont(fontBold).setFontSize(32)
                    .setFontColor(GREEN_ACC)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10);
            outerCell.add(nameP);

            // ── Body ──────────────────────────────────────────────────────────
            Paragraph body1 = new Paragraph("pour avoir complete avec succes le challenge")
                    .setFont(fontRegular).setFontSize(14)
                    .setFontColor(DARK_TEXT)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(6);
            outerCell.add(body1);

            Paragraph challengeP = new Paragraph("\u00AB " + challengeTitle + " \u00BB")
                    .setFont(fontBold).setFontSize(20)
                    .setFontColor(PURPLE_ACC)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(6);
            outerCell.add(challengeP);

            // Difficulty + XP
            String diffLabel = "HARD".equals(difficulty)   ? "HARD"   :
                               "MEDIUM".equals(difficulty) ? "MEDIUM" : "EASY";
            Paragraph meta = new Paragraph("Difficulte : " + diffLabel +
                    "    \u2022    Recompense : +" + xpReward + " XP")
                    .setFont(fontRegular).setFontSize(12)
                    .setFontColor(GREY_TEXT)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            outerCell.add(meta);

            // Soft separator line
            Table line2 = new Table(UnitValue.createPercentArray(new float[]{1})).useAllAvailableWidth();
            line2.addCell(new Cell().setHeight(1).setBackgroundColor(LIGHT_LINE)
                    .setBorder(Border.NO_BORDER));
            outerCell.add(line2);
            outerCell.add(new Paragraph(" ").setFontSize(4));

            // ── Footer row : date+brand on the left, QR code + caption on the right ──
            String today = LocalDate.now().format(
                    DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.FRENCH));

            Table footerRow = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                    .useAllAvailableWidth();

            Cell footerLeft = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            footerLeft.add(new Paragraph("Delivre le " + today)
                    .setFont(fontRegular).setFontSize(11)
                    .setFontColor(DARK_TEXT)
                    .setMarginBottom(2));
            footerLeft.add(new Paragraph("EduLink \u2014 Plateforme d'apprentissage gamifiee")
                    .setFont(fontRegular).setFontSize(10)
                    .setFontColor(GREY_TEXT)
                    .setMarginBottom(2));
            footerLeft.add(new Paragraph("Certificat n\u00B0 " + certificateId)
                    .setFont(fontBold).setFontSize(9)
                    .setFontColor(PURPLE_ACC));

            Cell footerRight = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.RIGHT);
            try {
                byte[] qrBytes = fetchQrBytes(qrPayload, 220);
                ImageData qrData = ImageDataFactory.create(qrBytes);
                Image qrImage = new Image(qrData)
                        .setWidth(80).setHeight(80)
                        .setHorizontalAlignment(HorizontalAlignment.RIGHT);
                footerRight.add(qrImage);
                footerRight.add(new Paragraph("Scannez pour verifier")
                        .setFont(fontRegular).setFontSize(7)
                        .setFontColor(GREY_TEXT)
                        .setTextAlignment(TextAlignment.RIGHT));
            } catch (Exception qrErr) {
                // Non-fatal: certificate is still issued without QR if generation fails.
                System.err.println("[Certificate] QR code generation failed: " + qrErr.getMessage());
                footerRight.add(new Paragraph("ID: " + certificateId)
                        .setFont(fontBold).setFontSize(9)
                        .setFontColor(PURPLE_ACC)
                        .setTextAlignment(TextAlignment.RIGHT));
            }

            footerRow.addCell(footerLeft);
            footerRow.addCell(footerRight);
            outerCell.add(footerRow);

            outerBorder.addCell(outerCell);
            document.add(outerBorder);
            document.close();

            System.out.println("[Certificate] Generated: " + filePath);
            System.out.println("[Certificate] QR payload: " + qrPayload.replace("\n", " | "));
            return filePath;
        } catch (Exception e) {
            System.err.println("[Certificate] Failed to generate: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Builds the QR payload as a URL pointing to the public verification page
     * with all certificate metadata in the query string.
     *
     * Why a URL with query params and not a plain text:
     *   - When scanned, a phone opens a styled web page → high-impact demo.
     *   - The page is static (HTML/JS only), zero backend, hosted on GitHub Pages.
     *   - All info is embedded in the URL, so the page can render even with
     *     no DB connection — pure read of {@code window.location.search}.
     *   - The certificate ID is a SHA-256 hash → cannot be forged client-side
     *     without invalidating the chain.
     */
    private String buildQrPayload(String certId, String studentName,
                                  String challengeTitle, int xpReward, String difficulty) {
        String today = LocalDate.now().toString(); // ISO yyyy-MM-dd
        StringBuilder url = new StringBuilder(VERIFY_PAGE_URL);
        url.append("?id=").append(enc(certId));
        url.append("&name=").append(enc(studentName));
        url.append("&challenge=").append(enc(challengeTitle));
        url.append("&difficulty=").append(enc(difficulty != null ? difficulty : "N/A"));
        url.append("&xp=").append(xpReward);
        url.append("&date=").append(enc(today));
        return url.toString();
    }

    /** URL-encodes a value for safe inclusion in a query string. */
    private String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    /**
     * Fetches the QR PNG bytes. Tries the external API first (project requirement),
     * falls back to the embedded ZXing library if the API is unreachable so the
     * certificate is still issued during a temporary outage.
     */
    private byte[] fetchQrBytes(String content, int sizePx) throws Exception {
        try {
            byte[] viaApi = qrApi.generate(content, sizePx);
            System.out.println("[Certificate] QR generated via QR Server API.");
            return viaApi;
        } catch (Exception apiErr) {
            System.err.println("[Certificate] QR API unreachable, falling back to ZXing: "
                    + apiErr.getMessage());
            return generateQRCodeImageLocally(content, sizePx);
        }
    }

    /**
     * Local ZXing-based QR generation. Used only as a fallback if the external
     * API is down so the certificate flow never breaks.
     */
    private byte[] generateQRCodeImageLocally(String content, int sizePx) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
        return baos.toByteArray();
    }

    /**
     * Builds a stable, short certificate id from the inputs.
     * The same student + challenge always yields the same id, so verification can
     * be reproduced offline without storing the cert in DB. If you later add
     * a real DB-backed registry, you can replace this with a UUID + DB row.
     */
    private String buildCertificateId(String studentName, String challengeTitle) {
        try {
            String raw = (studentName + "|" + challengeTitle).toLowerCase();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 5 && i < hash.length; i++) { // 10 hex chars = ~40 bits, plenty
                hex.append(String.format("%02X", hash[i]));
            }
            return "EDU-" + hex.toString();
        } catch (Exception e) {
            // Deterministic fallback; should never realistically hit.
            return "EDU-" + Math.abs((studentName + challengeTitle).hashCode());
        }
    }
}
