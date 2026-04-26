<?php

namespace App\Service;

use App\Entity\User;
use App\Repository\NoteRepository;
use App\Repository\PersonalTaskRepository;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class StudyAdvisorService
{
    private const GROQ_URL = 'https://api.groq.com/openai/v1/chat/completions';

    public function __construct(
        private readonly HttpClientInterface $httpClient,
        private readonly NoteRepository $noteRepository,
        private readonly PersonalTaskRepository $taskRepository,
        private readonly SentimentService $sentimentService,
        private readonly string $groqApiKey,
    ) {
    }

    public function getWeeklyAdvice(User $user): array
    {
        $summary = $this->buildWeeklySummary($user);

        try {
            $prompt = $this->buildPrompt($summary);

            $response = $this->httpClient->request('POST', self::GROQ_URL, [
                'headers' => [
                    'Authorization' => 'Bearer ' . $this->groqApiKey,
                    'Content-Type' => 'application/json',
                ],
                'json' => [
                    'model' => 'llama-3.3-70b-versatile',
                    'messages' => [
                        ['role' => 'system', 'content' => 'You are a friendly and encouraging AI study advisor for university students. Give concise, actionable study tips in 3-5 bullet points. Be specific based on the data provided. Respond in the same language the student uses (check their name and note titles for clues). Keep it under 200 words.'],
                        ['role' => 'user', 'content' => $prompt],
                    ],
                    'temperature' => 0.7,
                    'max_tokens' => 400,
                ],
                'timeout' => 15.0,
            ]);

            $data = $response->toArray(throw: true);
            $advice = $data['choices'][0]['message']['content'] ?? null;

            return [
                'advice' => $advice,
                'generated_at' => (new \DateTime())->format('d/n H:i'),
                'summary' => $summary,
            ];
        } catch (\Throwable $e) {
            return [
                'advice' => null,
                'error' => 'Failed to generate advice: ' . $e->getMessage(),
                'summary' => $summary,
            ];
        }
    }

    private function buildPrompt(array $summary): string
    {
        $name = $summary['student_name'] ?? 'Student';
        $notesCount = $summary['notes_count'];
        $tags = !empty($summary['notes_tags']) ? implode(', ', $summary['notes_tags']) : 'none';
        $tasksTotal = $summary['tasks_total'];
        $tasksCompleted = $summary['tasks_completed'];
        $rate = $summary['tasks_completion_rate'];

        $sentimentSummary = '';
        foreach ($summary['notes_sentiments'] as $ns) {
            $sentimentSummary .= "- \"{$ns['title']}\" → {$ns['sentiment']}\n";
        }

        return <<<PROMPT
Student: $name
This week's activity:
- Notes written: $notesCount (topics: $tags)
- Tasks: $tasksCompleted/$tasksTotal completed ($rate% completion rate)

Note sentiments:
$sentimentSummary

Based on this data, give personalized study advice. If task completion is low, encourage better planning. If sentiments are negative, suggest stress management. If few notes, encourage more active note-taking. Be specific and encouraging.
PROMPT;
    }

    private function buildWeeklySummary(User $user): array
    {
        $oneWeekAgo = new \DateTime('-7 days');

        $allNotes = $this->noteRepository->findByUserOrderedByDate($user);
        $recentNotes = array_filter($allNotes, function ($note) use ($oneWeekAgo) {
            return $note->getCreatedAt() >= $oneWeekAgo;
        });

        $notesSentiments = [];
        $notesTags = [];
        foreach ($recentNotes as $note) {
            $sentiment = $this->sentimentService->analyze($note->getContent());
            $notesSentiments[] = [
                'title' => $note->getTitle(),
                'sentiment' => $sentiment['sentiment'],
            ];

            if ($note->getTag() && !in_array($note->getTag(), $notesTags)) {
                $notesTags[] = $note->getTag();
            }
        }

        $allTasks = $this->taskRepository->findByUserOrderedByDate($user);
        $recentTasks = array_filter($allTasks, function ($task) use ($oneWeekAgo) {
            return $task->getCreatedAt() >= $oneWeekAgo;
        });

        $tasksTotal = count($recentTasks);
        $tasksCompleted = count(array_filter($recentTasks, fn($t) => $t->isCompleted()));
        $completionRate = $tasksTotal > 0 ? ($tasksCompleted / $tasksTotal) * 100 : 0;

        return [
            'student_name' => $user->getFullName(),
            'notes_count' => count($recentNotes),
            'notes_tags' => $notesTags,
            'notes_sentiments' => $notesSentiments,
            'tasks_total' => $tasksTotal,
            'tasks_completed' => $tasksCompleted,
            'tasks_completion_rate' => round($completionRate, 1),
        ];
    }
}

