<?php

namespace App\Controller;

use App\Entity\UserChallenge;
use App\Entity\UserTask;
use App\Entity\Task;
use App\Repository\ChallengeRepository;
use App\Repository\UserChallengeRepository;
use App\Repository\UserTaskRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\File\Exception\FileException;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class ChallengeController extends AbstractController
{
    #[Route('/challenges', name: 'challenge_index', methods: ['GET'])]
    public function index(Request $request, ChallengeRepository $challengeRepo, UserChallengeRepository $ucRepo): Response
    {
        $q = trim((string) $request->query->get('q', ''));
        $order = (string) $request->query->get('order', 'newest');

        $qb = $challengeRepo->createQueryBuilder('c');

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

        $challenges = $qb->getQuery()->getResult();

        $user = $this->getUser();
        $joinedChallengeIds = [];

        if ($user) {
            $participations = $ucRepo->findBy(['user' => $user]);
            $joinedChallengeIds = array_map(
                fn(UserChallenge $uc) => $uc->getChallenge()?->getId(),
                $participations
            );
            $joinedChallengeIds = array_values(array_filter($joinedChallengeIds));
        }

        return $this->render('challenge/index.html.twig', [
            'challenges' => $challenges,
            'joinedChallengeIds' => $joinedChallengeIds,
            'q' => $q,
            'order' => $order,
        ]);
    }

    #[Route('/challenges/{id}/join', name: 'challenge_join', methods: ['POST'])]
    public function join(
        int $id,
        Request $request,
        ChallengeRepository $challengeRepo,
        UserChallengeRepository $ucRepo,
        EntityManagerInterface $em
    ): Response {
        $this->denyAccessUnlessGranted('ROLE_USER');

        if (!$this->isCsrfTokenValid('join_challenge_' . $id, (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Action invalide (CSRF).');
            return $this->redirectToRoute('challenge_index');
        }

        $challenge = $challengeRepo->find($id);
        if (!$challenge) {
            throw $this->createNotFoundException('Challenge not found');
        }

        /** @var \App\Entity\User $user */
        $user = $this->getUser();

        $existing = $ucRepo->findOneBy(['user' => $user, 'challenge' => $challenge]);
        if ($existing) {
            $this->addFlash('info', 'Tu as déjà rejoint ce challenge.');
            return $this->redirectToRoute('challenge_index');
        }

        $uc = new UserChallenge();
        $uc->setUser($user);
        $uc->setChallenge($challenge);
        $uc->setStatus(UserChallenge::STATUS_IN_PROGRESS);
        $uc->setProofFileName(null);

        // ✅ Create UserTask placeholders for each challenge task
        foreach ($challenge->getTasks() as $task) {
            $ut = new UserTask();
            $ut->setTask($task);
            $ut->setUserChallenge($uc);
            $ut->setIsCompleted(false);
            $em->persist($ut);
            $uc->addUserTask($ut);
        }

        $uc->updateProgress(); // Initial progress (0/X)

        $em->persist($uc);
        $em->flush();

        $this->addFlash('success', 'Challenge rejoint !');
        return $this->redirectToRoute('challenge_index');
    }

    #[Route('/my-challenges', name: 'my_challenges', methods: ['GET'])]
    public function myChallenges(UserChallengeRepository $ucRepo, EntityManagerInterface $em): Response
    {
        $this->denyAccessUnlessGranted('ROLE_USER');

        $participations = $ucRepo->findBy(
            ['user' => $this->getUser()],
            ['id' => 'DESC']
        );

        // ✅ Self-healing: ensure all UserTask placeholders exist for joined challenges
        $needsFlush = false;
        foreach ($participations as $p) {
            $challenge = $p->getChallenge();
            if (!$challenge)
                continue;

            $existingTaskIds = [];
            foreach ($p->getUserTasks() as $ut) {
                if ($ut->getTask()) {
                    $existingTaskIds[] = $ut->getTask()->getId();
                }
            }

            foreach ($challenge->getTasks() as $task) {
                if (!in_array($task->getId(), $existingTaskIds, true)) {
                    $newUt = new UserTask();
                    $newUt->setTask($task);
                    $newUt->setUserChallenge($p);
                    $newUt->setIsCompleted(false);
                    $em->persist($newUt);
                    $p->addUserTask($newUt);
                    $needsFlush = true;
                }
            }
        }

        if ($needsFlush) {
            foreach ($participations as $p) {
                $p->updateProgress();
            }
            $em->flush();
        }

        return $this->render('challenge/my_challenges.html.twig', [
            'participations' => $participations,
        ]);
    }

    #[Route('/my-challenges/{id}/update', name: 'update_progress', methods: ['POST'])]
    public function updateProgress(
        UserChallenge $userChallenge,
        Request $request,
        EntityManagerInterface $em
    ): Response {
        $this->denyAccessUnlessGranted('ROLE_USER');

        // CSRF
        if (!$this->isCsrfTokenValid('update_progress_' . $userChallenge->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Action invalide (CSRF).');
            return $this->redirectToRoute('my_challenges');
        }

        if ($userChallenge->getUser() !== $this->getUser()) {
            throw $this->createAccessDeniedException();
        }

        if ($userChallenge->getStatus() === UserChallenge::STATUS_COMPLETED) {
            $this->addFlash('info', 'Challenge déjà validé.');
            return $this->redirectToRoute('my_challenges');
        }

        // Recalculate progress from actual UserTask completion (don't trust form input)
        $userChallenge->updateProgress();
        $parts = explode('/', $userChallenge->getProgress());
        $current = (int) $parts[0];
        $total = (int) ($parts[1] ?? 0);

        if ($current >= $total) {
            $uploadedFile = $request->files->get('proof_file');

            if (!$uploadedFile && !$userChallenge->getProofFileName()) {
                $this->addFlash('error', 'Pour terminer, tu dois joindre un fichier de preuve.');
                return $this->redirectToRoute('my_challenges');
            }

            if ($uploadedFile) {
                $userChallenge->setProofFile($uploadedFile);
                $userChallenge->setStatus(UserChallenge::STATUS_PENDING);
                $this->addFlash('success', 'Preuve envoyée ! En attente de validation admin.');
            }
        } else {
            $this->addFlash('error', 'Complete all tasks before uploading proof (' . $current . '/' . $total . ' done).');
            return $this->redirectToRoute('my_challenges');
        }

        $em->flush();
        return $this->redirectToRoute('my_challenges');
    }
    #[Route('/my-challenges/task/{id}/toggle', name: 'challenge_task_toggle', methods: ['POST'])]
    public function toggleTask(
        UserTask $userTask,
        EntityManagerInterface $em
    ): Response {
        $this->denyAccessUnlessGranted('ROLE_USER');

        $uc = $userTask->getUserChallenge();
        if (!$uc || $uc->getUser() !== $this->getUser()) {
            throw $this->createAccessDeniedException();
        }

        if ($uc->getStatus() === UserChallenge::STATUS_COMPLETED) {
            $this->addFlash('info', 'Challenge déjà validé.');
            return $this->redirectToRoute('my_challenges');
        }

        $userTask->setIsCompleted(!$userTask->isCompleted());
        $uc->updateProgress();

        $em->flush();

        return $this->json([
            'success' => true,
            'completed' => $userTask->isCompleted(),
            'progress' => $uc->getProgress()
        ]);
    }
}
