package com.edulink.gui.util;

import java.io.File;

/**
 * Centralized helper for resolving resource paths (images, PDFs, etc.)
 * relative to the project structure. Handles the fact that the CWD may be
 * either the project root (ESPRIT-PIDEVJAVA-3A61-2026-EDULINK/) or the
 * java subdirectory (ESPRIT-PIDEVJAVA-3A61-2026-EDULINK/java/).
 *
 * Resources (category images, PDF files) live under:
 *   <project-root>/src/main/resources/images/categories/
 *   <project-root>/src/main/resources/pdfs/
 *
 * The classpath resources (FXML views, CSS, models) live under:
 *   <project-root>/java/src/main/resources/
 */
public class ResourcePathResolver {

    /** The relative prefix from CWD to the resources root (images, pdfs). */
    private static String resourceBase;

    static {
        resourceBase = detectResourceBase();
        System.out.println("[ResourcePathResolver] Resolved resource base: " + new File(System.getProperty("user.dir"), resourceBase).getAbsolutePath());
    }

    /**
     * Detects the correct relative path prefix to the resources directory
     * containing images and PDFs.
     */
    private static String detectResourceBase() {
        String cwd = System.getProperty("user.dir");

        // Case 1: CWD is project root → src/main/resources exists directly
        File base1 = new File(cwd, "src/main/resources");
        if (base1.exists() && base1.isDirectory()) {
            // Check it contains our images/pdfs (not the java classpath resources)
            File imagesDir = new File(base1, "images/categories");
            File pdfsDir = new File(base1, "pdfs");
            if (imagesDir.exists() || pdfsDir.exists()) {
                return "src/main/resources";
            }
        }

        // Case 2: CWD is the java subdirectory → go up one level
        File base2 = new File(cwd, "../src/main/resources");
        if (base2.exists() && base2.isDirectory()) {
            return "../src/main/resources";
        }

        // Fallback: try the project-root style path
        return "src/main/resources";
    }

    /**
     * Returns the absolute File for the resource base directory.
     */
    public static File getResourceBaseDir() {
        return new File(System.getProperty("user.dir"), resourceBase);
    }

    /**
     * Returns the directory where category images are stored.
     */
    public static File getCategoryImagesDir() {
        File dir = new File(getResourceBaseDir(), "images/categories");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /**
     * Returns the directory where PDF resources are stored.
     */
    public static File getPdfsDir() {
        File dir = new File(getResourceBaseDir(), "pdfs");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /**
     * Returns the directory for general images (e.g. certificate_seal.png).
     * This falls back to the java classpath resources if the main one doesn't exist.
     */
    public static File getImagesDir() {
        File dir = new File(getResourceBaseDir(), "images");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /**
     * Resolves a stored relative path (e.g. "src/main/resources/pdfs/test.pdf")
     * to an absolute File, trying multiple known prefixes.
     */
    public static File resolveResourceFile(String storedPath) {
        if (storedPath == null || storedPath.trim().isEmpty()) return null;

        // If it's already absolute and exists, use it
        File absFile = new File(storedPath);
        if (absFile.isAbsolute() && absFile.exists()) return absFile;

        String cwd = System.getProperty("user.dir");

        // Try the path as-is relative to CWD
        File f = new File(cwd, storedPath);
        if (f.exists()) return f;

        // Try stripping known prefixes and re-resolving via our base
        String[] knownPrefixes = {
            "src/main/resources/",
            "java/src/main/resources/",
            "../src/main/resources/"
        };
        for (String prefix : knownPrefixes) {
            if (storedPath.startsWith(prefix)) {
                String relativePart = storedPath.substring(prefix.length());
                File resolved = new File(getResourceBaseDir(), relativePart);
                if (resolved.exists()) return resolved;
            }
        }

        // Try resolving from parent directory (if CWD is java/)
        File parentResolved = new File(cwd, "../" + storedPath);
        if (parentResolved.exists()) return parentResolved;

        // Try the java/ subdirectory path
        File javaResolved = new File(cwd, "java/" + storedPath);
        if (javaResolved.exists()) return javaResolved;

        // Last resort: return the CWD-relative file (may not exist)
        return f;
    }

    /**
     * Converts a resource-relative path to a canonical stored path
     * for database persistence. Always uses forward slashes.
     * E.g. "pdfs/test.pdf" → "src/main/resources/pdfs/test.pdf"
     */
    public static String toStoredPath(String subPath) {
        return (resourceBase + "/" + subPath).replace('\\', '/');
    }

    /**
     * Builds the stored path for a category image filename.
     */
    public static String categoryImageStoredPath(String filename) {
        return toStoredPath("images/categories/" + filename);
    }

    /**
     * Builds the stored path for a PDF filename.
     */
    public static String pdfStoredPath(String filename) {
        return toStoredPath("pdfs/" + filename);
    }
}
