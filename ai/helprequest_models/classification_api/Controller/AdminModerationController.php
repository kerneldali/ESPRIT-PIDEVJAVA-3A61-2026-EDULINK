<?php

namespace App\Controller;

use App\Entity\Cours;
use App\Entity\Matiere;
use App\Entity\Resource;
use App\Repository\CoursRepository;
use App\Repository\MatiereRepository;
use App\Repository\ResourceRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[Route('/admin/moderation')]
#[IsGranted('ROLE_ADMIN')]
class AdminModerationController extends AbstractController
{
    #[Route('/', name: 'app_admin_moderation')]
    public function index(
        MatiereRepository $matiereRepo,
        CoursRepository $coursRepo,
        ResourceRepository $resourceRepo
    ): Response
    {
        // Fetch all pending items
        $pendingMatieres = $matiereRepo->findBy(['status' => 'PENDING'], ['id' => 'DESC']);
        $pendingCours = $coursRepo->findBy(['status' => 'PENDING'], ['createdAt' => 'DESC']);
        $pendingResources = $resourceRepo->findBy(['status' => 'PENDING'], ['id' => 'DESC']);

        return $this->render('admin/moderation.html.twig', [
            'pendingMatieres' => $pendingMatieres,
            'pendingCours' => $pendingCours,
            'pendingResources' => $pendingResources,
        ]);
    }

    // MATIERE ACTIONS
    #[Route('/matiere/{id}/approve', name: 'app_admin_matiere_approve', methods: ['POST'])]
    public function approveMatiere(Matiere $matiere, Request $request, EntityManagerInterface $em): Response
    {
        if (!$this->isCsrfTokenValid('approve'.$matiere->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Invalid CSRF token.');
            return $this->redirectToRoute('app_admin_moderation', ['_fragment' => 'categories']);
        }
        $matiere->setStatus('APPROVED');
        $em->flush();

        $this->addFlash('success', 'Category approved successfully!');
        return $this->redirectToRoute('app_admin_moderation', ['_fragment' => 'categories']);
    }

    #[Route('/matiere/{id}/reject', name: 'app_admin_matiere_reject', methods: ['POST'])]
    public function rejectMatiere(Matiere $matiere, Request $request, EntityManagerInterface $em): Response
    {
        if (!$this->isCsrfTokenValid('reject'.$matiere->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Invalid CSRF token.');
            return $this->redirectToRoute('app_admin_moderation', ['_fragment' => 'categories']);
        }
        $em->remove($matiere);
        $em->flush();

        $this->addFlash('success', 'Category rejected and removed.');
        return $this->redirectToRoute('app_admin_moderation', ['_fragment' => 'categories']);
    }

    // COURS ACTIONS
    #[Route('/cours/{id}/approve', name: 'app_admin_cours_approve', methods: ['POST'])]
    public function approveCours(Cours $cours, Request $request, EntityManagerInterface $em): Response
    {
        if (!$this->isCsrfTokenValid('approve'.$cours->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Invalid CSRF token.');
            return $this->redirectToRoute('app_admin_moderation', ['_fragment' => 'courses']);
        }
        $cours->setStatus('APPROVED');
        $em->flush();

        $this->addFlash('success', 'Course approved successfully!');
        return $this->redirectToRoute('app_admin_moderation', ['_fragment' => 'courses']);
    }

    #[Route('/cours/{id}/reject', name: 'app_admin_cours_reject', methods: ['POST'])]
    public function rejectCours(Cours $cours, Request $request, EntityManagerInterface $em): Response
    {
        if (!$this->isCsrfTokenValid('reject'.$cours->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Invalid CSRF token.');
            return $this->redirectToRoute('app_admin_moderation', ['_fragment' => 'courses']);
        }
        $em->remove($cours);
        $em->flush();

        $this->addFlash('success', 'Course rejected and removed.');
        return $this->redirectToRoute('app_admin_moderation', ['_fragment' => 'courses']);
    }

    // RESOURCE ACTIONS
    #[Route('/resource/{id}/approve', name: 'app_admin_resource_approve', methods: ['POST'])]
    public function approveResource(Resource $resource, Request $request, EntityManagerInterface $em): Response
    {
        if (!$this->isCsrfTokenValid('approve'.$resource->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Invalid CSRF token.');
            return $this->redirectToRoute('app_admin_moderation', ['_fragment' => 'resources']);
        }
        $resource->setStatus('APPROVED');
        $em->flush();

        $this->addFlash('success', 'Resource approved successfully!');
        return $this->redirectToRoute('app_admin_moderation', ['_fragment' => 'resources']);
    }

    #[Route('/resource/{id}/reject', name: 'app_admin_resource_reject', methods: ['POST'])]
    public function rejectResource(Resource $resource, Request $request, EntityManagerInterface $em): Response
    {
        if (!$this->isCsrfTokenValid('reject'.$resource->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Invalid CSRF token.');
            return $this->redirectToRoute('app_admin_moderation', ['_fragment' => 'resources']);
        }
        $em->remove($resource);
        $em->flush();

        $this->addFlash('success', 'Resource rejected and removed.');
        return $this->redirectToRoute('app_admin_moderation', ['_fragment' => 'resources']);
    }
}
