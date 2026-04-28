<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Psr\Log\LoggerInterface;

/**
 * AiMicroservice — Bridge to the FastAPI AI Inference Server (Port 8000)
 */
class AiMicroservice
{
    private const TIMEOUT = 10;

    private function getHost(): string
    {
        return $_ENV['AI_SERVICES_HOST'] ?? getenv('AI_SERVICES_HOST') ?: '127.0.0.1';
    }

    private function getApiBase(): string
    {
        return 'http://' . $this->getHost() . ':8001';
    }

    private function getJournalApiBase(): string
    {
        return 'http://' . $this->getHost() . ':5006';
    }

    public function __construct(
        private HttpClientInterface $client,
        private ?LoggerInterface $logger = null
    ) {
    }

    /**
     * Prediction: Dropout Risk
     * @param array $features {login_frequency, quiz_score, assignment_completion, forum_posts, video_watch_rate, course_category}
     */
    public function predictDropout(array $features): array
    {
        try {
            $response = $this->client->request('POST', $this->getApiBase() . '/predict/dropout', [
                'json' => $features,
                'timeout' => 5,
            ]);
            return $response->toArray();
        } catch (\Exception $e) {
            $this->log('Dropout prediction failed: ' . $e->getMessage());
            return ['error' => true, 'message' => 'Service unavailable'];
        }
    }

    /**
     * Recommendation: Courses
     */
    public function recommendCourses(string $userText, int $topN = 5): array
    {
        try {
            $response = $this->client->request('POST', $this->getApiBase() . '/recommend/courses', [
                'json' => [
                    'user_text' => $userText,
                    'top_n' => $topN
                ],
                'timeout' => 5,
            ]);
            return $response->toArray();
        } catch (\Exception $e) {
            $this->log('Course recommendation failed: ' . $e->getMessage());
            return ['recommended_course_ids' => [], 'scores' => []];
        }
    }

    /**
     * Analysis: Course Quality
     * @param array $stats {enrollment_count, avg_rating, resource_count, completion_rate}
     */
    public function analyzeCourse(array $stats): array
    {
        try {
            // Default to 0.0 if avg_rating is not provided, matching the expectation of the Linear Regression model
            if (!isset($stats['avg_rating'])) {
                $stats['avg_rating'] = 0.0;
            }

            $response = $this->client->request('POST', $this->getApiBase() . '/analyze/course', [
                'json' => $stats,
                'timeout' => 5,
            ]);
            return $response->toArray();
        } catch (\Exception $e) {
            $this->log('Course analysis failed: ' . $e->getMessage());
            return ['error' => true, 'message' => 'Service unavailable'];
        }
    }

    /**
     * AI Chat Agent
     */
    public function chat(string $message, int $userId): array
    {
        try {
            $response = $this->client->request('POST', $this->getApiBase() . '/chat', [
                'json' => [
                    'message' => $message,
                    'user_id' => $userId
                ],
                'timeout' => self::TIMEOUT,
            ]);
            return $response->toArray();
        } catch (\Exception $e) {
            $this->log('AI Chat failed: ' . $e->getMessage());
            return ['response' => "I'm sorry, I'm having trouble connecting to my brain right now. Please try again later!"];
        }
    }

    /**
     * Journal Analysis: Prediction of category and tags
     */
    public function predictJournal(string $text): array
    {
        try {
            $response = $this->client->request('POST', $this->getJournalApiBase() . '/predict', [
                'json' => ['text' => $text],
                'timeout' => 5,
            ]);
            return ['success' => true, 'data' => $response->toArray()];
        } catch (\Exception $e) {
            $this->log('Journal prediction failed: ' . $e->getMessage());
            return ['success' => false, 'error' => 'Journal service unavailable'];
        }
    }

    private function log(string $message): void
    {
        $this->logger?->error('[AiMicroservice] ' . $message);
    }
}
