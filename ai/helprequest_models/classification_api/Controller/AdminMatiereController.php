<?php

namespace App\Controller;

use App\Entity\Matiere;
use App\Entity\Cours;
use App\Form\MatiereType;
use App\Repository\MatiereRepository;
use Doctrine\ORM\EntityManagerInterface;
use App\Service\CategoryImageService;
use Knp\Component\Pager\PaginatorInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/admin/categories')]
class AdminMatiereController extends AbstractController
{
    private CategoryImageService $imageService;
    private PaginatorInterface $paginator;

    public function __construct(CategoryImageService $imageService, PaginatorInterface $paginator)
    {
        $this->imageService = $imageService;
        $this->paginator = $paginator;
    }
    #[Route('/', name: 'app_admin_categories')]
    public function index(Request $request, MatiereRepository $matiereRepo): Response
    {
        $q = $request->query->get('q');
        $sort = $request->query->get('sort', 'name_asc');

        $qb = $matiereRepo->createQueryBuilder('m')
            ->where('m.status = :status')
            ->setParameter('status', 'APPROVED');

        if ($q) {
            $qb->andWhere('m.name LIKE :q')
                ->setParameter('q', '%' . $q . '%');
        }

        if ($sort === 'newest') {
            $qb->orderBy('m.id', 'DESC');
        } else {
            $qb->orderBy('m.name', 'ASC');
        }

        $paginatorOptions = [
            'sortFieldParameterName' => '_knp_sort',
            'sortDirectionParameterName' => '_knp_dir',
        ];

        $pagination = $this->paginator->paginate(
            $qb->getQuery(),
            $request->query->getInt('page', 1),
            12,
            $paginatorOptions
        );

        return $this->render('admin/categories/index.html.twig', [
            'pagination' => $pagination,
            'q' => $q,
            'sort' => $sort,
        ]);
    }

    #[Route('/{id}/manage', name: 'app_admin_category_manage')]
    public function manage(Matiere $matiere, Request $request, EntityManagerInterface $em): Response
    {
        $q = $request->query->get('q');
        $level = $request->query->get('level');
        $sort = $request->query->get('sort', 'newest');

        $coursRepo = $em->getRepository(Cours::class);
        $qb = $coursRepo->createQueryBuilder('c')
            ->where('c.matiere = :matiere')
            ->setParameter('matiere', $matiere);

        if ($q) {
            $qb->andWhere('c.title LIKE :q OR c.description LIKE :q')
                ->setParameter('q', '%' . $q . '%');
        }

        if ($level) {
            $qb->andWhere('c.level = :level')
                ->setParameter('level', $level);
        }

        switch ($sort) {
            case 'reward_high':
                $qb->orderBy('c.xp', 'DESC');
                break;
            case 'reward_low':
                $qb->orderBy('c.xp', 'ASC');
                break;
            case 'alpha_asc':
                $qb->orderBy('c.title', 'ASC');
                break;
            default:
                $qb->orderBy('c.id', 'DESC');
        }

        $pagination = $this->paginator->paginate(
            $qb->getQuery(),
            $request->query->getInt('page', 1),
            10,
            [
                'sortFieldParameterName' => '_knp_sort',
                'sortDirectionParameterName' => '_knp_dir',
            ]
        );

        return $this->render('admin/categories/manage.html.twig', [
            'matiere' => $matiere,
            'pagination' => $pagination,
            'q' => $q,
            'level' => $level,
            'sort' => $sort,
        ]);
    }

