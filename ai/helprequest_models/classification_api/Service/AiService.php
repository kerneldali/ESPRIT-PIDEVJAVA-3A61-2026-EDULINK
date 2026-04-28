<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;

class AiService
{
    private HttpClientInterface $httpClient;
    private string $apiKey;

    public function __construct(HttpClientInterface $httpClient, string $aliApiKey)
    {
        $this->httpClient = $httpClient;
        $this->apiKey = $aliApiKey;
    }

    /**
     * Generate task titles based on challenge context.
     * AI only splits the challenge into actionable tasks — it does NOT assign XP.
     * Total XP is set by the admin on the Challenge entity.
     */
    public function generateMissions(string $title, string $goal, int $count = 3): array
    {
        try {
            $prompt = <<<PROMPT
Tu es un expert en gamification pédagogique. Génère une liste de $count missions concrètes et spécifiques pour un challenge intitulé "$title" dont l'objectif est : "$goal".

Règles :
1. Les missions doivent être réalisables, concrètes et directement liées au contexte.
2. Chaque mission doit avoir un titre court (max 60 caractères).
3. NE PAS attribuer de points ou de valeur XP aux missions.
4. Réponds UNIQUEMENT avec un objet JSON strictement au format suivant :
{
  "missions": [
    {"title": "Titre de la mission 1"},
    {"title": "Titre de la mission 2"}
  ]
}
PROMPT;

            $response = $this->httpClient->request('POST', 'https://api.groq.com/openai/v1/chat/completions', [
                'headers' => [
                    'Authorization' => 'Bearer ' . $this->apiKey,
                    'Content-Type' => 'application/json',
                ],
                'json' => [
                    'model' => 'llama-3.3-70b-versatile',
                    'messages' => [
                        ['role' => 'user', 'content' => $prompt]
                    ],
                    'temperature' => 0.7,
                    'response_format' => ['type' => 'json_object']
                ]
            ]);

            $data = $response->toArray();

            if (!isset($data['choices'][0]['message']['content'])) {
                throw new \Exception('Réponse Groq vide ou malformée');
            }

            $contentRaw = $data['choices'][0]['message']['content'];
            $contentRaw = preg_replace('/^```json\s*|```$/', '', trim($contentRaw));
            $content = json_decode($contentRaw, true);

            if (json_last_error() !== JSON_ERROR_NONE) {
                if (preg_match('/\[.*\]/s', $contentRaw, $matches)) {
                    $content = json_decode($matches[0], true);
                }
            }

            if (!$content) {
                throw new \Exception('Impossible de parser le JSON généré par l\'IA: ' . json_last_error_msg());
            }

            // Handle different possible JSON structures
            $missions = [];
            if (isset($content['missions']) && is_array($content['missions'])) {
                $missions = $content['missions'];
            } elseif (is_array($content)) {
                // If it's a sequential array [...]
                if (isset($content[0])) {
                    $missions = $content;
                } else {
                    // If it's an associative array without 'missions' key, look for any nested array
                    foreach ($content as $val) {
                        if (is_array($val)) {
                            $missions = $val;
                            break;
                        }
                    }
                }
            }

            if (empty($missions)) {
                throw new \Exception('Aucune mission trouvée dans le JSON de l\'IA');
            }

            // Return only titles, strip any points the AI might have generated
            return array_map(fn($m) => ['title' => $m['title'] ?? 'Mission'], array_slice($missions, 0, $count));

        } catch (\Exception $e) {
            return $this->generateLocalMock($title, $goal, $count);
        }
    }

    /**
     * Fallback local logic — returns titles only, no points.
     */
    private function generateLocalMock(string $title, string $goal, int $count): array
    {
        $context = strtolower($title . ' ' . $goal);

        $library = [
            'eco' => [
                ['title' => 'Ramasser 10 déchets plastique'],
                ['title' => 'Éteindre tous les appareils en veille pendant 1h'],
                ['title' => 'Prendre une douche de moins de 5 minutes'],
                ['title' => 'Planter une graine ou s\'occuper d\'une plante'],
                ['title' => 'Calculer son empreinte carbone'],
            ],
            'sport' => [
                ['title' => 'Faire 20 pompes ou abdos'],
                ['title' => 'Courir pendant 15 minutes'],
                ['title' => 'Faire 5 minutes d\'étirements matinaux'],
                ['title' => 'Monter 5 étages à pied'],
                ['title' => 'Boire 1.5L d\'eau dans la journée'],
            ],
            'code' => [
                ['title' => 'Résoudre un bug critique'],
                ['title' => 'Écrire un test unitaire pour une fonction'],
                ['title' => 'Refactoriser 10 lignes de code legacy'],
                ['title' => 'Lire un article technique de 5 minutes'],
                ['title' => 'Partager son code sur GitHub'],
            ],
            'social' => [
                ['title' => 'Faire un compliment sincère à un collègue'],
                ['title' => 'Participer à une entraide communautaire'],
                ['title' => 'Écrire une lettre ou un mot de remerciement'],
                ['title' => 'Partager ses connaissances avec un débutant'],
            ],
            'default' => [
                ['title' => 'Terminer la lecture du dossier'],
                ['title' => 'Vérifier la conformité du travail'],
                ['title' => 'Prendre une photo de votre résultat'],
                ['title' => 'Rédiger un court rapport de synthèse'],
            ]
        ];

        $match = 'default';
        if (str_contains($context, 'planète') || str_contains($context, 'éco') || str_contains($context, 'climat') || str_contains($context, 'vert'))
            $match = 'eco';
        elseif (str_contains($context, 'sport') || str_contains($context, 'vitesse') || str_contains($context, 'muscle') || str_contains($context, 'santé'))
            $match = 'sport';
        elseif (str_contains($context, 'code') || str_contains($context, 'dévelop') || str_contains($context, 'programmation') || str_contains($context, 'app'))
            $match = 'code';
        elseif (str_contains($context, 'social') || str_contains($context, 'ami') || str_contains($context, 'groupe') || str_contains($context, 'aider'))
            $match = 'social';

        $missions = $library[$match];
        shuffle($missions);
        return array_slice($missions, 0, $count);
    }
}
