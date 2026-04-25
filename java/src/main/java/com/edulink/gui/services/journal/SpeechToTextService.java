package com.edulink.gui.services.journal;

import javafx.concurrent.Task;

/**
 * Service for Speech-to-Text conversion.
 */
public class SpeechToTextService {

    /**
     * Listens to audio and returns the transcribed text using Gemini AI backend.
     */
    public Task<String> listen() {
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                java.net.URL url = new java.net.URL("http://localhost:5001/record");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setReadTimeout(60000);

                int status = conn.getResponseCode();
                java.io.InputStream is = (status >= 400) ? conn.getErrorStream() : conn.getInputStream();

                if (is == null)
                    return "Error: No response from server";

                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(is, "utf-8"))) {
                    String response = br.lines().collect(java.util.stream.Collectors.joining()).trim();

                    // Robust parsing for "text": "..."
                    if (response.contains("\"text\"")) {
                        int quoteStart = response.indexOf("\"", response.indexOf("\"text\"") + 6) + 1;
                        int quoteEnd = response.lastIndexOf("\"");
                        if (quoteStart > 0 && quoteEnd > quoteStart) {
                            return response.substring(quoteStart, quoteEnd);
                        }
                    }

                    if (response.contains("\"error\"")) {
                        int quoteStart = response.indexOf("\"", response.indexOf("\"error\"") + 7) + 1;
                        int quoteEnd = response.lastIndexOf("\"");
                        if (quoteStart > 0 && quoteEnd > quoteStart) {
                            return "AI Error: " + response.substring(quoteStart, quoteEnd);
                        }
                    }

                    return "Error: " + response;
                }
            }
        };
    }
}
