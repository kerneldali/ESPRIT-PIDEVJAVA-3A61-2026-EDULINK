<?php

namespace App\Service;

use App\Entity\UserChallenge;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class MotivationCoach
{
    private const GEMINI_URL = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent';
    private const GROQ_URL = 'https://api.groq.com/openai/v1/chat/completions';

    public function __construct(
        private HttpClientInterface $client,
        private string $geminiApiKey,
        private string $groqApiKey
    ) {}

    public function getMotivationalMessage(UserChallenge $userChallenge): string
    {
        $challenge = $userChallenge->getChallenge();
        $title = $challenge->getTitle();
        $progress = $userChallenge->getProgress();

        $prompt = "Tu es un coach de motivation dynamique pour un étudiant. 
        Challenge: '$title'. Progression: $progress. 
        Génère un message d'encouragement très court (15 mots max) en français. Énergique ! Pas de guillemets.";

        // 1. Essayer Gemini
        try {
            if ($this->geminiApiKey && $this->geminiApiKey !== 'placeholder_key') {
                $response = $this->client->request('POST', self::GEMINI_URL . '?key=' . $this->geminiApiKey, [
                    'json' => ['contents' => [['parts' => [['text' => $prompt]]]]],
                    'timeout' => 5,
                ]);
                if ($response->getStatusCode() === 200) {
                    $data = $response->toArray();
                    $text = $data['candidates'][0]['content']['parts'][0]['text'] ?? null;
                    if ($text) return trim($text);
                }
            }
        } catch (\Exception $e) {}

        // 2. Essayer Groq (Llama 3)
        try {
            if ($this->groqApiKey && $this->groqApiKey !== 'placeholder_key') {
                $response = $this->client->request('POST', self::GROQ_URL, [
                    'headers' => ['Authorization' => 'Bearer ' . $this->groqApiKey],
                    'json' => [
                        'model' => 'llama3-8b-8192',
                        'messages' => [['role' => 'user', 'content' => $prompt]],
                        'max_tokens' => 50,
                        'temperature' => 0.7
                    ],
                    'timeout' => 5,
                ]);
                if ($response->getStatusCode() === 200) {
                    $data = $response->toArray();
                    $text = $data['choices'][0]['message']['content'] ?? null;
                    if ($text) return trim($text);
                }
            }
        } catch (\Exception $e) {}

        // 3. Fallback local varié
        $fallbacks = [
            "Allez champion ! Ta progression sur $title est top, continue !",
            "Incroyable effort ! Tu es déjà à $progress, ne lâche rien !",
            "Chaque pas compte. Tu vas écraser ce challenge !",
            "Regarde tout ce que tu as accompli. La victoire est proche !",
            "Tu es une machine ! $progress de fait, le reste n'est qu'une formalité !",
            "C'est du beau travail. Ton futur toi te remerciera !"
        ];

        return $fallbacks[array_rand($fallbacks)];
    }
}
