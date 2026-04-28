<?php

namespace App\EventListener;

use App\Entity\User;
use App\Service\BadgeService;
use Symfony\Bundle\SecurityBundle\Security;
use Symfony\Component\EventDispatcher\Attribute\AsEventListener;
use Symfony\Component\HttpKernel\Event\TerminateEvent;

class UserBadgeKernelListener
{
    private Security $security;
    private BadgeService $badgeService;

    public function __construct(Security $security, BadgeService $badgeService)
    {
        $this->security = $security;
        $this->badgeService = $badgeService;
    }

    #[AsEventListener(event: 'kernel.terminate')]
    public function onKernelTerminate(TerminateEvent $event): void
    {
        // After sending the response to the user, ensure their badges are in sync with their current XP
        $user = $this->security->getUser();
        if ($user instanceof User) {
            $this->badgeService->checkBadges($user);
        }
    }
}
