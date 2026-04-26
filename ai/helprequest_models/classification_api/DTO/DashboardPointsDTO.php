<?php

namespace App\DTO;

/**
 * DTO for aggregated transaction/points statistics (Doctrine Doctor optimization).
 * Combines 3 separate aggregation queries into 1 for the admin dashboard.
 */
class DashboardPointsDTO
{
    public function __construct(
        public readonly int $totalPoints,
        public readonly int $dailyPoints,
        public readonly int $activeUsers
    ) {
    }
}
