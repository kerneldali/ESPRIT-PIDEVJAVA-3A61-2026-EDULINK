<?php

namespace App\Entity;

use App\Repository\UserRepository;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use App\Entity\PersonalTask;
use Doctrine\ORM\Mapping as ORM;

use Symfony\Component\Security\Core\User\PasswordAuthenticatedUserInterface;
use Symfony\Component\Security\Core\User\UserInterface;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: UserRepository::class)]
class User implements UserInterface, PasswordAuthenticatedUserInterface
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\Column(length: 150)]
    #[Assert\NotBlank(message: "Email is required.")]
    #[Assert\Email(message: "The email '{{ value }}' is not a valid email.")]
    private ?string $email = null;

    #[ORM\Column(length: 150, nullable: true)]
    #[Assert\NotBlank(message: "Full name is required.")]
    private ?string $fullName = null;

    #[ORM\Column(length: 255)]
    private ?string $password = null;

    #[ORM\Column(type: 'integer', options: ['default' => 0])]
    private int $xp = 0;

    #[ORM\Column(type: 'float', options: ['default' => 0])]
    private float $walletBalance = 0;

    #[ORM\Column]
    private array $roles = [];

    #[ORM\Column(nullable: true)]
    private ?array $faceDescriptor = null;

    #[ORM\Column(type: 'boolean', options: ['default' => false])]
    private bool $isVerified = false;

    /**
     * @var Collection<int, UserBadge>
     */
    #[ORM\OneToMany(targetEntity: UserBadge::class, mappedBy: 'user', orphanRemoval: true)]
    private Collection $userBadges;

    /**
     * @var Collection<int, Transaction>
     */
    #[ORM\OneToMany(targetEntity: Transaction::class, mappedBy: 'user', orphanRemoval: true)]
    private Collection $transactions;



    /**
     * @var Collection<int, Event>
     */
    #[ORM\OneToMany(targetEntity: Event::class, mappedBy: 'organizer')]
    private Collection $events;

    /**
     * @var Collection<int, Reservation>
     */
    #[ORM\OneToMany(targetEntity: Reservation::class, mappedBy: 'user')]
    private Collection $reservations;

    /**
     * @var Collection<int, UserChallenge>
     */
    #[ORM\OneToMany(targetEntity: UserChallenge::class, mappedBy: 'user')]
    private Collection $userChallenges;

    /**
     * @var Collection<int, UserMatiereStat>
     */
    #[ORM\OneToMany(targetEntity: UserMatiereStat::class, mappedBy: 'user')]
    private Collection $userMatiereStats;

    /**
     * @var Collection<int, AiSentimentLog>
     */
    #[ORM\OneToMany(targetEntity: AiSentimentLog::class, mappedBy: 'user', orphanRemoval: true)]
    private Collection $aiSentimentLogs;

    public function __construct()
    {
        $this->userBadges = new ArrayCollection();
        $this->transactions = new ArrayCollection();
        $this->enrollments = new ArrayCollection();
        $this->cours = new ArrayCollection();
        $this->createdMatieres = new ArrayCollection();
        $this->resources = new ArrayCollection();
        $this->helpRequests = new ArrayCollection();
        $this->notes = new ArrayCollection();
        $this->notifications = new ArrayCollection();
        $this->reminders = new ArrayCollection();
        $this->sessionsAsTutor = new ArrayCollection();
        $this->tasks = new ArrayCollection();
        $this->events = new ArrayCollection();
        $this->reservations = new ArrayCollection();
        $this->userChallenges = new ArrayCollection();
        $this->userMatiereStats = new ArrayCollection();
        $this->aiSentimentLogs = new ArrayCollection();
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getEmail(): ?string
    {
        return $this->email;
    }

    public function setEmail(string $email): static
    {
        $this->email = $email;

        return $this;
    }

    public function getPassword(): ?string
    {
        return $this->password;
    }

    public function setPassword(string $password): static
    {
        $this->password = $password;

        return $this;
    }

    /**
     * @see UserInterface
     */
    public function getRoles(): array
    {
        $roles = $this->roles;
        // guarantee every user at least has ROLE_USER
        $roles[] = 'ROLE_USER';

        return array_unique($roles);
    }

    public function setRoles(array $roles): static
    {
        $this->roles = $roles;

        return $this;
    }

    /**
     * @return Collection<int, UserBadge>
     */
    public function getUserBadges(): Collection
    {
        return $this->userBadges;
    }

    public function addUserBadge(UserBadge $userBadge): static
    {
        if (!$this->userBadges->contains($userBadge)) {
            $this->userBadges->add($userBadge);
            $userBadge->setUser($this);
        }

        return $this;
    }

    public function removeUserBadge(UserBadge $userBadge): static
    {
        if ($this->userBadges->removeElement($userBadge)) {
            // set the owning side to null (unless already changed)
            if ($userBadge->getUser() === $this) {
                $userBadge->setUser(null);
            }
        }

        return $this;
    }

    public function addBadge(UserBadge $badge): static
    {
        if (!$this->userBadges->contains($badge)) {
            $this->userBadges->add($badge);
            $badge->setUser($this);
        }

        return $this;
    }

    public function getXp(): int
    {
        return $this->xp;
    }

    public function setXp(int $xp): static
    {
        $this->xp = $xp;
        $this->walletBalance = (float) $xp;

        return $this;
    }

    public function removeBadge(UserBadge $badge): static
    {
        if ($this->userBadges->removeElement($badge)) {
            // set the owning side to null (unless already changed)
            if ($badge->getUser() === $this) {
                $badge->setUser(null);
            }
        }

        return $this;
    }

    /**
     * @return Collection<int, Transaction>
     */
    public function getTransactions(): Collection
    {
        return $this->transactions;
    }

    public function addTransaction(Transaction $transaction): static
    {
        if (!$this->transactions->contains($transaction)) {
            $this->transactions->add($transaction);
            $transaction->setUser($this);
        }

        return $this;
    }

    public function removeTransaction(Transaction $transaction): static
    {
        if ($this->transactions->removeElement($transaction)) {
            // set the owning side to null (unless already changed)
            if ($transaction->getUser() === $this) {
                $transaction->setUser(null);
            }
        }

        return $this;
    }


    /**
     * A visual identifier that represents this user.
     *
     * @see UserInterface
     */
    public function getUserIdentifier(): string
    {
        return (string) $this->email;
    }

    /**
     * @see UserInterface
     */
    public function eraseCredentials(): void
    {
        // If you store any temporary, sensitive data on the user, clear it here
        // $this->plainPassword = null;
    }

    public function getFullName(): ?string
    {
        return $this->fullName;
    }

    public function setFullName(?string $fullName): static
    {
        $this->fullName = $fullName;

        return $this;
    }

    public function getFaceDescriptor(): ?array
    {
        return $this->faceDescriptor;
    }

    public function setFaceDescriptor(?array $faceDescriptor): static
    {
        $this->faceDescriptor = $faceDescriptor;

        return $this;
    }
    #[ORM\Column(length: 10, nullable: true)]
    private ?string $resetOtp = null;

    #[ORM\Column(nullable: true)]
    private ?\DateTime $resetOtpExpiresAt = null;

    /**
     * @var Collection<int, Enrollment>
     */
    #[ORM\OneToMany(targetEntity: Enrollment::class, mappedBy: 'student')]
    private Collection $enrollments;

    /**
     * @var Collection<int, Cours>
     */
    #[ORM\OneToMany(targetEntity: Cours::class, mappedBy: 'author')]
    private Collection $cours;

    /**
     * @var Collection<int, Matiere>
     */
    #[ORM\OneToMany(targetEntity: Matiere::class, mappedBy: 'creator')]
    private Collection $createdMatieres;

    /**
     * @var Collection<int, Resource>
     */
    #[ORM\OneToMany(targetEntity: Resource::class, mappedBy: 'author')]
    private Collection $resources;

    /**
     * @var Collection<int, HelpRequest>
     */
    #[ORM\OneToMany(targetEntity: HelpRequest::class, mappedBy: 'student', orphanRemoval: true)]
    private Collection $helpRequests;

    /**
     * @var Collection<int, Note>
     */
    #[ORM\OneToMany(targetEntity: Note::class, mappedBy: 'user', orphanRemoval: true)]
    private Collection $notes;

    /**
     * @var Collection<int, Notification>
     */
    #[ORM\OneToMany(targetEntity: Notification::class, mappedBy: 'user', orphanRemoval: true)]
    private Collection $notifications;

    /**
     * @var Collection<int, Reminder>
     */
    #[ORM\OneToMany(targetEntity: Reminder::class, mappedBy: 'user', orphanRemoval: true)]
    private Collection $reminders;

    /**
     * @var Collection<int, Session>
     */
    #[ORM\OneToMany(targetEntity: Session::class, mappedBy: 'tutor', orphanRemoval: true)]
    private Collection $sessionsAsTutor;

    /**
     * @var Collection<int, PersonalTask>
     */
    #[ORM\OneToMany(targetEntity: PersonalTask::class, mappedBy: 'user', orphanRemoval: true)]
    private Collection $tasks;

    public function getResetOtp(): ?string
    {
        return $this->resetOtp;
    }

    public function setResetOtp(?string $resetOtp): static
    {
        $this->resetOtp = $resetOtp;

        return $this;
    }

    public function getResetOtpExpiresAt(): ?\DateTime
    {
        return $this->resetOtpExpiresAt;
    }

    public function setResetOtpExpiresAt(?\DateTime $resetOtpExpiresAt): static
    {
        $this->resetOtpExpiresAt = $resetOtpExpiresAt;

        return $this;
    }

    /**
     * @return Collection<int, Enrollment>
     */
    public function getEnrollments(): Collection
    {
        return $this->enrollments;
    }

    public function addEnrollment(Enrollment $enrollment): static
    {
        if (!$this->enrollments->contains($enrollment)) {
            $this->enrollments->add($enrollment);
            $enrollment->setStudent($this);
        }

        return $this;
    }

    public function removeEnrollment(Enrollment $enrollment): static
    {
        if ($this->enrollments->removeElement($enrollment)) {
            // set the owning side to null (unless already changed)
            if ($enrollment->getStudent() === $this) {
                $enrollment->setStudent(null);
            }
        }

        return $this;
    }

    /**
     * @return Collection<int, Cours>
     */
    public function getCours(): Collection
    {
        return $this->cours;
    }

    public function addCour(Cours $cour): static
    {
        if (!$this->cours->contains($cour)) {
            $this->cours->add($cour);
            $cour->setAuthor($this);
        }

        return $this;
    }

    public function removeCour(Cours $cour): static
    {
        if ($this->cours->removeElement($cour)) {
            // set the owning side to null (unless already changed)
            if ($cour->getAuthor() === $this) {
                $cour->setAuthor(null);
            }
        }

        return $this;
    }

    /**
     * @return Collection<int, Matiere>
     */
    public function getCreatedMatieres(): Collection
    {
        return $this->createdMatieres;
    }

    public function addCreatedMatiere(Matiere $matiere): static
    {
        if (!$this->createdMatieres->contains($matiere)) {
            $this->createdMatieres->add($matiere);
            $matiere->setCreator($this);
        }

        return $this;
    }

    public function removeCreatedMatiere(Matiere $matiere): static
    {
        if ($this->createdMatieres->removeElement($matiere)) {
            if ($matiere->getCreator() === $this) {
                $matiere->setCreator(null);
            }
        }

        return $this;
    }

    /**
     * @return Collection<int, Resource>
     */
    public function getResources(): Collection
    {
        return $this->resources;
    }

    public function addResource(Resource $resource): static
    {
        if (!$this->resources->contains($resource)) {
            $this->resources->add($resource);
            $resource->setAuthor($this);
        }

        return $this;
    }

    public function removeResource(Resource $resource): static
    {
        if ($this->resources->removeElement($resource)) {
            if ($resource->getAuthor() === $this) {
                $resource->setAuthor(null);
            }
        }

        return $this;
    }

    /**
     * @return Collection<int, HelpRequest>
     */
    public function getHelpRequests(): Collection
    {
        return $this->helpRequests;
    }

    /**
     * @return Collection<int, Note>
     */
    public function getNotes(): Collection
    {
        return $this->notes;
    }

    /**
     * @return Collection<int, Notification>
     */
    public function getNotifications(): Collection
    {
        return $this->notifications;
    }

    /**
     * @return Collection<int, Reminder>
     */
    public function getReminders(): Collection
    {
        return $this->reminders;
    }

    /**
     * @return Collection<int, Session>
     */
    public function getSessionsAsTutor(): Collection
    {
        return $this->sessionsAsTutor;
    }

    /**
     * @return Collection<int, PersonalTask>
     */
    public function getTasks(): Collection
    {
        return $this->tasks;
    }

    public function getWalletBalance(): float
    {
        return $this->walletBalance;
    }

    public function setWalletBalance(float $walletBalance): static
    {
        $this->walletBalance = $walletBalance;
        $this->xp = (int) $walletBalance;

        return $this;
    }

    /**
     * @return Collection<int, Event>
     */
    public function getEvents(): Collection
    {
        return $this->events;
    }

    public function addEvent(Event $event): static
    {
        if (!$this->events->contains($event)) {
            $this->events->add($event);
            $event->setOrganizer($this);
        }
        return $this;
    }

    public function removeEvent(Event $event): static
    {
        if ($this->events->removeElement($event)) {
            if ($event->getOrganizer() === $this) {
                $event->setOrganizer(null);
            }
        }
        return $this;
    }

    /**
     * @return Collection<int, Reservation>
     */
    public function getReservations(): Collection
    {
        return $this->reservations;
    }

    public function addReservation(Reservation $reservation): static
    {
        if (!$this->reservations->contains($reservation)) {
            $this->reservations->add($reservation);
            $reservation->setUser($this);
        }
        return $this;
    }

    public function removeReservation(Reservation $reservation): static
    {
        if ($this->reservations->removeElement($reservation)) {
            if ($reservation->getUser() === $this) {
                $reservation->setUser(null);
            }
        }
        return $this;
    }

    public function isVerified(): bool
    {
        return $this->isVerified;
    }

    public function setIsVerified(bool $isVerified): static
    {
        $this->isVerified = $isVerified;
        return $this;
    }

    /**
     * @return Collection<int, AiSentimentLog>
     */
    public function getAiSentimentLogs(): Collection
    {
        return $this->aiSentimentLogs;
    }

    public function addAiSentimentLog(AiSentimentLog $aiSentimentLog): static
    {
        if (!$this->aiSentimentLogs->contains($aiSentimentLog)) {
            $this->aiSentimentLogs->add($aiSentimentLog);
            $aiSentimentLog->setUser($this);
        }

        return $this;
    }

    public function removeAiSentimentLog(AiSentimentLog $aiSentimentLog): static
    {
        if ($this->aiSentimentLogs->removeElement($aiSentimentLog)) {
            // set the owning side to null (unless already changed)
            if ($aiSentimentLog->getUser() === $this) {
                $aiSentimentLog->setUser(null);
            }
        }

        return $this;
    }

    /**
     * @return Collection<int, UserChallenge>
     */
    public function getUserChallenges(): Collection
    {
        return $this->userChallenges;
    }

    /**
     * @return Collection<int, UserMatiereStat>
     */
    public function getUserMatiereStats(): Collection
    {
        return $this->userMatiereStats;
    }
}
