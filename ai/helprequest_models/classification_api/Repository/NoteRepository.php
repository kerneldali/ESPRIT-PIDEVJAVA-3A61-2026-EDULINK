<?php

namespace App\Repository;

use App\Entity\Note;
use App\Entity\User;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Note>
 */
class NoteRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Note::class);
    }

    public function findByUserOrderedByDate(User $user)
    {
        return $this->createQueryBuilder('n')
            ->andWhere('n.user = :user')
            ->setParameter('user', $user)
            ->orderBy('n.createdAt', 'DESC')
            ->getQuery()
            ->getResult();
    }

    public function findByUserWithFilters(User $user, ?string $query = null, ?int $categoryId = null)
    {
        $qb = $this->createQueryBuilder('n')
            ->andWhere('n.user = :user')
            ->setParameter('user', $user);

        if ($categoryId) {
            $qb->andWhere('n.category = :categoryId')
               ->setParameter('categoryId', $categoryId);
        }

        // Fetch all candidates from the DB for this user/category matching base filters
        $candidateNotes = $qb->orderBy('n.createdAt', 'DESC')
            ->getQuery()
            ->getResult();

        if ($query && count($candidateNotes) > 0) {
            // Transform notes to JSON array for python semantic ranking
            $documents = [];
            foreach ($candidateNotes as $note) {
                $documents[] = [
                    'id' => $note->getId(),
                    'text' => $note->getTitle() . ' ' . $note->getContent() . ' ' . ($note->getTag() ?? '')
                ];
            }

            // Execute Python Semantic Ranker
            $pythonPath = 'C:\\Program Files\\Python311\\python.exe';
            if (strtoupper(substr(PHP_OS, 0, 3)) !== 'WIN') {
                $pythonPath = 'python3';
            }

            $env = [
                'PYTHONIOENCODING' => 'utf-8',
                'SystemRoot' => getenv('SystemRoot') ?: 'C:\\Windows',
                'PATH' => getenv('PATH')
            ];

            // Assuming the repo is physically located at the root kernel dir
            $projectDir = realpath(__DIR__ . '/../../');
            
            $process = new \Symfony\Component\Process\Process([
                $pythonPath,
                $projectDir . '/delizar/semantic_ranker.py',
                $query
            ], $projectDir, $env);
            
            // Send the JSON documents via STDIN to avoid Windows command line escaping issues
            $process->setInput(json_encode($documents));
            $process->setTimeout(30);

            try {
                $process->mustRun();
                $output = json_decode($process->getOutput(), true);
                
                if (isset($output['success']) && $output['success']) {
                    // Extract exactly matched Note IDs that scored higher than a threshold 
                    $semanticIds = [];
                    foreach ($output['results'] as $result) {
                        // Let's ensure a minimum semantic match (e.g. > 0.4 cosine sim)
                        if ($result['score'] > 0.4) {
                            $semanticIds[] = $result['id'];
                        }
                    }

                    // ALWAYS fetch standard text matches so we don't regress text searches
                    $fallbackQb = $this->createQueryBuilder('n')
                        ->andWhere('n.user = :user')
                        ->setParameter('user', $user)
                        ->andWhere('n.title LIKE :query OR n.content LIKE :query')
                        ->setParameter('query', '%' . $query . '%');
                    
                    if ($categoryId) {
                        $fallbackQb->andWhere('n.category = :categoryId')
                           ->setParameter('categoryId', $categoryId);
                    }
                    $exactMatches = $fallbackQb->orderBy('n.createdAt', 'DESC')->getQuery()->getResult();
                    
                    $orderedIds = [];
                    foreach ($exactMatches as $em) {
                        $orderedIds[] = $em->getId();
                    }
                    
                    // Add AI semantic matches to the end (excluding duplicates)
                    foreach ($semanticIds as $sid) {
                        if (!in_array($sid, $orderedIds)) {
                            $orderedIds[] = $sid;
                        }
                    }

                    if (empty($orderedIds)) {
                        return []; // No matches at all
                    }

                    // Re-fetch semantic notes in exact ranked order
                    $semanticQb = $this->createQueryBuilder('n')
                        ->andWhere('n.id IN (:ids)')
                        ->setParameter('ids', $orderedIds);
                    
                    $finalNotesUnordered = $semanticQb->getQuery()->getResult();
                    
                    // Restore exact sort order (Exact SQL matches first, then Semantic matches)
                    $finalNotes = [];
                    foreach ($orderedIds as $orderedId) {
                        foreach ($finalNotesUnordered as $fn) {
                            if ($fn->getId() == $orderedId) {
                                $finalNotes[] = $fn;
                                break;
                            }
                        }
                    }
                    
                    return $finalNotes;
                } else {
                    // Log the Python internal error for debugging
                    error_log("Semantic AI Error: " . ($output['error'] ?? 'Unknown JSON error'));
                }
            } catch (\Exception $e) {
                // Log the process failure
                error_log("Semantic AI Exception: " . $e->getMessage());
            }

            // Fallback: If AI fails (either exception or success=false), do standard SQL search
            $fallbackQb = $this->createQueryBuilder('n')
                ->andWhere('n.user = :user')
                ->setParameter('user', $user)
                ->andWhere('n.title LIKE :query OR n.content LIKE :query')
                ->setParameter('query', '%' . $query . '%');
            
            if ($categoryId) {
                $fallbackQb->andWhere('n.category = :categoryId')
                   ->setParameter('categoryId', $categoryId);
            }
            
            return $fallbackQb->orderBy('n.createdAt', 'DESC')->getQuery()->getResult();
        }

        return $candidateNotes;
    }

    // Deprecated tag-related methods removed
}
