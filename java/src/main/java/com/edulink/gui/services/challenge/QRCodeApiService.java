package com.edulink.gui.services.challenge;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Generates QR code images by calling an external HTTP API
 * (QR Server — https://goqr.me/api/).
 *
 * Why an API and not the embedded ZXing library:
 *   - Pedagogical requirement: the project must consume at least one external API.
 *   - Decouples QR generation from the binary — same approach a microservice
 *     architecture would take.
 *
 * Resilience:
 *   - 8s connect / 8s response timeout. If the API is slow or down, the caller
 *     can fall back to ZXing instead of blocking the certificate flow.
 *   - HTTP errors (non-2xx) are turned into IllegalStateException so the caller
 *     can detect failure cleanly.
 *
 * Endpoint:
 *   GET https://api.qrserver.com/v1/create-qr-code/?size={size}x{size}&data={url-encoded}
 *   → returns a PNG image (Content-Type: image/png).
 */
public class QRCodeApiService {

    private static final String BASE = "https://api.qrserver.com/v1/create-qr-code/";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    /**
     * Calls the QR Server API and returns the raw PNG bytes.
     *
     * @param content the text/URL/JSON to encode into the QR code
     * @param sizePx  width and height of the QR code in pixels (square)
     * @return the PNG bytes ready to embed in a PDF (iText {@code ImageDataFactory.create})
     * @throws Exception if the network call fails or returns a non-200 status
     */
    public byte[] generate(String content, int sizePx) throws Exception {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("QR content cannot be empty.");
        }
        // QR Server has a soft cap around 4296 chars for alphanumeric;
        // refuse anything obviously too big to keep the QR scannable.
        if (content.length() > 900) {
            throw new IllegalArgumentException(
                    "QR content too large (" + content.length() + " chars). " +
                    "Keep it under 900 to remain scannable on a phone.");
        }

        String encoded = URLEncoder.encode(content, StandardCharsets.UTF_8);
        // Error correction H = highest, recoverable even if the QR is partially
        // damaged when printed — appropriate for a certificate that may be photocopied.
        String url = BASE
                + "?size="   + sizePx + "x" + sizePx
                + "&ecc=H"
                + "&margin=0"
                + "&format=png"
                + "&data="   + encoded;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HttpResponse<byte[]> res = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() != 200) {
            throw new IllegalStateException(
                    "QR API returned HTTP " + res.statusCode() +
                    " (url len=" + url.length() + ")");
        }
        byte[] body = res.body();
        if (body == null || body.length < 100) {
            throw new IllegalStateException("QR API returned an empty / suspicious payload.");
        }
        return body;
    }
}
