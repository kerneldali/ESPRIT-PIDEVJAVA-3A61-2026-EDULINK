<?php

namespace App\Service;

class CategoryImageService
{
    /**
     * Generates a unique, abstract geometric cover using the DiceBear Shapes API.
     * Based on the category name to ensure consistency.
     */
    public function generateDiceBearUrl(string $matiereName): string
    {
        // DiceBear Shapes API - Secure, high performance, and requires no API key.
        return "https://api.dicebear.com/9.x/shapes/svg?seed=" . urlencode($matiereName);
    }

    /**
     * Legacy method name for compatibility, now routes to DiceBear.
     */
    public function generateAiImageUrl(string $matiereName): string
    {
        return $this->generateDiceBearUrl($matiereName);
    }

    /**
     * Fallback placeholder if something goes wrong.
     */
    public function getPlaceholderUrl(string $matiereName): string
    {
        return "https://images.unsplash.com/photo-1503676260728-1c00da094a0b?q=80&w=800&auto=format&fit=crop";
    }
}
