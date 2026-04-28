package com.edulink.gui.services;

import com.edulink.gui.util.GroqConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sound.sampled.*;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.UUID;

public class STTService {
    private static final String GROQ_STT_URL = "https://api.groq.com/openai/v1/audio/transcriptions";
    private AudioFormat format;
    private TargetDataLine line;
    private File currentAudioFile;

    public STTService() {
        // Linear PCM 16kHz, 16 bits, mono, signed, little-endian
        format = new AudioFormat(16000, 16, 1, true, false);
    }

    public void startRecording() throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Line not supported");
        }
        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        currentAudioFile = new File(System.getProperty("java.io.tmpdir"), "edulink_stt_" + UUID.randomUUID() + ".wav");

        Thread recorderThread = new Thread(() -> {
            try (AudioInputStream ais = new AudioInputStream(line)) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, currentAudioFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        recorderThread.start();
    }

    public String stopRecordingAndTranscribe() throws IOException {
        if (line != null) {
            line.stop();
            line.close();
        }

        if (currentAudioFile == null || !currentAudioFile.exists()) {
            return "No audio file recorded.";
        }

        String transcription = transcribeWithGroq(currentAudioFile);
        currentAudioFile.delete(); // Cleanup
        return transcription;
    }

    private String transcribeWithGroq(File audioFile) throws IOException {
        String apiKey = GroqConfig.mahdi_api_key;
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("dummy_key_to_be_replaced")) {
            return "Groq API key not configured.";
        }

        String boundary = "---" + UUID.randomUUID().toString();
        byte[] fileBytes = Files.readAllBytes(audioFile.toPath());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(bos, "UTF-8"), true);

        // Add file part
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(audioFile.getName()).append("\"\r\n");
        writer.append("Content-Type: audio/wav\r\n\r\n");
        writer.flush();
        bos.write(fileBytes);
        writer.append("\r\n");

        // Add model part
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"model\"\r\n\r\n");
        writer.append("whisper-large-v3\r\n");

        writer.append("--").append(boundary).append("--\r\n");
        writer.flush();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_STT_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(bos.toByteArray()))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.body());
                return root.path("text").asText();
            } else {
                return "STT Error (" + response.statusCode() + "): " + response.body();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "STT Interrupted";
        }
    }
}
