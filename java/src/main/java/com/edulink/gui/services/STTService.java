package com.edulink.gui.services;

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
    private static final String PYTHON_STT_URL = "http://localhost:5003/transcribe";
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

        System.out.println("STT: sending " + currentAudioFile.length() + " bytes to transcription service");
        return transcribeWithPython(currentAudioFile);
    }

    private String transcribeWithPython(File audioFile) throws IOException {
        String boundary = "---" + UUID.randomUUID().toString();
        byte[] fileBytes = Files.readAllBytes(audioFile.toPath());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(bos, "UTF-8"), true);

        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(audioFile.getName())
                .append("\"\r\n");
        writer.append("Content-Type: audio/wav\r\n\r\n");
        writer.flush();
        bos.write(fileBytes);
        writer.append("\r\n");
        writer.append("--").append(boundary).append("--\r\n");
        writer.flush();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PYTHON_STT_URL))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(bos.toByteArray()))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.body());
                return root.path("text").asText().trim();
            } else {
                return "STT Error (" + response.statusCode() + "): " + response.body();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "STT Interrupted";
        } finally {
            if (audioFile.exists())
                audioFile.delete();
        }
    }
}
