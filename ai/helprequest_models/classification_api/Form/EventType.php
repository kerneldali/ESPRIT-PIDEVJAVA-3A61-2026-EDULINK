<?php

namespace App\Form;

use App\Entity\Event;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\DateTimeType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\CheckboxType;
use Symfony\Component\Form\Extension\Core\Type\FileType;
use Symfony\Component\Validator\Constraints\File;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\NotNull;
use Symfony\Component\Validator\Constraints\Length;
use Symfony\Component\Validator\Constraints\GreaterThanOrEqual;

class EventType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('title', TextType::class, [
                'constraints' => [
                    new NotBlank(['message' => 'Event title is required']),
                    new Length(['max' => 255]),
                ],
            ])
            ->add('description', TextareaType::class, [
                'required' => false,
                'constraints' => [
                    new Length(['max' => 5000]),
                ],
            ])
            ->add('dateStart', DateTimeType::class, [
                'widget' => 'single_text',
                'constraints' => [
                    new NotNull(['message' => 'Start date is required']),
                ],
            ])
            ->add('dateEnd', DateTimeType::class, [
                'widget' => 'single_text',
                'constraints' => [
                    new NotNull(['message' => 'End date is required']),
                ],
            ])
            ->add('maxCapacity', IntegerType::class, [
                'required' => false,
                'constraints' => [
                    new GreaterThanOrEqual(['value' => 1, 'message' => 'Capacity must be at least {{ value }}']),
                ],
            ])
            ->add('isOnline', CheckboxType::class, [
                'required' => false,
            ])
            ->add('location', TextType::class, [
                'required' => false,
                'constraints' => [
                    new Length(['max' => 500]),
                ],
            ])
            ->add('image', FileType::class, [
                'label' => 'Image de l\'événement',
                'mapped' => false,
                'required' => false,
                'attr' => ['class' => 'form-input-custom'],
                'constraints' => [
                    new File([
                        'maxSize' => '2M',
                        'mimeTypes' => ['image/jpeg', 'image/png', 'image/webp'],
                        'mimeTypesMessage' => 'Veuillez uploader une image valide (JPG, PNG, WEBP)',
                    ])
                ],
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Event::class,
        ]);
    }
}
