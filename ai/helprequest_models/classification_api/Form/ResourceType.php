<?php

namespace App\Form;

use App\Entity\Resource;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\Extension\Core\Type\FileType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\File;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\Length;

class ResourceType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('title', TextType::class, [
                'label' => 'Resource Title',
                'attr' => ['class' => 'form-control', 'placeholder' => 'e.g. Introduction to Python'],
                'constraints' => [
                    new NotBlank(['message' => 'Resource title is required']),
                    new Length(['max' => 255, 'maxMessage' => 'Title cannot exceed {{ limit }} characters']),
                ],
            ])
            ->add('type', ChoiceType::class, [
                'choices' => [
                    'YouTube Video' => 'VIDEO',
                    'File (PDF, Word, etc.)' => 'FILE',
                ],
                'attr' => ['class' => 'form-control'],
                'constraints' => [
                    new NotBlank(['message' => 'Please select a resource type']),
                ],
            ])
            ->add('url', TextType::class, [
                'label' => 'Resource URL / Video ID',
                'required' => false,
                'attr' => ['class' => 'form-control', 'placeholder' => 'https://... or YouTube video ID'],
                'constraints' => [
                    new Length(['max' => 2000, 'maxMessage' => 'URL cannot exceed {{ limit }} characters']),
                ],
            ])
            ->add('file', FileType::class, [
                'label' => 'Upload File (PDF or Word)',
                'mapped' => false,
                'required' => false,
                'constraints' => [
                    new File([
                        'maxSize' => '10M',
                        'mimeTypes' => [
                            'application/pdf',
                            'application/x-pdf',
                            'application/msword',
                            'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
                        ],
                        'mimeTypesMessage' => 'Please upload a valid PDF or Word document',
                    ])
                ],
                'attr' => [
                    'class' => 'form-control',
                    'accept' => '.pdf,.doc,.docx'
                ]
            ])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults(['data_class' => Resource::class]);
    }
}