<?php

namespace App\Controller;

use App\Entity\Challenge;
use App\Entity\UserChallenge;
use App\Entity\Task;
use App\Form\ChallengeType;
use App\Form\TaskType;
use App\Repository\ChallengeRepository;
use App\Repository\UserChallengeRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use App\Service\BadgeService;
use App\Service\GroqService;

#[Route('/admin/challenge')]
final class ChallengeAdminController extends AbstractController
{
    private BadgeService $badgeService;
    private GroqService $groqService;

    public function __construct(BadgeService $badgeService, GroqService $groqService)
    {
        $this->badgeService = $badgeService;
        $this->groqService = $groqService;
    }
    /* ========================
       CHALLENGE CRUD
    ======================== */

    #[Route(name: 'app_challenge_admin_index', methods: ['GET'])]
    public function index(Request $request, ChallengeRepository $challengeRepository): Response
    {
        $q = trim((string) $request->query->get('q', ''));
        $order = (string) $request->query->get('order', 'newest');

        $qb = $challengeRepository->createQueryBuilder('c');

        if ($q !== '') {
            $qb->andWhere('c.title LIKE :q OR c.goal LIKE :q')
                ->setParameter('q', '%' . $q . '%');
        }

        switch ($order) {
            case 'alpha_asc':
                $qb->orderBy('c.title', 'ASC');
                break;
            case 'alpha_desc':
                $qb->orderBy('c.title', 'DESC');
                break;
            case 'xp_desc':
                $qb->orderBy('c.rewardPoints', 'DESC');
                break;
            case 'xp_asc':
                $qb->orderBy('c.rewardPoints', 'ASC');
                break;
            case 'newest':
            default:
                $qb->orderBy('c.id', 'DESC');
                break;
        }

        return $this->render('challenge_admin/index.html.twig', [
            'challenges' => $qb->getQuery()->getResult(),
            'q' => $q,
            'order' => $order,
        ]);
    }

    #[Route('/new', name: 'app_challenge_admin_new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $entityManager): Response
    {
        $challenge = new Challenge();
        $form = $this->createForm(ChallengeType::class, $challenge);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            foreach ($challenge->getTasks() as $task) {
                $task->setChallenge($challenge);
                $entityManager->persist($task);
            }
            $entityManager->persist($challenge);
            $entityManager->flush();
            $this->addFlash('success', 'Challenge créé avec ' . count($challenge->getTasks()) . ' tâche(s).');
            return $this->redirectToRoute('app_challenge_admin_index');
        }

