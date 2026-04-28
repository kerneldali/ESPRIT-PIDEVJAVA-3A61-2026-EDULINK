<?php

namespace App\Controller;

use App\Entity\HelpRequest;
use App\Entity\Message;
use App\Entity\Review;
use App\Entity\Session;
use App\Entity\User;
use App\Form\HelpRequestType;
use App\Form\MessageType;
use App\Form\ReviewType;
use App\Repository\HelpRequestRepository;
use App\Repository\SessionRepository;
use App\Service\AssistanceService;
use App\Service\GeminiService;
use App\Service\MLService;
use App\Service\NotificationService;
use App\Service\SmartMatchingService;
use Doctrine\ORM\EntityManagerInterface;
use Knp\Component\Pager\PaginatorInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpFoundation\File\UploadedFile;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

/**
 * FRONT-OFFICE: Help Board Controller
 * Full CRUD + Chat + Review
 */
#[Route('/help-board')]
#[IsGranted('ROLE_USER')]
class HelpRequestController extends AbstractController
{
    #[Route('/', name: 'app_help_request_index', methods: ['GET'])]
    public function index(HelpRequestRepository $helpRequestRepository, Request $request, PaginatorInterface $paginator): Response
    {
        $filters = [
            'search' => $request->query->get('search'),
            'min_bounty' => $request->query->get('min_bounty'),
            'sort' => $request->query->get('sort', 'newest'),
        ];

        $allRequests = $helpRequestRepository->findOpenRequests($filters);
        $pagination = $paginator->paginate($allRequests, $request->query->getInt('page', 1), 10);

        return $this->render('help_request/index.html.twig', [
            'help_requests' => $pagination,
            'filters' => $filters,
            'my_requests' => $helpRequestRepository->findBy(
                ['student' => $this->getUser()],
                ['createdAt' => 'DESC']
            ),
        ]);
    }

    #[Route('/show/{id}', name: 'app_help_request_show', methods: ['GET'])]
    public function show(HelpRequest $helpRequest, Request $request, SmartMatchingService $smartMatchingService): Response
    {
        // Retrieve AI-suggested tutors (from session flash or compute fresh)
        $suggestedTutors = $request->getSession()->get('suggested_tutors_' . $helpRequest->getId(), null);
        if ($suggestedTutors === null && $helpRequest->getStatus() === 'OPEN') {
            $suggestedTutors = $smartMatchingService->findBestTutors($helpRequest);
        }
        $request->getSession()->remove('suggested_tutors_' . $helpRequest->getId());

        return $this->render('help_request/show.html.twig', [
            'help_request' => $helpRequest,
            'suggested_tutors' => $suggestedTutors ?? [],
        ]);
    }

    #[Route('/new', name: 'app_help_request_new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $entityManager, GeminiService $geminiService, SmartMatchingService $smartMatchingService, MLService $mlService): Response
    {
        $helpRequest = new HelpRequest();
        $form = $this->createForm(HelpRequestType::class, $helpRequest);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            /** @var \App\Entity\User $user */
            $user = $this->getUser();

            if ($user->getWalletBalance() < $helpRequest->getBounty()) {
                $this->addFlash('error', 'Insufficient wallet balance for this bounty.');
                return $this->redirectToRoute('app_help_request_new');
            }

            $helpRequest->setStudent($user);

            // AI Auto-Classification — Local ML first, Gemini fallback
            $classification = $mlService->classifyRequest(
                (string) $helpRequest->getTitle(),
                (string) $helpRequest->getDescription()
            );

            // If local ML failed, fall back to Gemini API
            if ($classification['source'] === 'FALLBACK') {
                $classification = $geminiService->classifyHelpRequest(
                    (string) $helpRequest->getTitle(),
                    (string) $helpRequest->getDescription()
                );
                $classification['source'] = 'GEMINI';
            }

            $helpRequest->setCategory($classification['category']);
            $helpRequest->setDifficulty($classification['difficulty']);

            $entityManager->persist($helpRequest);
            $entityManager->flush();

            // AI Smart Matching — find best tutors
            $suggestedTutors = $smartMatchingService->findBestTutors($helpRequest);
            $request->getSession()->set('suggested_tutors_' . $helpRequest->getId(), $suggestedTutors);

            $source = $classification['source'];
            $this->addFlash('success', "Help request posted! Classified as: {$classification['category']} ({$classification['difficulty']}) — via {$source}");
            return $this->redirectToRoute('app_help_request_show', ['id' => $helpRequest->getId()]);
        }

