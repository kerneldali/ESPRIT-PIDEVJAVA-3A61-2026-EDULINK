<?php

namespace App\Command;

use App\Entity\Badge;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\Console\Attribute\AsCommand;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use Symfony\Component\Console\Style\SymfonyStyle;

#[AsCommand(
    name: 'app:seed-badges',
    description: 'Seeds initial badges for the gamification system',
)]
class SeedBadgesCommand extends Command
{
    private EntityManagerInterface $entityManager;

    public function __construct(EntityManagerInterface $entityManager)
    {
        parent::__construct();
        $this->entityManager = $entityManager;
    }

    protected function execute(InputInterface $input, OutputInterface $output): int
    {
        $io = new SymfonyStyle($input, $output);

        $badges = [
            [
                'name' => 'Apprenti Aventurier',
                'minPoints' => 10,
                'icon' => 'medal'
            ],
            [
                'name' => 'Explorateur Confirmé',
                'minPoints' => 100,
                'icon' => 'award'
            ],
            [
                'name' => 'Maître du Savoir',
                'minPoints' => 500,
                'icon' => 'trophy'
            ],
            [
                'name' => 'Légende Edulink',
                'minPoints' => 1000,
                'icon' => 'sparkles'
            ],
        ];

        foreach ($badges as $data) {
            $existing = $this->entityManager->getRepository(Badge::class)->findOneBy(['name' => $data['name']]);
            if (!$existing) {
                $badge = new Badge();
                $badge->setName($data['name']);
                $badge->setMinPoints($data['minPoints']);
                $badge->setIcon($data['icon']);
                $this->entityManager->persist($badge);
                $io->note(sprintf('Created badge: %s', $data['name']));
            }
        }

        $this->entityManager->flush();
        $io->success('Badges seeded successfully!');

        return Command::SUCCESS;
    }
}
