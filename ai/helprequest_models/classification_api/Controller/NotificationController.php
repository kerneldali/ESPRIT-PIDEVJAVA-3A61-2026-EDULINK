<?php

namespace App\Controller;

use App\Entity\Notification;
use App\Repository\NotificationRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class NotificationController extends AbstractController
{
    public function recent(NotificationRepository $notificationRepository): Response
    {
        $user = $this->getUser();
        $notifications = [];
        $count = 0;

        if ($user) {
            $notifications = $notificationRepository->findUnreadByUser($user);
            $count = count($notifications);
        }

        return $this->render('components/_notifications_widget.html.twig', [
            'notifications' => $notifications,
            'count' => $count,
        ]);
    }

    #[Route('/notification/read/{id}', name: 'app_notification_read', methods: ['POST'])]
    public function markRead(
        Notification $notification,
        EntityManagerInterface $entityManager
    ): JsonResponse {
        if ($notification->getUser() !== $this->getUser()) {
            return new JsonResponse(['ok' => false, 'error' => 'Forbidden'], 403);
        }

        $notification->setIsRead(true);
        $entityManager->flush();

        return new JsonResponse(['ok' => true]);
    }
}
