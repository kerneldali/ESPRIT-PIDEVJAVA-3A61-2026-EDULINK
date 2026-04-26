<?php

namespace App\Service;

use App\Entity\UserChallenge;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class MotivationCoach
{
    private const API_URL = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent';

    public function __construct(
        private HttpClientInterface $client,
        private string $geminiApiKey
    ) {}

    public function getMotivationalMessage(UserChallenge $userChallenge): string
    {
        $challenge = $userChallenge->getChallenge();
        $title = $challenge->getTitle();
        $goal = $challenge->getGoal();
        $progress = $userChallenge->getProgress();

        $prompt = "Tu es un coach de motivation dynamique et encourageant pour un étudiant. 
        L'étudiant participe à un challenge intitulé '$title'. 
        Son but est '$goal'. 
        Sa progression actuelle est $progress. 
        Génère un court message d'encouragement (maximum 20 mots) très énergique en français pour le booster. 
        Ne donne que le message, sans guillemets.";

        try {
            $response = $this->client->request('POST', self::API_URL . '?key=' . $this->geminiApiKey, [
                'json' => [
                    'contents' => [
                        ['parts' => [['text' => $prompt]]]
                    ],
                    'generationConfig' => [
                        'temperature' => 0.8,
                        'maxOutputTokens' => 100,
                    ],
                ],
                'timeout' => 10,
            ]);

            $data = $response->toArray();
            return $data['candidates'][0]['content']['parts'][0]['text'] ?? "Allez, continue comme ça ! Tu es sur la bonne voie.";
        } catch (\Exception $e) {
            return "Allez ! Chaque petit pas compte. Tu vas y arriver !";
        }
    }
}
