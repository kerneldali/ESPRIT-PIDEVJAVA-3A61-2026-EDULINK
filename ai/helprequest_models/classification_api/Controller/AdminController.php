<?php

namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpFoundation\JsonResponse;
use App\Service\AiMicroservice;
use App\Entity\User;
use App\Entity\Cours;
use Symfony\Component\Routing\Attribute\Route;
use Doctrine\ORM\EntityManagerInterface;

class AdminController extends AbstractController
{
    private AiMicroservice $aiMicroservice;

    public function __construct(AiMicroservice $aiMicroservice)
    {
        $this->aiMicroservice = $aiMicroservice;
    }

    #[Route('/admin/dashboard', name: 'admin_dashboard')]
    public function dashboard(
        \App\Repository\MatiereRepository $matiereRepo,
        \App\Repository\CoursRepository $coursRepo,
        \App\Repository\UserRepository $userRepo,
        \App\Repository\TransactionRepository $transactionRepo
    ): Response {
        $pendingMatieres = $matiereRepo->findBy(['status' => 'PENDING']);
        $pendingCourses = $coursRepo->findBy(['status' => 'PENDING']);

        $userStats = $userRepo->getUserStatistics();
        $totalXp = $transactionRepo->getTotalPointsEarned();
        $dailyPoints = $transactionRepo->getPointsEarnedToday();
        $activeUsers = $transactionRepo->countActiveUsersToday();
        $recentTransactions = $transactionRepo->findBy([], ['date' => 'DESC'], 5);

        // Check AI Microservice health (silent check)
        $aiStatus = true; // Could be a real ping if needed

        return $this->render('admin/dashboard.html.twig', [
            'pendingMatieres' => $pendingMatieres,
            'pendingCourses' => $pendingCourses,
            'userStats' => $userStats,
            'totalXp' => $totalXp,
            'dailyPoints' => $dailyPoints,
            'activeUsers' => $activeUsers,
            'recentTransactions' => $recentTransactions,
            'aiStatus' => $aiStatus,
        ]);
    }

    #[Route('/admin/events', name: 'admin_events')]
    public function events(Request $request, \App\Repository\EventRepository $eventRepository): Response
    {
        $filters = [
            'search' => $request->query->get('q'),
            'dateFilter' => $request->query->get('filter'),
        ];
        $sort = (string) $request->query->get('sort', 'dateStart');
        $direction = (string) $request->query->get('direction', 'DESC');

        $events = $eventRepository->findByFilter($filters, $sort, $direction);
        $stats = $eventRepository->getEventStats();

        return $this->render('admin/events.html.twig', [
            'events' => $events,
            'stats' => $stats,
            'current_filters' => $filters,
            'current_sort' => $sort,
            'current_direction' => $direction,
        ]);
    }

    #[Route('/admin/events/preview', name: 'admin_event_preview')]
    public function adminEventPreview(Request $request, \App\Repository\EventRepository $eventRepository): Response
    {
        $filter = $request->query->get('filter', 'all');
        $search = $request->query->get('search');

        $qb = $eventRepository->createQueryBuilder('e')
            ->orderBy('e.dateStart', 'DESC');

        if ($search) {
            $qb->andWhere('e.title LIKE :search OR e.description LIKE :search')
                ->setParameter('search', '%' . $search . '%');
        }

        if ($filter === 'upcoming') {
            $qb->andWhere('e.dateStart > :now')
                ->setParameter('now', new \DateTime());
        }

        $events = $qb->getQuery()->getResult();

        return $this->render('admin/event_preview.html.twig', [
            'events' => $events,
            'current_filter' => $filter,
        ]);
    }

    #[Route('/admin/challenges', name: 'admin_challenges')]
    public function challenges(): Response
    {
        return $this->redirectToRoute('app_challenge_admin_index');
    }

    // Assistance route is handled by App\Controller\Admin\AssistanceController


    // ─────────────────────────────────────────────
    // AI MICROSERVICE INTEGRATION
    // ─────────────────────────────────────────────

