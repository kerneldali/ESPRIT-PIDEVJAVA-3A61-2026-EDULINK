<?php

namespace App\Repository;

use App\Entity\User;
use App\DTO\UserStatsDTO;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<User>
 */
class UserRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, User::class);
    }

    /**
     * Optimized: single query with DTO hydration (Doctrine Doctor fix).
     * Replaces 5 separate COUNT/SUM queries with 1 consolidated query.
     *
     * @return array Returns statistics about users
     */
    public function getUserStatistics(): array
    {
        $conn = $this->getEntityManager()->getConnection();

        $row = $conn->executeQuery("
            SELECT
                COUNT(*) AS totalUsers,
                SUM(CASE WHEN roles LIKE '%\"ROLE_STUDENT\"%' THEN 1 ELSE 0 END) AS totalStudents,
                SUM(CASE WHEN roles LIKE '%\"ROLE_TUTOR\"%' THEN 1 ELSE 0 END) AS totalTutors,
                SUM(CASE WHEN roles LIKE '%\"ROLE_ADMIN\"%' THEN 1 ELSE 0 END) AS totalAdmins,
                COALESCE(SUM(wallet_balance), 0) AS totalXp
            FROM user
        ")->fetchAssociative();

        $dto = new UserStatsDTO(
            totalUsers: (int) $row['totalUsers'],
            totalStudents: (int) $row['totalStudents'],
            totalTutors: (int) $row['totalTutors'],
            totalAdmins: (int) $row['totalAdmins'],
            totalXp: (int) $row['totalXp'],
        );

        return $dto->toArray();
    }

    /**
     * @return User[] Returns an array of User objects based on filters
     */
    public function findByFilter(array $filters = [], string $sort = 'id', string $direction = 'DESC'): array
    {
        $qb = $this->createQueryBuilder('u');

        if (!empty($filters['search'])) {
            $qb->andWhere('u.email LIKE :search OR u.fullName LIKE :search')
                ->setParameter('search', '%' . $filters['search'] . '%');
        }

        if (!empty($filters['role'])) {
            $qb->andWhere('u.roles LIKE :role')
                ->setParameter('role', '%"' . $filters['role'] . '"%');
        }

        // Allowed sort fields to prevent SQL injection
        $allowedSorts = ['id', 'email', 'fullName', 'xp'];
        if (in_array($sort, $allowedSorts)) {
            $qb->orderBy('u.' . $sort, $direction);
        } else {
            $qb->orderBy('u.id', 'DESC');
        }

        return $qb->getQuery()->getResult();
    }
}
