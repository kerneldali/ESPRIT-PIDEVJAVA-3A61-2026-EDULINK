<?php

namespace App\Service;

use Psr\Log\LoggerInterface;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class GeminiEventService
{
    private HttpClientInterface $httpClient;
    private string $apiKey;
    private LoggerInterface $logger;

    private const API_URL = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent';

    public function __construct(
        HttpClientInterface $httpClient,
        string $geminiApiKey,
        LoggerInterface $logger
    ) {
        $this->httpClient = $httpClient;
        $this->apiKey = $geminiApiKey;
        $this->logger = $logger;
    }

    public function generateDescription(
        string $title,
        string $dateStart,
        string $dateEnd,
        int $capacity
    ): string {
        if (empty($this->apiKey)) {
            $this->logger->warning('Gemini API key is not configured');
            return $this->fallbackDescription($title, $dateStart, $dateEnd, $capacity);
        }

        try {
            $prompt = <<<PROMPT
You are an expert event copywriter for an educational platform called EduLink.
Write a professional, engaging description (2-3 sentences) for the following event:

- Title: "$title"
- Start: $dateStart
- End: $dateEnd
- Capacity: $capacity participants

Rules:
1. Be concise and compelling
2. Highlight what attendees will gain
3. Use a professional but friendly tone
4. Do NOT include the dates or capacity in the description (they are shown separately)
5. Return ONLY the description text, no quotes, no labels
PROMPT;

            $response = $this->httpClient->request('POST', self::API_URL . '?key=' . $this->apiKey, [
                'headers' => [
                    'Content-Type' => 'application/json',
                ],
                'json' => [
                    'contents' => [
                        [
                            'parts' => [
                                ['text' => $prompt]
                            ]
                        ]
                    ],
                    'generationConfig' => [
                        'temperature' => 0.7,
                        'maxOutputTokens' => 256,
                    ]
                ],
            ]);

            $statusCode = $response->getStatusCode();

            if ($statusCode !== 200) {
                $errorBody = $response->getContent(false);
                $this->logger->error("Gemini API Error ($statusCode): $errorBody");
                return $this->fallbackDescription($title, $dateStart, $dateEnd, $capacity);
            }

            $data = $response->toArray();
            $text = $data['candidates'][0]['content']['parts'][0]['text'] ?? null;

            if (!$text) {
                $this->logger->warning('Gemini returned empty content');
                return $this->fallbackDescription($title, $dateStart, $dateEnd, $capacity);
            }

            return trim($text);

        } catch (\Exception $e) {
            $this->logger->error('Gemini Exception: ' . $e->getMessage());
            return $this->fallbackDescription($title, $dateStart, $dateEnd, $capacity);
        }
    }

    private function fallbackDescription(
        string $title,
        string $dateStart,
        string $dateEnd,
        int $capacity
    ): string {
        return "Join us for \"$title\" — an exciting event designed to bring together "
            . "up to $capacity participants for collaborative learning and networking. "
            . "Don't miss this opportunity to expand your skills and connect with peers!";
    }
}
