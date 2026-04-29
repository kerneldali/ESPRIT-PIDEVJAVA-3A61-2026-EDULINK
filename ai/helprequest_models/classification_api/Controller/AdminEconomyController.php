<?php

namespace App\Controller;

use App\Entity\Transaction;
use App\Entity\User;
use App\Repository\TransactionRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/admin/economy')]
class AdminEconomyController extends AbstractController
{
    #[Route('/', name: 'admin_economy')]
    public function index(TransactionRepository $transactionRepository): Response
    {
        $transactions = $transactionRepository->findBy([], ['date' => 'DESC'], 50); // Last 50 transactions

        $totalPoints = $transactionRepository->getTotalPointsEarned();
        $dailyPoints = $transactionRepository->getPointsEarnedToday();
        $activeUsers = $transactionRepository->countActiveUsersToday();

        // Calculate "Supply" as total points (or fetch from User entity if you store balance there)
        // For now, let's use Total Points Earned as a proxy for activity volume or supply

        return $this->render('admin/economy.html.twig', [
            'transactions' => $transactions,
            'total_points' => $totalPoints,
            'daily_points' => $dailyPoints,
            'active_users' => $activeUsers,
        ]);
    }

    #[Route('/transfer', name: 'admin_economy_transfer', methods: ['POST'])]
    public function transfer(Request $request, EntityManagerInterface $entityManager, \App\Service\BadgeService $badgeService): Response
    {
        $email = $request->request->get('email');
        $amount = (int) $request->request->get('amount');
        $type = $request->request->get('type'); // 'grant' or 'refund'
        $reason = $request->request->get('reason');

        if (!$email || !$amount) {
            $this->addFlash('error', 'Email and amount are required.');
            return $this->redirectToRoute('admin_economy');
        }

        $user = $entityManager->getRepository(User::class)->findOneBy(['email' => $email]);
        if (!$user) {
            $this->addFlash('error', 'User not found.');
            return $this->redirectToRoute('admin_economy');
        }

        $transaction = new Transaction();
        $transaction->setUser($user);
        $transaction->setDate(new \DateTime());
        $transaction->setType(strtoupper((string) $type)); // GRANT or REFUND

        $transaction->setAmount($amount);

        // Update User Balance (Unified XP/Wallet)
        $user->setWalletBalance($user->getWalletBalance() + $amount);

        // Check for new badges after XP change
        $badgeService->checkBadges($user);

        $entityManager->persist($transaction);

        // Create Notification
        $notification = new \App\Entity\Notification();
        $notification->setUser($user);
        $emoji = strtoupper((string) $type) === 'REFUND' ? '🔄' : '💰';
        $notification->setMessage("$emoji You received $amount points from Admin ($type). Note: " . ($reason ?: 'No reason provided'));
        $notification->setLink('/student/dashboard'); // Or transaction history
        $entityManager->persist($notification);

        $entityManager->flush();

        $this->addFlash('success', "Successfully sent $amount points to {$user->getEmail()}.");
        return $this->redirectToRoute('admin_economy');
    }
}
