<?php

namespace App\Service;

use App\Entity\HelpRequest;
use App\Entity\User;
use App\Repository\HelpRequestRepository;
use Doctrine\ORM\EntityManagerInterface;
use App\Service\MLService;

/**
 * AI Smart Matching Service
 * Finds the best tutor candidates for a help request using:
 *  - TF-IDF cosine similarity (via local ML API) for semantic text matching
 *  - Weighted scoring: (0.35 × textSimilarity) + (0.25 × avgRating) + (0.20 × completedSessions) + (0.20 × availability)
 */
class SmartMatchingService
{
    public function __construct(
        private EntityManagerInterface $em,
        private MLService $mlService
    ) {
    }

    /**
     * Find the top N best matching tutors for a given help request.
     *
     * @return array<int, array{user: User, score: float, avgRating: float, completedSessions: int, isAvailable: bool, similarity: float}>
     */
    public function findBestTutors(HelpRequest $helpRequest, int $limit = 3): array
    {
        $student = $helpRequest->getStudent();
        $requestText = $helpRequest->getTitle() . ' ' . $helpRequest->getDescription();

        // Get all users who have been tutors at least once (excluding the requesting student)
        $tutorCandidates = $this->em->createQuery(
            'SELECT DISTINCT u FROM App\Entity\User u
             JOIN App\Entity\Session s WITH s.tutor = u
             WHERE u != :student'
        )
            ->setParameter('student', $student)
            ->getResult();

        if (empty($tutorCandidates)) {
            return [];
        }

        // Build tutor histories for TF-IDF matching
        $tutorHistories = $this->buildTutorHistories($tutorCandidates);

        // Call ML API for TF-IDF similarity scores
        $similarityScores = $this->getTfidfScores($requestText, $tutorHistories);

        $scoredCandidates = [];

        foreach ($tutorCandidates as $tutor) {
            $stats = $this->calculateTutorStats($tutor, $helpRequest->getCategory());

            // Use TF-IDF similarity if available, otherwise fall back to binary category match
            $textSimilarity = $similarityScores[$tutor->getId()] ?? ($stats['categoryMatch'] ? 1.0 : 0.0);

            $score = $this->calculateScore($stats, $textSimilarity);

            $scoredCandidates[] = [
                'user' => $tutor,
                'score' => round($score * 100),
                'avgRating' => $stats['avgRating'],
                'completedSessions' => $stats['completedSessions'],
                'isAvailable' => $stats['isAvailable'],
                'categoryMatch' => $stats['categoryMatch'],
                'similarity' => round($textSimilarity, 2),
            ];
        }

        // Sort by score descending
        usort($scoredCandidates, fn($a, $b) => $b['score'] <=> $a['score']);

        return array_slice($scoredCandidates, 0, $limit);
    }

    /**
     * Build a text history for each tutor from their past session help requests.
     * @return array<int, array{id: int, history: string}>
     */
    private function buildTutorHistories(array $tutors): array
    {
        $histories = [];

        foreach ($tutors as $tutor) {
            $pastTexts = $this->em->createQuery(
                'SELECT hr.title, hr.description, hr.category FROM App\Entity\Session s
                 JOIN s.helpRequest hr
                 WHERE s.tutor = :tutor AND s.isActive = false'
            )
                ->setParameter('tutor', $tutor)
                ->getResult();

            $combined = '';
            foreach ($pastTexts as $row) {
                $combined .= ($row['category'] ?? '') . ' ' . $row['title'] . ' ' . $row['description'] . ' ';
            }

            $combined = trim($combined);
            if (!empty($combined)) {
                $histories[] = [
                    'id' => $tutor->getId(),
                    'history' => $combined,
                ];
            }
        }

        return $histories;
    }

    /**
     * Call ML API for TF-IDF cosine similarity scores.
     * @return array<int, float>  Map of tutor_id => similarity (0.0 to 1.0)
     */
    private function getTfidfScores(string $requestText, array $tutorHistories): array
    {
        if (empty($tutorHistories)) {
            return [];
        }

        return $this->mlService->smartMatch($requestText, $tutorHistories);
    }

    private function calculateTutorStats(User $tutor, ?string $category): array
    {
        // 1. Completed sessions count
        $completedSessions = (int) $this->em->createQuery(
            'SELECT COUNT(s.id) FROM App\Entity\Session s
             WHERE s.tutor = :tutor AND s.isActive = false'
        )
            ->setParameter('tutor', $tutor)
            ->getSingleScalarResult();

        // 2. Average rating from reviews
        $avgRating = (float) $this->em->createQuery(
            'SELECT AVG(r.rating) FROM App\Entity\Review r
             JOIN r.session s WHERE s.tutor = :tutor'
        )
            ->setParameter('tutor', $tutor)
            ->getSingleScalarResult();

        // 3. Category match — has tutor helped in this category before?
        $categoryMatch = false;
        if ($category) {
            $categorySessionCount = (int) $this->em->createQuery(
                'SELECT COUNT(s.id) FROM App\Entity\Session s
                 JOIN s.helpRequest hr
                 WHERE s.tutor = :tutor AND hr.category = :category AND s.isActive = false'
            )
                ->setParameter('tutor', $tutor)
                ->setParameter('category', $category)
                ->getSingleScalarResult();
            $categoryMatch = $categorySessionCount > 0;
        }

        // 4. Availability — does tutor have any active sessions?
        $activeSessions = (int) $this->em->createQuery(
            'SELECT COUNT(s.id) FROM App\Entity\Session s
             WHERE s.tutor = :tutor AND s.isActive = true'
        )
            ->setParameter('tutor', $tutor)
            ->getSingleScalarResult();

        return [
            'completedSessions' => $completedSessions,
            'avgRating' => $avgRating ?: 0.0,
            'categoryMatch' => $categoryMatch,
            'isAvailable' => $activeSessions === 0,
        ];
    }

    /**
     * Weighted scoring with TF-IDF text similarity.
     *
     * Score = (0.35 × textSimilarity) + (0.25 × avgRating) + (0.20 × completedSessions) + (0.20 × availability)
     */
    private function calculateScore(array $stats, float $textSimilarity): float
    {
        $ratingScore = $stats['avgRating'] / 5.0;
        $sessionsScore = min($stats['completedSessions'] / 10.0, 1.0);
        $availabilityScore = $stats['isAvailable'] ? 1.0 : 0.5;

        return (0.35 * $textSimilarity)
            + (0.25 * $ratingScore)
            + (0.20 * $sessionsScore)
            + (0.20 * $availabilityScore);
    }
}
