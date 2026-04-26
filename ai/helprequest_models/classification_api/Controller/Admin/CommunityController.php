<?php

namespace App\Controller\Admin;

use App\Entity\CommunityPost;
use App\Entity\PostReport;
use App\Repository\CommunityPostRepository;
use App\Repository\PostReportRepository;
use App\Repository\PostCommentRepository;
use App\Repository\PostReactionRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

/**
 * BACK-OFFICE: Admin Community Feed Controller
 * Manages reported posts, post moderation, and community stats
 */
#[Route('/admin/community')]
#[IsGranted('ROLE_ADMIN')]
class CommunityController extends AbstractController
{
    #[Route('/', name: 'app_admin_community_index', methods: ['GET'])]
    public function index(
        PostReportRepository $reportRepo,
        CommunityPostRepository $postRepo,
        PostCommentRepository $commentRepo,
        PostReactionRepository $reactionRepo,
        EntityManagerInterface $em
    ): Response {
        $pendingReports = $reportRepo->findPendingReports();
        $allReports = $reportRepo->findBy([], ['createdAt' => 'DESC'], 100);
        $allPosts = $postRepo->findRecentPosts(100);

        // Count stats
        $totalPosts = count($allPosts);
        $totalReports = count($allReports);
        $pendingCount = count($pendingReports);

        // Count comments and reactions
        $counts = $em->getConnection()->executeQuery("
            SELECT
                (SELECT COUNT(*) FROM post_comment) AS totalComments,
                (SELECT COUNT(*) FROM post_reaction) AS totalReactions
        ")->fetchAssociative();
        $totalComments = (int) $counts['totalComments'];
        $totalReactions = (int) $counts['totalReactions'];

        return $this->render('admin/community/index.html.twig', [
            'pending_reports' => $pendingReports,
            'all_reports' => $allReports,
            'all_posts' => $allPosts,
            'stats' => [
                'totalPosts' => $totalPosts,
                'totalReports' => $totalReports,
                'pendingReports' => $pendingCount,
                'totalComments' => $totalComments,
                'totalReactions' => $totalReactions,
            ],
        ]);
    }

    #[Route('/report/{id}', name: 'app_admin_community_view_report', methods: ['GET'])]
    public function viewReport(PostReport $report): Response
    {
        return $this->render('admin/community/view_report.html.twig', [
            'report' => $report,
        ]);
    }

    #[Route('/report/{id}/dismiss', name: 'app_admin_community_dismiss_report', methods: ['POST'])]
    public function dismissReport(PostReport $report, EntityManagerInterface $em): Response
    {
        $report->setStatus('DISMISSED');
        $em->flush();

        $this->addFlash('success', 'Report dismissed.');
        return $this->redirectToRoute('app_admin_community_index');
    }

    #[Route('/report/{id}/action', name: 'app_admin_community_action_report', methods: ['POST'])]
    public function actionReport(PostReport $report, EntityManagerInterface $em): Response
    {
        // Mark report as actioned and delete the offending post
        $report->setStatus('ACTIONED');
        $post = $report->getPost();

        if ($post) {
            // Delete related data
            $em->createQuery('DELETE FROM App\Entity\PostComment c WHERE c.post = :post')
                ->setParameter('post', $post)->execute();
            $em->createQuery('DELETE FROM App\Entity\PostReaction r WHERE r.post = :post')
                ->setParameter('post', $post)->execute();
            // Mark all reports for this post as actioned
            $em->createQuery('UPDATE App\Entity\PostReport r SET r.status = :status WHERE r.post = :post')
                ->setParameter('status', 'ACTIONED')
                ->setParameter('post', $post)->execute();

            $em->remove($post);
        }

        $em->flush();

        $this->addFlash('success', 'Post removed and report actioned.');
        return $this->redirectToRoute('app_admin_community_index');
    }

    #[Route('/post/{id}/delete', name: 'app_admin_community_delete_post', methods: ['POST'])]
    public function deletePost(CommunityPost $post, EntityManagerInterface $em): Response
    {
        // Delete related data
        $em->createQuery('DELETE FROM App\Entity\PostComment c WHERE c.post = :post')
            ->setParameter('post', $post)->execute();
        $em->createQuery('DELETE FROM App\Entity\PostReaction r WHERE r.post = :post')
            ->setParameter('post', $post)->execute();
        $em->createQuery('DELETE FROM App\Entity\PostReport r WHERE r.post = :post')
            ->setParameter('post', $post)->execute();

        $em->remove($post);
        $em->flush();

        $this->addFlash('success', 'Community post deleted by admin.');
        return $this->redirectToRoute('app_admin_community_index');
    }
}
