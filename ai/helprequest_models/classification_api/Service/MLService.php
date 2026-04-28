<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Psr\Log\LoggerInterface;

/**
 * MLService — Local Machine Learning inference via Flask API (127.0.0.1:5000)
 *
 * Endpoints used:
 *   POST /predict         -> Toxicity detection (TF-IDF + Logistic Regression)
 *   POST /detect-language -> Language detection  (langdetect)
 *   POST /sentiment       -> Sentiment analysis  (TextBlob)
 */
class MLService
{
    private string $apiBase;
    private const TIMEOUT = 3;

    private function getApiBase(): string
    {
        if (!isset($this->apiBase)) {
            $host = $_ENV['AI_SERVICES_HOST'] ?? getenv('AI_SERVICES_HOST') ?: '127.0.0.1';
            $this->apiBase = 'http://' . $host . ':5000';
        }
        return $this->apiBase;
    }

    public function __construct(
        private HttpClientInterface $client,
        private ?LoggerInterface $logger = null
    ) {
    }

    /**
     * Detects toxicity via local ML model.
     * @return array{isToxic: bool, source: string}
     */
    public function analyzeToxicity(string $text): array
    {
        if (empty(trim($text))) {
            return ['isToxic' => false, 'source' => 'NONE'];
        }

        try {
            $response = $this->client->request('POST', $this->getApiBase() . '/predict', [
                'json' => ['text' => $text],
                'timeout' => self::TIMEOUT,
            ]);
            $data = $response->toArray();
            return [
                'isToxic' => $data['is_toxic'] ?? false,
                'source' => 'LOCAL ML',
            ];
        } catch (\Exception $e) {
            $this->log('Toxicity check failed: ' . $e->getMessage());
            return ['isToxic' => $this->keywordFallback($text), 'source' => 'KEYWORD FALLBACK'];
        }
    }

    /**
     * Detects the language of a given text.
     * @return array{language_code: string, language_name: string}
     */
    public function detectLanguage(string $text): array
    {
        $default = ['language_code' => 'unknown', 'language_name' => 'Unknown'];
        if (empty(trim($text))) {
            return $default;
        }

        try {
            $response = $this->client->request('POST', $this->getApiBase() . '/detect-language', [
                'json' => ['text' => $text],
                'timeout' => self::TIMEOUT,
            ]);
            return $response->toArray();
        } catch (\Exception $e) {
            $this->log('Language detection failed: ' . $e->getMessage());
            return $default;
        }
    }

    /**
     * Analyzes the sentiment of a text.
     * @return array{label: string, polarity: float, subjectivity: float}
     */
    public function analyzeSentiment(string $text): array
    {
        $default = ['label' => 'neutral', 'polarity' => 0.0, 'subjectivity' => 0.0];
        if (empty(trim($text))) {
            return $default;
        }

        try {
            $response = $this->client->request('POST', $this->getApiBase() . '/sentiment', [
                'json' => ['text' => $text],
                'timeout' => self::TIMEOUT,
            ]);
            return $response->toArray();
        } catch (\Exception $e) {
            $this->log('Sentiment analysis failed: ' . $e->getMessage());
            return $default;
        }
    }

    /**
     * TF-IDF cosine similarity for smart tutor matching.
     *
     * @param string $requestText  The help request description
     * @param array  $tutors       Array of ['id' => int, 'history' => string]
     * @return array<int, float>   Map of tutor_id => similarity (0.0 to 1.0)
     */
    public function smartMatch(string $requestText, array $tutors): array
    {
        if (empty(trim($requestText)) || empty($tutors)) {
            return [];
        }

        try {
            $response = $this->client->request('POST', $this->getApiBase() . '/smart-match', [
                'json' => [
                    'request_text' => $requestText,
                    'tutors' => $tutors,
                ],
                'timeout' => 5,
            ]);
            $data = $response->toArray();
            $result = [];
            foreach ($data['scores'] ?? [] as $score) {
                $result[$score['id']] = $score['similarity'];
            }
            return $result;
        } catch (\Exception $e) {
            $this->log('Smart match failed: ' . $e->getMessage());
            return [];
        }
    }

    /**
     * Classifies a help request into category + difficulty using local TF-IDF model.
     * @return array{category: string, difficulty: string, confidence: float, source: string}
     */
    public function classifyRequest(string $title, string $description): array
    {
        $default = ['category' => 'General', 'difficulty' => 'Medium', 'confidence' => 0.0, 'source' => 'FALLBACK'];

        if (empty(trim($title)) && empty(trim($description))) {
            return $default;
        }

        try {
            $response = $this->client->request('POST', $this->getApiBase() . '/classify', [
                'json' => [
                    'title' => $title,
                    'description' => $description,
                ],
                'timeout' => self::TIMEOUT,
            ]);
            return $response->toArray();
        } catch (\Exception $e) {
            $this->log('Classification failed: ' . $e->getMessage());
            return $default;
        }
    }

    /**
     * Suggests 3 contextual reply options based on recent conversation messages.
     * Uses local TF-IDF + sentiment analysis via Flask API.
     * @param array $recentMessages Array of strings like "User: I need help with X"
     * @return array 3 suggestion strings
     */
    public function suggestReplies(array $recentMessages): array
    {
        $fallback = ['I understand, let me help.', 'Can you explain more?', 'Let\'s work on this step by step.'];

        if (empty($recentMessages)) {
            return $fallback;
        }

        try {
            $response = $this->client->request('POST', $this->getApiBase() . '/suggest-replies', [
                'json' => ['messages' => array_slice($recentMessages, -5)],
                'timeout' => self::TIMEOUT,
            ]);
            $data = $response->toArray();
            $suggestions = $data['suggestions'] ?? [];
            return count($suggestions) >= 3 ? array_slice($suggestions, 0, 3) : $fallback;
        } catch (\Exception $e) {
            $this->log('Suggest replies failed: ' . $e->getMessage());
            return $fallback;
        }
    }

    /**
     * Last-resort keyword-based toxicity fallback (multilingual).
     */
    private function keywordFallback(string $text): bool
    {
        $toxicKeywords = [
            'badword',
            'hate',
            'stupid',
            'idiot',
            'fuck',
            'shit',
            'damn',
            'ass', // EN
            'merde',
            'connard',
            'putain',
            'salope',
            'encule',
            'batard',           // FR
            'zebi',
            'namimouk',
            'bhim',
            'mnayek',
            '3asba',
            'nayek',
            'kols',       // TN
        ];
        $lower = strtolower($text);
        foreach ($toxicKeywords as $kw) {
            if (str_contains($lower, $kw))
                return true;
        }
        return false;
    }

    private function log(string $message): void
    {
        $this->logger?->warning('[MLService] ' . $message);
    }
}
