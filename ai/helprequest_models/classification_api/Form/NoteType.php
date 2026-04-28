<?php

namespace App\Form;

use App\Entity\Category;
use App\Entity\Note;
use App\Repository\CategoryRepository;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\FileType;
use Symfony\Component\Validator\Constraints\File;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\Length;

class NoteType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('title', TextType::class, [
                'label' => 'Note Title',
                'attr' => [
                    'placeholder' => 'Note Title...',
                    'class' => 'form-control'
                ],
                'constraints' => [
                    new NotBlank(['message' => 'Note title cannot be empty']),
                    new Length(['max' => 255, 'maxMessage' => 'Title cannot exceed {{ limit }} characters']),
                ],
            ])
            ->add('content', TextareaType::class, [
                'label' => 'Note Content',
                'attr' => [
                    'placeholder' => 'Write something...',
                    'class' => 'form-control',
                    'rows' => 6
                ],
                'constraints' => [
                    new NotBlank(['message' => 'Note content cannot be empty']),
                    new Length(['max' => 50000, 'maxMessage' => 'Content cannot exceed {{ limit }} characters']),
                ],
            ])
            ->add('attachment', FileType::class, [
                'label' => 'Attachment (PDF, Image)',
                'mapped' => false,
                'required' => false,
                'attr' => [
                    'class' => 'form-control',
                    'accept' => '.pdf,.jpg,.jpeg,.png'
                ],
                'constraints' => [
                    new File([
                        'maxSize' => '5M',
                        'mimeTypes' => ['application/pdf', 'image/jpeg', 'image/png'],
                        'mimeTypesMessage' => 'Please upload a valid file (PDF, JPG, PNG)',
                    ]),
                ],
            ])
            ->add('category', EntityType::class, [
                'class' => Category::class,
                'label' => 'Category',
                'required' => true,
                'placeholder' => 'Choose a category...',
                'choice_label' => 'name',
                'query_builder' => function (CategoryRepository $cr) use ($options) {
                    $qb = $cr->createQueryBuilder('c')->orderBy('c.name', 'ASC');
                    if (!empty($options['user'])) {
                        $qb->andWhere('c.owner = :owner')->setParameter('owner', $options['user']);
                    } else {
                        $qb->andWhere('1 = 0');
                    }
                    return $qb;
                },
                'attr' => [
                    'class' => 'form-control'
                ]
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Note::class,
            'user' => null,
        ]);
    }
}
