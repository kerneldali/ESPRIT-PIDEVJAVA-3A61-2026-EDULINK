<?php

namespace App\Service;

use App\Entity\Challenge;

class ChallengeManager
{
    public function validate(Challenge $challenge): bool
    {
        if (empty($challenge->getTitle())) {
            throw new \InvalidArgumentException("Challenge Title cannot be empty.");
        }

        if ($challenge->getRewardPoints() <= 0) {
            throw new \InvalidArgumentException("Reward Points must be strictly positive (> 0).");
        }

        return true;
    }
}
