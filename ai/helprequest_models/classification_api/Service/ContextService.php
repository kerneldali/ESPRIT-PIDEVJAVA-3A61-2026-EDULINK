<?php

namespace App\Service;

use App\Entity\User;
use App\Repository\MatiereRepository;
use App\Repository\CoursRepository;
use App\Repository\EventRepository;
use App\Repository\CommunityPostRepository;

class ContextService
{
    private MatiereRepository $matiereRepository;
    private CoursRepository $coursRepository;
    private EventRepository $eventRepository;
    private CommunityPostRepository $communityPostRepository;

    public function __construct(
        MatiereRepository $matiereRepository,
        CoursRepository $coursRepository,
        EventRepository $eventRepository,
        CommunityPostRepository $communityPostRepository
    ) {
        $this->matiereRepository = $matiereRepository;
        $this->coursRepository = $coursRepository;
        $this->eventRepository = $eventRepository;
        $this->communityPostRepository = $communityPostRepository;
    }

    /**
     * Gathers live data about the user to provide context for the AI.
     */
    public function getUserContext(User $user): string
    {
        $context = "USER LIVE DATA (PERSONAL CONTEXT):\n";
        $context .= "- Name: " . $user->getFullName() . "\n";
        $context .= "- Total XP / Wallet Balance: " . $user->getWalletBalance() . " PTS (Note: XP and Wallet Balance are the same resource on EduLink).\n";
        $context .= "- Roles: " . implode(', ', $user->getRoles()) . "\n";

        // 1. Notes (last 5)
        $notes = $user->getNotes();
        $recentNotes = array_slice($notes->toArray(), -5);
        $context .= "- Personal Notes (" . count($notes) . " total): " . (empty($recentNotes) ? "No notes yet." : "");
        foreach ($recentNotes as $note) {
            $context .= "\n  * '" . $note->getTitle() . "': " . substr($note->getContent(), 0, 100) . "...";
        }
        $context .= "\n";

        // 2. Course Enrollments
        $enrollments = $user->getEnrollments();
        $context .= "- Course Enrollments (" . count($enrollments) . " total): " . (count($enrollments) === 0 ? "Not enrolled in any courses yet." : "");
        foreach ($enrollments as $enrollment) {
            $course = $enrollment->getCours();
            $context .= "\n  * " . $course->getTitle() . " (" . $enrollment->getProgress() . "% completed)";
        }
        $context .= "\n";

        // 3. Help Requests
        $helpRequests = $user->getHelpRequests();
        $context .= "- Support/Help Requests (" . count($helpRequests) . " total): " . (count($helpRequests) === 0 ? "No active help requests." : "");
        foreach ($helpRequests as $hr) {
            $context .= "\n  * [" . $hr->getStatus() . "] " . $hr->getTitle();
        }
        $context .= "\n";

        // 4. Challenges
        $userChallenges = $user->getUserChallenges();
        $context .= "- Challenges (" . count($userChallenges) . " total): " . (count($userChallenges) === 0 ? "No challenges joined yet." : "");
        foreach ($userChallenges as $uc) {
            $challenge = $uc->getChallenge();
            $context .= "\n  * '" . $challenge->getTitle() . "' [" . $uc->getStatus() . "] - Progress: " . $uc->getProgress() . " - Reward: " . $challenge->getRewardPoints() . " XP";
        }
        $context .= "\n";

        // 5. Badges
        $userBadges = $user->getUserBadges();
        $context .= "- Earned Badges (" . count($userBadges) . " total): " . (count($userBadges) === 0 ? "No badges earned yet." : "");
        foreach ($userBadges as $ub) {
            $badge = $ub->getBadge();
            $context .= "\n  * '" . $badge->getName() . "' (requires " . $badge->getMinPoints() . " XP, unlocked at " . $ub->getUnlockedAt()->format('Y-m-d') . ")";
        }
        $context .= "\n";

        // 6. Reminders (last 5)
        $reminders = $user->getReminders();
        $recentReminders = array_slice($reminders->toArray(), -5);
        $context .= "- Reminders (" . count($reminders) . " total): " . (empty($recentReminders) ? "No reminders set." : "");
        foreach ($recentReminders as $reminder) {
            $context .= "\n  * '" . $reminder->getTitle() . "' [" . $reminder->getStatus() . "] - " . ($reminder->getReminderTime() ? $reminder->getReminderTime()->format('Y-m-d H:i') : 'no date');
        }
        $context .= "\n";

        // 7. Personal Tasks
        $tasks = $user->getTasks();
        $context .= "- Personal Tasks (" . count($tasks) . " total): " . (count($tasks) === 0 ? "No personal tasks." : "");
        foreach ($tasks as $task) {
            $status = $task->isCompleted() ? 'Done' : 'Pending';
            $context .= "\n  * [$status] " . $task->getTitle();
        }
        $context .= "\n";

        // 8. Recent Transactions (last 5)
        $transactions = $user->getTransactions();
        $recentTx = array_slice($transactions->toArray(), -5);
        $context .= "- Recent Transactions (" . count($transactions) . " total): " . (empty($recentTx) ? "No transactions yet." : "");
        foreach ($recentTx as $tx) {
            $context .= "\n  * [" . $tx->getType() . "] " . ($tx->getAmount() >= 0 ? '+' : '') . $tx->getAmount() . " XP on " . $tx->getDate()->format('Y-m-d');
        }
        $context .= "\n";

        // 9. Community Posts (via repository, last 5)
        $recentPosts = $this->communityPostRepository->findBy(
            ['author' => $user],
            ['createdAt' => 'DESC'],
            5
        );
        $context .= "- Community Posts (" . count($recentPosts) . " recent): " . (empty($recentPosts) ? "No community posts yet." : "");
        foreach ($recentPosts as $post) {
            $content = substr($post->getContent(), 0, 80);
            $context .= "\n  * [" . $post->getType() . "] " . $content . "... (" . $post->getLikesCount() . " likes)";
        }

        return $context;
    }

    /**
     * Gathers general platform knowledge for the AI.
     */
    public function getPlatformContext(): string
    {
        $matieres = $this->matiereRepository->findAll();
        $courses = $this->coursRepository->findBy([], ['id' => 'DESC'], 15);
        $events = $this->eventRepository->findBy([], ['id' => 'DESC'], 10);

        $matiereNames = array_map(fn($m) => $m->getName(), $matieres);

        $courseDetails = [];
        foreach ($courses as $c) {
            $matiereName = ($c->getMatiere()) ? $c->getMatiere()->getName() : 'General';
            $courseDetails[] = $c->getTitle() . " (Subject: " . $matiereName . ", " . $c->getXp() . " XP, Level: " . $c->getLevel() . ")";
        }

        $eventDetails = [];
        foreach ($events as $e) {
            $dateStr = ($e->getDateStart()) ? $e->getDateStart()->format('Y-m-d') : 'TBA';
            $eventDetails[] = $e->getTitle() . " (Date: " . $dateStr . ")";
        }

        $context = "EDULINK PLATFORM KNOWLEDGE (AVAILABLE CONTENT):\n";
        $context .= "- Subject Categories: " . (empty($matiereNames) ? 'None' : implode(', ', $matiereNames)) . "\n";
        $context .= "- Available Courses: " . (empty($courseDetails) ? 'None' : implode(', ', $courseDetails)) . "\n";
        $context .= "- Upcoming Events: " . (empty($eventDetails) ? 'None' : implode(', ', $eventDetails)) . "\n";

        return $context;
    }
}
