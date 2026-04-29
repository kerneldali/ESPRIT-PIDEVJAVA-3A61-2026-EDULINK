<?php

namespace App\Controller\Admin;

use App\Entity\HelpRequest;
use App\Repository\HelpRequestRepository;
use App\Repository\MessageRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

/**
 * BACK-OFFICE: Admin Assistance Controller
 * Handles all admin-facing help request and report management
 */
#[Route('/admin/assistance')]
#[IsGranted('ROLE_ADMIN')]
class AssistanceController extends AbstractController
{
    #[Route('/', name: 'app_admin_assistance_index', methods: ['GET'])]
    #[Route('/', name: 'admin_assistance', methods: ['GET'])]
    public function index(HelpRequestRepository $helpRequestRepository): Response
    {
        return $this->render('admin/assistance/index.html.twig', [
            'tickets' => $helpRequestRepository->findSupportTickets(),
            'all_requests' => $helpRequestRepository->findBy([], ['createdAt' => 'DESC'], 100),
            'stats' => $helpRequestRepository->getAssistanceStats(),
        ]);
    }

    #[Route('/stats-dashboard', name: 'app_admin_assistance_stats', methods: ['GET'])]
    public function statsDashboard(HelpRequestRepository $helpRequestRepository): Response
    {
        return $this->render('admin/assistance/stats.html.twig', [
            'stats' => $helpRequestRepository->getAssistanceStats(),
            'monthlyData' => $helpRequestRepository->getMonthlyRequestCounts(),
            'categoryData' => $helpRequestRepository->getCategoryDistribution(),
            'resolutionData' => $helpRequestRepository->getResolutionBreakdown(),
            'topTutors' => $helpRequestRepository->getTopTutors(),
        ]);
    }

    #[Route('/view/{id}', name: 'app_admin_assistance_view', methods: ['GET'])]
    public function view(HelpRequest $helpRequest): Response
    {
        return $this->render('admin/assistance/view.html.twig', [
            'help_request' => $helpRequest,
        ]);
    }

    #[Route('/resolve/{id}', name: 'app_admin_assistance_resolve', methods: ['POST'])]
    public function resolve(HelpRequest $helpRequest, EntityManagerInterface $entityManager): Response
    {
        $helpRequest->setIsTicket(false);
        $helpRequest->setStatus('CLOSED');
        $helpRequest->setCloseReason('ADMIN_RESOLVED');
        $entityManager->flush();

        $this->addFlash('success', 'Ticket resolved by admin.');
        return $this->redirectToRoute('app_admin_assistance_index');
    }

    #[Route('/delete/{id}', name: 'app_admin_assistance_delete', methods: ['POST'])]
    public function delete(HelpRequest $helpRequest, EntityManagerInterface $entityManager): Response
    {
        $entityManager->remove($helpRequest);
        $entityManager->flush();

        $this->addFlash('success', 'Help request deleted by admin.');
        return $this->redirectToRoute('app_admin_assistance_index');
    }

    #[Route('/export/requests', name: 'app_admin_assistance_export_requests', methods: ['GET'])]
    public function exportRequests(HelpRequestRepository $helpRequestRepository): Response
    {
        $requests = $helpRequestRepository->findBy([], ['createdAt' => 'DESC'], 500);

        $response = new Response();
        $response->headers->set('Content-Type', 'text/csv; charset=UTF-8');
        $response->headers->set('Content-Disposition', 'attachment; filename="help_requests_' . date('Y-m-d_His') . '.csv"');

        $output = "\xEF\xBB\xBF"; // UTF-8 BOM for Excel
        $output .= "ID,Student,Email,Title,Description,Status,Bounty,Is Ticket,Close Reason,Created At,Tutor,Rating\n";

        foreach ($requests as $req) {
            $tutor = $req->getSession() ? ($req->getSession()->getTutor()?->getFullName() ?? 'N/A') : 'N/A';
            $rating = ($req->getSession() && $req->getSession()->getReview()) ? $req->getSession()->getReview()->getRating() : 'N/A';

            $output .= sprintf(
                "%d,%s,%s,%s,%s,%s,%d,%s,%s,%s,%s,%s\n",
                $req->getId(),
                $this->csvEscape($req->getStudent()?->getFullName() ?? 'N/A'),
                $this->csvEscape($req->getStudent()?->getEmail() ?? 'N/A'),
                $this->csvEscape((string) $req->getTitle()),
                $this->csvEscape(mb_substr((string) $req->getDescription(), 0, 200)),
                $req->getStatus(),
                $req->getBounty(),
                $req->isIsTicket() ? 'Yes' : 'No',
                $req->getCloseReason() ?? 'N/A',
                $req->getCreatedAt()?->format('Y-m-d H:i:s') ?? 'N/A',
                $this->csvEscape($tutor),
                $rating
            );
        }

        $response->setContent($output);
        return $response;
    }

