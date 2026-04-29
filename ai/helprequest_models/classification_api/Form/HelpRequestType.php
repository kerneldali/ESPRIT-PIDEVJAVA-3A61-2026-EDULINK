<?php

namespace App\Form;

use App\Entity\HelpRequest;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\GreaterThanOrEqual;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\Length;

class HelpRequestType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('title', TextType::class, [
                'label' => 'Title',
                'attr' => [
                    'placeholder' => 'Enter a brief title for your request',
                    'class' => 'form-input'
                ],
                'constraints' => [
                    new NotBlank(['message' => 'Please enter a title']),
                    new Length(['max' => 255, 'maxMessage' => 'Title cannot exceed {{ limit }} characters']),
                ],
            ])
            ->add('description', TextareaType::class, [
                'label' => 'Description',
                'attr' => [
                    'placeholder' => 'Describe your problem in detail...',
                    'rows' => 5,
                    'class' => 'form-input'
                ],
                'constraints' => [
                    new NotBlank(['message' => 'Please provide a description']),
                    new Length(['max' => 5000, 'maxMessage' => 'Description cannot exceed {{ limit }} characters']),
                ],
            ])
            ->add('bounty', IntegerType::class, [
                'label' => 'Bounty (Points)',
                'required' => false,
                'attr' => [
                    'placeholder' => '0',
                    'class' => 'form-input'
                ],
                'constraints' => [
                    new GreaterThanOrEqual(['value' => 0, 'message' => 'Bounty cannot be negative']),
                ],
            ])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => HelpRequest::class,
        ]);
    }
}
