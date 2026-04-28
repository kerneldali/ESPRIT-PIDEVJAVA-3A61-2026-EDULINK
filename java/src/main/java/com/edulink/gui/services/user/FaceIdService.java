package com.edulink.gui.services.user;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.io.ByteArrayInputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FaceIdService {
    
    private VideoCapture capture;
    private ScheduledExecutorService timer;
    private CascadeClassifier faceCascade;
    private boolean isCameraActive = false;

    // Load OpenCV native library
    static {
        try {
            OpenCV.loadShared();
        } catch (Exception e) {
            System.err.println("❌ Failed to load OpenCV: " + e.getMessage());
        }
    }

    public FaceIdService() {
        this.faceCascade = new CascadeClassifier();
        String cascadePath = getClass().getResource("/models/haarcascade_frontalface_default.xml").getPath();
        // Fix path issue on Windows (remove leading slash if present)
        if (cascadePath.startsWith("/")) { cascadePath = cascadePath.substring(1); }
        faceCascade.load(cascadePath);
    }

    /**
     * Starts the camera, streams to the ImageView, detects faces, and calls onSuccess when 
     * a steady face is detected for 2 seconds.
     */
    public void startCamera(ImageView cameraFeed, Label statusLabel, Consumer<Boolean> onComplete) {
        if (!isCameraActive) {
            capture = new VideoCapture(0);
            if (capture.isOpened()) {
                isCameraActive = true;
                
                final int[] consecutiveDetections = {0}; // Track steady detection
                
                Runnable frameGrabber = () -> {
                    Mat frame = new Mat();
                    if (capture.read(frame)) {
                        // 1. Detect Face
                        MatOfRect faces = new MatOfRect();
                        Mat grayFrame = new Mat();
                        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
                        Imgproc.equalizeHist(grayFrame, grayFrame);
                        
                        faceCascade.detectMultiScale(grayFrame, faces, 1.1, 4, 0, new Size(100, 100), new Size());
                        Rect[] facesArray = faces.toArray();
                        
                        // 2. Draw Box
                        for (Rect rect : facesArray) {
                            Imgproc.rectangle(frame, rect.tl(), rect.br(), new Scalar(0, 255, 0), 3);
                        }
                        
                        // 3. Logic: If exactly one face is looking, start "authenticating"
                        if (facesArray.length == 1) {
                            consecutiveDetections[0]++;
                            Platform.runLater(() -> statusLabel.setText("Face detected! Hold steady... " + (consecutiveDetections[0] * 10) + "%"));
                        } else {
                            consecutiveDetections[0] = 0;
                            Platform.runLater(() -> statusLabel.setText("Please look at the camera"));
                        }

                        // 4. Update UI
                        Image imageToShow = matToImage(frame);
                        Platform.runLater(() -> cameraFeed.setImage(imageToShow));

                        // 5. Success Condition (~2 seconds of steady detection at 30fps)
                        if (consecutiveDetections[0] > 10) {
                            stopCamera();
                            Platform.runLater(() -> {
                                statusLabel.setText("Face verified!");
                                onComplete.accept(true);
                            });
                        }
                    }
                };

                timer = Executors.newSingleThreadScheduledExecutor();
                timer.scheduleAtFixedRate(frameGrabber, 0, 100, TimeUnit.MILLISECONDS); // 10fps for stability
            } else {
                Platform.runLater(() -> statusLabel.setText("❌ Failed to open webcam"));
            }
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
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }
}