    #[Route('/export/tickets', name: 'app_admin_assistance_export_tickets', methods: ['GET'])]
    public function exportTickets(HelpRequestRepository $helpRequestRepository): Response
    {
        $tickets = $helpRequestRepository->findSupportTickets();

        $response = new Response();
        $response->headers->set('Content-Type', 'text/csv; charset=UTF-8');
        $response->headers->set('Content-Disposition', 'attachment; filename="reported_tickets_' . date('Y-m-d_His') . '.csv"');

        $output = "\xEF\xBB\xBF";
        $output .= "ID,Student,Email,Title,Status,Close Reason,Created At\n";

        foreach ($tickets as $ticket) {
            $output .= sprintf(
                "%d,%s,%s,%s,%s,%s,%s\n",
                $ticket->getId(),
                $this->csvEscape($ticket->getStudent()?->getFullName() ?? 'N/A'),
                $this->csvEscape($ticket->getStudent()?->getEmail() ?? 'N/A'),
                $this->csvEscape($ticket->getTitle()),
                $ticket->getStatus(),
                $ticket->getCloseReason() ?? 'N/A',
                $ticket->getCreatedAt()?->format('Y-m-d H:i:s') ?? 'N/A'
            );
        }

        $response->setContent($output);
        return $response;
    }

    #[Route('/export/stats', name: 'app_admin_assistance_export_stats', methods: ['GET'])]
    public function exportStats(HelpRequestRepository $helpRequestRepository): Response
    {
        $stats = $helpRequestRepository->getAssistanceStats();

        $response = new Response();
        $response->headers->set('Content-Type', 'text/csv; charset=UTF-8');
        $response->headers->set('Content-Disposition', 'attachment; filename="assistance_stats_' . date('Y-m-d_His') . '.csv"');

        $output = "\xEF\xBB\xBF";
        $output .= "Metric,Value\n";
        $output .= "Total Requests,{$stats['totalRequests']}\n";
        $output .= "Open Requests,{$stats['openRequests']}\n";
        $output .= "In Progress,{$stats['inProgressRequests']}\n";
        $output .= "Closed Requests,{$stats['closedRequests']}\n";
        $output .= "Total Bounty,{$stats['totalBounty']}\n";
        $output .= "Reported Tickets,{$stats['reportedTickets']}\n";
        $output .= "Total Sessions,{$stats['totalSessions']}\n";
        $output .= "Active Sessions,{$stats['activeSessions']}\n";
        $output .= "Average Rating,{$stats['avgRating']}\n";
        $output .= "Total Reviews,{$stats['totalReviews']}\n";
        $output .= "Total Messages,{$stats['totalMessages']}\n";
        $output .= "Toxic Messages,{$stats['toxicMessages']}\n";
        $output .= "Resolution Rate,{$stats['resolutionRate']}%\n";

        $response->setContent($output);
        return $response;
    }

    private function csvEscape(string $value): string
    {
        $value = str_replace('"', '""', $value);
        if (str_contains($value, ',') || str_contains($value, '"') || str_contains($value, "\n")) {
            return '"' . $value . '"';
        }
        return $value;
    }

    #[Route('/toxic', name: 'app_admin_assistance_toxic', methods: ['GET'])]
    public function toxicMessages(MessageRepository $messageRepository, EntityManagerInterface $em): Response
    {
        $toxicMessages = $messageRepository->findToxicMessages();

        $totalMessages = (int) $em->createQuery('SELECT COUNT(m.id) FROM App\Entity\Message m')
            ->getSingleScalarResult();

        return $this->render('admin/assistance/toxic.html.twig', [
            'toxic_messages' => $toxicMessages,
            'totalMessages' => $totalMessages,
        ]);
    }

    #[Route('/export/toxic', name: 'app_admin_assistance_export_toxic', methods: ['GET'])]
    public function exportToxicMessages(MessageRepository $messageRepository): Response
    {
        $toxicMessages = $messageRepository->findToxicMessages();

        $response = new Response();
        $response->headers->set('Content-Type', 'text/csv; charset=UTF-8');
        $response->headers->set('Content-Disposition', 'attachment; filename="toxic_messages_' . date('Y-m-d_His') . '.csv"');

        $output = "\xEF\xBB\xBF"; // UTF-8 BOM
        $output .= "ID,Sender,Email,Content,Session ID,Timestamp\n";

        foreach ($toxicMessages as $msg) {
            $output .= sprintf(
                "%d,%s,%s,%s,%s,%s\n",
                $msg->getId(),
                $this->csvEscape($msg->getSender()?->getFullName() ?? 'N/A'),
                $this->csvEscape($msg->getSender()?->getEmail() ?? 'N/A'),
                $this->csvEscape(mb_substr((string) $msg->getContent(), 0, 300)),
                $msg->getSession()?->getId() ?? 'N/A',
                $msg->getTimestamp()?->format('Y-m-d H:i:s') ?? 'N/A'
            );
        }

        $response->setContent($output);
        return $response;
    }
}