<?php

namespace App\Form;

use App\Entity\Challenge;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Form\Extension\Core\Type\CollectionType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\Length;
use Symfony\Component\Validator\Constraints\GreaterThanOrEqual;
use Symfony\Component\Validator\Constraints\NotNull;

class ChallengeType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('title', TextType::class, [
                'label' => 'Titre',
                'attr' => ['class' => 'form-control'],
                'constraints' => [
                    new NotBlank(['message' => 'Le titre ne peut pas être vide']),
                    new Length(['max' => 255, 'maxMessage' => 'Le titre ne doit pas dépasser {{ limit }} caractères']),
                ],
            ])
            ->add('goal', TextareaType::class, [
                'label' => 'Objectif',
                'attr' => ['class' => 'form-control', 'rows' => 3],
                'constraints' => [
                    new NotBlank(['message' => 'L\'objectif ne peut pas être vide']),
                    new Length(['max' => 2000, 'maxMessage' => 'L\'objectif ne doit pas dépasser {{ limit }} caractères']),
                ],
            ])
            ->add('rewardPoints', IntegerType::class, [
                'label' => 'Points de récompense',
                'attr' => ['class' => 'form-control'],
                'constraints' => [
                    new NotNull(['message' => 'Les points de récompense sont obligatoires']),
                    new GreaterThanOrEqual(['value' => 1, 'message' => 'Les points doivent être au minimum {{ value }}']),
                ],
            ])
            ->add('tasks', CollectionType::class, [
                'entry_type' => TaskType::class,
                'entry_options' => ['label' => false],
                'allow_add' => true,
                'allow_delete' => true,
                'by_reference' => false,
                'label' => false,
            ])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Challenge::class,
        ]);
    }
}
