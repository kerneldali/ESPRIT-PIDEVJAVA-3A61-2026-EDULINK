<?php

namespace App\Service;

use App\Entity\Cours;

class CourseManager
{
    public function validate(Cours $cours): bool
    {
        if (empty($cours->getTitle())) {
            throw new \InvalidArgumentException("Course Title cannot be empty.");
        }

        if ($cours->getPricePoints() < 0) {
            throw new \InvalidArgumentException("Price (Points) cannot be negative.");
        }

        return true;
    }
}
