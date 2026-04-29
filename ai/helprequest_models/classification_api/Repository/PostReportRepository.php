<?php

namespace App\Repository;

use App\Entity\PostReport;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

class PostReportRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, PostReport::class);
    }

    public function findPendingReports(): array
    {
        return $this->findBy(['status' => 'PENDING'], ['createdAt' => 'DESC']);
    }
}