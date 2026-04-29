<?php

namespace App\Service;

use App\Entity\User;
use App\Entity\UserBadge;
use App\Repository\BadgeRepository;
use App\Repository\UserBadgeRepository;
use Doctrine\ORM\EntityManagerInterface;
use Psr\Log\LoggerInterface;

class BadgeService
{
    private EntityManagerInterface $entityManager;
    private BadgeRepository $badgeRepository;
    private UserBadgeRepository $userBadgeRepository;
    private LoggerInterface $logger;

    public function __construct(
        EntityManagerInterface $entityManager,
        BadgeRepository $badgeRepository,
        UserBadgeRepository $userBadgeRepository,
        LoggerInterface $logger
    ) {
        $this->entityManager = $entityManager;
        $this->badgeRepository = $badgeRepository;
        $this->userBadgeRepository = $userBadgeRepository;
        $this->logger = $logger;
    }

    /**
     * Checks and awards badges to a user based on their current XP (wallet balance).
     * Returns the list of newly awarded badges.
     */
    public function checkBadges(User $user): array
    {
        $newlyAwarded = [];
        $xp = $user->getWalletBalance();
        
        // 1. Get all available badges
        $allBadges = $this->badgeRepository->findAll();
        
        // 2. Get badges the user already has
        $existingUserBadges = $this->userBadgeRepository->findBy(['user' => $user]);
        $existingBadgeIds = array_map(function($ub) {
            return $ub->getBadge()->getId();
        }, $existingUserBadges);

        foreach ($allBadges as $badge) {
            // If user doesn't have the badge and has enough XP
            if (!in_array($badge->getId(), $existingBadgeIds) && $xp >= $badge->getMinPoints()) {
                $userBadge = new UserBadge();
                $userBadge->setUser($user);
                $userBadge->setBadge($badge);
                $userBadge->setUnlockedAt(new \DateTime());
                
                $this->entityManager->persist($userBadge);
                $newlyAwarded[] = $badge;
                
                $this->logger->info(sprintf('Awarded badge "%s" to user %s', $badge->getName(), $user->getEmail()));
            }
        }

        if (!empty($newlyAwarded)) {
            $this->entityManager->flush();
        }

        return $newlyAwarded;
    }
}
