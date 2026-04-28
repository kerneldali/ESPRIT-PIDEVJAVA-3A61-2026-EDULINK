<?php

namespace App\Repository;

use App\Entity\Message;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Message>
 *
 * @method Message|null find($id, $lockMode = null, $lockVersion = null)
 * @method Message|null findOneBy(array $criteria, array $orderBy = null)
 * @method Message[]    findAll()
 * @method Message[]    findBy(array $criteria, array $orderBy = null, $limit = null, $offset = null)
 */
class MessageRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Message::class);
    }

    /**
     * @return Message[] All messages flagged as toxic, newest first
     */
    public function findToxicMessages(): array
    {
        return $this->createQueryBuilder('m')
            ->andWhere('m.isToxic = :toxic')
            ->setParameter('toxic', true)
            ->innerJoin('m.sender', 's')
            ->addSelect('s')
            ->innerJoin('m.session', 'sess')
            ->addSelect('sess')
            ->orderBy('m.timestamp', 'DESC')
            ->getQuery()
            ->getResult();
    }
}
