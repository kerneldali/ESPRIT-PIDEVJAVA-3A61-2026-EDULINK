<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Psr\Log\LoggerInterface;

/**
 * GeminiService — Google Gemini AI integration
 *
 * Responsibilities (Gemini API only):
 *   - summarizeSession()      -> AI summary of a chat session
 *   - classifyHelpRequest()   -> Auto-categorize & set difficulty
 *   - suggestReplies()        -> AI-suggested quick replies for tutors
 */
class GeminiService
{
    private const API_URL = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent';

    public function __construct(
        private HttpClientInterface $client,
        private string $geminiApiKey,
        private ?LoggerInterface $logger = null
    ) {
    }

    /**
     * Summarizes a chat session into 2-3 sentences.
     */
    public function summarizeSession(array $messages): string
    {
        if (empty($messages)) {
            return 'No messages in this session.';
        }

        $conversation = implode("\n", array_map(fn($m) => "- {$m}", $messages));
        $prompt = "Summarize this tutoring chat session in 2-3 sentences. Focus on what was discussed and whether the problem was resolved.\n\nConversation:\n$conversation";

        try {
            return $this->callGemini($prompt);
        } catch (\Exception $e) {
            $this->log('Session summary failed: ' . $e->getMessage());
            return 'Session completed with ' . count($messages) . ' messages exchanged.';
        }
    }

    /**
     * Auto-classifies a help request by subject category and difficulty.
     * @return array{category: string, difficulty: string}
     */
    public function classifyHelpRequest(string $title, string $description): array
    {
        $default = ['category' => 'General', 'difficulty' => 'Medium'];

        if (empty(trim($title))) {
            return $default;
        }

        $prompt = "Classify this student help request into a category and difficulty level.

Title: \"$title\"
Description: \"$description\"

Categories: Math, Physics, Chemistry, Biology, Computer Science, Languages, History, Geography, Philosophy, Economics, General
Difficulty levels: Easy, Medium, Hard

Reply in EXACTLY this format (nothing else):
Category: [category]
Difficulty: [level]";

        try {
            $response = $this->callGemini($prompt);
            return $this->parseClassification($response, $default);
        } catch (\Exception $e) {
            $this->log('Classification failed: ' . $e->getMessage());
            return $default;
        }
    }

    /**
     * Suggests 3 quick reply options based on the recent conversation.
     */
    public function suggestReplies(array $recentMessages): array
    {
        $fallback = ['I understand, let me help.', 'Can you explain more?', 'I think the answer is...'];

        if (empty($recentMessages)) {
            return $fallback;
        }

        $conversation = implode("\n", array_map(fn($m) => "- {$m}", array_slice($recentMessages, -5)));
        $prompt = "Based on this tutoring conversation, suggest exactly 3 short reply options (max 15 words each) that a helpful tutor might send next. Output ONLY the 3 replies, one per line, no numbering.\n\nRecent messages:\n$conversation";

        try {
            $response = $this->callGemini($prompt);
            $lines = array_filter(array_map('trim', explode("\n", $response)));
            $suggestions = array_slice($lines, 0, 3);
            return count($suggestions) >= 3 ? $suggestions : $fallback;
        } catch (\Exception $e) {
            $this->log('Suggest replies failed: ' . $e->getMessage());
            return $fallback;
        }
    }

    /**
     * Calls the Gemini API with a text prompt.
     */
    private function callGemini(string $prompt): string
    {
        if ($this->geminiApiKey === 'placeholder_key' || empty($this->geminiApiKey)) {
            throw new \RuntimeException('Gemini API key not configured');
        }

        $response = $this->client->request('POST', self::API_URL . '?key=' . $this->geminiApiKey, [
            'json' => [
                'contents' => [
                    ['parts' => [['text' => $prompt]]]
                ],
                'generationConfig' => [
                    'temperature' => 0.3,
                    'maxOutputTokens' => 256,
                ],
            ],
            'timeout' => 10,
        ]);

        $data = $response->toArray();
        return $data['candidates'][0]['content']['parts'][0]['text'] ?? '';
    }

    private function parseClassification(string $response, array $default): array
    {
        $result = $default;
        if (preg_match('/Category:\s*(.+)/i', $response, $m)) {
            $result['category'] = trim($m[1]);
        }
        if (preg_match('/Difficulty:\s*(.+)/i', $response, $m)) {
            $result['difficulty'] = trim($m[1]);
        }

        $validCategories = ['Math', 'Physics', 'Chemistry', 'Biology', 'Computer Science', 'Languages', 'History', 'Geography', 'Philosophy', 'Economics', 'General'];
        if (!in_array($result['category'], $validCategories)) {
            $result['category'] = 'General';
        }

        $validDifficulties = ['Easy', 'Medium', 'Hard'];
        if (!in_array($result['difficulty'], $validDifficulties)) {
            $result['difficulty'] = 'Medium';
        }

        return $result;
    }

    private function log(string $message): void
    {
        $this->logger?->warning('[GeminiService] ' . $message);
    }
}
