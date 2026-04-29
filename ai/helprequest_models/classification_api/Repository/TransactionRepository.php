<?php

namespace App\Repository;

use App\Entity\Transaction;
use App\DTO\DashboardPointsDTO;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Transaction>
 */
class TransactionRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Transaction::class);
    }

    /**
     * Optimized: single query for all dashboard aggregations (Doctrine Doctor fix).
     * Replaces 3 separate SUM/COUNT queries with 1 consolidated query + DTO.
     */
    public function getDashboardStats(): DashboardPointsDTO
    {
        $today = new \DateTime('today');
        $conn = $this->getEntityManager()->getConnection();

        $row = $conn->executeQuery("
            SELECT
                COALESCE(SUM(CASE WHEN amount > 0 THEN amount ELSE 0 END), 0) AS totalPoints,
                COALESCE(SUM(CASE WHEN amount > 0 AND date >= :today THEN amount ELSE 0 END), 0) AS dailyPoints,
                COUNT(DISTINCT CASE WHEN date >= :today THEN user_id ELSE NULL END) AS activeUsers
            FROM transaction
        ", ['today' => $today->format('Y-m-d')])->fetchAssociative();

        return new DashboardPointsDTO(
            totalPoints: (int) $row['totalPoints'],
            dailyPoints: (int) $row['dailyPoints'],
            activeUsers: (int) $row['activeUsers'],
        );
    }

    public function getTotalPointsEarned(): int
    {
        return $this->getDashboardStats()->totalPoints;
    }

    public function getPointsEarnedToday(): int
    {
        return $this->getDashboardStats()->dailyPoints;
    }

    public function countActiveUsersToday(): int
    {
        return $this->getDashboardStats()->activeUsers;
    }
}

