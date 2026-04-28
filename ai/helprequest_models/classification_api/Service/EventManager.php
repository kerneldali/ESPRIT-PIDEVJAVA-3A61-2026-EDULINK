<?php

namespace App\Service;

use App\Entity\Event;

class EventManager
{
    public function validate(Event $event): bool
    {
        if ($event->getMaxCapacity() <= 0) {
            throw new \InvalidArgumentException("Max Participants (Capacity) must be greater than 0.");
        }

        if ($event->getDateStart() < new \DateTime()) {
            throw new \InvalidArgumentException("Event Date cannot be in the past.");
        }

        return true;
    }
}
