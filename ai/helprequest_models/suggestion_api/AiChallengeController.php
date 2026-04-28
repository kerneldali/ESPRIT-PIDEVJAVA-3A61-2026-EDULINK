<?php

namespace App\Controller;

use App\Entity\Challenge;
use App\Entity\Task;
use App\Service\AiTaskGenerator;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/admin/ai')]
class AiChallengeController extends AbstractController
{
    #[Route('/generate-tasks/{id}', name: 'admin_ai_generate_tasks', methods: ['POST'])]
    public function generateTasks(
        Challenge $challenge, 
        AiTaskGenerator $aiService, 
        EntityManagerInterface $em
    ): JsonResponse {
        // 1. Appel du service IA
        $tasksData = $aiService->generateTasks($challenge->getTitle(), $challenge->getGoal());

        if (empty($tasksData)) {
            return $this->json(['error' => 'IA non disponible ou aucune tâche générée'], 500);
        }

        // 2. Création et enregistrement des tâches
        foreach ($tasksData as $data) {
            $task = new Task();
            $task->setTitle($data['title']);
            $task->setPoints($data['points'] ?? 10);
            $task->setChallenge($challenge);
            $em->persist($task);
        }

        $em->flush();

        return $this->json([
            'status' => 'success',
            'message' => count($tasksData) . ' tâches ont été générées automatiquement.',
            'tasks' => $tasksData
        ]);
    }
}
