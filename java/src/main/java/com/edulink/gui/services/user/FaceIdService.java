package com.edulink.gui.services.user;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FaceIdService {
    
    private VideoCapture capture;
    private ScheduledExecutorService timer;
    private CascadeClassifier faceCascade;
    private boolean isCameraActive = false;
    private static boolean opencvLoaded = false;

    // Load OpenCV native library
    static {
        try {
            nu.pattern.OpenCV.loadShared();
            opencvLoaded = true;
            System.out.println("✅ OpenCV loaded successfully");
        } catch (Throwable e) {
            System.err.println("❌ Failed to load OpenCV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public FaceIdService() {
        if (!opencvLoaded) {
            System.err.println("❌ FaceIdService: OpenCV not loaded, face ID will not work");
            return;
        }

        this.faceCascade = new CascadeClassifier();
        try {
            // Extract the cascade XML from resources to a temp file
            // This avoids path issues with spaces/special characters in the project path
            URL resourceUrl = getClass().getResource("/models/haarcascade_frontalface_default.xml");
            if (resourceUrl == null) {
                System.err.println("❌ Haar cascade file not found in resources!");
                return;
            }

            File tempCascade = File.createTempFile("haarcascade_frontalface", ".xml");
            tempCascade.deleteOnExit();

            try (InputStream is = getClass().getResourceAsStream("/models/haarcascade_frontalface_default.xml");
                 OutputStream os = new FileOutputStream(tempCascade)) {
                if (is == null) {
                    System.err.println("❌ Could not open cascade resource stream!");
                    return;
                }
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

            boolean loaded = faceCascade.load(tempCascade.getAbsolutePath());
            if (loaded) {
                System.out.println("✅ Haar cascade classifier loaded successfully");
            } else {
                System.err.println("❌ Failed to load Haar cascade classifier from: " + tempCascade.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("❌ Error initializing FaceIdService: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Starts the camera, streams to the ImageView, detects faces, and calls onSuccess when 
     * a steady face is detected for 2 seconds.
     */
    public void startCamera(ImageView cameraFeed, Label statusLabel, Consumer<Boolean> onComplete) {
        if (!opencvLoaded) {
            Platform.runLater(() -> statusLabel.setText("❌ OpenCV not available — cannot use Face ID"));
            return;
        }

        if (faceCascade == null || faceCascade.empty()) {
            Platform.runLater(() -> statusLabel.setText("❌ Face detection model not loaded"));
            return;
        }

        if (!isCameraActive) {
            Platform.runLater(() -> statusLabel.setText("Opening camera..."));

            new Thread(() -> {
                try {
                    capture = new VideoCapture(0);

                    // Give camera time to initialize
                    Thread.sleep(500);

                    if (!capture.isOpened()) {
                        // Try index 1 (some systems have multiple cameras)
                        capture = new VideoCapture(1);
                        Thread.sleep(500);
                    }

                    if (capture.isOpened()) {
                        isCameraActive = true;
                        System.out.println("✅ Camera opened successfully");

                        final int[] consecutiveDetections = {0};

                        Runnable frameGrabber = () -> {
                            try {
                                Mat frame = new Mat();
                                if (capture != null && capture.isOpened() && capture.read(frame) && !frame.empty()) {
                                    // 1. Detect Face
                                    MatOfRect faces = new MatOfRect();
                                    Mat grayFrame = new Mat();
                                    Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
                                    Imgproc.equalizeHist(grayFrame, grayFrame);

                                    faceCascade.detectMultiScale(grayFrame, faces, 1.1, 4, 0,
                                            new Size(100, 100), new Size());
                                    Rect[] facesArray = faces.toArray();

                                    // 2. Draw Box
                                    for (Rect rect : facesArray) {
                                        Imgproc.rectangle(frame, rect.tl(), rect.br(),
                                                new Scalar(0, 255, 0), 3);
                                    }

                                    // 3. Logic: If exactly one face is detected
                                    if (facesArray.length == 1) {
                                        consecutiveDetections[0]++;
                                        int progress = Math.min(consecutiveDetections[0] * 10, 100);
                                        Platform.runLater(() -> statusLabel.setText(
                                                "✅ Face detected! Hold steady... " + progress + "%"));
                                    } else {
                                        consecutiveDetections[0] = 0;
                                        Platform.runLater(() -> statusLabel.setText(
                                                "👁 Please look at the camera"));
                                    }

                                    // 4. Update UI
                                    Image imageToShow = matToImage(frame);
                                    Platform.runLater(() -> cameraFeed.setImage(imageToShow));

                                    // 5. Success after ~2 seconds of steady detection
                                    if (consecutiveDetections[0] > 10) {
                                        stopCamera();
                                        Platform.runLater(() -> {
                                            statusLabel.setText("✅ Face verified successfully!");
                                            onComplete.accept(true);
                                        });
                                    }

                                    // Clean up native mats
                                    frame.release();
                                    grayFrame.release();
                                    faces.release();
                                }
                            } catch (Exception e) {
                                System.err.println("Frame grab error: " + e.getMessage());
                            }
                        };

                        timer = Executors.newSingleThreadScheduledExecutor();
                        timer.scheduleAtFixedRate(frameGrabber, 0, 100, TimeUnit.MILLISECONDS);
                    } else {
                        System.err.println("❌ Could not open any camera");
                        Platform.runLater(() -> statusLabel.setText(
                                "❌ Could not open webcam. Make sure no other app is using it."));
                    }
                } catch (Exception e) {
                    System.err.println("❌ Camera init error: " + e.getMessage());
                    e.printStackTrace();
                    Platform.runLater(() -> statusLabel.setText("❌ Camera error: " + e.getMessage()));
                }
            }).start();
        }
    }

    public void stopCamera() {
        if (isCameraActive) {
            isCameraActive = false;
            try {
                if (timer != null && !timer.isShutdown()) {
                    timer.shutdown();
                    timer.awaitTermination(500, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (capture != null && capture.isOpened()) {
                capture.release();
            }
        }
    }

    private Image matToImage(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        Image img = new Image(new ByteArrayInputStream(buffer.toArray()));
        buffer.release();
        return img;
    }
}
