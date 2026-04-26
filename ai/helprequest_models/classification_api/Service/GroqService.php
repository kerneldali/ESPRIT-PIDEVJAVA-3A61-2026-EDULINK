<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Psr\Log\LoggerInterface;

class GroqService
{
    private string $apiKey;
    private HttpClientInterface $httpClient;
    private LoggerInterface $logger;
    private const API_URL = 'https://api.groq.com/openai/v1/chat/completions';
    private const MODEL = 'llama-3.3-70b-versatile';

    public function __construct(string $groqApiKey, HttpClientInterface $httpClient, LoggerInterface $logger)
    {
        $this->apiKey = $groqApiKey;
        $this->httpClient = $httpClient;
        $this->logger = $logger;
    }

    public function generateContent(string $prompt): string
    {
        if (empty($this->apiKey)) {
            return "Error: GROQ_API_KEY is not configured in your .env file.";
        }

        try {
            $response = $this->httpClient->request('POST', self::API_URL, [
                'headers' => [
                    'Authorization' => 'Bearer ' . $this->apiKey,
                    'Content-Type' => 'application/json',
                ],
                'json' => [
                    'model' => self::MODEL,
                    'messages' => [
                        [
                            'role' => 'system',
                            'content' => 'You are a helpful educational assistant.'
                        ],
                        [
                            'role' => 'user',
                            'content' => $prompt
                        ]
                    ],
                    'temperature' => 0.7,
                    'max_tokens' => 2048,
                ]
            ]);

            $statusCode = $response->getStatusCode();

            if ($statusCode !== 200) {
                $content = $response->getContent(false);
                $this->logger->error("Groq API Error ($statusCode): " . $content);

                if ($statusCode === 429) {
                    return "Error: Groq Quota Exceeded (429). Please wait a moment.";
                }

                return "Error: AI Service (Groq) Failed ($statusCode). Please try again.";
            }

            $data = $response->toArray();
            return $data['choices'][0]['message']['content'] ?? 'No content generated.';

        } catch (\Exception $e) {
            $this->logger->error('Groq Exception: ' . $e->getMessage());
            return "Error: " . $e->getMessage();
        }
    }

    public function generateQuiz(string $text): string
    {
        $prompt = "Generate exactly 5 multiple-choice questions based on the following text. " .
            "You MUST return ONLY a valid JSON array, with NO markdown, NO explanation, NO extra text. " .
            "Each object must have exactly these keys: \"question\" (string), \"options\" (array of 4 strings), \"answer\" (integer index 0-3 of the correct option). " .
            "Example format: [{\"question\":\"What is X?\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"answer\":0}] " .
            "Text:\n\n" . substr($text, 0, 20000);

        return $this->generateContent($prompt);
    }

    public function generateSummary(string $text): string
    {
        $prompt = "Summarize the following text in 3-5 bullet points. Text: \n\n" . substr($text, 0, 30000);
        return $this->generateContent($prompt);
    }
}
