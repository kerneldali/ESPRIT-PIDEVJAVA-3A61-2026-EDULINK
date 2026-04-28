<?php

namespace App\Service;

use App\Entity\HelpRequest;
use App\Entity\Session;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Email;
use Twig\Environment;
use Psr\Log\LoggerInterface;

class NotificationService
{
    public function __construct(
        private MailerInterface $mailer,
        private Environment $twig,
        private string $mailerFrom,
        private ?LoggerInterface $logger = null
    ) {
    }

    /**
     * Notify student that a tutor has joined their help request.
     */
    public function notifyRequestJoined(HelpRequest $helpRequest, \App\Entity\User $tutor): void
    {
        $student = $helpRequest->getStudent();
        if (!$student || !$student->getEmail())
            return;

        try {
            $html = $this->twig->render('email/request_joined.html.twig', [
                'helpRequest' => $helpRequest,
                'tutor' => $tutor,
                'student' => $student,
            ]);

            $email = (new Email())
                ->from($this->mailerFrom)
                ->to($student->getEmail())
                ->subject('Someone offered to help: ' . $helpRequest->getTitle())
                ->html($html);

            $this->mailer->send($email);
        } catch (\Exception $e) {
            $this->logger?->error('[NotificationService] Failed to send join email: ' . $e->getMessage());
        }
    }

    /**
     * Notify both participants when a session is closed.
     */
    public function notifySessionClosed(Session $session, ?string $summary = null): void
    {
        $helpRequest = $session->getHelpRequest();
        $student = $helpRequest->getStudent();
        $tutor = $session->getTutor();

        $recipients = array_filter([$student, $tutor], fn($u) => $u && $u->getEmail());

        foreach ($recipients as $user) {
            try {
                $html = $this->twig->render('email/session_closed.html.twig', [
                    'session' => $session,
                    'helpRequest' => $helpRequest,
                    'user' => $user,
                    'summary' => $summary,
                ]);

                $email = (new Email())
                    ->from($this->mailerFrom)
                    ->to($user->getEmail())
                    ->subject('Session closed: ' . $helpRequest->getTitle())
                    ->html($html);

                $this->mailer->send($email);
            } catch (\Exception $e) {
                $this->logger?->error('[NotificationService] Failed to send close email: ' . $e->getMessage());
            }
        }
    }
    /**
     * Notify admin about toxic content.
     */
    public function notifyAdminOfToxicContent(\App\Entity\User $user, string $content, string $context, string $source = 'UNKNOWN'): void
    {
        try {
            $html = $this->twig->render('email/admin_toxic_alert.html.twig', [
                'user' => $user,
                'content' => $content,
                'context' => $context,
                'source' => $source,
            ]);

            $email = (new Email())
                ->from($this->mailerFrom)
                ->to($this->mailerFrom) // Send to admin (app owner)
                ->subject("⚠️ Toxic Content Alert [$source]: $context")
                ->html($html);

            $this->mailer->send($email);
        } catch (\Exception $e) {
            $this->logger?->error('[NotificationService] Failed to send toxic alert: ' . $e->getMessage());
        }
    }

    /**
     * Notify a tutor that a student has invited them to help with a request.
     */
    public function notifyTutorInvited(HelpRequest $helpRequest, \App\Entity\User $tutor): void
    {
        if (!$tutor->getEmail()) {
            return;
        }

        try {
            $html = $this->twig->render('email/tutor_invited.html.twig', [
                'helpRequest' => $helpRequest,
                'tutor' => $tutor,
                'student' => $helpRequest->getStudent(),
            ]);

            $email = (new Email())
                ->from($this->mailerFrom)
                ->to($tutor->getEmail())
                ->subject('🎯 A student needs your help: ' . $helpRequest->getTitle())
                ->html($html);

            $this->mailer->send($email);
        } catch (\Exception $e) {
            $this->logger?->error('[NotificationService] Failed to send tutor invite email: ' . $e->getMessage());
        }
    }
}
