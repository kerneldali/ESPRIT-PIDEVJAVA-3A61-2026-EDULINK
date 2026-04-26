<?php

namespace App\Service;

use App\Entity\Note;

class NoteManager
{
    public function validate(Note $note): bool
    {
        if (empty($note->getTitle())) {
            throw new \InvalidArgumentException("Note Title is mandatory.");
        }

        if (empty($note->getContent())) {
            throw new \InvalidArgumentException("Note Content is mandatory.");
        }

        return true;
    }
}
