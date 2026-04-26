<?php

namespace App\Repository;

use App\Entity\PostReaction;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

class PostReactionRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, PostReaction::class);
    }

    public function findUserReaction(int $userId, int $postId): ?PostReaction
    {
        return $this->findOneBy(['user' => $userId, 'post' => $postId]);
    }

    public function getReactionCounts(int $postId): array
    {
        $results = $this->createQueryBuilder('r')
            ->select('r.type, COUNT(r.id) as cnt')
            ->where('r.post = :postId')
            ->setParameter('postId', $postId)
            ->groupBy('r.type')
            ->getQuery()
            ->getResult();

        $counts = [];
        foreach ($results as $row) {
            $counts[$row['type']] = (int) $row['cnt'];
        }
        return $counts;
    }
}