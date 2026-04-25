package com.edulink.gui.services.journal;

/**
 * Custom Sentiment Analysis Service implemented from scratch using Naive Bayes.
 */
public class SentimentAnalysisService {

    public void train(String text, String label) {
        // Training is now handled by the Python microservice
    }

    public String predict(String text) {
        try {
            java.net.URL url = new java.net.URL("http://localhost:5000/predict");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonInput = "{\"text\": \"" + text.replace("\"", "\\\"") + "\"}";
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(jsonInput.getBytes("utf-8"));
            }

            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream(), "utf-8"))) {
                String response = br.lines().collect(java.util.stream.Collectors.joining());
                if (response.contains("\"positive\""))
                    return "positive";
                if (response.contains("\"negative\""))
                    return "negative";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "neutral";
    }

}
