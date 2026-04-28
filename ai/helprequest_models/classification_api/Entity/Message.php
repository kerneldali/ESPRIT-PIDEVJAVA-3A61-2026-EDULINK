<?php

namespace App\Entity;

use App\Repository\MessageRepository;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;
use Symfony\Component\Validator\Context\ExecutionContextInterface;
use Symfony\Component\HttpFoundation\File\File;
use Vich\UploaderBundle\Mapping\Annotation as Vich;

#[ORM\Entity(repositoryClass: MessageRepository::class)]
#[Vich\Uploadable]
class Message
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\Column(type: Types::TEXT, nullable: true)]
    #[Assert\Length(
        min: 1,
        max: 5000,
        minMessage: 'Message must be at least {{ limit }} character.',
        maxMessage: 'Message cannot exceed {{ limit }} characters.'
    )]
    private ?string $content = null;

    #[ORM\Column]
    private ?\DateTimeImmutable $timestamp = null;

    #[ORM\ManyToOne(inversedBy: 'messages')]
    #[ORM\JoinColumn(nullable: false)]
    private ?Session $session = null;

    #[ORM\ManyToOne]
    #[ORM\JoinColumn(nullable: false)]
    private ?User $sender = null;

    #[ORM\Column]
    private bool $isToxic = false;

    #[Vich\UploadableField(mapping: 'chat_attachments', fileNameProperty: 'attachmentName', size: 'attachmentSize')]
    #[Assert\File(
        maxSize: '5M',
        maxSizeMessage: 'File cannot exceed 5 MB.',
        mimeTypes: [
            'image/jpeg',
            'image/png',
            'image/gif',
            'image/webp',
            'application/pdf',
            'application/msword',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            'text/plain',
            'application/zip',
        ],
        mimeTypesMessage: 'Only images, PDF, Word, TXT and ZIP files are allowed.'
    )]
    private ?File $attachmentFile = null;

    #[ORM\Column(length: 255, nullable: true)]
    private ?string $attachmentName = null;

    #[ORM\Column(nullable: true)]
    private ?int $attachmentSize = null;

    #[ORM\Column(nullable: true)]
    private ?\DateTimeImmutable $updatedAt = null;

    public function __construct()
    {
        $this->timestamp = new \DateTimeImmutable();
    }

    /**
     * Either content or attachment must be provided.
     */
    #[Assert\Callback]
    public function validateContentOrAttachment(ExecutionContextInterface $context): void
    {
        if (empty($this->content) && $this->attachmentFile === null && $this->attachmentName === null) {
            $context->buildViolation('Please provide a message or attach a file.')
                ->atPath('content')
                ->addViolation();
        }
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getContent(): ?string
    {
        return $this->content;
    }

    public function setContent(string $content): static
    {
        $this->content = $content;

        return $this;
    }

    public function getTimestamp(): ?\DateTimeImmutable
    {
        return $this->timestamp;
    }

    public function setTimestamp(\DateTimeImmutable $timestamp): static
    {
        $this->timestamp = $timestamp;

        return $this;
    }

    public function getSession(): ?Session
    {
        return $this->session;
    }

    public function setSession(?Session $session): static
    {
        $this->session = $session;

        return $this;
    }

    public function getSender(): ?User
    {
        return $this->sender;
    }

    public function setSender(?User $sender): static
    {
        $this->sender = $sender;

        return $this;
    }

    public function isIsToxic(): bool
    {
        return $this->isToxic;
    }

    public function setIsToxic(bool $isToxic): static
    {
        $this->isToxic = $isToxic;

        return $this;
    }

    public function getAttachmentFile(): ?File
    {
        return $this->attachmentFile;
    }

    public function setAttachmentFile(?File $attachmentFile = null): void
    {
        $this->attachmentFile = $attachmentFile;
        if (null !== $attachmentFile) {
            $this->updatedAt = new \DateTimeImmutable();
        }
    }

    public function getAttachmentName(): ?string
    {
        return $this->attachmentName;
    }

    public function setAttachmentName(?string $attachmentName): void
    {
        $this->attachmentName = $attachmentName;
    }

    public function getAttachmentSize(): ?int
    {
        return $this->attachmentSize;
    }

    public function setAttachmentSize(?int $attachmentSize): void
    {
        $this->attachmentSize = $attachmentSize;
    }

    public function getUpdatedAt(): ?\DateTimeImmutable
    {
        return $this->updatedAt;
    }
}
