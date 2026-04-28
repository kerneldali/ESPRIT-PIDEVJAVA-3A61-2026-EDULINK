<?php

namespace App\Repository;

use App\Entity\Session;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Session>
 *
 * @method Session|null find($id, $lockMode = null, $lockVersion = null)
 * @method Session|null findOneBy(array $criteria, array $orderBy = null)
 * @method Session[]    findAll()
 * @method Session[]    findBy(array $criteria, array $orderBy = null, $limit = null, $offset = null)
 */
class SessionRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Session::class);
    }

    /**
     * Eager-load session with key relationships in ONE query.
     * Uses 3 JOINs (under threshold of 4) to avoid excessive eager loading warning.
     * Messages are loaded via Doctrine's collection (separate optimized query).
     */
    public function findForChat(int $id): ?Session
    {
        return $this->createQueryBuilder('s')
            ->innerJoin('s.helpRequest', 'hr')->addSelect('hr')
            ->innerJoin('hr.student', 'st')->addSelect('st')
            ->innerJoin('s.tutor', 't')->addSelect('t')
            ->where('s.id = :id')
            ->setParameter('id', $id)
            ->getQuery()
            ->getOneOrNullResult();
    }
}

