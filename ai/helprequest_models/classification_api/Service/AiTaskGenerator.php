<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;

class AiTaskGenerator
{
    private HttpClientInterface $httpClient;
    private string $aiApiUrl;

    public function __construct(HttpClientInterface $httpClient, string $aiApiUrl = 'http://localhost:8000')
    {
        $this->httpClient = $httpClient;
        $this->aiApiUrl = $aiApiUrl;
    }

    /**
     * Appelle le microservice Python pour générer des tâches
     */
    public function generateTasks(string $title, string $goal): array
    {
        try {
            $response = $this->httpClient->request('POST', $this->aiApiUrl . '/predict', [
                'json' => [
                    'challenge_title' => $title,
                    'challenge_goal' => $goal,
                ],
            ]);

            if ($response->getStatusCode() !== 200) {
                return [];
            }

            $data = $response->toArray();
            return $data['generated_tasks'] ?? [];
        } catch (\Exception $e) {
            // Log error or handle it
            return [];
        }
    }
}
