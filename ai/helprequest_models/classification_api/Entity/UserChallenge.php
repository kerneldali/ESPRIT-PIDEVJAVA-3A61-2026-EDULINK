<?php

namespace App\Entity;

use App\Repository\UserChallengeRepository;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;
use Symfony\Component\HttpFoundation\File\File;
use Vich\UploaderBundle\Mapping\Annotation as Vich;

#[ORM\Entity(repositoryClass: UserChallengeRepository::class)]
#[Vich\Uploadable]
class UserChallenge
{
    public const STATUS_IN_PROGRESS = 'IN_PROGRESS';
    public const STATUS_PENDING     = 'PENDING';
    public const STATUS_COMPLETED   = 'COMPLETED';

    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[Assert\NotBlank(message: 'La progression est obligatoire.')]
    #[Assert\Regex(
        pattern: '/^\d+\/\d+$/',
        message: 'La progression doit être au format X/Y (ex: 2/3).'
    )]
    #[ORM\Column(length: 50)]
    private string $progress = '0/3';

    #[Assert\NotBlank(message: 'Le statut est obligatoire.')]
    #[Assert\Choice(
        choices: [
            self::STATUS_IN_PROGRESS,
            self::STATUS_PENDING,
            self::STATUS_COMPLETED
        ],
        message: 'Statut invalide.'
    )]
    #[ORM\Column(length: 50)]
    private string $status = self::STATUS_IN_PROGRESS;

    // ✅ VichUploader mapping
    #[Vich\UploadableField(mapping: 'challenge_proofs', fileNameProperty: 'proofFileName')]
    private ?File $proofFile = null;

    #[ORM\Column(length: 255, nullable: true)]
    private ?string $proofFileName = null;

    #[ORM\Column(nullable: true)]
    private ?\DateTimeImmutable $updatedAt = null;


    #[ORM\ManyToOne(inversedBy: 'userChallenges')]
    #[ORM\JoinColumn(nullable: false)]
    private ?User $user = null;

    #[ORM\ManyToOne(inversedBy: 'userChallenges')]
    #[ORM\JoinColumn(nullable: false)]
    private ?Challenge $challenge = null;

    /**
     * @var Collection<int, UserTask>
     */
    #[ORM\OneToMany(mappedBy: 'userChallenge', targetEntity: UserTask::class, cascade: ['persist', 'remove'], orphanRemoval: true)]
    private Collection $userTasks;


    public function __construct()
    {
        $this->userTasks = new ArrayCollection();
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getProgress(): string
    {
        return $this->progress;
    }

    public function setProgress(string $progress): static
    {
        $this->progress = $progress;
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

    public function getProofFileName(): ?string
    {
        return $this->proofFileName;
    }

    public function setProofFileName(?string $proofFileName): static
    {
        $this->proofFileName = $proofFileName;
        return $this;
    }

    public function getProofFile(): ?File
    {
        return $this->proofFile;
    }

    public function setProofFile(?File $proofFile = null): void
    {
        $this->proofFile = $proofFile;

        if (null !== $proofFile) {
            $this->updatedAt = new \DateTimeImmutable();
        }
    }

    public function getUpdatedAt(): ?\DateTimeImmutable
    {
        return $this->updatedAt;
    }


    public function getUser(): ?User
    {
        return $this->user;
    }

    public function setUser(?User $user): static
    {
        $this->user = $user;
        return $this;
    }

    public function getChallenge(): ?Challenge
    {
        return $this->challenge;
    }

    public function setChallenge(?Challenge $challenge): static
    {
        $this->challenge = $challenge;
        return $this;
    }

    /**
     * @return Collection<int, UserTask>
     */
    public function getUserTasks(): Collection
    {
        return $this->userTasks;
    }

    public function addUserTask(UserTask $userTask): static
    {
        if (!$this->userTasks->contains($userTask)) {
            $this->userTasks->add($userTask);
            $userTask->setUserChallenge($this);
        }

        return $this;
    }

    public function removeUserTask(UserTask $userTask): static
    {
        if ($this->userTasks->removeElement($userTask)) {
            // set the owning side to null (unless already changed)
            if ($userTask->getUserChallenge() === $this) {
                $userTask->setUserChallenge(null);
            }
        }

        return $this;
    }

    public function updateProgress(): void
    {
        $total = count($this->userTasks);
        if ($total === 0) {
            $this->progress = '0/0';
            return;
        }

        $completed = 0;
        foreach ($this->userTasks as $userTask) {
            if ($userTask->isCompleted()) {
                $completed++;
            }
        }

        $this->progress = sprintf('%d/%d', $completed, $total);
    }
}
