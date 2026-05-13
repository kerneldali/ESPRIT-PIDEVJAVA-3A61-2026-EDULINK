<?php

namespace App\Controller;

use App\Service\Web3Service;
use App\Entity\User;
use Doctrine\ORM\EntityManagerInterface;
use App\Form\TransferPointsType;
use App\Entity\Transaction;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/wallet')]
class WalletController extends AbstractController
{
    public function __construct(
        private Web3Service $web3Service,
        private EntityManagerInterface $entityManager
    ) {}

    #[Route('', name: 'app_wallet_index', methods: ['GET', 'POST'])]
    public function index(Request $request): Response
    {
        /** @var User $user */
        $user = $this->getUser();
        if (!$user) {
            return $this->redirectToRoute('app_login');
        }

        // --- Handle Local Transfer Form ---
        $transferForm = $this->createForm(TransferPointsType::class);
        $transferForm->handleRequest($request);

        if ($transferForm->isSubmitted() && $transferForm->isValid()) {
            $data = $transferForm->getData();
            $amount = $data['amount'];
            $recipientEmail = $data['recipientEmail'];

            if ($user->getWalletBalance() < $amount) {
                $this->addFlash('error', 'Insufficient funds!');
            } elseif ($recipientEmail === $user->getEmail()) {
                $this->addFlash('error', 'You cannot transfer points to yourself.');
            } else {
                $recipient = $this->entityManager->getRepository(User::class)->findOneBy(['email' => $recipientEmail]);

                if (!$recipient) {
                    $this->addFlash('error', 'Recipient not found.');
                } else {
                    // Create local transaction records
                    $tSender = new Transaction();
                    $tSender->setUser($user);
                    $tSender->setAmount((int) (-$amount));
                    $tSender->setType('TRANSFER_SENT');
                    $tSender->setDate(new \DateTime());

                    $tRecipient = new Transaction();
                    $tRecipient->setUser($recipient);
                    $tRecipient->setAmount($amount);
                    $tRecipient->setType('TRANSFER_RECEIVED');
                    $tRecipient->setDate(new \DateTime());

                    $user->setWalletBalance($user->getWalletBalance() - $amount);
                    $recipient->setWalletBalance($recipient->getWalletBalance() + $amount);

                    $this->entityManager->persist($tSender);
                    $this->entityManager->persist($tRecipient);
                    $this->entityManager->flush();

                    $this->addFlash('success', "Successfully sent $amount PTS to $recipientEmail!");
                    return $this->redirectToRoute('app_wallet_index');
                }
            }
        }

        // --- Calculate Stats ---
        $transactions = $user->getTransactions();
        $breakdown = ['COURSES' => 0, 'EVENTS' => 0, 'TRANSFERS' => 0];
        $totalSpent = 0; $totalEarned = 0;

        foreach ($transactions as $t) {
            $amt = $t->getAmount();
            $type = strtoupper($t->getType() ?? 'OTHER');
            if ($amt < 0) {
                $absAmount = abs($amt); $totalSpent += $absAmount;
                if (str_contains($type, 'COURSE')) $breakdown['COURSES'] += $absAmount;
                elseif (str_contains($type, 'EVENT')) $breakdown['EVENTS'] += $absAmount;
                elseif (str_contains($type, 'TRANSFER')) $breakdown['TRANSFERS'] += $absAmount;
            } else {
                $totalEarned += $amt;
            }
        }

        // Fetch on-chain transaction history via raw SQL
        $conn = $this->entityManager->getConnection();
        $sql = "SELECT * FROM token_transaction WHERE from_user_id = :uid OR to_user_id = :uid ORDER BY created_at DESC LIMIT 20";
        $web3History = $conn->fetchAllAssociative($sql, ['uid' => $user->getId()]);

        return $this->render('wallet/index.html.twig', [
            'user' => $user,
            'history' => $web3History,
            'localTransactions' => $transactions,
            'breakdown' => $breakdown,
            'totalSpent' => $totalSpent,
            'totalEarned' => $totalEarned,
            'transferForm' => $transferForm->createView(),
        ]);
    }

    #[Route('/refresh', name: 'app_wallet_refresh', methods: ['POST'])]
    public function refresh(): JsonResponse
    {
        /** @var User $user */
        $user = $this->getUser();
        if (!$user || !$user->getEthWalletAddress()) {
            return new JsonResponse(['error' => 'No wallet found'], 400);
        }

        $balance = $this->web3Service.getBalance($user->getEthWalletAddress());
        
        $user->setWalletBalance($balance);
        $this->entityManager->flush();

        return new JsonResponse([
            'balance' => number_format($balance, 2),
            'status' => 'success'
        ]);
    }

    #[Route('/generate', name: 'app_wallet_generate', methods: ['POST'])]
    public function generate(Request $request): Response
    {
        /** @var User $user */
        $user = $this->getUser();
        if (!$user) return $this->redirectToRoute('app_login');

        if ($user->getEthWalletAddress()) {
            $this->addFlash('warning', 'You already have a wallet!');
            return $this->redirectToRoute('app_wallet_index');
        }

        $wallet = $this->web3Service.generateWallet();
        
        $user->setEthWalletAddress($wallet['address']);
        $user->setEthPrivateKey($wallet['privateKey']);
        $user->setWalletBalance(1000.0); // Welcome bonus

        $this->entityManager->flush();

        // Log the bonus transaction
        $conn = $this->entityManager->getConnection();
        $conn->insert('token_transaction', [
            'to_user_id' => $user->getId(),
            'amount' => 1000,
            'tx_type' => 'MINT',
            'tx_hash' => 'web3-mint-' . bin2hex(random_bytes(16)),
            'note' => 'Welcome bonus from Symfony',
            'created_at' => date('Y-m-d H:i:s')
        ]);

        $this->addFlash('success', 'Wallet generated! 1000 EDU tokens have been credited to your account.');
        
        return $this->redirectToRoute('app_wallet_index');
    #[Route('/log-tx', name: 'app_wallet_log_tx', methods: ['POST'])]
    public function logTx(Request $request): JsonResponse
    {
        /** @var User $user */
        $user = $this->getUser();
        if (!$user) return new JsonResponse(['error' => 'Unauthorized'], 401);

        $data = json_decode($request->getContent(), true);
        $txHash = $data['txHash'] ?? null;
        $amount = $data['amount'] ?? 0;
        $toAddress = $data['toAddress'] ?? null;
        $type = $data['type'] ?? 'TRANSFER';

        if (!$txHash) return new JsonResponse(['error' => 'Missing hash'], 400);

        // Resolve recipient ID if possible
        $recipient = $this->entityManager->getRepository(User::class)->findOneBy(['ethWalletAddress' => $toAddress]);
        $recipientId = $recipient ? $recipient->getId() : null;

        $conn = $this->entityManager->getConnection();
        $conn->insert('token_transaction', [
            'from_user_id' => $user->getId(),
            'to_user_id' => $recipientId,
            'amount' => $amount,
            'tx_hash' => $txHash,
            'tx_type' => $type,
            'note' => 'Web3 transaction confirmed via Metamask',
            'created_at' => date('Y-m-d H:i:s')
        ]);

        return new JsonResponse(['status' => 'success']);
    }
}