    /**
     * Consolidates student metric gathering to ensure consistency between UI and AI.
     * 
     * @return array<string, int|float|string>
     */
    private function getStudentMetricFeatures(User $user, EntityManagerInterface $em): array
    {
        $conn = $em->getConnection();
        $uid = $user->getId();

        // 1. Forum Interactions (Posts + Comments + Reactions)
        $postCount = (int) $em->createQuery('SELECT COUNT(p.id) FROM App\Entity\CommunityPost p WHERE p.author = :u')
            ->setParameter('u', $user)->getSingleScalarResult();

        $commentCount = (int) $em->createQuery('SELECT COUNT(c.id) FROM App\Entity\PostComment c WHERE c.author = :u')
            ->setParameter('u', $user)->getSingleScalarResult();

        $reactionCount = (int) $em->createQuery('SELECT COUNT(r.id) FROM App\Entity\PostReaction r WHERE r.user = :u')
            ->setParameter('u', $user)->getSingleScalarResult();

        // 2. Help Requests
        $helpCount = (int) $em->createQuery('SELECT COUNT(h.id) FROM App\Entity\HelpRequest h WHERE h.student = :u')
            ->setParameter('u', $user)->getSingleScalarResult();

        // 3. Assignments (Completed Challenges)
        $assignmentsCompleted = (int) $em->createQuery('SELECT COUNT(uc.id) FROM App\Entity\UserChallenge uc WHERE uc.user = :u AND uc.status = :s')
            ->setParameter('u', $user)
            ->setParameter('s', \App\Entity\UserChallenge::STATUS_COMPLETED)
            ->getSingleScalarResult();

        // 4. Quiz Average — read from enrollment.quiz_average_score (saved when student submits quiz)
        $enrollment = $user->getEnrollments()->first() ?: null;
        $quizAverage = $enrollment ? (float)$enrollment->getQuizAverageScore() : 0.0;

        // 5. Course Progress & Category
        $courseProgress = $enrollment ? (float)$enrollment->getProgress() : 0.0;
        $categoryName = ($enrollment && $enrollment->getCours() && $enrollment->getCours()->getMatiere())
            ? $enrollment->getCours()->getMatiere()->getName()
            : 'General';

        // 6. Video Watch % — ratio of completed resources to total course resources
        $videoWatchPercent = 0.0;
        try {
            if ($enrollment && $enrollment->getCours()) {
                $coursId = $enrollment->getCours()->getId();
                $totalResources = (int) $em->createQuery(
                    'SELECT COUNT(r.id) FROM App\Entity\Resource r WHERE r.cours = :cid'
                )->setParameter('cid', $enrollment->getCours())->getSingleScalarResult();
                $completedCount = count($enrollment->getCompletedResources());
                $videoWatchPercent = $totalResources > 0
                    ? round(($completedCount / $totalResources) * 100, 1)
                    : 0.0;
            }
        } catch (\Exception $e) {
            $videoWatchPercent = 0.0;
        }

        // 7. Login Frequency (logins in last 7 days) & Avg Session Minutes from user_sessions table
        $loginFreq = 0.0;
        $avgSession = 30.0; // default fallback
        try {
            $sessionData = $conn->executeQuery(
                'SELECT COUNT(*) as login_count,
                        AVG(TIMESTAMPDIFF(MINUTE, login_at, IFNULL(logout_at, NOW()))) as avg_minutes
                 FROM user_sessions
                 WHERE user_id = :uid AND login_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)',
                ['uid' => $uid]
            )->fetchAssociative();

            $loginFreq  = (float) ($sessionData['login_count'] ?? 0);
            $avgSession = (float) ($sessionData['avg_minutes'] ?? 30.0);
            if ($avgSession <= 0) $avgSession = 30.0;
        } catch (\Exception $e) {
            // user_sessions table may be empty
        }

