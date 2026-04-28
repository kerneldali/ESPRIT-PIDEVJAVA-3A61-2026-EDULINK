<?php

namespace App\Controller\Admin;

use App\Entity\User;
use App\Form\UserType;
use App\Repository\UserRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use App\Entity\AiSentimentLog;

#[Route('/admin/users')]
class UserController extends AbstractController
{
    #[Route('/', name: 'app_admin_user_index', methods: ['GET'])]
    public function index(Request $request, UserRepository $userRepository): Response
    {
        // 1. Get Filters from Request
        $filters = [
            'search' => $request->query->get('q'),
            'role' => $request->query->get('role'),
        ];
        $sort = (string) $request->query->get('sort', 'id');
        $direction = (string) $request->query->get('direction', 'DESC');

        // 2. Get Users based on filters
        $users = $userRepository->findByFilter($filters, $sort, $direction);

        // 3. Get Statistics
        $stats = $userRepository->getUserStatistics();

        return $this->render('admin/user/index.html.twig', [
            'users' => $users,
            'stats' => $stats,
            'current_filters' => $filters,
            'current_sort' => $sort,
            'current_direction' => $direction,
        ]);
    }

    #[Route('/new', name: 'app_admin_user_new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $entityManager, UserPasswordHasherInterface $userPasswordHasher): Response
    {
        $user = new User();
        // Set default/initial values
        $user->setWalletBalance(0);

        $form = $this->createForm(UserType::class, $user, ['is_new' => true]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {

            // Hash the password
            $plainPassword = $form->get('password')->getData();
            if ($plainPassword) {
                $user->setPassword(
                    $userPasswordHasher->hashPassword(
                        $user,
                        $plainPassword
                    )
                );
            }

            $entityManager->persist($user);
            $entityManager->flush();

            return $this->redirectToRoute('app_admin_user_index', [], Response::HTTP_SEE_OTHER);
        }

        return $this->render('admin/user/new.html.twig', [
            'user' => $user,
            'form' => $form->createView(),
        ]);
    }

    #[Route('/{id}/edit', name: 'app_admin_user_edit', methods: ['GET', 'POST'])]
    public function edit(Request $request, User $user, EntityManagerInterface $entityManager, UserPasswordHasherInterface $userPasswordHasher): Response
    {
        $form = $this->createForm(UserType::class, $user, ['is_new' => false]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {

            $plainPassword = $form->get('password')->getData();
            if ($plainPassword) {
                $user->setPassword(
                    $userPasswordHasher->hashPassword(
                        $user,
                        $plainPassword
                    )
                );
            }

            $entityManager->flush();

            return $this->redirectToRoute('app_admin_user_index', [], Response::HTTP_SEE_OTHER);
        }

        return $this->render('admin/user/edit.html.twig', [
            'user' => $user,
            'form' => $form->createView(),
        ]);
    }

    #[Route('/{id}', name: 'app_admin_user_delete', methods: ['POST'])]
    public function delete(Request $request, User $user, EntityManagerInterface $entityManager): Response
    {
        if ($this->isCsrfTokenValid('delete' . $user->getId(), (string) $request->request->get('_token'))) {
            $entityManager->remove($user);
            $entityManager->flush();
        }

        return $this->redirectToRoute('app_admin_user_index', [], Response::HTTP_SEE_OTHER);
    }

    #[Route('/api/{id}/sentiment-logs', name: 'admin_api_user_sentiment_logs', methods: ['GET'])]
    public function getUserSentimentLogs(User $user, EntityManagerInterface $entityManager): JsonResponse
    {
        // For demonstration, fetch recent logs from the DB. 
        // If empty, generate some fake ones to allow training demonstrations.
        $logs = $user->getAiSentimentLogs()->toArray();
        usort($logs, fn($a, $b) => $b->getId() <=> $a->getId());
        $logs = array_slice($logs, 0, 10);

        $data = array_map(function (AiSentimentLog $log) {
            return [
                'id' => $log->getId(),
                'text' => $log->getText(),
                'sentiment' => $log->getSentiment(),
                'confidence' => $log->getConfidence()
            ];
        }, $logs);

        // If no logs, let's just return a placeholder so the admin can test "teaching"
        if (empty($data)) {
            $data = [
                ['id' => 'temp1', 'text' => 'The course was decent, but I was a bit bored.', 'sentiment' => 'Neutral', 'confidence' => 0.65],
                ['id' => 'temp2', 'text' => 'I hated this module, very confusing!', 'sentiment' => 'Negative', 'confidence' => 0.82],
                ['id' => 'temp3', 'text' => 'Excellent explanations, highly recommended.', 'sentiment' => 'Positive', 'confidence' => 0.95]
            ];
        }

        return new JsonResponse(['status' => 'success', 'logs' => $data]);
    }

    #[Route('/api/sentiment/teach', name: 'admin_api_sentiment_teach', methods: ['POST'])]
    public function teachSentiment(Request $request, HttpClientInterface $httpClient, EntityManagerInterface $em): JsonResponse
    {
        $data = json_decode($request->getContent(), true);
        $text = $data['text'] ?? '';
        $correctLabel = $data['correct_label'] ?? '';

        if (!$text || !$correctLabel) {
            return new JsonResponse(['status' => 'error', 'message' => 'Missing text or label'], 400);
        }

        try {
            // Send feedback to local Taki Model
            $host = $_ENV['AI_SERVICES_HOST'] ?? getenv('AI_SERVICES_HOST') ?: '127.0.0.1';
            $response = $httpClient->request('POST', 'http://' . $host . ':5005/teach_sentiment', [
                'json' => [
                    'text' => $text,
                    'correct_label' => $correctLabel
                ]
            ]);

            $result = $response->toArray();
            return new JsonResponse($result);
        } catch (\Exception $e) {
            return new JsonResponse(['status' => 'error', 'message' => 'Failed to reach AI Server on port 5005'], 500);
        }
    }
}
