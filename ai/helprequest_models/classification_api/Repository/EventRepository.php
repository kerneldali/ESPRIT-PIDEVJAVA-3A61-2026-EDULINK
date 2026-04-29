<?php

namespace App\Repository;

use App\Entity\Event;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Event>
 */
class EventRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Event::class);
    }

    public function getEventStats(): array
    {
        $conn = $this->getEntityManager()->getConnection();

        $row = $conn->executeQuery("
            SELECT
                COUNT(*) AS totalEvents,
                SUM(CASE WHEN date_start >= NOW() THEN 1 ELSE 0 END) AS upcomingEvents,
                SUM(CASE WHEN date_end < NOW() THEN 1 ELSE 0 END) AS pastEvents,
                (SELECT COUNT(*) FROM reservation) AS totalReservations
            FROM event
        ")->fetchAssociative();

        return [
            'totalEvents' => (int) $row['totalEvents'],
            'upcomingEvents' => (int) $row['upcomingEvents'],
            'pastEvents' => (int) $row['pastEvents'],
            'totalReservations' => (int) $row['totalReservations'],
        ];
    }

    /** @return Event[] */
    public function findByFilter(array $filters = [], string $sort = 'dateStart', string $direction = 'DESC'): array
    {
        $qb = $this->createQueryBuilder('e')
            ->leftJoin('e.organizer', 'o')
            ->addSelect('o');

        if (!empty($filters['search'])) {
            $qb->andWhere('e.title LIKE :search OR o.email LIKE :search OR o.fullName LIKE :search')
                ->setParameter('search', '%' . $filters['search'] . '%');
        }

        if (!empty($filters['dateFilter'])) {
            $now = new \DateTime();
            if ($filters['dateFilter'] === 'upcoming') {
                $qb->andWhere('e.dateStart >= :now')->setParameter('now', $now);
            } elseif ($filters['dateFilter'] === 'past') {
                $qb->andWhere('e.dateEnd < :now')->setParameter('now', $now);
            }
        }

        $allowedSorts = ['id', 'title', 'dateStart', 'maxCapacity'];
        if (in_array($sort, $allowedSorts)) {
            $qb->orderBy('e.' . $sort, strtoupper($direction) === 'ASC' ? 'ASC' : 'DESC');
        } else {
            $qb->orderBy('e.dateStart', 'DESC');
        }

        return $qb->getQuery()->getResult();
    }
}