        return $this->render('challenge_admin/new.html.twig', [
            'form' => $form,
        ]);
    }

    #[Route('/{id}/edit', name: 'app_challenge_admin_edit', methods: ['GET', 'POST'])]
    public function edit(Request $request, Challenge $challenge, EntityManagerInterface $entityManager): Response
    {
        $form = $this->createForm(ChallengeType::class, $challenge);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            foreach ($challenge->getTasks() as $task) {
                $task->setChallenge($challenge);
                $entityManager->persist($task);
            }
            $entityManager->flush();
            $this->addFlash('success', 'Challenge mis à jour avec ' . count($challenge->getTasks()) . ' tâche(s).');
            return $this->redirectToRoute('app_challenge_admin_edit', ['id' => $challenge->getId()]);
        }

        return $this->render('challenge_admin/edit.html.twig', [
            'challenge' => $challenge,
            'form' => $form,
        ]);
    }

    #[Route('/{id}', name: 'app_challenge_admin_delete', methods: ['POST'])]
    public function delete(Request $request, Challenge $challenge, EntityManagerInterface $entityManager): Response
    {
        if ($this->isCsrfTokenValid('delete' . $challenge->getId(), $request->getPayload()->getString('_token'))) {
            $entityManager->remove($challenge);
            $entityManager->flush();
        }

        return $this->redirectToRoute('app_challenge_admin_index');
    }

    /* ========================
       AI MISSION GENERATION
    ======================== */

    #[Route('/ai/generate-missions', name: 'admin_ai_generate_missions', methods: ['POST'])]
    public function aiGenerateMissions(Request $request): JsonResponse
    {
        $data = json_decode($request->getContent(), true);
        $title = $data['title'] ?? '';
        $goal = $data['goal'] ?? '';
        $count = min((int) ($data['count'] ?? 3), 10);

        if (!$title) {
            return new JsonResponse(['error' => 'Title is required'], 400);
        }

        $prompt = "You are an expert educational designer. Generate exactly {$count} practical task/mission titles for a student challenge.\n"
            . "Challenge Title: {$title}\n"
            . ($goal ? "Challenge Goal: {$goal}\n" : '')
            . "Return ONLY a valid JSON array of objects, each with a single key \"title\". No markdown, no explanation.\n"
            . 'Example: [{"title":"Complete the intro quiz"},{"title":"Write a summary"}]';

        $raw = $this->groqService->generateContent($prompt);

        // Strip markdown fences if any
        $raw = preg_replace('/^```(?:json)?\s*/m', '', (string) $raw);
        $raw = preg_replace('/\s*```\s*$/m', '', (string) $raw);
        $raw = trim((string) $raw);

        // Extract JSON array
        if (preg_match('/(\[[\s\S]*\])/', $raw, $matches)) {
            $raw = $matches[1];
        }

        $missions = json_decode($raw, true);

        if (!is_array($missions)) {
            return new JsonResponse(['error' => 'AI returned invalid format', 'raw' => $raw], 500);
        }

        return new JsonResponse($missions);
    }

    /* ========================
       TASK CRUD
    ======================== */

    #[Route('/{id}/task/new', name: 'app_challenge_admin_task_new', methods: ['GET', 'POST'])]
    public function newTask(Request $request, Challenge $challenge, EntityManagerInterface $entityManager): Response
    {
        $task = new Task();
        $task->setChallenge($challenge);

        $form = $this->createForm(TaskType::class, $task);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->persist($task);
            $entityManager->flush();

            $this->addFlash('success', 'Tâche ajoutée avec succès.');
            return $this->redirectToRoute('app_challenge_admin_edit', ['id' => $challenge->getId()]);
        }

        return $this->render('challenge_admin/task_new.html.twig', [
            'challenge' => $challenge,
            'task' => $task,
            'form' => $form->createView(),
        ]);
    }

    #[Route('/task/{id}/edit', name: 'app_challenge_admin_task_edit', methods: ['GET', 'POST'])]
    public function editTask(Request $request, Task $task, EntityManagerInterface $entityManager): Response
    {
        $form = $this->createForm(TaskType::class, $task);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->flush();

            $this->addFlash('success', 'Tâche mise à jour.');
            $challengeId = $task->getChallenge() ? $task->getChallenge()->getId() : null;
            return $this->redirectToRoute('app_challenge_admin_edit', ['id' => $challengeId]);
        }

        return $this->render('challenge_admin/task_edit.html.twig', [
            'task' => $task,
            'challenge' => $task->getChallenge(),
            'form' => $form->createView(),
        ]);
    }

    #[Route('/task/{id}/delete', name: 'app_challenge_admin_task_delete', methods: ['POST'])]
    public function deleteTask(Request $request, Task $task, EntityManagerInterface $entityManager): Response
    {
        $challengeId = $task->getChallenge() ? $task->getChallenge()->getId() : null;
        if ($this->isCsrfTokenValid('delete_task' . $task->getId(), (string) $request->request->get('_token'))) {
            $entityManager->remove($task);
            $entityManager->flush();
            $this->addFlash('success', 'Tâche supprimée.');
        }

        return $this->redirectToRoute('app_challenge_admin_edit', ['id' => $challengeId]);
    }

    /* ========================
       VALIDATION SECTION
    ======================== */

    #[Route('/submissions', name: 'admin_submissions', methods: ['GET'])]
    public function submissions(UserChallengeRepository $ucRepo): Response
    {
        $submissions = $ucRepo->findBy(
            ['status' => UserChallenge::STATUS_PENDING],
            ['id' => 'DESC']
        );

        return $this->render('challenge_admin/submissions.html.twig', [
            'submissions' => $submissions
        ]);
    }

    #[Route('/submission/{id}/validate', name: 'admin_validate_submission', methods: ['POST'])]
    public function validateSubmission(
        UserChallenge $userChallenge,
        Request $request,
        EntityManagerInterface $em
    ): Response {

        if (!$this->isCsrfTokenValid('validate_' . $userChallenge->getId(), (string) $request->request->get('_token'))) {
            return $this->redirectToRoute('admin_submissions');
        }

        if ($userChallenge->getStatus() !== UserChallenge::STATUS_PENDING) {
            return $this->redirectToRoute('admin_submissions');
        }

        // Donner les points
        $user = $userChallenge->getUser();
        $reward = $userChallenge->getChallenge() ? ($userChallenge->getChallenge()->getRewardPoints() ?? 0) : 0;

        // Mise à jour Unisifiée (XP = Wallet Balance)
        if ($user) {
            $user->setWalletBalance($user->getWalletBalance() + $reward);
        }

        // Log transaction
        $transaction = new \App\Entity\Transaction();
        $transaction->setUser($user);
        $transaction->setAmount($reward);
        $transaction->setType('CHALLENGE_COMPLETION');
        $transaction->setDate(new \DateTime());
        $em->persist($transaction);

        $userChallenge->setStatus(UserChallenge::STATUS_COMPLETED);

        // ✅ Check for new badges
        $newBadges = [];
        if ($user) {
            $newBadges = $this->badgeService->checkBadges($user);
        }

        $em->flush();

        $msg = 'Challenge validé. Points crédités.';
        if (!empty($newBadges)) {
            $badgeNames = array_map(fn($b) => $b->getName(), $newBadges);
            $msg .= ' 🎉 Nouveaux badges débloqués : ' . implode(', ', $badgeNames);
        }

        $this->addFlash('success', $msg);
        return $this->redirectToRoute('admin_submissions');
    }

    #[Route('/submission/{id}/reject', name: 'admin_reject_submission', methods: ['POST'])]
    public function rejectSubmission(
        UserChallenge $userChallenge,
        Request $request,
        EntityManagerInterface $em
    ): Response {

        if (!$this->isCsrfTokenValid('reject_' . $userChallenge->getId(), (string) $request->request->get('_token'))) {
            return $this->redirectToRoute('admin_submissions');
        }

        if ($userChallenge->getStatus() !== UserChallenge::STATUS_PENDING) {
            return $this->redirectToRoute('admin_submissions');
        }

        $userChallenge->setStatus(UserChallenge::STATUS_IN_PROGRESS);
        $userChallenge->setProofFileName(null);

        $em->flush();

        $this->addFlash('info', 'Soumission refusée.');
        return $this->redirectToRoute('admin_submissions');
    }
}
