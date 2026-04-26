package com.edulink.gui.services.user;

import java.util.Random;

public class CaptchaService {
    
    private int expectedAnswer;
    private final Random random = new Random();

    public String generateMathCaptcha() {
        int a = random.nextInt(10) + 1;
        int b = random.nextInt(10) + 1;
        expectedAnswer = a + b;
        return a + " + " + b + " = ?";
    }

    public boolean verifyCaptcha(String input) {
        if (input == null || input.isBlank()) return false;
        try {
            int answer = Integer.parseInt(input.trim());
            return answer == expectedAnswer;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