        return $this->render('help_request/new.html.twig', [
            'help_request' => $helpRequest,
            'form' => $form,
        ]);
    }

    #[Route('/edit/{id}', name: 'app_help_request_edit', methods: ['GET', 'POST'])]
    public function edit(HelpRequest $helpRequest, Request $request, EntityManagerInterface $entityManager): Response
    {
        if ($helpRequest->getStudent() !== $this->getUser()) {
            $this->addFlash('error', 'You can only edit your own requests.');
            return $this->redirectToRoute('app_help_request_index');
        }

        if ($helpRequest->getStatus() !== 'OPEN') {
            $this->addFlash('error', 'Cannot edit a request that is no longer open.');
            return $this->redirectToRoute('app_help_request_index');
        }

        $form = $this->createForm(HelpRequestType::class, $helpRequest);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            /** @var \App\Entity\User $user */
            $user = $this->getUser();
            if ($user->getWalletBalance() < $helpRequest->getBounty()) {
                $this->addFlash('error', 'Insufficient wallet balance for this bounty.');
                return $this->redirectToRoute('app_help_request_edit', ['id' => $helpRequest->getId()]);
            }

            $entityManager->flush();
            $this->addFlash('success', 'Help request updated successfully!');
            return $this->redirectToRoute('app_help_request_index');
        }

        return $this->render('help_request/edit.html.twig', [
            'help_request' => $helpRequest,
            'form' => $form,
        ]);
    }

    #[Route('/delete/{id}', name: 'app_help_request_delete', methods: ['POST'])]
    public function delete(HelpRequest $helpRequest, Request $request, EntityManagerInterface $entityManager): Response
    {
        if (!$this->isCsrfTokenValid('delete_help_' . $helpRequest->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Invalid security token.');
            return $this->redirectToRoute('app_help_request_index');
        }

        if ($helpRequest->getStudent() !== $this->getUser()) {
            $this->addFlash('error', 'You can only delete your own requests.');
            return $this->redirectToRoute('app_help_request_index');
        }

        if ($helpRequest->getStatus() !== 'OPEN') {
            $this->addFlash('error', 'Cannot delete a request that is in progress or closed.');
            return $this->redirectToRoute('app_help_request_index');
        }

        $entityManager->remove($helpRequest);
        $entityManager->flush();

        $this->addFlash('success', 'Help request deleted.');
        return $this->redirectToRoute('app_help_request_index');
    }

    #[Route('/status/{id}', name: 'app_help_request_status', methods: ['GET'])]
    public function status(HelpRequest $helpRequest): \Symfony\Component\HttpFoundation\JsonResponse
    {
        $session = $helpRequest->getSession();
        return $this->json([
            'status' => $helpRequest->getStatus(),
            'has_session' => $session !== null,
            'session_id' => $session?->getId(),
            'tutor_name' => $session?->getTutor()?->getFullName(),
        ]);
    }

    #[Route('/invite-tutor/{id}/{tutorId}', name: 'app_help_request_invite_tutor', methods: ['POST'])]
    public function inviteTutor(
        HelpRequest $helpRequest,
        int $tutorId,
        Request $request,
        EntityManagerInterface $entityManager,
        NotificationService $notificationService
    ): Response {
        if (!$this->isCsrfTokenValid('invite_tutor_' . $helpRequest->getId() . '_' . $tutorId, (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Invalid security token.');
            return $this->redirectToRoute('app_help_request_show', ['id' => $helpRequest->getId()]);
        }

        /** @var \App\Entity\User $user */
        $user = $this->getUser();

        if ($helpRequest->getStudent() !== $user) {
            $this->addFlash('error', 'Only the request owner can invite tutors.');
            return $this->redirectToRoute('app_help_request_show', ['id' => $helpRequest->getId()]);
        }

        if ($helpRequest->getStatus() !== 'OPEN') {
            $this->addFlash('error', 'This request is no longer open.');
            return $this->redirectToRoute('app_help_request_show', ['id' => $helpRequest->getId()]);
        }

        $tutor = $entityManager->getRepository(\App\Entity\User::class)->find($tutorId);
        if (!$tutor) {
            $this->addFlash('error', 'Tutor not found.');
            return $this->redirectToRoute('app_help_request_show', ['id' => $helpRequest->getId()]);
        }

        if ($tutor === $user) {
            $this->addFlash('error', 'You cannot invite yourself.');
            return $this->redirectToRoute('app_help_request_show', ['id' => $helpRequest->getId()]);
        }

        $notificationService->notifyTutorInvited($helpRequest, $tutor);

        // In-app notification for the tutor
        $notification = new \App\Entity\Notification();
        $notification->setUser($tutor);
        $notification->setMessage(
            '📩 ' . ($user->getFullName() ?? $user->getEmail()) . ' needs your help with: "' . $helpRequest->getTitle() . '"'
        );
        $notification->setLink('/help-board/show/' . $helpRequest->getId());
        $entityManager->persist($notification);
        $entityManager->flush();

        $this->addFlash('success', 'Invitation sent to ' . ($tutor->getFullName() ?? $tutor->getEmail()) . '! They will be notified by email and in-app.');
        return $this->redirectToRoute('app_help_request_show', ['id' => $helpRequest->getId()]);
    }

    #[Route('/join/{id}', name: 'app_help_request_join', methods: ['POST'])]
    public function join(HelpRequest $helpRequest, Request $request, AssistanceService $assistanceService, NotificationService $notificationService): Response
    {
        if (!$this->isCsrfTokenValid('join_help_' . $helpRequest->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Invalid security token.');
            return $this->redirectToRoute('app_help_request_index');
        }

        /** @var \App\Entity\User $user */
        $user = $this->getUser();

        if ($helpRequest->getStudent() === $user) {
            $this->addFlash('error', 'You cannot join your own request.');
            return $this->redirectToRoute('app_help_request_index');
        }

        if ($helpRequest->getStatus() !== 'OPEN') {
            $this->addFlash('error', 'This request is no longer open.');
            return $this->redirectToRoute('app_help_request_index');
        }

        $session = new Session();
        $session->setHelpRequest($helpRequest);
        $session->setTutor($user);

        $assistanceService->createSession($session);

        // Email notification to student
        $notificationService->notifyRequestJoined($helpRequest, $user);

        // In-app notification for the student
        $notification = new \App\Entity\Notification();
        $notification->setUser($helpRequest->getStudent());
        $notification->setMessage(
            '🎓 ' . ($user->getFullName() ?? $user->getEmail()) . ' has joined your help request: "' . $helpRequest->getTitle() . '"'
        );
        $notification->setLink('/help-board/chat/' . $session->getId());
        $assistanceService->getEntityManager()->persist($notification);
        $assistanceService->getEntityManager()->flush();

        return $this->redirectToRoute('app_help_request_chat', ['id' => $session->getId()]);
    }

    #[Route('/chat/{id}', name: 'app_help_request_chat', methods: ['GET', 'POST'])]
    public function chat(
        int $id,
        Request $request,
        EntityManagerInterface $entityManager,
        MLService $mlService,
        SessionRepository $sessionRepository
    ): Response {
        // Eager-load all relationships in 1 query (Doctrine Doctor fix)
        $session = $sessionRepository->findForChat($id);
        if (!$session) {
            throw $this->createNotFoundException('Session not found.');
        }

        /** @var \App\Entity\User $user */
        $user = $this->getUser();

        if ($session->getTutor() !== $user && $session->getHelpRequest()->getStudent() !== $user) {
            throw $this->createAccessDeniedException('You are not a participant in this session.');
        }

        if (!$session->getJitsiRoomId()) {
            $session->setJitsiRoomId('edulink-help-' . bin2hex(random_bytes(8)));
            $entityManager->flush();
        }

        $message = new Message();
        $form = $this->createForm(MessageType::class, $message);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            if (!$session->isIsActive()) {
                $this->addFlash('error', 'This session is closed.');
                return $this->redirectToRoute('app_help_request_chat', ['id' => $session->getId()]);
            }

            $content = (string) ($message->getContent() ?? '');

            $toxicityResult = $mlService->analyzeToxicity($content);
            if ($toxicityResult['isToxic']) {
                $message->setIsToxic(true);
                $this->addFlash('warning', 'Your message triggered our toxicity filter but was sent.');
            }

            $message->setSession($session);
            $message->setSender($user);

            $entityManager->persist($message);
            $entityManager->flush();

            return $this->redirectToRoute('app_help_request_chat', ['id' => $session->getId()]);
        }

        return $this->render('help_request/chat.html.twig', [
            'session' => $session,
            'form' => $form,
        ]);
    }

    #[Route('/chat/{id}/messages', name: 'app_help_request_chat_messages', methods: ['GET'])]
    public function chatMessages(Session $session, Request $request, MLService $mlService): \Symfony\Component\HttpFoundation\JsonResponse
    {
        /** @var \App\Entity\User $user */
        $user = $this->getUser();
        if ($session->getTutor() !== $user && $session->getHelpRequest()->getStudent() !== $user) {
            return $this->json(['error' => 'Forbidden'], 403);
        }

        $afterId = (int) $request->query->get('after', 0);
        $messages = [];
        $recentTexts = [];
        foreach ($session->getMessages() as $msg) {
            /** @var \App\Entity\User $msgSender */
            $msgSender = $msg->getSender();
            $recentTexts[] = ($msgSender->getFullName() ?? $msgSender->getEmail()) . ': ' . $msg->getContent();
            if ($afterId > 0 && $msg->getId() <= $afterId) {
                continue;
            }
            $msgData = [
                'id' => $msg->getId(),
                'content' => htmlspecialchars((string) $msg->getContent(), ENT_QUOTES, 'UTF-8'),
                'sender_id' => $msgSender->getId(),
                'sender_name' => $msgSender->getFullName() ?? $msgSender->getEmail(),
                'timestamp' => $msg->getTimestamp() ? $msg->getTimestamp()->format('H:i') : null,
                'is_toxic' => $msg->isIsToxic(),
                'is_mine' => $msgSender === $user,
                'attachment_url' => $msg->getAttachmentName() ? '/uploads/chat/' . $msg->getAttachmentName() : null,
            ];
            $messages[] = $msgData;
        }

        // AI Suggested Replies — now using local ML model instead of Gemini
        $suggestions = [];
        if ($session->isIsActive() && !empty($recentTexts)) {
            $suggestions = $mlService->suggestReplies($recentTexts);
        }

        return $this->json([
            'messages' => $messages,
            'session_active' => $session->isIsActive(),
            'suggestions' => $suggestions,
        ]);
    }

    #[Route('/chat/{id}/send', name: 'app_help_request_chat_send', methods: ['POST'])]
    public function chatSend(
        Session $session,
        Request $request,
        EntityManagerInterface $entityManager,
        MLService $mlService,
        NotificationService $notificationService
    ): \Symfony\Component\HttpFoundation\JsonResponse {
        /** @var \App\Entity\User $user */
        $user = $this->getUser();
        if ($session->getTutor() !== $user && $session->getHelpRequest()->getStudent() !== $user) {
            return $this->json(['error' => 'Forbidden'], 403);
        }

        if (!$session->isIsActive()) {
            return $this->json(['error' => 'Session is closed'], 400);
        }

        $data = json_decode($request->getContent(), true);
        $content = trim($data['content'] ?? '');
        $token = $data['_token'] ?? '';

        if (!$this->isCsrfTokenValid('chat_send_' . $session->getId(), (string) $token)) {
            return $this->json(['error' => 'Invalid security token'], 403);
        }

        if (empty($content)) {
            return $this->json(['error' => 'Message cannot be empty'], 400);
        }
        if (mb_strlen($content) > 5000) {
            return $this->json(['error' => 'Message too long (max 5000 chars)'], 400);
        }

        $message = new Message();
        $message->setContent($content);
        $message->setSession($session);
        $message->setSender($user);

        $toxicityResult = $mlService->analyzeToxicity($content);
        if ($toxicityResult['isToxic']) {
            $message->setIsToxic(true);
            $notificationService->notifyAdminOfToxicContent(
                $user,
                $content,
                "Chat Session #{$session->getId()}",
                $toxicityResult['source']
            );
        }

        // ML: Language detection + Sentiment (non-blocking, best-effort)
        $langResult = $mlService->detectLanguage($content);
        $sentimentResult = $mlService->analyzeSentiment($content);

        $entityManager->persist($message);
        $entityManager->flush();

        return $this->json([
            'success' => true,
            'message' => [
                'id' => $message->getId(),
                'content' => htmlspecialchars((string) $message->getContent(), ENT_QUOTES, 'UTF-8'),
                'sender_id' => $user->getId(),
                'sender_name' => $user->getFullName() ?? $user->getEmail(),
                'timestamp' => $message->getTimestamp() ? $message->getTimestamp()->format('H:i') : null,
                'is_toxic' => $message->isIsToxic(),
                'is_mine' => true,
                'language' => $langResult['language_name'],
                'sentiment' => $sentimentResult['label'],
            ],
            'toxic_warning' => $message->isIsToxic(),
        ]);
    }

    #[Route('/message/edit/{id}', name: 'app_help_request_message_edit', methods: ['GET', 'POST'])]
    public function editMessage(Message $message, Request $request, EntityManagerInterface $entityManager): Response
    {
        if ($message->getSender() !== $this->getUser()) {
            $this->addFlash('error', 'You can only edit your own messages.');
            return $this->redirectToRoute('app_help_request_chat', ['id' => $message->getSession()->getId()]);
        }

        $form = $this->createForm(MessageType::class, $message);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->flush();
            $this->addFlash('success', 'Message updated.');
            return $this->redirectToRoute('app_help_request_chat', ['id' => $message->getSession()->getId()]);
        }

        return $this->render('help_request/edit_message.html.twig', [
            'message' => $message,
            'form' => $form,
        ]);
    }

    #[Route('/close/{id}', name: 'app_help_request_close', methods: ['POST'])]
    public function close(Session $session, Request $request, AssistanceService $assistanceService, GeminiService $geminiService, NotificationService $notificationService): Response
    {
        if (!$this->isCsrfTokenValid('close_session_' . $session->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Invalid security token.');
            return $this->redirectToRoute('app_help_request_chat', ['id' => $session->getId()]);
        }

        /** @var \App\Entity\User $user */
        $user = $this->getUser();
        $helpRequest = $session->getHelpRequest();
        if (!$helpRequest) {
            $this->addFlash('error', 'Help request not found.');
            return $this->redirectToRoute('app_help_request_chat', ['id' => $session->getId()]);
        }
        $student = $helpRequest->getStudent();

        if ($student !== $user && $session->getTutor() !== $user) {
            $this->addFlash('error', 'Only session participants can close.');
            return $this->redirectToRoute('app_help_request_chat', ['id' => $session->getId()]);
        }

        $reason = $request->request->get('close_reason', 'RESOLVED');

        if (!in_array($reason, ['RESOLVED', 'CANCELLED', 'REPORTED'])) {
            $reason = 'RESOLVED';
        }

        $helpRequest->setCloseReason($reason);

        if ($reason === 'REPORTED') {
            $helpRequest->setIsTicket(true);
        }

        // AI Session Summary
        $summary = null;
        $messageTexts = [];
        foreach ($session->getMessages() as $msg) {
            /** @var \App\Entity\User $msgSender */
            $msgSender = $msg->getSender();
            $senderName = $msgSender->getFullName() ?? $msgSender->getEmail();
            $messageTexts[] = $senderName . ': ' . $msg->getContent();
        }
        if (!empty($messageTexts)) {
            $summary = $geminiService->summarizeSession($messageTexts);
            $session->setSummary($summary);
        }

        if ($reason === 'RESOLVED') {
            $assistanceService->closeSession($session);

            // Email notification
            $notificationService->notifySessionClosed($session, $summary);

            $this->addFlash('success', 'Session closed. Bounty transferred. Please rate your tutor.');
            return $this->redirectToRoute('app_help_request_review', ['id' => $session->getId()]);
        }

        $session->setIsActive(false);
        $session->setEndedAt(new \DateTimeImmutable());
        $helpRequest->setStatus('CLOSED');
        $assistanceService->getEntityManager()->flush();

        // Email notification for cancel/report too
        $notificationService->notifySessionClosed($session, $summary);

        if ($reason === 'REPORTED') {
            $this->addFlash('warning', 'Session reported. An admin will review this.');
        } else {
            $this->addFlash('info', 'Session cancelled.');
        }

        return $this->redirectToRoute('app_help_request_index');
    }

    #[Route('/review/{id}', name: 'app_help_request_review', methods: ['GET', 'POST'])]
    public function review(Session $session, Request $request, EntityManagerInterface $entityManager): Response
    {
        if ($session->getHelpRequest()->getStudent() !== $this->getUser()) {
            return $this->redirectToRoute('app_help_request_index');
        }

        if ($session->getReview()) {
            $this->addFlash('info', 'You have already reviewed this session.');
            return $this->redirectToRoute('app_help_request_index');
        }

        $review = new Review();
        $form = $this->createForm(ReviewType::class, $review);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $review->setSession($session);
            $entityManager->persist($review);
            $entityManager->flush();

            $this->addFlash('success', 'Thank you for your feedback!');
            return $this->redirectToRoute('app_help_request_index');
        }

        return $this->render('help_request/review.html.twig', [
            'session' => $session,
            'form' => $form->createView(),
            'ai_summary' => $session->getSummary(),
        ]);
    }

    // ==================== COMMUNITY FEED ====================

    #[Route('/community', name: 'app_community_feed', methods: ['GET', 'POST'])]
    public function communityFeed(
        Request $request,
        EntityManagerInterface $entityManager,
        \App\Repository\CommunityPostRepository $postRepo,
        \App\Repository\PostCommentRepository $commentRepo,
        \App\Repository\PostReactionRepository $reactionRepo,
        PaginatorInterface $paginator,
        MLService $mlService,
        NotificationService $notificationService
    ): Response {
        $post = new \App\Entity\CommunityPost();
        $form = $this->createForm(\App\Form\CommunityPostType::class, $post);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {

            // Toxicity Check (MLService)
            $fullContent = $post->getContent();
            $toxicityResult = $mlService->analyzeToxicity($fullContent);

            if ($toxicityResult['isToxic']) {
                $this->addFlash('error', 'Your post contains inappropriate content and has been rejected.');

                // Notify Admin
                /** @var \App\Entity\User $loggedUser */
                $loggedUser = $this->getUser();
                if ($loggedUser) {
                    $notificationService->notifyAdminOfToxicContent(
                        $loggedUser,
                        (string) $fullContent,
                        "Community Post Attempt",
                        (string) $toxicityResult['source']
                    );
                }

                return $this->redirectToRoute('app_community_feed');
            }

            /** @var User|null $loggedUser */
            $loggedUser = $this->getUser();
            $post->setAuthor($loggedUser);
            $entityManager->persist($post);
            $entityManager->flush();

            $this->addFlash('success', 'Post published!');
            return $this->redirectToRoute('app_community_feed');
        }

        $communityPosts = $postRepo->findRecentPosts(100);
        /** @var \App\Entity\User $user */
        $user = $this->getUser();

        // Build feed items with comments and reactions
        $feedItems = [];
        foreach ($communityPosts as $p) {
            $postId = (int) $p->getId();
            $userId = (int) $user->getId();
            $reactions = $reactionRepo->getReactionCounts($postId);
            $userReaction = $reactionRepo->findUserReaction($userId, $postId);
            $comments = $commentRepo->findByPost($postId);

            $feedItems[] = [
                'post' => $p,
                'reactions' => $reactions,
                'userReaction' => $userReaction ? $userReaction->getType() : null,
                'totalReactions' => array_sum($reactions),
                'comments' => $comments,
                'commentCount' => count($comments),
            ];
        }

        // Filter
        $feedFilter = $request->query->get('feed_filter', 'all');
        if ($feedFilter !== 'all') {
            $feedItems = array_filter(
                $feedItems,
                fn($item) =>
                $item['post']->getType() === $feedFilter
            );
        }

        // Paginate feed items
        $paginatedFeed = $paginator->paginate($feedItems, $request->query->getInt('page', 1), 10);

        return $this->render('help_request/community_feed.html.twig', [
            'form' => $form,
            'feed_items' => $paginatedFeed,
            'feed_filter' => $feedFilter,
            'post_count' => count($communityPosts),
        ]);
    }

    #[Route('/community/react/{id}/{type}', name: 'app_community_react', methods: ['POST'])]
    public function reactToPost(
        \App\Entity\CommunityPost $post,
        string $type,
        Request $request,
        EntityManagerInterface $entityManager,
        \App\Repository\PostReactionRepository $reactionRepo
    ): Response {
        if (!$this->isCsrfTokenValid('react_post_' . $post->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Invalid security token.');
            return $this->redirectToRoute('app_community_feed');
        }

        $allowed = ['like', 'love', 'insightful', 'funny', 'support'];
        if (!in_array($type, $allowed)) {
            $this->addFlash('error', 'Invalid reaction type.');
            return $this->redirectToRoute('app_community_feed');
        }

        /** @var \App\Entity\User $user */
        $user = $this->getUser();
        $existing = $reactionRepo->findUserReaction((int) $user->getId(), (int) $post->getId());

        if ($existing) {
            if ($existing->getType() === $type) {
                $entityManager->remove($existing);
            } else {
                $existing->setType($type);
            }
        } else {
            $reaction = new \App\Entity\PostReaction();
            $reaction->setUser($user);
            $reaction->setPost($post);
            $reaction->setType($type);
            $entityManager->persist($reaction);
        }

        $entityManager->flush();
        return $this->redirectToRoute('app_community_feed');
    }

    #[Route('/community/comment/{id}', name: 'app_community_comment', methods: ['POST'])]
    public function addComment(
        \App\Entity\CommunityPost $post,
        Request $request,
        EntityManagerInterface $entityManager
    ): Response {
        if (!$this->isCsrfTokenValid('comment_post_' . $post->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Invalid security token.');
            return $this->redirectToRoute('app_community_feed');
        }

        $content = trim($request->request->get('comment_content', ''));
        if (empty($content)) {
            $this->addFlash('error', 'Comment cannot be empty.');
            return $this->redirectToRoute('app_community_feed');
        }

        if (mb_strlen($content) > 1000) {
            $this->addFlash('error', 'Comment too long (max 1000 chars).');
            return $this->redirectToRoute('app_community_feed');
        }

        $comment = new \App\Entity\PostComment();
        /** @var User|null $loggedUser */
        $loggedUser = $this->getUser();
        $comment->setAuthor($loggedUser);
        $comment->setPost($post);
        $comment->setContent($content);
        $entityManager->persist($comment);
        $entityManager->flush();

        $this->addFlash('success', 'Comment posted!');
        return $this->redirectToRoute('app_community_feed');
    }

    #[Route('/community/comment/delete/{id}', name: 'app_community_comment_delete', methods: ['POST'])]
    public function deleteComment(
        \App\Entity\PostComment $comment,
        Request $request,
        EntityManagerInterface $entityManager
    ): Response {
        if (!$this->isCsrfTokenValid('delete_comment_' . $comment->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Invalid security token.');
            return $this->redirectToRoute('app_community_feed');
        }

        if ($comment->getAuthor() !== $this->getUser()) {
            $this->addFlash('error', 'You can only delete your own comments.');
            return $this->redirectToRoute('app_community_feed');
        }
        $entityManager->remove($comment);
        $entityManager->flush();
        $this->addFlash('success', 'Comment deleted.');
        return $this->redirectToRoute('app_community_feed');
    }

    #[Route('/community/report/{id}', name: 'app_community_report', methods: ['POST'])]
    public function reportPost(
        \App\Entity\CommunityPost $post,
        Request $request,
        EntityManagerInterface $entityManager
    ): Response {
        if (!$this->isCsrfTokenValid('report_post_' . $post->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Invalid security token.');
            return $this->redirectToRoute('app_community_feed');
        }

        $reason = $request->request->get('report_reason', 'inappropriate');
        $details = trim($request->request->get('report_details', ''));

        $allowed = ['inappropriate', 'spam', 'harassment', 'misinformation', 'hate_speech', 'other'];
        if (!in_array($reason, $allowed)) {
            $reason = 'inappropriate';
        }

        if (mb_strlen($details) > 500) {
            $details = mb_substr($details, 0, 500);
        }

        $report = new \App\Entity\PostReport();
        /** @var User|null $loggedUser */
        $loggedUser = $this->getUser();
        $report->setReporter($loggedUser);
        $report->setPost($post);
        $report->setReason((string) $reason);
        if ($details) {
            $report->setDetails($details);
        }
        $entityManager->persist($report);
        $entityManager->flush();

        $this->addFlash('success', 'Report submitted to admins. Thank you for keeping the community safe.');
        return $this->redirectToRoute('app_community_feed');
    }

    #[Route('/community/delete/{id}', name: 'app_community_delete', methods: ['POST'])]
    public function deleteCommunityPost(
        \App\Entity\CommunityPost $post,
        Request $request,
        EntityManagerInterface $entityManager
    ): Response {
        if (!$this->isCsrfTokenValid('delete_post_' . $post->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Invalid security token.');
            return $this->redirectToRoute('app_community_feed');
        }

        if ($post->getAuthor() !== $this->getUser()) {
            $this->addFlash('error', 'You can only delete your own posts.');
            return $this->redirectToRoute('app_community_feed');
        }

        // Delete related comments, reactions, reports
        $entityManager->createQuery('DELETE FROM App\Entity\PostComment c WHERE c.post = :post')
            ->setParameter('post', $post)->execute();
        $entityManager->createQuery('DELETE FROM App\Entity\PostReaction r WHERE r.post = :post')
            ->setParameter('post', $post)->execute();
        $entityManager->createQuery('DELETE FROM App\Entity\PostReport r WHERE r.post = :post')
            ->setParameter('post', $post)->execute();

        $entityManager->remove($post);
        $entityManager->flush();

        $this->addFlash('success', 'Post deleted.');
        return $this->redirectToRoute('app_community_feed');
    }
}
