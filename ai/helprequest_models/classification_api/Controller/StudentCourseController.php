<?php

namespace App\Controller;

use App\Entity\Cours;
use App\Entity\Enrollment;
use App\Entity\User;
use App\Repository\CoursRepository;
use App\Repository\EnrollmentRepository;
use Doctrine\ORM\EntityManagerInterface;
use Knp\Component\Pager\PaginatorInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/student/courses')]
class StudentCourseController extends AbstractController
{
    #[Route('/', name: 'app_student_courses')]
    public function index(
        \Symfony\Component\HttpFoundation\Request $request,
        CoursRepository $coursRepo,
        EnrollmentRepository $enrollmentRepo,
        EntityManagerInterface $entityManager,
        PaginatorInterface $paginator
    ): Response {
        $user = $this->getUser();
        if (!$user)
            return $this->redirectToRoute('app_login');

        // Search & Sort Params
        $query = $request->query->get('q');
        $sort = $request->query->get('sort', 'newest'); // default to newest
        $level = $request->query->get('level'); // Filter by level

        // 1. Fetch "My Learning" (Enrollments)
        // Note: Better to do this via custom repository method for searching enrollments too, but for now we focus on Marketplace searching.
        $myEnrollments = $enrollmentRepo->findBy(['student' => $user]);

        // 2. Fetch "Marketplace" (Approved courses) with Search/Sort
        // 2. Fetch "Marketplace" Data
        $categorySort = $request->query->get('categorySort', 'alpha');
        $matiereRepo = $entityManager->getRepository(\App\Entity\Matiere::class);
        $matiereQB = $matiereRepo->createQueryBuilder('m')
            ->where('m.status = :status')
            ->setParameter('status', 'APPROVED');

        if ($sort == 'alpha_asc' || $categorySort == 'alpha') {
            $matiereQB->orderBy('m.name', 'ASC');
        } else {
            $matiereQB->orderBy('m.createdAt', 'DESC');
        }
        $matieres = $matiereQB->getQuery()->getResult();

        $selectedCategoryId = $request->query->get('category');
        $selectedCategory = null;
        $marketplaceCourses = [];

        $qb = $coursRepo->createQueryBuilder('c')
            ->where('c.status = :status')
            ->setParameter('status', 'APPROVED');

        // If a category is selected, filter by it
        if ($selectedCategoryId) {
            $selectedCategory = $entityManager->getRepository(\App\Entity\Matiere::class)->find($selectedCategoryId);
            if ($selectedCategory) {
                $qb->andWhere('c.matiere = :matiere')
                    ->setParameter('matiere', $selectedCategory);
            }
        }

        // Apply Search (Global or within Category)
        if ($query) {
            $qb->andWhere('c.title LIKE :query OR c.description LIKE :query')
                ->setParameter('query', '%' . $query . '%');
        }

        // Apply Level Filter
        if ($level) {
            $qb->andWhere('c.level = :level')
                ->setParameter('level', $level);
        }

        // Apply Sort
        switch ($sort) {
            case 'reward_high':
                $qb->orderBy('c.xp', 'DESC');
                break;
            case 'reward_low':
                $qb->orderBy('c.xp', 'ASC');
                break;
            case 'alpha_asc':
                $qb->orderBy('c.title', 'ASC');
                break;
            case 'price_high':
                $qb->orderBy('c.pricePoints', 'DESC');
                break;
            case 'price_low':
                $qb->orderBy('c.pricePoints', 'ASC');
                break;
            case 'newest':
            default:
                $qb->orderBy('c.createdAt', 'DESC');
                break;
        }

        // Contextual Pagination
        // Remove 'sort' from query to prevent KnpPaginator from interpreting it as a DB field
        $paginatorOptions = [
            'defaultSortFieldName' => null,
            'defaultSortDirection' => 'asc',
            'sortFieldParameterName' => '_knp_sort', // Use a non-conflicting param name
            'sortDirectionParameterName' => '_knp_dir',
        ];

        if (!$selectedCategoryId && !$query) {
            // Main view is "All Categories" - Paginate Categories
            $pagination = $paginator->paginate(
                $matiereQB->getQuery(),
                $request->query->getInt('page', 1),
                12,
                $paginatorOptions
            );
        } else {
            // Searching or browsing a category - Paginate Courses
            $pagination = $paginator->paginate(
                $qb->getQuery(),
                $request->query->getInt('page', 1),
                10,
                $paginatorOptions
            );
        }

        // Also filter categories if searching globally
        if ($query && !$selectedCategoryId) {
            $matiereQB->andWhere('m.name LIKE :query')
                ->setParameter('query', '%' . $query . '%');
            $matieres = $matiereQB->getQuery()->getResult();
        }

        // 3. Fetch "Creator Studio" (All content I created)
        // Data for Creator Studio (strictly filtered by current user)
        $myCreatedCourses = $coursRepo->findBy(['author' => $user], ['createdAt' => 'DESC']);
        $myCreatedMatieres = $entityManager->getRepository(\App\Entity\Matiere::class)->findBy(['creator' => $user], ['id' => 'DESC']);
        $myCreatedResources = $entityManager->getRepository(\App\Entity\Resource::class)->findBy(['author' => $user], ['id' => 'DESC']);

        // 4. PASS ALL VARIABLES TO THE VIEW
        $currentTab = $request->query->get('tab', 'my-courses'); // Default based on logic, or passed param
        // If searching or browsing category, default to marketplace
        if ($query || $selectedCategoryId) {
            $currentTab = 'marketplace';
        }

        return $this->render('student/courses.html.twig', [
            'myEnrollments' => $myEnrollments,
            'pagination' => $pagination,
            'matieres' => (!$selectedCategoryId && !$query) ? $pagination : $matieres,
            'selectedCategory' => $selectedCategory,
            'myCreatedCourses' => $myCreatedCourses,
            'myCreatedMatieres' => $myCreatedMatieres,
            'myCreatedResources' => $myCreatedResources,
            'searchQuery' => $query,
            'currentSort' => $sort,
            'currentLevel' => $level,
            'currentTab' => $currentTab
        ]);
    }