    #[Route('/{id}/new-course', name: 'app_admin_category_new_course', methods: ['GET', 'POST'])]
    public function newCourse(Matiere $matiere, Request $request, EntityManagerInterface $em): Response
    {
        $cours = new Cours();
        $cours->setMatiere($matiere);
        $cours->setStatus('APPROVED'); // Admin-created = auto-approved
        /** @var \App\Entity\User $user */
        $user = $this->getUser();
        $cours->setAuthor($user);
        $cours->setCreatedAt(new \DateTimeImmutable());

        $form = $this->createForm(\App\Form\CoursType::class, $cours, [
            'hide_matiere' => true
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $em->persist($cours);
            $em->flush();

            $this->addFlash('success', 'Course added successfully!');
            return $this->redirectToRoute('app_admin_category_manage', ['id' => $matiere->getId()]);
        } elseif ($form->isSubmitted() && !$form->isValid()) {
            foreach ($form->getErrors(true) as $error) {
                $this->addFlash('error', $error->getMessage());
            }
        }

        return $this->render('admin/categories/new_course.html.twig', [
            'matiere' => $matiere,
            'form' => $form->createView(),
        ]);
    }

    #[Route('/new', name: 'app_admin_category_new', methods: ['GET', 'POST'])]
    public function newMatiere(Request $request, EntityManagerInterface $em): Response
    {
        $matiere = new Matiere();
        $matiere->setStatus('APPROVED'); // Admin-created = auto-approved
        /** @var \App\Entity\User $user */
        $user = $this->getUser();
        $matiere->setCreator($user);

        $form = $this->createForm(MatiereType::class, $matiere);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $imageFile = $form->get('imageFile')->getData();
            if ($imageFile) {
                $newFilename = uniqid() . '.' . $imageFile->getClientOriginalExtension();
                try {
                    $projectDir = $this->getParameter('kernel.project_dir');
                    $dir = is_string($projectDir) ? $projectDir : '';
                    $imageFile->move(
                        $dir . '/public/uploads/categories',
                        $newFilename
                    );
                    $matiere->setImageUrl('/uploads/categories/' . $newFilename);
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
            $em->persist($matiere);
            $em->flush();

            $this->addFlash('success', 'Category created successfully!');
            return $this->redirectToRoute('app_admin_categories');
        } elseif ($form->isSubmitted() && !$form->isValid()) {
            foreach ($form->getErrors(true) as $error) {
                $this->addFlash('error', $error->getMessage());
            }
        }

        return $this->render('admin/categories/new.html.twig', [
            'form' => $form->createView(),
        ]);
    }

    #[Route('/{id}/edit', name: 'app_admin_category_edit', methods: ['GET', 'POST'])]
    public function edit(Matiere $matiere, Request $request, EntityManagerInterface $em): Response
    {
        $form = $this->createForm(MatiereType::class, $matiere);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $imageFile = $form->get('imageFile')->getData();
            if ($imageFile) {
                $newFilename = uniqid() . '.' . $imageFile->getClientOriginalExtension();
                try {
                    $projectDir = $this->getParameter('kernel.project_dir');
                    $dir = is_string($projectDir) ? $projectDir : '';
                    $imageFile->move(
                        $dir . '/public/uploads/categories',
                        $newFilename
                    );
                    $matiere->setImageUrl('/uploads/categories/' . $newFilename);
                } catch (\Exception $e) {
                    $this->addFlash('error', 'Category image upload failed');
                }
            } elseif (!$matiere->getImageUrl()) {
                // Auto-generate AI image if none exists and no file uploaded
                try {
                    $aiUrl = $this->imageService->generateAiImageUrl((string) $matiere->getName());
                    $matiere->setImageUrl($aiUrl);
                } catch (\Exception $e) {
                    $matiere->setImageUrl($this->imageService->getPlaceholderUrl((string) $matiere->getName()));
                }
            }
            $em->flush();

            $this->addFlash('success', 'Category updated successfully!');
            return $this->redirectToRoute('app_admin_categories');
        }

        return $this->render('admin/categories/edit.html.twig', [
            'matiere' => $matiere,
            'form' => $form->createView(),
        ]);
    }

    #[Route('/{id}/delete', name: 'app_admin_category_delete', methods: ['POST'])]
    public function delete(Matiere $matiere, Request $request, EntityManagerInterface $em): Response
    {
        if ($this->isCsrfTokenValid('delete' . $matiere->getId(), (string) $request->request->get('_token'))) {
            $em->remove($matiere);
            $em->flush();

            $this->addFlash('success', 'Category deleted successfully!');
        }

        return $this->redirectToRoute('app_admin_categories');
    }

    #[Route('/{id}/refresh-image', name: 'app_admin_category_refresh_image', methods: ['POST'])]
    public function refreshImage(Matiere $matiere, EntityManagerInterface $em): Response
    {
        try {
            $aiUrl = $this->imageService->generateAiImageUrl((string) $matiere->getName());
            $matiere->setImageUrl($aiUrl);
            $em->flush();
            $this->addFlash('success', 'Category visual refreshed!');
        } catch (\Exception $e) {
            $this->addFlash('error', 'Failed to refresh AI image.');
        }

        return $this->redirectToRoute('app_admin_categories');
    }
}
