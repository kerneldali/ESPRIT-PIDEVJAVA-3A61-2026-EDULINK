<?php

namespace App\Service;

use App\Entity\User;

class UserManager
{
    public function validate(User $user): bool
    {
        if (!filter_var($user->getEmail(), FILTER_VALIDATE_EMAIL)) {
            throw new \InvalidArgumentException("Invalid email format.");
        }

        if (strlen($user->getPassword()) < 8) {
            throw new \InvalidArgumentException("Password must be at least 8 characters long.");
        }

        return true;
    }
}