    #[Route('/enroll/{id}', name: 'app_student_enroll')]
    public function enroll(Cours $cours, EntityManagerInterface $em, EnrollmentRepository $enrollRepo): Response
    {
        $user = $this->getUser();
        if (!$user)
            return $this->redirectToRoute('app_login');

        // Check if already enrolled
        $existing = $enrollRepo->findOneBy(['student' => $user, 'cours' => $cours]);

        if (!$existing) {
            $enrollment = new Enrollment();
            /** @var User $loggedUser */
            $loggedUser = $this->getUser();
            $enrollment->setStudent($loggedUser);
            $enrollment->setCours($cours);
            $enrollment->setEnrolledAt(new \DateTimeImmutable());
            $enrollment->setProgress(0);

            $em->persist($enrollment);
            $em->flush();
            $this->addFlash('success', 'Enrolled!');
        } else {
            $this->addFlash('warning', 'Already enrolled.');
        }

        return $this->redirectToRoute('app_student_courses');
    }

    #[Route('/{id}', name: 'app_student_course_detail')]
    public function show(Cours $cours, EnrollmentRepository $enrollRepo): Response
    {
        /** @var \App\Entity\User|null $user */
        $user = $this->getUser();
        $enrollment = $user ? $enrollRepo->findOneBy(['student' => $user, 'cours' => $cours]) : null;

        return $this->render('student/course_detail.html.twig', [
            'cours' => $cours,
            'isEnrolled' => $enrollment !== null,
            'enrollment' => $enrollment
        ]);
    }

    #[Route('/complete/{id}', name: 'app_student_complete_course', methods: ['POST'])]
    public function complete(Cours $cours, EntityManagerInterface $em, EnrollmentRepository $enrollRepo, \App\Service\BadgeService $badgeService): Response
    {
        /** @var \App\Entity\User|null $user */
        $user = $this->getUser();
        if (!$user)
            return $this->redirectToRoute('app_login');

        $enrollment = $enrollRepo->findOneBy(['student' => $user, 'cours' => $cours]);
        if (!$enrollment) {
            $this->addFlash('error', 'Not enrolled in this course.');
            return $this->redirectToRoute('app_student_courses');
        }

        $resources = $cours->getResources()->filter(fn($r) => $r->getStatus() === 'APPROVED');
        $completedCount = count($enrollment->getCompletedResources());
        $totalCount = count($resources);

        if ($completedCount < $totalCount) {
            $this->addFlash('warning', 'Please finish all resources before completing the course.');
            return $this->redirectToRoute('app_student_course_detail', ['id' => $cours->getId()]);
        }

        if ($enrollment->getCompletedAt() === null) {
            $enrollment->setProgress(100);
            $enrollment->setCompletedAt(new \DateTimeImmutable());

            $reward = ($cours->getXp() ?? 0);
            if ($reward > 0) {
                // Add to Wallet/XP (unified)
                $user->setWalletBalance($user->getWalletBalance() + $reward);
    
                // Create Transaction
                $transaction = new \App\Entity\Transaction();
                $transaction->setUser($user);
                $transaction->setAmount($reward);
                $transaction->setType('COURSE_COMPLETION');
                $transaction->setDate(new \DateTime());
                $em->persist($transaction);
            }

            // Check for new badges after XP change
            $newBadges = $badgeService->checkBadges($user);

            $em->flush();

            $badgeMsg = '';
            if (!empty($newBadges)) {
                $badgeNames = array_map(fn($badge) => $badge->getName(), $newBadges);
                $badgeMsg = ' 🏅 New badge(s): ' . implode(', ', $badgeNames) . '!';
            }
            $this->addFlash('success', 'Course completed! You earned ' . $reward . ' XP.' . $badgeMsg);
        }

        return $this->redirectToRoute('app_student_course_detail', ['id' => $cours->getId()]);
    }

    #[Route('/resource/{id}/complete', name: 'app_student_complete_resource', methods: ['POST'])]
    public function completeResource(\App\Entity\Resource $resource, EntityManagerInterface $em, EnrollmentRepository $enrollRepo): Response
    {
        $user = $this->getUser();
        if (!$user)
            return $this->json(['success' => false, 'message' => 'Auth required'], 403);

        $cours = $resource->getCours();
        $enrollment = $enrollRepo->findOneBy(['student' => $user, 'cours' => $cours]);

        if (!$enrollment) {
            return $this->json(['success' => false, 'message' => 'Not enrolled'], 404);
        }

        // Add to completed
        $enrollment->addCompletedResource((int) $resource->getId());

        // Update progress percentage
        $resources = [];
        if ($cours) {
            $resources = $cours->getResources()->filter(fn($r) => $r->getStatus() === 'APPROVED');
        }
        $total = count($resources);
        $done = count($enrollment->getCompletedResources());

        $percentage = $total > 0 ? floor(($done / $total) * 100) : 100;
        $enrollment->setProgress((int) $percentage);

        $em->flush();

        return $this->redirectToRoute('app_student_course_detail', ['id' => $cours ? $cours->getId() : null]);
    }
}