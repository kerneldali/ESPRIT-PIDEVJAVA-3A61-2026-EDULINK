package com.edulink.gui.services.user;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Affine;

import java.util.Random;

public class CaptchaService {
    
    private String expectedAnswer;
    private final Random random = new Random();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    public void generateVisualCaptcha(Canvas canvas) {
        // Generate random 5-character string (excluding confusing chars like O/0, I/1/l)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        expectedAnswer = sb.toString();

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double width = canvas.getWidth();
        double height = canvas.getHeight();

        // 1. Draw Background (Gradient effect via rects)
        gc.setFill(Color.web("#e2e8f0"));
        gc.fillRect(0, 0, width, height);
        for(int i=0; i<height; i+=4) {
            gc.setFill(Color.color(0.9, 0.9, 0.95, 0.4));
            gc.fillRect(0, i, width, 2);
        }

        // 2. Add Heavy Noise (Dots & small boxes)
        for (int i = 0; i < 150; i++) {
            gc.setFill(Color.color(random.nextDouble(), random.nextDouble(), random.nextDouble(), 0.3));
            double x = random.nextDouble() * width;
            double y = random.nextDouble() * height;
            double r = random.nextDouble() * 4 + 1;
            if (random.nextBoolean()) gc.fillOval(x, y, r, r);
            else gc.fillRect(x, y, r, r);
        }

        // 3. Add Distracting Bezier Curves
        gc.setLineWidth(2.5);
        for (int i = 0; i < 4; i++) {
            gc.setStroke(Color.color(random.nextDouble() * 0.6, random.nextDouble() * 0.6, random.nextDouble() * 0.6, 0.5));
            gc.beginPath();
            gc.moveTo(random.nextDouble() * width * 0.2, random.nextDouble() * height);
            gc.bezierCurveTo(
                random.nextDouble() * width, random.nextDouble() * height,
                random.nextDouble() * width, random.nextDouble() * height,
                width - (random.nextDouble() * width * 0.2), random.nextDouble() * height
            );
            gc.stroke();
        }

        // 4. Draw Characters with advanced Affine transforms
        String[] fonts = {"Arial", "Verdana", "Courier New", "Tahoma"};
        double startX = 15;
        for (int i = 0; i < expectedAnswer.length(); i++) {
            char c = expectedAnswer.charAt(i);
            
            gc.setFont(Font.font(fonts[random.nextInt(fonts.length)], FontWeight.EXTRA_BOLD, 28 + random.nextInt(6)));
            
            Color charColor = Color.color(random.nextDouble() * 0.4, random.nextDouble() * 0.4, random.nextDouble() * 0.4);
            gc.setFill(charColor);
            gc.setStroke(charColor.darker());
            gc.setLineWidth(1.0);

            // Save state, apply transform, draw, restore state
            gc.save();
            Affine affine = new Affine();
            
            // Translations and distortions
            double angle = random.nextInt(60) - 30; // -30 to 30 deg
            double shearX = (random.nextDouble() - 0.5) * 0.5; // -0.25 to 0.25
            double scaleY = 0.8 + random.nextDouble() * 0.4; // 0.8 to 1.2
            
            affine.appendTranslation(startX, height / 2 + 10);
            affine.appendRotation(angle);
            affine.appendShear(shearX, 0);
            affine.appendScale(1.0, scaleY);
            
            gc.setTransform(affine);
            
            // Draw at 0,0 because we translated the context
            if (random.nextBoolean()) {
                gc.fillText(String.valueOf(c), 0, 0);
            } else {
                gc.strokeText(String.valueOf(c), 0, 0);
            }
            gc.restore();

            startX += 22 + random.nextInt(8); // Variable spacing
        }
        
        // 5. Foreground interference line
        gc.setStroke(Color.color(0.2, 0.2, 0.2, 0.4));
        gc.setLineWidth(1.5);
        gc.strokeLine(0, random.nextDouble() * height, width, random.nextDouble() * height);
    }

    public boolean verifyCaptcha(String input) {
        if (input == null || input.isBlank() || expectedAnswer == null) return false;
        return input.trim().equalsIgnoreCase(expectedAnswer);
    }
}
