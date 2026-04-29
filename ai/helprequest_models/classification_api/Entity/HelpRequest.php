<?php

namespace App\Entity;

use App\Repository\HelpRequestRepository;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: HelpRequestRepository::class)]
class HelpRequest
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\Column(length: 255)]
    #[Assert\NotBlank(message: 'The title cannot be empty.')]
    #[Assert\Length(
        min: 3,
        max: 255,
        minMessage: 'The title must be at least {{ limit }} characters.',
        maxMessage: 'The title cannot exceed {{ limit }} characters.'
    )]
    private ?string $title = null;

    #[ORM\Column(type: Types::TEXT)]
    #[Assert\NotBlank(message: 'The description cannot be empty.')]
    #[Assert\Length(
        min: 10,
        minMessage: 'The description must be at least {{ limit }} characters.'
    )]
    private ?string $description = null;

    #[ORM\Column(length: 20)]
    #[Assert\Choice(
        choices: ['OPEN', 'IN_PROGRESS', 'CLOSED'],
        message: 'Invalid status. Allowed: OPEN, IN_PROGRESS, CLOSED.'
    )]
    private string $status = 'OPEN';

    #[ORM\Column]
    #[Assert\NotNull(message: 'Bounty is required.')]
    #[Assert\PositiveOrZero(message: 'Bounty cannot be negative.')]
    private int $bounty = 0;

    #[ORM\Column]
    private bool $isTicket = false;

    #[ORM\Column]
    private ?\DateTimeImmutable $createdAt = null;

    #[ORM\ManyToOne(inversedBy: 'helpRequests')]
    #[ORM\JoinColumn(nullable: false)]
    private ?User $student = null;

    #[ORM\OneToOne(mappedBy: 'helpRequest', cascade: ['persist', 'remove'])]
    private ?Session $session = null;

    #[ORM\Column(length: 50, nullable: true)]
    private ?string $category = null;

    #[ORM\Column(length: 20, nullable: true)]
    private ?string $difficulty = null;

    #[ORM\Column(length: 30, nullable: true)]
    private ?string $closeReason = null;

    public function __construct()
    {
        $this->createdAt = new \DateTimeImmutable();
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getTitle(): ?string
    {
        return $this->title;
    }

    public function setTitle(string $title): static
    {
        $this->title = $title;
        return $this;
    }

    public function getDescription(): ?string
    {
        return $this->description;
    }

    public function setDescription(string $description): static
    {
        $this->description = $description;
        return $this;
    }

    public function getStatus(): string
    {
        return $this->status;
    }

    public function setStatus(string $status): static
    {
        $this->status = $status;
        return $this;
    }

    public function getBounty(): int
    {
        return $this->bounty;
    }

    public function setBounty(?int $bounty): static
    {
        $this->bounty = $bounty ?? 0;
        return $this;
    }

    public function isIsTicket(): bool
    {
        return $this->isTicket;
    }

    public function setIsTicket(bool $isTicket): static
    {
        $this->isTicket = $isTicket;
        return $this;
    }

    public function getCreatedAt(): ?\DateTimeImmutable
    {
        return $this->createdAt;
    }

    public function setCreatedAt(\DateTimeImmutable $createdAt): static
    {
        $this->createdAt = $createdAt;
        return $this;
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

    public function getSession(): ?Session
    {
        return $this->session;
    }

    public function setSession(?Session $session): static
    {
        if ($session === null && $this->session !== null) {
            $this->session->setHelpRequest(null);
        }

        if ($session !== null && $session->getHelpRequest() !== $this) {
            $session->setHelpRequest($this);
        }

        $this->session = $session;
        return $this;
    }

    public function getCloseReason(): ?string
    {
        return $this->closeReason;
    }

    public function setCloseReason(?string $closeReason): static
    {
        $this->closeReason = $closeReason;
        return $this;
    }

    public function getCategory(): ?string
    {
        return $this->category;
    }

    public function setCategory(?string $category): static
    {
        $this->category = $category;
        return $this;
    }

    public function getDifficulty(): ?string
    {
        return $this->difficulty;
    }

    public function setDifficulty(?string $difficulty): static
    {
        $this->difficulty = $difficulty;
        return $this;
    }
}