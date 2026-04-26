<?php

namespace App\Repository;

use App\Entity\HelpRequest;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<HelpRequest>
 *
 * @method HelpRequest|null find($id, $lockMode = null, $lockVersion = null)
 * @method HelpRequest|null findOneBy(array $criteria, array $orderBy = null)
 * @method HelpRequest[]    findAll()
 * @method HelpRequest[]    findBy(array $criteria, array $orderBy = null, $limit = null, $offset = null)
 */
class HelpRequestRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, HelpRequest::class);
    }

    /**
     * @return HelpRequest[] Returns an array of HelpRequest objects
     */
    public function findOpenRequests(array $filters = []): array
    {
        $qb = $this->createQueryBuilder('h')
            ->andWhere('h.status = :val')
            ->setParameter('val', 'OPEN')
            ->andWhere('h.isTicket = :isTicket')
            ->setParameter('isTicket', false);

        if (!empty($filters['exclude_student'])) {
            $qb->andWhere('h.student != :me')
                ->setParameter('me', $filters['exclude_student']);
        }

        if (!empty($filters['search'])) {
            $qb->andWhere('h.title LIKE :search OR h.description LIKE :search')
                ->setParameter('search', '%' . $filters['search'] . '%');
        }

        if (!empty($filters['min_bounty'])) {
            $qb->andWhere('h.bounty >= :min_bounty')
                ->setParameter('min_bounty', $filters['min_bounty']);
        }

        $sort = $filters['sort'] ?? 'newest';
        switch ($sort) {
            case 'oldest':
                $qb->orderBy('h.createdAt', 'ASC');
                break;
            case 'bounty_high':
                $qb->orderBy('h.bounty', 'DESC');
                break;
            case 'bounty_low':
                $qb->orderBy('h.bounty', 'ASC');
                break;
            default: // newest
                $qb->orderBy('h.createdAt', 'DESC');
        }

        return $qb->getQuery()->getResult();
    }

    public function findSupportTickets(int $limit = 50): array
    {
        return $this->createQueryBuilder('h')
            ->andWhere('h.isTicket = :isTicket')
            ->setParameter('isTicket', true)
            ->orderBy('h.createdAt', 'DESC')
            ->setMaxResults($limit)
            ->getQuery()
            ->getResult()
        ;
    }

    public function getAssistanceStats(): array
    {
        $conn = $this->getEntityManager()->getConnection();

        // Query 1: HelpRequest stats (consolidated from 6 queries)
        $hr = $conn->executeQuery("
            SELECT
                COUNT(*) AS totalRequests,
                SUM(CASE WHEN status = 'OPEN' THEN 1 ELSE 0 END) AS openRequests,
                SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS inProgressRequests,
                SUM(CASE WHEN status = 'CLOSED' THEN 1 ELSE 0 END) AS closedRequests,
                COALESCE(SUM(bounty), 0) AS totalBounty,
                SUM(CASE WHEN is_ticket = 1 THEN 1 ELSE 0 END) AS reportedTickets
            FROM help_request
        ")->fetchAssociative();

        // Query 2: Session, Message, Review stats (consolidated from 6 queries)
        $extra = $conn->executeQuery("
            SELECT
                (SELECT COUNT(*) FROM session) AS totalSessions,
                (SELECT COUNT(*) FROM session WHERE is_active = 1) AS activeSessions,
                (SELECT COUNT(*) FROM message) AS totalMessages,
                (SELECT COUNT(*) FROM message WHERE is_toxic = 1) AS toxicMessages,
                (SELECT COUNT(*) FROM review) AS totalReviews,
                (SELECT COALESCE(AVG(rating), 0) FROM review) AS avgRating
        ")->fetchAssociative();

        $totalRequests = (int) $hr['totalRequests'];
        $closedRequests = (int) $hr['closedRequests'];

        return [
            'totalRequests' => $totalRequests,
            'openRequests' => (int) $hr['openRequests'],
            'inProgressRequests' => (int) $hr['inProgressRequests'],
            'closedRequests' => $closedRequests,
            'totalBounty' => (int) $hr['totalBounty'],
            'reportedTickets' => (int) $hr['reportedTickets'],
            'totalSessions' => (int) $extra['totalSessions'],
            'activeSessions' => (int) $extra['activeSessions'],
            'totalMessages' => (int) $extra['totalMessages'],
            'toxicMessages' => (int) $extra['toxicMessages'],
            'resolutionRate' => $totalRequests > 0 ? round(($closedRequests / $totalRequests) * 100) : 0,
            'avgRating' => round((float) $extra['avgRating'], 1),
            'totalReviews' => (int) $extra['totalReviews'],
        ];
    }

    /**
     * Monthly request counts for the last 6 months (for line chart)
     */
    public function getMonthlyRequestCounts(): array
    {
        $conn = $this->getEntityManager()->getConnection();
        $sql = "SELECT DATE_FORMAT(created_at, '%Y-%m') as month, COUNT(*) as total
                FROM help_request
                WHERE created_at >= DATE_SUB(NOW(), INTERVAL 6 MONTH)
                GROUP BY month ORDER BY month ASC";
        return $conn->executeQuery($sql)->fetchAllAssociative();
    }

    /**
     * Category distribution (for doughnut chart) — raw SQL to avoid DTO hydration warning.
     */
    public function getCategoryDistribution(): array
    {
        return $this->getEntityManager()->getConnection()->executeQuery("
            SELECT COALESCE(category, 'Uncategorized') AS category, COUNT(*) AS total
            FROM help_request GROUP BY category ORDER BY total DESC
        ")->fetchAllAssociative();
    }

    /**
     * Resolution breakdown by close reason (for bar chart) — raw SQL to avoid DTO hydration warning.
     */
    public function getResolutionBreakdown(): array
    {
        return $this->getEntityManager()->getConnection()->executeQuery("
            SELECT COALESCE(close_reason, 'OPEN') AS reason, COUNT(*) AS total
            FROM help_request GROUP BY close_reason
        ")->fetchAllAssociative();
    }

    /**
     * Top tutors by completed sessions with avg rating — raw SQL to fix LEFT JOIN + GROUP BY warnings.
     */
    public function getTopTutors(int $limit = 5): array
    {
        return $this->getEntityManager()->getConnection()->executeQuery("
            SELECT u.id, u.full_name AS fullName, u.email,
                   COUNT(s.id) AS sessionCount,
                   COALESCE(AVG(r.rating), 0) AS avgRating
            FROM session s
            INNER JOIN user u ON s.tutor_id = u.id
            LEFT JOIN review r ON r.session_id = s.id
            WHERE s.is_active = 0
            GROUP BY u.id, u.full_name, u.email
            ORDER BY sessionCount DESC
            LIMIT :lim
        ", ['lim' => $limit])->fetchAllAssociative();
    }
}
