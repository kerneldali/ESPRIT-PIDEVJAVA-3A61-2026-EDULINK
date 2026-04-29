<?php

namespace App\Entity;

use App\Repository\EnrollmentRepository;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: EnrollmentRepository::class)]
class Enrollment
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\ManyToOne(inversedBy: 'enrollments')]
    private ?User $student = null;

    #[ORM\ManyToOne(inversedBy: 'enrollments')]
    #[ORM\JoinColumn(nullable: false, onDelete: "CASCADE")]
    private ?Cours $cours = null;

    #[ORM\Column]
    private ?\DateTimeImmutable $enrolledAt = null;

    #[ORM\Column]
    private ?int $progress = null;

    #[ORM\Column(nullable: true)]
    private ?\DateTimeImmutable $completedAt = null;

    #[ORM\Column(type: 'json', options: ['default' => '[]'])]
    private array $completedResources = [];

    #[ORM\Column(type: 'float', options: ['default' => 0.0])]
    private float $loginFrequencyPerWeek = 0.0;

    #[ORM\Column(type: 'float', options: ['default' => 0.0])]
    private float $avgSessionMinutes = 0.0;

    #[ORM\Column(type: 'float', options: ['default' => 0.0])]
    private float $assignmentsCompleted = 0.0;

    #[ORM\Column(type: 'float', options: ['default' => 0.0])]
    private float $quizAverageScore = 0.0;

    #[ORM\Column(type: 'float', options: ['default' => 0.0])]
    private float $forumInteractions = 0.0;

    #[ORM\Column(type: 'float', options: ['default' => 0.0])]
    private float $videoWatchPercent = 0.0;


    public function getId(): ?int
    {
        return $this->id;
    }

    public function getStudent(): ?User
    {
        return $this->student;
    }

    public function setStudent(?User $student): static
    {
        $this->student = $student;

        return $this;
    }

    public function getCours(): ?Cours
    {
        return $this->cours;
    }

    public function setCours(?Cours $cours): static
    {
        $this->cours = $cours;

        return $this;
    }

    public function getEnrolledAt(): ?\DateTimeImmutable
    {
        return $this->enrolledAt;
    }

    public function setEnrolledAt(\DateTimeImmutable $enrolledAt): static
    {
        $this->enrolledAt = $enrolledAt;

        return $this;
    }

    public function getProgress(): ?int
    {
        return $this->progress;
    }

    public function setProgress(int $progress): static
    {
        $this->progress = $progress;

        return $this;
    }

    public function getCompletedAt(): ?\DateTimeImmutable
    {
        return $this->completedAt;
    }

    public function setCompletedAt(?\DateTimeImmutable $completedAt): static
    {
        $this->completedAt = $completedAt;

        return $this;
    }

    public function getCompletedResources(): array
    {
        return $this->completedResources ?? [];
    }

    public function setCompletedResources(array $completedResources): static
    {
        $this->completedResources = $completedResources;

        return $this;
    }

    public function addCompletedResource(int $resourceId): static
    {
        if (!in_array($resourceId, $this->completedResources)) {
            $this->completedResources[] = $resourceId;
        }

        return $this;
    }

    public function getLoginFrequencyPerWeek(): float { return $this->loginFrequencyPerWeek; }
    public function setLoginFrequencyPerWeek(float $val): static { $this->loginFrequencyPerWeek = $val; return $this; }

    public function getAvgSessionMinutes(): float { return $this->avgSessionMinutes; }
    public function setAvgSessionMinutes(float $val): static { $this->avgSessionMinutes = $val; return $this; }

    public function getAssignmentsCompleted(): float { return $this->assignmentsCompleted; }
    public function setAssignmentsCompleted(float $val): static { $this->assignmentsCompleted = $val; return $this; }

    public function getQuizAverageScore(): float { return $this->quizAverageScore; }
    public function setQuizAverageScore(float $val): static { $this->quizAverageScore = $val; return $this; }

    public function getForumInteractions(): float { return $this->forumInteractions; }
    public function setForumInteractions(float $val): static { $this->forumInteractions = $val; return $this; }

    public function getVideoWatchPercent(): float { return $this->videoWatchPercent; }
    public function setVideoWatchPercent(float $val): static { $this->videoWatchPercent = $val; return $this; }
}
