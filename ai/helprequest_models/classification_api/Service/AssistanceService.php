<?php

namespace App\Service;

use App\Entity\Badge;
use App\Entity\Session;
use App\Entity\Transaction;
use App\Entity\User;
use App\Entity\UserBadge;
use Doctrine\ORM\EntityManagerInterface;

class AssistanceService
{
    public function __construct(
        private
        EntityManagerInterface $entityManager
    ) {
    }

    public function createSession(Session $session): void
    {
        // Generate unique Jitsi room ID for video calls
        $session->setJitsiRoomId('edulink-help-' . bin2hex(random_bytes(8)));
        $session->setIsActive(true);

        $this->entityManager->persist($session);
        // Connect session to help request
        $helpRequest = $session->getHelpRequest();
        $helpRequest->setStatus('IN_PROGRESS');

        $this->entityManager->flush();
    }

    public function closeSession(Session $session): void
    {
        if (!$session->isIsActive()) {
            return;
        }

        $this->entityManager->beginTransaction();

        try {
            // 1. Close Session
            $session->setIsActive(false);
            $session->setEndedAt(new \DateTimeImmutable());

            $helpRequest = $session->getHelpRequest();
            $helpRequest->setStatus('CLOSED');

            // 2. Transfer Bounty
            $bounty = $helpRequest->getBounty();
            $student = $helpRequest->getStudent();
            $tutor = $session->getTutor();

            if ($bounty > 0 && $student->getWalletBalance() >= $bounty) {
                // Deduct from Student
                $newStudentBalance = $student->getWalletBalance() - $bounty;
                $student->setWalletBalance($newStudentBalance);

                $debitTransaction = new Transaction();
                $debitTransaction->setUser($student);
                $debitTransaction->setAmount(-$bounty);
                $debitTransaction->setType('ASSISTANCE_PAYMENT');
                $debitTransaction->setDate(new \DateTime());
                $this->entityManager->persist($debitTransaction);

                // Credit to Tutor
                $newTutorBalance = $tutor->getWalletBalance() + $bounty;
                $tutor->setWalletBalance($newTutorBalance);

                $creditTransaction = new Transaction();
                $creditTransaction->setUser($tutor);
                $creditTransaction->setAmount($bounty);
                $creditTransaction->setType('ASSISTANCE_REWARD');
                $creditTransaction->setDate(new \DateTime());
                $this->entityManager->persist($creditTransaction);
            }

            // 3. Award Badges based on completed sessions
            $this->awardBadges($tutor);

            $this->entityManager->flush();
            $this->entityManager->commit();

        } catch (\Exception $e) {
            $this->entityManager->rollback();
            throw $e;
        }
    }

    /**
     * Check and award badges to a tutor based on completed session count.
     */
    private function awardBadges(User $tutor): void
    {
        // Count completed sessions as tutor
        $completedCount = (int) $this->entityManager
            ->createQuery('SELECT COUNT(s.id) FROM App\Entity\Session s WHERE s.tutor = :tutor AND s.isActive = false')
            ->setParameter('tutor', $tutor)
            ->getSingleScalarResult();

        // Get all badges where minPoints <= completed count
        $eligibleBadges = $this->entityManager->getRepository(Badge::class)
            ->createQueryBuilder('b')
            ->andWhere('b.minPoints <= :count')
            ->setParameter('count', $completedCount)
            ->getQuery()
            ->getResult();

        // Get badges the tutor already has
        $existingBadgeIds = array_map('intval', $this->entityManager->getRepository(UserBadge::class)
            ->createQueryBuilder('ub')
            ->select('IDENTITY(ub.badge)')
            ->andWhere('ub.user = :user')
            ->setParameter('user', $tutor)
            ->getQuery()
            ->getSingleColumnResult());

        foreach ($eligibleBadges as $badge) {
            if (!in_array($badge->getId(), $existingBadgeIds, true)) {
                $userBadge = new UserBadge();
                $userBadge->setUser($tutor);
                $userBadge->setBadge($badge);
                $userBadge->setUnlockedAt(new \DateTime());
                $this->entityManager->persist($userBadge);
            }
        }
    }

    public function getEntityManager(): EntityManagerInterface
    {
        return $this->entityManager;
    }
}
