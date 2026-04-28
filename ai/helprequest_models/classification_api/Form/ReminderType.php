<?php

namespace App\Form;

use App\Entity\Reminder;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\DateTimeType;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\NotNull;
use Symfony\Component\Validator\Constraints\Length;

class ReminderType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('title', TextType::class, [
                'label' => 'Reminder Title',
                'attr' => [
                    'placeholder' => 'Reminder title...',
                    'class' => 'form-control'
                ],
                'constraints' => [
                    new NotBlank(['message' => 'Reminder title cannot be empty']),
                    new Length(['max' => 255, 'maxMessage' => 'Title cannot exceed {{ limit }} characters']),
                ],
            ])
            ->add('description', TextareaType::class, [
                'label' => 'Description',
                'required' => false,
                'attr' => [
                    'placeholder' => 'Add details...',
                    'class' => 'form-control',
                    'rows' => 3
                ],
                'constraints' => [
                    new Length(['max' => 5000, 'maxMessage' => 'Description cannot exceed {{ limit }} characters']),
                ],
            ])
            ->add('reminderTime', DateTimeType::class, [
                'label' => 'Reminder Date & Time',
                'widget' => 'single_text',
                'required' => true,
                'attr' => [
                    'class' => 'form-control',
                    'type' => 'datetime-local'
                ],
                'constraints' => [
                    new NotNull(['message' => 'Please select a date and time for the reminder']),
                ],
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Reminder::class,
        ]);
    }
}
