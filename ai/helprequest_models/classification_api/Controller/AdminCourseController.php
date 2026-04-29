<?php

namespace App\Controller;

use App\Entity\Cours;
use App\Entity\Resource;
use App\Form\CoursType;
use App\Form\ResourceType;
use App\Repository\CoursRepository;
use Doctrine\ORM\EntityManagerInterface;
use App\Service\CategoryImageService;
use Knp\Component\Pager\PaginatorInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/admin/courses')]
class AdminCourseController extends AbstractController
{
    private PaginatorInterface $paginator;
    private CategoryImageService $imageService;

    public function __construct(CategoryImageService $imageService, PaginatorInterface $paginator)
    {
        $this->imageService = $imageService;
        $this->paginator = $paginator;
    }

    #[Route('/', name: 'app_admin_courses')]
    public function index(Request $request, CoursRepository $courseRepo): Response
    {
        $sort = (string) $request->query->get('sort', 'c.createdAt');
        $direction = (string) $request->query->get('direction', 'desc');

        $query = $courseRepo->createQueryBuilder('c')
            ->orderBy($sort, $direction)
            ->getQuery();

        $pagination = $this->paginator->paginate(
            $query,
            $request->query->getInt('page', 1),
            10
        );

        return $this->render('admin/courses.html.twig', [
            'pagination' => $pagination,
        ]);
    }

    // 2. CREATE NEW OFFICIAL COURSE
    #[Route('/new', name: 'app_admin_course_new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $em): Response
    {
        $cours = new Cours();
        $form = $this->createForm(CoursType::class, $cours);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            // Handle Matiere
            $matiereName = $form->get('matiere_name')->getData();
            if ($matiereName) {
                $matiereRepo = $em->getRepository(\App\Entity\Matiere::class);
                $matiere = $matiereRepo->findOneBy(['name' => $matiereName]);

                if (!$matiere) {
                    $matiere = new \App\Entity\Matiere();
                    $matiere->setName($matiereName);
                    $matiere->setStatus('APPROVED'); // Admin created, so approved
                    
                    // Auto-generate AI image
                    try {
                        $aiUrl = $this->imageService->generateAiImageUrl((string) $matiere->getName());
                        $matiere->setImageUrl($aiUrl);
                    } catch (\Exception $e) {
                        $matiere->setImageUrl($this->imageService->getPlaceholderUrl((string) $matiere->getName()));
                    }
                    
                    $em->persist($matiere);
                }
                $cours->setMatiere($matiere);
            }

            /** @var \App\Entity\User $user */
            $user = $this->getUser();
            $cours->setAuthor($user);
            $cours->setStatus('APPROVED'); // Auto-approved
            $cours->setCreatedAt(new \DateTimeImmutable());

            $em->persist($cours);
            $em->flush();

            $this->addFlash('success', 'Course created successfully!');
            return $this->redirectToRoute('app_admin_courses');
        } elseif ($form->isSubmitted() && !$form->isValid()) {
            foreach ($form->getErrors(true) as $error) {
                $this->addFlash('error', $error->getMessage());
            }
        }

        // Points to templates/admin/new_course.html.twig
        return $this->render('admin/new_course.html.twig', [
            'form' => $form->createView(),
        ]);
    }

    // 3. MANAGE COURSE (Resources)
    #[Route('/{id}/manage', name: 'app_admin_course_manage', methods: ['GET'])]
    public function manage(Cours $cours): Response
    {
        // Points to templates/admin/manage_course.html.twig
        return $this->render('admin/manage_course.html.twig', [
            'cours' => $cours,
        ]);
    }

    // 4. EDIT COURSE
    #[Route('/{id}/edit', name: 'app_admin_course_edit', methods: ['GET', 'POST'])]
    public function edit(Cours $cours, Request $request, EntityManagerInterface $em): Response
    {
        $form = $this->createForm(CoursType::class, $cours, [
            'hide_matiere' => true
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $em->flush();

            $this->addFlash('success', 'Course updated successfully!');
            $matiereId = $cours->getMatiere() ? $cours->getMatiere()->getId() : null;
            return $this->redirectToRoute('app_admin_category_manage', ['id' => $matiereId]);
        }

        return $this->render('admin/edit_course.html.twig', [
            'cours' => $cours,
            'form' => $form->createView(),
        ]);
    }

    // 5. DELETE COURSE
    #[Route('/{id}/delete', name: 'app_admin_course_delete', methods: ['POST'])]
    public function delete(Cours $cours, Request $request, EntityManagerInterface $em): Response
    {
        $matiereId = $cours->getMatiere() ? $cours->getMatiere()->getId() : null;
        
        if ($this->isCsrfTokenValid('delete'.$cours->getId(), (string) $request->request->get('_token'))) {
            $em->remove($cours);
            $em->flush();

            $this->addFlash('success', 'Course deleted successfully!');
        }

        return $this->redirectToRoute('app_admin_category_manage', ['id' => $matiereId]);
    }
}