<?php

namespace App\Entity;

use App\Repository\CommunityPostRepository;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: CommunityPostRepository::class)]
class CommunityPost
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\ManyToOne]
    #[ORM\JoinColumn(nullable: false)]
    private ?User $author = null;

    #[ORM\Column(type: Types::TEXT)]
    #[Assert\NotBlank(message: 'Post content cannot be empty.', normalizer: 'trim')]
    #[Assert\Length(max: 2000, maxMessage: 'Post cannot exceed {{ limit }} characters.')]
    private ?string $content = null;

    #[ORM\Column(length: 30)]
    #[Assert\Choice(choices: ['tip','question','discussion','celebration'], message: 'Choose a valid post type.')]
    private string $type = 'discussion';

    #[ORM\Column]
    private ?\DateTimeImmutable $createdAt = null;

    #[ORM\Column]
    private int $likesCount = 0;

    #[ORM\Column(length: 100, nullable: true)]
    #[Assert\Regex(pattern: '/^[A-Za-z0-9][A-Za-z0-9\-\_\.]{0,99}$/', message: 'Tag must be letters, numbers, hyphens, underscores or dots')]
    private ?string $tag = null;

    public function __construct()
    {
        $this->createdAt = new \DateTimeImmutable();
    }

    public function getId(): ?int { return $this->id; }

    public function getAuthor(): ?User { return $this->author; }
    public function setAuthor(?User $author): static { $this->author = $author; return $this; }

    public function getContent(): ?string { return $this->content; }
    public function setContent(?string $content): static { $this->content = $content; return $this; }

    public function getType(): string { return $this->type; }
    public function setType(string $type): static { $this->type = $type; return $this; }

    public function getCreatedAt(): ?\DateTimeImmutable { return $this->createdAt; }
    public function setCreatedAt(\DateTimeImmutable $createdAt): static { $this->createdAt = $createdAt; return $this; }

    public function getLikesCount(): int { return $this->likesCount; }
    public function setLikesCount(int $likesCount): static { $this->likesCount = $likesCount; return $this; }
    public function incrementLikes(): static { $this->likesCount++; return $this; }

    public function getTag(): ?string { return $this->tag; }
    public function setTag(?string $tag): static { $this->tag = $tag; return $this; }
}
