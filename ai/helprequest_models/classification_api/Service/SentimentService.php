<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Contracts\HttpClient\Exception\TransportExceptionInterface;

/**
 * Calls the Python Flask sentiment analysis microservice
 * and returns the analysis result.
 */
class SentimentService
{
    private function getApiUrl(): string
    {
        $host = $_ENV['AI_SERVICES_HOST'] ?? getenv('AI_SERVICES_HOST') ?: 'localhost';
        return 'http://' . $host . ':5001/analyze';
    }
    private const TIMEOUT = 5.0;

    public function __construct(private readonly HttpClientInterface $httpClient)
    {
    }

    /**
     * Analyze the sentiment of a note's text content.
     *
     * @return array{sentiment: string, score: float, motivational_phrase: string|null}
     */
    public function analyze(string $text): array
    {
        try {
            $response = $this->httpClient->request('POST', $this->getApiUrl(), [
                'json' => ['text' => $text],
                'timeout' => self::TIMEOUT,
            ]);

            $data = $response->toArray(throw: true);

            return [
                'sentiment' => $data['sentiment'] ?? 'neutral',
                'score' => $data['score'] ?? 0.0,
                'motivational_phrase' => $data['motivational_phrase'] ?? null,
            ];
        } catch (TransportExceptionInterface $e) {
            // Python API is unreachable — fail gracefully, do not block note saving
            return [
                'sentiment' => 'neutral',
                'score' => 0.0,
                'motivational_phrase' => null,
                'error' => 'Sentiment service unavailable',
            ];
        } catch (\Throwable $e) {
            return [
                'sentiment' => 'neutral',
                'score' => 0.0,
                'motivational_phrase' => null,
                'error' => 'Analysis failed: ' . $e->getMessage(),
            ];
        }
    }
}