        // Ensure no NaN/Null and cast to explicit types
        return [
            'student_id'                => (int) ($uid ?: 1),
            'course_category'           => (string) $categoryName,
            'login_frequency_per_week'  => (float) $loginFreq,
            'avg_session_minutes'       => (float) $avgSession,
            'course_progress_percent'   => (float) $courseProgress,
            'assignments_completed'     => (float) $assignmentsCompleted,
            'quiz_average_score'        => (float) $quizAverage,
            'forum_interactions'        => (float) ($postCount + $commentCount + $reactionCount),
            'help_requests'             => (float) $helpCount,
            'video_watch_percent'       => (float) $videoWatchPercent,
            'device_type'               => 'Desktop'
        ];
    }

    #[Route('/admin/api/student-indicators/{id}', name: 'admin_api_student_indicators', methods: ['GET'])]
    public function studentIndicators(User $user, EntityManagerInterface $em): JsonResponse
    {
        try {
            $features = $this->getStudentMetricFeatures($user, $em);
            return new JsonResponse([
                'status' => 'success',
                'data'   => $features,
                'explanation' => 'These academic and engagement indicators are analyzed by our AI model to estimate dropout probability.'
            ]);
        } catch (\Exception $e) {
            return new JsonResponse(['status' => 'error', 'message' => $e->getMessage()], 500);
        }
    }

    #[Route('/admin/api/predict-dropout/{id}', name: 'admin_api_predict_dropout', methods: ['POST', 'GET'])]
    public function predictDropoutAction(User $user, EntityManagerInterface $em): JsonResponse
    {
        try {
            $features = $this->getStudentMetricFeatures($user, $em);
            $result = $this->aiMicroservice->predictDropout($features);
            
            if (isset($result['error'])) {
                throw new \Exception($result['error']);
            }

            return new JsonResponse($result);
        } catch (\Exception $e) {
            // Log for backend
            error_log("AI prediction failed for user " . $user->getId() . ": " . $e->getMessage());
            
            return new JsonResponse([
                'status' => 'error',
                'message' => 'The AI Microservice is currently unavailable or returned an error.',
                'detail' => $e->getMessage()
            ], 503);
        }
    }

    /**
     * Consolidates course stats gathering to ensure consistency between UI and AI.
     * 
     * @return array<string, int|float|string>
     */
    private function getCourseMetricStats(Cours $course): array
    {
        $enrollmentCount = count($course->getEnrollments());
        
        $totalProgress = 0;
        foreach ($course->getEnrollments() as $e) {
            $totalProgress += $e->getProgress();
        }
        $avgProgress = $enrollmentCount > 0 ? ($totalProgress / $enrollmentCount) : 0;

        return [
            'enrollment_count' => (int) $enrollmentCount,
            'resource_count'   => (int) count($course->getResources()),
            'completion_rate'  => (float) $avgProgress
        ];
    }

    #[Route('/admin/api/course-performance-basis/{id}', name: 'admin_api_course_performance_basis', methods: ['GET'])]
    public function coursePerformanceBasis(Cours $course): JsonResponse
    {
        try {
            $stats = $this->getCourseMetricStats($course);
            return new JsonResponse([
                'status' => 'success',
                'data'   => $stats,
                'category' => $course->getMatiere() ? $course->getMatiere()->getName() : 'N/A'
            ]);
        } catch (\Exception $e) {
            return new JsonResponse(['status' => 'error', 'message' => $e->getMessage()], 500);
        }
    }

    #[Route('/admin/api/analyze-course/{id}', name: 'admin_api_analyze_course', methods: ['POST', 'GET'])]
    public function analyzeCourseAction(Cours $course): JsonResponse
    {
        try {
            $stats = $this->getCourseMetricStats($course);
            $result = $this->aiMicroservice->analyzeCourse($stats);
            
            if (isset($result['error'])) {
                throw new \Exception($result['message'] ?? 'Service unavailable');
            }

            $result['input_stats'] = $stats;
            return new JsonResponse($result);
        } catch (\Exception $e) {
            error_log("AI course analysis failed for course " . $course->getId() . ": " . $e->getMessage());
            return new JsonResponse([
                'status' => 'error',
                'message' => 'The AI Microservice is currently unavailable.',
                'detail' => $e->getMessage()
            ], 503);
        }
    }

    /**
     * Returns live stats for a course before the AI analysis.
     */
    #[Route('/admin/api/course-stats/{id}', name: 'admin_api_course_stats', methods: ['GET'])]
    public function courseStats(Cours $course): JsonResponse
    {
        $enrollmentCount = count($course->getEnrollments());
        $enrollments = $course->getEnrollments();
        $totalProgress = 0;
        foreach ($enrollments as $e) {
            $totalProgress += $e->getProgress();
        }
        $avgProgress = $enrollmentCount > 0 ? round($totalProgress / $enrollmentCount, 1) : 0;

        return new JsonResponse([
            'title'            => $course->getTitle(),
            'category'         => $course->getMatiere() ? $course->getMatiere()->getName() : 'N/A',
            'enrollment_count' => $enrollmentCount,
            'avg_completion'   => $avgProgress,
            'resource_count'   => count($course->getResources()),
        ]);
    }
}
