<?php

namespace App\Controller;

use App\Entity\Event;
use App\Entity\Reservation;
use App\Form\EventType;
use App\Repository\EventRepository;
use App\Service\GeminiEventService;
use App\Service\GoogleMeetService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\File\Exception\FileException;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/student')]
final class EventController extends AbstractController
{
    private const ALLOWED_IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'gif', 'webp'];
    private const MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10 MB

    public function __construct(
        private GeminiEventService $geminiService,
        private GoogleMeetService $meetService,
    ) {
    }

    #[Route('/events', name: 'app_event_index')]
    public function index(Request $request, EventRepository $eventRepository): Response
    {
        $filter = $request->query->get('filter', 'all');
        $search = $request->query->get('search');
        $user = $this->getUser();

        $queryBuilder = $eventRepository->createQueryBuilder('e')
            ->orderBy('e.dateStart', 'ASC');

        if ($search) {
            $queryBuilder->andWhere('e.title LIKE :search')
                ->setParameter('search', '%' . $search . '%');
        }

        if ($filter === 'upcoming') {
            $queryBuilder->andWhere('e.dateStart >= :today')
                ->setParameter('today', new \DateTime());
        } elseif ($filter === 'mine' && $user) {
            $queryBuilder->andWhere('e.organizer = :user')
                ->setParameter('user', $user);
        }

        $events = $queryBuilder->getQuery()->getResult();

        $form = $this->createForm(EventType::class, new Event());

        return $this->render('student/events.html.twig', [
            'events' => $events,
            'currentFilter' => $filter,
            'form' => $form->createView(),
        ]);
    }

    #[Route('/events/generate-description', name: 'app_event_generate_description', methods: ['POST'])]
    public function generateDescription(Request $request): JsonResponse
    {
        $this->denyAccessUnlessGranted('ROLE_USER');

        $data = json_decode($request->getContent(), true);

        $title = trim($data['title'] ?? '');
        $dateStart = $data['dateStart'] ?? '';
        $dateEnd = $data['dateEnd'] ?? '';
        $maxCapacity = (int) ($data['maxCapacity'] ?? 50);

        if (strlen($title) < 3) {
            return new JsonResponse([
                'success' => false,
                'error' => 'Please enter an event title (at least 3 characters) before generating.',
            ], 400);
        }

        $description = $this->geminiService->generateDescription($title, $dateStart, $dateEnd, $maxCapacity);

        return new JsonResponse([
            'success' => true,
            'description' => $description,
        ]);
    }

    #[Route('/events/new', name: 'app_event_new', methods: ['POST'])]
    public function new(Request $request, EntityManagerInterface $entityManager, \App\Service\PredictionService $predictionService): Response
    {
        $this->denyAccessUnlessGranted('ROLE_USER');

        $title = trim((string) $request->request->get('title', ''));
        $description = trim((string) $request->request->get('description', ''));
        $dateStartRaw = $request->request->get('dateStart');
        $dateEndRaw = $request->request->get('dateEnd');
        $maxCapacity = (int) $request->request->get('maxCapacity', 0);

        // --- Validation ---
        if (strlen($title) < 3) {
            $this->addFlash('error', 'Title must be at least 3 characters.');
            return $this->redirectToRoute('app_event_index');
        }
        if (strlen($description) < 10) {
            $this->addFlash('error', 'Description must be at least 10 characters.');
            return $this->redirectToRoute('app_event_index');
        }
        if (!$dateStartRaw || !$dateEndRaw) {
            $this->addFlash('error', 'Start and end dates are required.');
            return $this->redirectToRoute('app_event_index');
        }

        $dateStart = new \DateTime((string) $dateStartRaw);
        $dateEnd = new \DateTime((string) $dateEndRaw);

        if ($dateEnd <= $dateStart) {
            $this->addFlash('error', 'End date must be after start date.');
            return $this->redirectToRoute('app_event_index');
        }
        if ($maxCapacity < 1) {
            $this->addFlash('error', 'Max capacity must be at least 1.');
            return $this->redirectToRoute('app_event_index');
        }

        $event = new Event();
        /** @var \App\Entity\User $loggedUser */
        $loggedUser = $this->getUser();
        $event->setOrganizer($loggedUser);
        $event->setTitle($title);
        $event->setDescription($description);
        $event->setDateStart($dateStart);
        $event->setDateEnd($dateEnd);
        $event->setMaxCapacity($maxCapacity);

        $isOnline = $request->request->get('isOnline') === 'on';
        $event->setIsOnline($isOnline);

        if ($isOnline) {
            $meetLink = $this->meetService->generateMeetLink($title);
            $event->setMeetLink($meetLink);
            $event->setLocation(null);
        } else {
            $locationValue = $request->request->get('location');
            $event->setLocation(is_scalar($locationValue) ? (string) $locationValue : null);
        }

        // --- Image upload with validation ---
        $imageFile = $request->files->get('imageFile');
        if ($imageFile) {
            $error = $this->validateImage($imageFile);
            if ($error) {
                $this->addFlash('error', $error);
                return $this->redirectToRoute('app_event_index');
            }
            $newFilename = $this->uploadImage($imageFile);
            if ($newFilename) {
                $event->setImage($newFilename);
            }
        }

        $entityManager->persist($event);
        $entityManager->flush();

        // --- Call Python ML API ---
        $predictedScore = $predictionService->predictSuccess($event);
        $event->setPredictedScore($predictedScore);
        $entityManager->flush();
        
        if ($predictedScore === 0) {
            $this->addFlash('error', 'Événement créé. Succès prédit : Faible');
        } elseif ($predictedScore === 1) {
            $this->addFlash('warning', 'Événement créé. Succès prédit : Moyen');
        } elseif ($predictedScore === 2) {
            $this->addFlash('success', 'Événement créé. Succès prédit : Fort');
        } else {
            $this->addFlash('success', 'Event created successfully!'); // Fallback
        }

        return $this->redirectToRoute('app_event_index');
    }

    #[Route('/events/{id}/edit', name: 'app_event_edit', methods: ['GET', 'POST'])]
    public function edit(Request $request, Event $event, EntityManagerInterface $entityManager): Response
    {
        /** @var \App\Entity\User $user */
        $user = $this->getUser();
        if ($event->getOrganizer() !== $user && !in_array('ROLE_ADMIN', $user->getRoles())) {
            throw $this->createAccessDeniedException("You can only edit your own events.");
        }

        // GET: show the edit form
        if ($request->isMethod('GET')) {
            return $this->render('event/edit.html.twig', [
                'event' => $event,
            ]);
        }

        // POST: process the update
        $title = trim((string) $request->request->get('title', ''));
        $description = trim((string) $request->request->get('description', ''));
        $maxCapacity = (int) $request->request->get('maxCapacity', 0);

        if (strlen($title) < 3) {
            $this->addFlash('error', 'Title must be at least 3 characters.');
            return $this->redirectToRoute('app_event_edit', ['id' => $event->getId()]);
        }
        if ($maxCapacity < 1) {
            $this->addFlash('error', 'Max capacity must be at least 1.');
            return $this->redirectToRoute('app_event_edit', ['id' => $event->getId()]);
        }

        $event->setTitle($title);
        $event->setDescription($description);
        $event->setMaxCapacity($maxCapacity);

        $dateStartRaw = $request->request->get('dateStart');
        $dateEndRaw = $request->request->get('dateEnd');
        if ($dateStartRaw) {
            $event->setDateStart(new \DateTime((string) $dateStartRaw));
        }
        if ($dateEndRaw) {
            $event->setDateEnd(new \DateTime((string) $dateEndRaw));
        }

        $isOnline = $request->request->get('isOnline') === 'on';
        $event->setIsOnline($isOnline);
        if ($isOnline) {
            if (!$event->getMeetLink()) {
                $event->setMeetLink($this->meetService->generateMeetLink($title));
            }
            $event->setLocation(null);
        } else {
            $locationValue = $request->request->get('location');
            $event->setLocation(is_scalar($locationValue) ? (string) $locationValue : null);
        }

        $imageFile = $request->files->get('image');
        if ($imageFile) {
            $error = $this->validateImage($imageFile);
            if ($error) {
                $this->addFlash('error', $error);
                return $this->redirectToRoute('app_event_edit', ['id' => $event->getId()]);
            }
            if ($event->getImage()) {
                $oldPath = (string) $this->getParameter('events_images_directory') . '/' . $event->getImage();
                if (file_exists($oldPath)) {
                    @unlink($oldPath);
                }
            }
            $newFilename = $this->uploadImage($imageFile);
            if ($newFilename) {
                $event->setImage($newFilename);
            }
        }

        $entityManager->flush();

        $this->addFlash('success', 'Event updated successfully!');
        return $this->redirectToRoute('app_event_index');
    }

    #[Route('/events/{id}/delete', name: 'app_event_delete', methods: ['POST'])]
    public function delete(Request $request, Event $event, EntityManagerInterface $entityManager): Response
    {
        if ($this->getUser() !== $event->getOrganizer()) {
            $this->addFlash('error', 'You can only delete your own events!');
            return $this->redirectToRoute('app_event_index');
        }

        if ($this->isCsrfTokenValid('delete' . $event->getId(), (string) $request->request->get('_token'))) {
            // Remove image file
            if ($event->getImage()) {
                $path = (string) $this->getParameter('events_images_directory') . '/' . $event->getImage();
                if (file_exists($path)) {
                    @unlink($path);
                }
            }
            $entityManager->remove($event);
            $entityManager->flush();
            $this->addFlash('success', 'Event deleted successfully.');
        }

        return $this->redirectToRoute('app_event_index');
    }

    #[Route('/events/{id}/reserve', name: 'app_reservation_new', methods: ['POST'])]
    public function reserve(Event $event, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_USER');
        $user = $this->getUser();

        // Already reserved?
        foreach ($event->getReservations() as $reservation) {
            if ($reservation->getUser() === $user) {
                $this->addFlash('info', 'You already joined this event!');
                return $this->redirectToRoute('app_event_index');
            }
        }

        // Full?
        if ($event->getReservations()->count() >= $event->getMaxCapacity()) {
            $this->addFlash('error', 'This event is full!');
            return $this->redirectToRoute('app_event_index');
        }

        // Can't join your own event
        if ($event->getOrganizer() === $user) {
            $this->addFlash('info', 'You are the organizer of this event.');
            return $this->redirectToRoute('app_event_index');
        }

        $reservation = new Reservation();
        /** @var \App\Entity\User $loggedUser */
        $loggedUser = $this->getUser();
        $reservation->setUser($loggedUser);
        $reservation->setEvent($event);
        $reservation->setReservedAt(new \DateTime());

        $entityManager->persist($reservation);
        $entityManager->flush();

        $this->addFlash('success', 'You have joined the event!');
        return $this->redirectToRoute('app_event_index');
    }

    // --- Private Helpers ---

    private function validateImage(\Symfony\Component\HttpFoundation\File\UploadedFile $file): ?string
    {
        if (!$file->isValid()) {
            return 'Image upload failed: ' . $file->getErrorMessage();
        }

        $ext = strtolower($file->getClientOriginalExtension());
        if (!in_array($ext, self::ALLOWED_IMAGE_EXTENSIONS, true)) {
            return 'Invalid image format. Allowed: ' . implode(', ', self::ALLOWED_IMAGE_EXTENSIONS);
        }

        if ($file->getSize() > self::MAX_IMAGE_SIZE) {
            return 'Image too large. Maximum size is 10 MB.';
        }

        return null;
    }

    private function uploadImage(\Symfony\Component\HttpFoundation\File\UploadedFile $file): ?string
    {
        $ext = strtolower($file->getClientOriginalExtension());
        $newFilename = uniqid('event_', true) . '.' . $ext;

        try {
            $file->move(
                (string) $this->getParameter('events_images_directory'),
                $newFilename
            );
            return $newFilename;
        } catch (FileException $e) {
            $this->addFlash('error', 'Failed to save image.');
            return null;
        }
    }
}
