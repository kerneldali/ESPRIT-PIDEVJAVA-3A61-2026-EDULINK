<?php

namespace App\Controller;

use App\Entity\Cours;
use App\Entity\Resource;
use App\Entity\User;
use App\Form\CoursType;
use App\Form\ResourceType;
use App\Repository\CoursRepository;
use Doctrine\ORM\EntityManagerInterface;
use App\Service\CategoryImageService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/creator')]
class CreatorController extends AbstractController
{
    private CategoryImageService $imageService;

    public function __construct(CategoryImageService $imageService)
    {
        $this->imageService = $imageService;
    }
    // 1. DASHBOARD REDIRECT
    #[Route('/', name: 'app_creator_dashboard')]
    public function index(): Response
    {
        // We redirect to the student dashboard because that's where the tabs are
        return $this->redirectToRoute('app_student_courses');
    }

    // 2. CREATE COURSE
    #[Route('/course/new', name: 'app_creator_course_new')]
    public function newCourse(Request $request, EntityManagerInterface $em): Response
    {
        $cours = new Cours();
        // Check for pre-selected category
        $categoryId = $request->query->get('category');
        $hideMatiere = false;
        
        if ($categoryId) {
            $matiere = $em->getRepository(\App\Entity\Matiere::class)->find($categoryId);
            if ($matiere) {
                $cours->setMatiere($matiere);
                $hideMatiere = true;
            }
        }

        $form = $this->createForm(CoursType::class, $cours, [
            'hide_matiere' => $hideMatiere
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            // Handle matiere_name unmapped field
            if (!$hideMatiere && $form->has('matiere_name')) {
                $matiereName = trim($form->get('matiere_name')->getData() ?? '');
                if ($matiereName !== '') {
                    $existingMatiere = $em->getRepository(\App\Entity\Matiere::class)
                        ->findOneBy(['name' => $matiereName]);
                    if ($existingMatiere) {
                        $cours->setMatiere($existingMatiere);
                    } else {
                        $newMatiere = new \App\Entity\Matiere();
                        $newMatiere->setName($matiereName);
                        $newMatiere->setStatus('PENDING');
                        /** @var User $loggedUser */
                        $loggedUser = $this->getUser();
                        $newMatiere->setCreator($loggedUser);
                        $newMatiere->setCreatedAt(new \DateTimeImmutable());
                        
                        // Auto-generate AI image
                        try {
                            $aiUrl = $this->imageService->generateAiImageUrl((string) $matiereName);
                            $newMatiere->setImageUrl($aiUrl);
                        } catch (\Exception $e) {
                            $newMatiere->setImageUrl($this->imageService->getPlaceholderUrl((string) $matiereName));
                        }
                        
                        $em->persist($newMatiere);
                        $cours->setMatiere($newMatiere);
                    }
                }
            }

            /** @var User $loggedUser */
            $loggedUser = $this->getUser();
            $cours->setAuthor($loggedUser);
            $cours->setStatus('PENDING');
            $cours->setCreatedAt(new \DateTimeImmutable());
            
            $em->persist($cours);
            $em->flush();
            
            $this->addFlash('success', 'Course proposed successfully! Now you can add resources.');
            return $this->redirectToRoute('app_creator_course_manage', ['id' => $cours->getId()]);
        } elseif ($form->isSubmitted() && !$form->isValid()) {
            foreach ($form->getErrors(true) as $error) {
                $this->addFlash('error', $error->getMessage());
            }
        }

        return $this->render('creator/new_course.html.twig', [
            'form' => $form->createView(),
        ]);
    }

    // 3. MANAGE COURSE
    #[Route('/course/{id}/manage', name: 'app_creator_course_manage')]
    public function manageCourse(Cours $cours): Response
    {
        // Security check
        if ($cours->getAuthor() !== $this->getUser()) {
            throw $this->createAccessDeniedException();
        }

        return $this->render('creator/manage_course.html.twig', [
            'cours' => $cours,
        ]);
    }

    // 4. ADD RESOURCE
    #[Route('/course/{id}/resource/new', name: 'app_creator_resource_new')]
    public function newResource(Cours $cours, Request $request, EntityManagerInterface $em): Response
    {
        // Security check
        if ($cours->getAuthor() !== $this->getUser()) {
            throw $this->createAccessDeniedException();
        }

        $resource = new Resource();
        $resource->setCours($cours);

        $form = $this->createForm(ResourceType::class, $resource);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $file = $form->get('file')->getData();
            if ($file) {
                $newFilename = uniqid().'.'.$file->getClientOriginalExtension();
                try {
                    $projectDir = $this->getParameter('kernel.project_dir');
                    $dir = is_string($projectDir) ? $projectDir : '';
                    $file->move(
                        $dir . '/public/uploads/resources',
                        $newFilename
                    );
                    $resource->setUrl('/uploads/resources/'.$newFilename);
                } catch (\Exception $e) {
                    $this->addFlash('error', 'File upload failed');
                }
            }

            /** @var User $loggedUser */
            $loggedUser = $this->getUser();
            $resource->setAuthor($loggedUser);
            $resource->setStatus('PENDING'); // Student proposals are pending
            
            $em->persist($resource);
            $em->flush();
            
            $this->addFlash('success', 'Resource added successfully! Waiting for moderation.');
            
            return $this->redirectToRoute('app_creator_course_manage', ['id' => $cours->getId()]);
        } elseif ($form->isSubmitted() && !$form->isValid()) {
            foreach ($form->getErrors(true) as $error) {
                $this->addFlash('error', $error->getMessage());
            }
        }

        return $this->render('creator/resource_new.html.twig', [
            'form' => $form->createView(),
            'cours' => $cours
        ]);
    }

