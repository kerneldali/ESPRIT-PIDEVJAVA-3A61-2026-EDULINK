<?php

namespace App\DTO;

/**
 * DTO for aggregated user statistics (Doctrine Doctor optimization).
 * Uses Doctrine NEW operator for 3-5x faster hydration vs array results.
 */
class UserStatsDTO
{
    public function __construct(
        public readonly int $totalUsers,
        public readonly int $totalStudents,
        public readonly int $totalTutors,
        public readonly int $totalAdmins,
        public readonly int $totalXp
    ) {
    }

    public function getTopRole(): string
    {
        $counts = [
            'Student' => $this->totalStudents,
            'Tutor' => $this->totalTutors,
            'Admin' => $this->totalAdmins,
        ];
        arsort($counts);
        return array_key_first($counts);
    }

    public function toArray(): array
    {
        return [
            'totalUsers' => $this->totalUsers,
            'totalStudents' => $this->totalStudents,
            'totalTutors' => $this->totalTutors,
            'totalAdmins' => $this->totalAdmins,
            'totalXp' => $this->totalXp,
            'topRole' => $this->getTopRole(),
        ];
    }
}
