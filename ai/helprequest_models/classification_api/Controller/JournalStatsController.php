<?php

namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class JournalStatsController extends AbstractController
{
    #[Route('/admin/journal-statistics', name: 'admin_journal_stats')]
    public function index(
        \App\Repository\NoteRepository $noteRepository,
        \App\Repository\CategoryRepository $categoryRepository,
        \Symfony\Component\HttpFoundation\Request $request
    ): Response
    {
        $notes = $noteRepository->findAll();
        $searchQuery = trim((string)$request->query->get('q', ''));

        $statsByUser = [];
        $last7Days = [];
        $today = new \DateTimeImmutable('today');
        for ($i = 6; $i >= 0; $i--) {
            $day = $today->sub(new \DateInterval('P' . $i . 'D'))->format('Y-m-d');
            $last7Days[$day] = 0;
        }

        foreach ($notes as $note) {
            $user = $note->getUser();
            if (!$user) continue;
            $uid = $user->getId();
            if (!isset($statsByUser[$uid])) {
                $statsByUser[$uid] = [
                    'fullName' => $user->getFullName(),
                    'email' => $user->getEmail(),
                    'totalNotes' => 0,
                    'totalCategories' => 0,
                    'avgNotesPerCategory' => 0,
                ];
            }
            $statsByUser[$uid]['totalNotes']++;

            $createdAt = $note->getCreatedAt();
            if ($createdAt instanceof \DateTimeInterface) {
                $key = $createdAt->format('Y-m-d');
                if (isset($last7Days[$key])) {
                    $last7Days[$key]++;
                }
            }
        }

        foreach ($statsByUser as $uid => &$row) {
            $userObj = null;
            foreach ($notes as $n) {
                if ($n->getUser() && $n->getUser()->getId() === $uid) {
                    $userObj = $n->getUser();
                    break;
                }
            }
            if ($userObj) {
                $row['totalCategories'] = count($categoryRepository->findBy(['owner' => $userObj]));
                $row['avgNotesPerCategory'] = $row['totalCategories'] > 0 ? round($row['totalNotes'] / $row['totalCategories'], 2) : $row['totalNotes'];
            }
        }
        unset($row);

        if ($searchQuery) {
            $q = mb_strtolower($searchQuery);
            $statsByUser = array_filter($statsByUser, function ($s) use ($q) {
                return str_contains(mb_strtolower($s['fullName'] ?? ''), $q)
                    || str_contains(mb_strtolower($s['email'] ?? ''), $q);
            });
        }

        $totalStudents = count($statsByUser);
        $grandTotalNotes = array_sum(array_map(fn($s) => $s['totalNotes'], $statsByUser));
        $avgNotesPerStudent = $totalStudents > 0 ? round($grandTotalNotes / $totalStudents, 2) : 0;
        $avgCategoriesPerStudent = $totalStudents > 0 ? round(array_sum(array_map(fn($s) => $s['totalCategories'], $statsByUser)) / $totalStudents, 2) : 0;

        usort($statsByUser, fn($a, $b) => $b['totalNotes'] <=> $a['totalNotes']);
        $leaderboardNotes = array_slice($statsByUser, 0, 10);
        usort($statsByUser, fn($a, $b) => $b['totalCategories'] <=> $a['totalCategories']);
        $leaderboardCategories = array_slice($statsByUser, 0, 10);

        return $this->render('admin/journal_statistics.html.twig', [
            'journalStats' => $statsByUser,
            'searchQuery' => $searchQuery,
            'summary' => [
                'totalStudents' => $totalStudents,
                'grandTotalNotes' => $grandTotalNotes,
                'avgNotesPerStudent' => $avgNotesPerStudent,
                'avgCategoriesPerStudent' => $avgCategoriesPerStudent,
                'last7Days' => $last7Days,
                'topByNotes' => $leaderboardNotes,
                'topByCategories' => $leaderboardCategories,
            ],
        ]);
    }
}