    // 6. PROPOSE MATIERE
    #[Route('/matiere/new', name: 'app_creator_matiere_new')]
    public function newMatiere(Request $request, EntityManagerInterface $em): Response
    {
        $matiere = new \App\Entity\Matiere();
        // Students propose categories, so they are PENDING by default
        $matiere->setStatus('PENDING');

        // We can reuse MatiereType since it just has the name field
        $form = $this->createForm(\App\Form\MatiereType::class, $matiere);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $imageFile = $form->get('imageFile')->getData();
            if ($imageFile) {
                $newFilename = uniqid().'.'.$imageFile->getClientOriginalExtension();
                try {
                    $projectDir = $this->getParameter('kernel.project_dir');
                    $dir = is_string($projectDir) ? $projectDir : '';
                    $imageFile->move(
                        $dir . '/public/uploads/categories',
                        $newFilename
                    );
                    $matiere->setImageUrl('/uploads/categories/'.$newFilename);
                } catch (\Exception $e) {
                    $this->addFlash('error', 'Category image upload failed');
                }
            } else {
                // Auto-generate AI image if none uploaded
                try {
                    $aiUrl = $this->imageService->generateAiImageUrl((string) $matiere->getName());
                    $matiere->setImageUrl($aiUrl);
                } catch (\Exception $e) {
                    $matiere->setImageUrl($this->imageService->getPlaceholderUrl((string) $matiere->getName()));
                }
            }
            /** @var User $loggedUser */
            $loggedUser = $this->getUser();
            $matiere->setCreator($loggedUser);
            
            $em->persist($matiere);
            $em->flush();

            // Redirect back to courses, maybe with a flash message saying "Waiting for approval"
            $this->addFlash('success', 'Category proposed successfully! Waiting for admin approval.');
            return $this->redirectToRoute('app_student_courses');
        } elseif ($form->isSubmitted() && !$form->isValid()) {
            foreach ($form->getErrors(true) as $error) {
                $this->addFlash('error', $error->getMessage());
            }
        }

        return $this->render('creator/new_matiere.html.twig', [
            'form' => $form->createView(),
        ]);
    }
}