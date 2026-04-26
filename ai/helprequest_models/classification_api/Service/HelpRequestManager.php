<?php

namespace App\Service;

use App\Entity\HelpRequest;

class HelpRequestManager
{
    public function validate(HelpRequest $helpRequest): bool
    {
        if (empty($helpRequest->getDescription())) {
            throw new \InvalidArgumentException("Description cannot be empty.");
        }

        if ($helpRequest->getBounty() < 0) {
            throw new \InvalidArgumentException("Bounty offered cannot be negative.");
        }

        return true;
    }
}
