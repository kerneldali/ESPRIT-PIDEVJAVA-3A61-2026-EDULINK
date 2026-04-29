package com.edulink.gui.services.challenge;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Builds AI-generated cover image URLs for challenges using Pollinations.ai.
 *
 * Pollinations is a free, key-less Stable-Diffusion proxy:
 *   GET https://image.pollinations.ai/prompt/{url-encoded-prompt}?...
 *
 * Why a service and not a one-liner inside the controller:
 *   - We craft a consistent prompt style so all generated images share an aesthetic
 *     (educational / motivational / vibrant) rather than looking random.
 *   - We pin width/height/seed so the same challenge keeps producing the same image
 *     across reloads (deterministic — important for caching by JavaFX Image).
 *   - The URL is the value persisted in DB; the bytes are fetched lazily by the UI.
 */
public class ChallengeImageService {

    private static final String BASE = "https://image.pollinations.ai/prompt/";
    private static final int WIDTH  = 768;
    private static final int HEIGHT = 384;

    /**
     * Builds a stable Pollinations URL from the challenge title and difficulty.
     * The resulting URL is a direct PNG; just feed it to {@code new javafx.scene.image.Image(url, true)}.
     */
    public String buildImageUrl(String title, String difficulty) {
        if (title == null || title.isBlank()) {
            title = "learning challenge";
        }
        String safeTitle = title.trim();

        // Difficulty influences the visual mood so HARD challenges feel more intense.
        String mood;
        switch (difficulty == null ? "" : difficulty.toUpperCase()) {
            case "EASY"   -> mood = "soft pastel colors, friendly, welcoming";
            case "HARD"   -> mood = "dramatic lighting, intense, cinematic, deep blue and purple";
            case "MEDIUM" -> mood = "vibrant colors, dynamic, energetic";
            default       -> mood = "vibrant colors, dynamic";
        }

        String prompt =
            "Educational challenge cover illustration: " + safeTitle +
            ". " + mood +
            ". Modern flat illustration, clean composition, no text, " +
            "academic theme, university students, gamified learning, " +
            "high quality digital art, 16:9 aspect ratio.";

        // Seed derived from the title → same title = same image.
        int seed = Math.abs(safeTitle.hashCode() % 100_000);

        String encoded = URLEncoder.encode(prompt, StandardCharsets.UTF_8);
        // Pollinations uses '+' for spaces inside path; both work but %20 is safer.
        encoded = encoded.replace("+", "%20");

        return BASE + encoded
                + "?width="  + WIDTH
                + "&height=" + HEIGHT
                + "&seed="   + seed
                + "&nologo=true"
                + "&model=flux";
    }
}
