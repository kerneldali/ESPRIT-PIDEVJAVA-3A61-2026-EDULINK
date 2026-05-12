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
import java.time.Duration;
import java.util.UUID;

public class STTService {
    private static final String GROQ_STT_URL = "https://api.groq.com/openai/v1/audio/transcriptions";
    private AudioFormat format;
    private TargetDataLine line;
    private File currentAudioFile;
    private Thread recorderThread;

    public STTService() {
        format = new AudioFormat(16000, 16, 1, true, false);
    }

    public void startRecording() throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Microphone not supported");
        }
        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        currentAudioFile = new File(System.getProperty("java.io.tmpdir"), "edulink_stt_" + UUID.randomUUID() + ".wav");

        // blocks until line.close() is called, which lets AudioSystem finalize the WAV
        // header cleanly
        recorderThread = new Thread(() -> {
            try (AudioInputStream ais = new AudioInputStream(line)) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, currentAudioFile);
                System.out.println("STT: WAV written — file size: " + currentAudioFile.length() + " bytes");
            } catch (IOException e) {
                System.err.println("STT recorder thread error: " + e.getMessage());
            }
        });
        recorderThread.setDaemon(false); // let this thread finish even if the window closes
        recorderThread.start();
    }

    public String stopRecordingAndTranscribe() throws IOException {
        if (line != null) {
            line.stop();
            line.close();
            line = null;
        }

        // join the thread so we don't check the file size before it's done writing
        if (recorderThread != null) {
            try {
                recorderThread.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            recorderThread = null;
        }

        if (currentAudioFile == null || !currentAudioFile.exists() || currentAudioFile.length() < 500) {
            return "Recording was too short or microphone is not capturing audio. Please try again.";
        }

        System.out.println("STT: sending " + currentAudioFile.length() + " bytes to Groq Whisper API");
        return transcribeWithGroq(currentAudioFile);
    }

    private String transcribeWithGroq(File audioFile) throws IOException {
        String boundary = "----EduLinkBoundary" + UUID.randomUUID().toString();
        byte[] fileBytes = Files.readAllBytes(audioFile.toPath());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        // -- file part --
        bos.write(("--" + boundary + "\r\n").getBytes());
        bos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + audioFile.getName() + "\"\r\n").getBytes());
        bos.write(("Content-Type: audio/wav\r\n\r\n").getBytes());
        bos.write(fileBytes);
        bos.write(("\r\n").getBytes());

        // -- model part --
        bos.write(("--" + boundary + "\r\n").getBytes());
        bos.write(("Content-Disposition: form-data; name=\"model\"\r\n\r\n").getBytes());
        bos.write(("whisper-large-v3-turbo\r\n").getBytes());

        // -- language part (optional, helps accuracy) --
        bos.write(("--" + boundary + "\r\n").getBytes());
        bos.write(("Content-Disposition: form-data; name=\"language\"\r\n\r\n").getBytes());
        bos.write(("en\r\n").getBytes());

        // -- closing boundary --
        bos.write(("--" + boundary + "--\r\n").getBytes());

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_STT_URL))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Authorization", "Bearer " + GroqConfig.mahdi_api_key)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofByteArray(bos.toByteArray()))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("🎙 Groq STT response status: " + response.statusCode());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.body());
                String text = root.path("text").asText().trim();
                System.out.println("🎙 Transcribed text: " + text);
                return text.isEmpty() ? "Could not understand the audio. Please try again." : text;
            } else {
                System.err.println("❌ Groq STT error: " + response.body());
                return "Speech-to-text error (" + response.statusCode() + "): " + response.body();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Speech-to-text was interrupted.";
        } finally {
            if (audioFile.exists())
                audioFile.delete();
        }
    }
}
